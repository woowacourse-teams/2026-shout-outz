"""코드 마크다운 github-teamdoc. 가짜 GitHub 클라이언트로 네트워크 없이 돌린다."""

import hashlib
from datetime import datetime, timedelta, timezone

from collector.config import Settings
from collector.github import GitHubError
from collector.index import Index
from collector.keys import raw_path, teamdoc_key
from collector.runner import collect
from collector.sources.teamdoc import TeamdocSource
from collector.store import LocalStore

T0 = datetime(2026, 11, 5, 3, 0, tzinfo=timezone.utc)
REPO = "2026-Chalkak"


class FakeGitHub:
    def __init__(self):
        self.repos = [{"name": REPO, "default_branch": "main", "has_discussions": False}]
        self.commit = "c1"
        self.tree: dict[str, tuple[str, str]] = {}  # 경로 → (sha, mode), 모두 blob
        self.extra_entries: list[dict] = []
        self.truncated = False
        self.tree_error = False
        self.files: dict[str, bytes] = {}
        self.exists: dict[str, dict | None] = {}  # GET /repos/{org}/{repo}
        self.calls: list[str] = []

    def org_repos(self):
        return list(self.repos)

    def rest_json(self, path, params=None, ok404=False):
        self.calls.append(path)
        org_repo = f"/repos/woowacourse-teams/{REPO}"
        if path == f"{org_repo}/branches/main":
            return {"commit": {"sha": self.commit}}
        if path == f"{org_repo}/git/trees/{self.commit}" and not self.tree_error:
            assert params == {"recursive": "1"}
            entries = [{"path": p, "mode": m, "type": "blob", "sha": s, "size": len(self.files.get(p, b""))}
                       for p, (s, m) in self.tree.items()]
            return {"sha": "t", "tree": entries + self.extra_entries, "truncated": self.truncated}
        if path == f"{org_repo}/commits":
            assert params["sha"] == self.commit and params["per_page"] == 1
            return [{"commit": {"committer": {"date": "2026-10-29T04:54:18Z"}, "author": {"name": "smiinii"}},
                     "author": {"login": "smiinii"} if params["path"] == "README.md" else None}]
        if path.startswith("/repos/woowacourse-teams/") and path.count("/") == 3:
            repo = path.rsplit("/", 1)[1]
            if repo in self.exists:
                return self.exists[repo]
        raise GitHubError(404, path)

    def raw(self, repo, commit, path):
        self.calls.append(f"raw:{commit}:{path}")
        assert commit == self.commit  # 브랜치가 아니라 커밋에 고정
        return self.files[path]


def blob(gh, path, content: bytes, mode="100644"):
    gh.files[path] = content
    gh.tree[path] = (hashlib.sha1(content).hexdigest(), mode)


_DAY = [0]


def run(tmp_path, gh, **kw):
    _DAY[0] += 1
    box = [T0 + timedelta(days=_DAY[0])]

    def clock():
        box[0] += timedelta(seconds=1)
        return box[0]
    store = LocalStore(str(tmp_path))
    rep = collect(Settings(store_url=str(tmp_path)), ["github-teamdoc"], store=store,
                  make_source=lambda n: TeamdocSource(gh), clock=clock, **kw)
    return store, rep, rep.sources["github-teamdoc"]


def counts(sr):
    return {k: v for k, v in sr.counts.items() if v}


def test_first_run_record_and_filters(tmp_path):
    gh = FakeGitHub()
    blob(gh, "README.md", "# 찰칵".encode())
    blob(gh, "docs/Guide.MD", b"guide")
    blob(gh, "notes.txt", b"not md")
    blob(gh, "a.markdown", b"not md either")
    blob(gh, "android/CLAUDE.md", b"AGENTS.md", mode="120000")  # 바로 가기
    gh.extra_entries = [{"path": "docs", "mode": "040000", "type": "tree", "sha": "x"},
                        {"path": "sub.md", "mode": "160000", "type": "commit", "sha": "y"}]
    store, rep, sr = run(tmp_path, gh)
    assert counts(sr) == {"new": 2}
    assert sr.extra["repos_checked"] == 1 and sr.extra["trees_truncated"] == []
    rec = store.read_json(raw_path("github-teamdoc", teamdoc_key(REPO, "README.md")))
    assert rec["found_in"] == {"repo": f"woowacourse-teams/{REPO}", "default_branch": "main", "commit": "c1"}
    assert rec["file"]["path"] == "README.md" and rec["file"]["sha"] == gh.tree["README.md"][0]
    assert rec["file"]["size"] == len("# 찰칵".encode())
    assert rec["file"]["last_commit"] == {"at": "2026-10-29T04:54:18Z", "author_name": "smiinii", "author_login": "smiinii"}
    assert rec["content"] == "# 찰칵"
    guide = store.read_json(raw_path("github-teamdoc", teamdoc_key(REPO, "docs/Guide.MD")))
    assert guide["file"]["last_commit"]["author_login"] is None
    idx = Index(store, "github-teamdoc").load()
    assert idx.get(teamdoc_key(REPO, "README.md"))["sha"] == gh.tree["README.md"][0]
    assert idx.get(teamdoc_key(REPO, "android/CLAUDE.md")) is None


def test_unchanged_changed_new_and_gone(tmp_path):
    gh = FakeGitHub()
    blob(gh, "README.md", b"v1")
    blob(gh, "docs/a.md", b"a")
    blob(gh, "docs/b.md", b"b")
    run(tmp_path, gh)

    gh.calls.clear()
    gh.commit = "c2"  # 코드만 바뀐 커밋
    store, rep, sr = run(tmp_path, gh)
    assert counts(sr) == {"unchanged": 3}
    assert not [c for c in gh.calls if c.startswith("raw:")]

    gh.commit = "c3"
    blob(gh, "README.md", b"v2")
    blob(gh, "docs/c.md", b"c")
    del gh.tree["docs/b.md"]
    store, rep, sr = run(tmp_path, gh)
    assert counts(sr) == {"new": 1, "changed": 1, "unchanged": 1, "gone": 1}
    assert sr.gone_keys == [teamdoc_key(REPO, "docs/b.md")]
    idx = Index(store, "github-teamdoc").load()
    assert idx.get(teamdoc_key(REPO, "docs/b.md"))["gone"]["reason"] == "not_in_tree"
    rec = store.read_json(raw_path("github-teamdoc", teamdoc_key(REPO, "README.md")))
    assert rec["version"] == 2 and rec["content"] == "v2" and rec["found_in"]["commit"] == "c3"


def test_truncated_tree_skips_gone(tmp_path):
    gh = FakeGitHub()
    blob(gh, "a.md", b"a")
    blob(gh, "b.md", b"b")
    run(tmp_path, gh)
    del gh.tree["b.md"]
    blob(gh, "a.md", b"a2")
    gh.truncated = True
    store, rep, sr = run(tmp_path, gh)
    assert counts(sr) == {"changed": 1}  # 바뀐 문서는 처리하고 사라짐은 판단하지 않는다
    assert sr.extra["trees_truncated"] == [REPO]


def test_listing_failure_skips_gone(tmp_path):
    gh = FakeGitHub()
    blob(gh, "a.md", b"a")
    run(tmp_path, gh)
    gh.tree_error = True  # 트리 요청이 실패
    gh.tree.clear()
    store, rep, sr = run(tmp_path, gh)
    assert counts(sr) == {}
    assert sr.extra["repos_failed"][0]["repo"] == REPO


def test_repo_removed_from_org(tmp_path):
    gh = FakeGitHub()
    blob(gh, "a.md", b"a")
    blob(gh, "b.md", b"b")
    run(tmp_path, gh)
    gh.repos = []
    gh.exists[REPO] = {"name": REPO, "owner": {"login": "woowacourse-teams"}}  # 아직 있음: 사라짐 아님
    store, rep, sr = run(tmp_path, gh)
    assert counts(sr) == {}
    gh.exists[REPO] = None  # 404
    store, rep, sr = run(tmp_path, gh)
    assert counts(sr) == {"gone": 2}
    idx = Index(store, "github-teamdoc").load()
    assert {e["gone"]["reason"] for e in idx.entries.values()} == {"repo_not_found"}
    assert gh.calls.count(f"/repos/woowacourse-teams/{REPO}") == 2  # 레포마다 한 번(실행 두 번)


def test_repo_renamed_is_gone(tmp_path):
    gh = FakeGitHub()
    blob(gh, "a.md", b"a")
    run(tmp_path, gh)
    gh.repos = []
    gh.exists[REPO] = {"name": "2026-Chalkak-new", "owner": {"login": "woowacourse-teams"}}
    store, rep, sr = run(tmp_path, gh)
    assert sr.gone_keys == [teamdoc_key(REPO, "a.md")]
    gone = Index(store, "github-teamdoc").load().get(teamdoc_key(REPO, "a.md"))["gone"]
    assert gone["reason"] == "repo_renamed" and gone["renamed_to"] == "2026-Chalkak-new"  # 새 이름도 남긴다(R18-8)


def test_repos_narrowing_does_not_touch_other_repos(tmp_path):
    gh = FakeGitHub()
    blob(gh, "a.md", b"a")
    run(tmp_path, gh)
    gh.exists[REPO] = None
    store, rep, sr = run(tmp_path, gh, repos=["2026-other"])  # 다른 레포로 좁히면 이 레포 키는 건드리지 않는다
    assert counts(sr) == {} and sr.extra["repos_checked"] == 0


def test_not_utf8_content_is_replaced(tmp_path):
    gh = FakeGitHub()
    blob(gh, "a.md", b"ok \xff\xfe")
    store, rep, sr = run(tmp_path, gh)
    rec = store.read_json(raw_path("github-teamdoc", teamdoc_key(REPO, "a.md")))
    assert rec["content"].startswith("ok ") and "�" in rec["content"]


def test_raw_failure_is_counted(tmp_path):
    gh = FakeGitHub()
    blob(gh, "a.md", b"a")
    gh.tree["missing.md"] = ("sha-m", "100644")  # raw 에 없음 → KeyError 가 아니라 실패로 남아야
    def raw(repo, commit, path):
        if path == "missing.md":
            raise GitHubError(404, "raw")
        return gh.files[path]
    gh.raw = raw
    store, rep, sr = run(tmp_path, gh)
    assert counts(sr) == {"new": 1, "failed": 1}
    assert sr.failures[0]["reason"] == "http_404" and "raw.githubusercontent.com" in sr.failures[0]["url"]


class BranchGone(FakeGitHub):
    """기본 브랜치 조회가 404. exists 에 레포 정보를 두면 다시 물을 때 그 값을 준다."""

    def rest_json(self, path, params=None, ok404=False):
        if path == f"/repos/woowacourse-teams/{REPO}/branches/main":
            self.calls.append(path)
            return None
        return super().rest_json(path, params, ok404)


def test_default_branch_changed_mid_run_is_repo_failure_not_gone(tmp_path):
    gh = FakeGitHub()
    blob(gh, "README.md", b"v1")
    run(tmp_path, gh)
    gh2 = BranchGone()
    gh2.exists[REPO] = {"name": REPO, "owner": {"login": "woowacourse-teams"}, "default_branch": "develop"}
    store, rep, sr = run(tmp_path, gh2)
    assert counts(sr) == {} and sr.extra["repos_failed"][0]["repo"] == REPO
    assert "기본 브랜치가 바뀜" in sr.extra["repos_failed"][0]["reason"]
    assert Index(store, "github-teamdoc").load().get(teamdoc_key(REPO, "README.md"))["gone"] is None


def test_empty_repo_has_no_documents(tmp_path):
    gh = BranchGone()
    gh.exists[REPO] = {"name": REPO, "owner": {"login": "woowacourse-teams"}, "default_branch": "main"}
    store, rep, sr = run(tmp_path, gh)
    assert counts(sr) == {} and sr.extra["repos_failed"] == [] and rep.status == "success"


def test_only_rejects_non_markdown(tmp_path):
    gh = FakeGitHub()
    store, rep, sr = run(tmp_path, gh, only=[teamdoc_key(REPO, "logo.png")])
    assert sr.failure_reasons == {"not_markdown": 1}
    assert not [c for c in gh.calls if c.startswith("raw:")]
