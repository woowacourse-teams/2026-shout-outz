"""위키 github-wiki. 위키 주소를 로컬 git 저장소로 바꿔 끼운 git 으로 네트워크 없이 돌린다."""

import os
import shutil
import subprocess
from datetime import datetime, timedelta, timezone
from pathlib import Path

from collector.config import Settings
from collector.index import Index
from collector.keys import raw_path, wiki_key
from collector.runner import collect
from collector.sources import wiki as wiki_mod
from collector.sources.wiki import WikiSource
from collector.store import LocalStore

T0 = datetime(2026, 11, 5, 3, 0, tzinfo=timezone.utc)
REPO = "2026-Harudle"
GIT_ENV = {"GIT_AUTHOR_NAME": "JeongYeonJun", "GIT_AUTHOR_EMAIL": "a@example.com",
           "GIT_COMMITTER_NAME": "github", "GIT_COMMITTER_EMAIL": "c@example.com",
           "GIT_CONFIG_GLOBAL": os.devnull, "GIT_CONFIG_NOSYSTEM": "1"}


class FakeGitHub:
    def __init__(self):
        self.repos = [{"name": REPO, "default_branch": "main"}]
        self.exists = {}

    def org_repos(self):
        return list(self.repos)

    def rest_json(self, path, params=None, ok404=False):
        return self.exists.get(path.rsplit("/", 1)[1])

    def git_env(self):
        return dict(os.environ, **GIT_ENV, GIT_CONFIG_COUNT="1", GIT_CONFIG_KEY_0="core.quotePath",
                    GIT_CONFIG_VALUE_0="false")


class FakeGit:
    """위키 주소를 로컬 폴더로 바꾸고 나머지는 진짜 git 을 부른다. 폴더가 없으면 GitHub 의 "Repository not found"."""

    def __init__(self, wikis: Path):
        self.wikis = wikis
        self.calls: list[list[str]] = []
        self.fail_cat_file = False
        self.clone_dirs: list[str] = []

    def __call__(self, args, env, cwd=None, timeout=300):
        self.calls.append(args)
        args = list(args)
        for i, a in enumerate(args):
            if a.startswith("https://github.com/woowacourse-teams/"):
                local = self.wikis / a.rsplit("/", 1)[1]
                if not local.exists():
                    return subprocess.CompletedProcess(args, 128, b"", b"remote: Repository not found.\nfatal: repository not found\n")
                args[i] = str(local)
        if args[0] == "clone":
            self.clone_dirs.append(args[-1])
        if self.fail_cat_file and args[0] == "cat-file":
            return subprocess.CompletedProcess(args, 1, b"", b"boom")
        return wiki_mod.run_git(args, env, cwd, timeout)

    def count(self, cmd):
        return sum(1 for a in self.calls if a[0] == cmd)


class Wiki:
    """로컬 위키 저장소(GitHub 위키 대신)."""

    def __init__(self, root: Path):
        self.dir = root / f"{REPO}.wiki.git"
        self.dir.mkdir(parents=True)
        self.git("init", "-q", "-b", "master")
        self.n = 0

    def git(self, *args):
        subprocess.run(["git", *args], cwd=self.dir, check=True, capture_output=True, env=dict(os.environ, **GIT_ENV))

    def commit(self, files: dict[str, bytes | None], when: str):
        for path, content in files.items():
            p = self.dir / path
            if content is None:
                p.unlink()
            else:
                p.write_bytes(content)
        self.git("add", "-A")
        env = dict(os.environ, **GIT_ENV, GIT_AUTHOR_DATE=when, GIT_COMMITTER_DATE=when)
        subprocess.run(["git", "commit", "-q", "-m", "c"], cwd=self.dir, check=True, capture_output=True, env=env)

    def head(self):
        return subprocess.run(["git", "rev-parse", "HEAD"], cwd=self.dir, capture_output=True, text=True).stdout.strip()


_DAY = [0]


def run(tmp_path, gh, git, **kw):
    _DAY[0] += 1
    box = [T0 + timedelta(days=_DAY[0])]

    def clock():
        box[0] += timedelta(seconds=1)
        return box[0]
    store = LocalStore(str(tmp_path / "store"))
    rep = collect(Settings(store_url=str(tmp_path / "store")), ["github-wiki"], store=store,
                  make_source=lambda n: WikiSource(gh, git=git), clock=clock, **kw)
    return store, rep, rep.sources["github-wiki"]


def counts(sr):
    return {k: v for k, v in sr.counts.items() if v}


def make_wiki(tmp_path):
    wiki = Wiki(tmp_path / "wikis")
    wiki.commit({"Home.md": b"# home", "회의록 정리.md": "회의 내용".encode(),
                 "logo.png": b"\x89PNG"}, "2026-10-29T12:39:35+09:00")
    return wiki, FakeGitHub(), FakeGit(tmp_path / "wikis")


def test_first_run_korean_key_record_and_state(tmp_path):
    wiki, gh, git = make_wiki(tmp_path)
    store, rep, sr = run(tmp_path, gh, git)
    assert counts(sr) == {"new": 2}
    key = wiki_key(REPO, "회의록 정리.md")
    assert key == "woowacourse-teams/2026-Harudle:wiki/회의록 정리.md"
    rec = store.read_json(raw_path("github-wiki", key))
    assert rec["found_in"] == {"repo": f"woowacourse-teams/{REPO}", "wiki_commit": wiki.head()}
    assert rec["file"]["path"] == "회의록 정리.md" and rec["content"] == "회의 내용"
    assert rec["file"]["size"] == len("회의 내용".encode())
    assert rec["file"]["last_commit"] == {"at": "2026-10-29T03:39:35Z", "author_name": "JeongYeonJun"}
    sha = subprocess.run(["git", "rev-parse", "HEAD:회의록 정리.md"], cwd=wiki.dir, capture_output=True, text=True).stdout.strip()
    assert rec["file"]["sha"] == sha
    assert Index(store, "github-wiki").load().get(key)["sha"] == sha
    assert store.read_json("state/github-wiki.json") == {"wiki_commits": {REPO: wiki.head()}}
    assert git.count("clone") == 1 and sr.extra["wikis_cloned"] == 1
    assert all(not Path(d).exists() for d in git.clone_dirs)  # 임시 clone 은 지운다


def test_unchanged_head_does_not_clone(tmp_path):
    wiki, gh, git = make_wiki(tmp_path)
    run(tmp_path, gh, git)
    git.calls.clear()
    store, rep, sr = run(tmp_path, gh, git)
    assert counts(sr) == {"unchanged": 2}
    assert git.count("clone") == 0 and git.count("ls-remote") == 1


def test_changed_file_and_image_only_commit(tmp_path):
    wiki, gh, git = make_wiki(tmp_path)
    run(tmp_path, gh, git)
    wiki.commit({"logo.png": b"\x89PNG2"}, "2026-10-30T00:00:00Z")  # .md 는 그대로인 커밋
    git.calls.clear()
    store, rep, sr = run(tmp_path, gh, git)
    assert counts(sr) == {"unchanged": 2} and git.count("clone") == 1 and git.count("cat-file") == 0
    assert store.read_json("state/github-wiki.json")["wiki_commits"][REPO] == wiki.head()

    wiki.commit({"Home.md": b"# home v2", "new.MD": b"new"}, "2026-10-31T00:00:00Z")
    store, rep, sr = run(tmp_path, gh, git)
    assert counts(sr) == {"new": 1, "changed": 1, "unchanged": 1}
    rec = store.read_json(raw_path("github-wiki", wiki_key(REPO, "Home.md")))
    assert rec["version"] == 2 and rec["content"] == "# home v2" and rec["file"]["last_commit"]["at"] == "2026-10-31T00:00:00Z"


def test_deleted_file_is_gone(tmp_path):
    wiki, gh, git = make_wiki(tmp_path)
    run(tmp_path, gh, git)
    wiki.commit({"Home.md": None}, "2026-10-30T00:00:00Z")
    store, rep, sr = run(tmp_path, gh, git)
    assert counts(sr) == {"unchanged": 1, "gone": 1}
    assert Index(store, "github-wiki").load().get(wiki_key(REPO, "Home.md"))["gone"]["reason"] == "not_in_wiki"


def test_wiki_removed_marks_all_gone(tmp_path):
    wiki, gh, git = make_wiki(tmp_path)
    run(tmp_path, gh, git)
    shutil.rmtree(wiki.dir)
    store, rep, sr = run(tmp_path, gh, git)
    assert counts(sr) == {"gone": 2} and sr.errors == []
    assert {e["gone"]["reason"] for e in Index(store, "github-wiki").load().entries.values()} == {"wiki_not_found"}
    assert store.read_json("state/github-wiki.json") == {"wiki_commits": {}}


def test_no_wiki_is_not_an_error(tmp_path):
    gh, git = FakeGitHub(), FakeGit(tmp_path / "wikis")
    store, rep, sr = run(tmp_path, gh, git)
    assert counts(sr) == {} and sr.errors == [] and sr.extra["wikis_missing"] == 1 and rep.status == "success"


def test_fetch_failure_keeps_old_head(tmp_path):
    wiki, gh, git = make_wiki(tmp_path)
    git.fail_cat_file = True
    store, rep, sr = run(tmp_path, gh, git)
    assert counts(sr) == {"failed": 2} and sr.failure_reasons == {"git_error": 2}
    assert store.read_json("state/github-wiki.json") == {"wiki_commits": {}}  # 다음 실행에서 다시 clone 하도록
    git.fail_cat_file = False
    git.calls.clear()
    store, rep, sr = run(tmp_path, gh, git)
    assert counts(sr) == {"new": 2} and git.count("clone") == 1


def test_repo_removed_from_org(tmp_path):
    wiki, gh, git = make_wiki(tmp_path)
    run(tmp_path, gh, git)
    gh.repos = []
    store, rep, sr = run(tmp_path, gh, git)  # exists 에 없음 → 404
    assert counts(sr) == {"gone": 2}
    assert {e["gone"]["reason"] for e in Index(store, "github-wiki").load().entries.values()} == {"repo_not_found"}


def test_only_fetches_one_file(tmp_path):
    wiki, gh, git = make_wiki(tmp_path)
    store, rep, sr = run(tmp_path, gh, git, only=[wiki_key(REPO, "Home.md"), wiki_key(REPO, "없는.md"),
                                                  wiki_key(REPO, "images/logo.png")])
    assert counts(sr) == {"new": 1, "failed": 2}
    assert sr.failure_reasons == {"not_in_wiki": 1, "not_markdown": 1}
    assert store.read_json("state/github-wiki.json") is None  # --only 는 상태를 고치지 않는다
