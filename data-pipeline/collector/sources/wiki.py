"""위키 github-wiki(06 문서 3-3, 5-3, 5-5, 5-7, 6장, 08 문서 4-2).

목록: 레포마다 git ls-remote 로 위키 최신 커밋(HEAD).
  - "Repository not found": 위키 없음(오류 아님). 아는 키가 있으면 wiki_not_found 로 사라짐.
  - 상태(state/github-wiki.json 의 wiki_commits)에 남긴 커밋과 같음: clone 하지 않는다. 그 레포의 아는 키를
    저장된 sha 그대로 목록에 넣어 "바뀌지 않음" 으로 세고 사라짐 판단에서도 빠지게 한다.
  - 다름: 전체 clone(--depth 없이, bare) → git ls-tree -r -z HEAD 로 파일별 sha. .md 만, 바로 가기 제외.
한 건 받기: 같은 clone 에서 git cat-file 로 본문, git log -1 로 마지막 커밋. 레포 하나를 한 번만 clone 하고, 그 레포의
  받을 건을 다 받으면 지운다.
상태: 새 위키 커밋은 그 레포의 받을 건이 모두 성공했을 때만 finish 에서 저장한다(실패한 파일을 다음 실행에서 다시 보도록).
"""

from __future__ import annotations

import logging
import shutil
import subprocess
import tempfile
from datetime import datetime, timezone
from pathlib import Path

from ..config import ORG
from ..keys import repo_of_key, wiki_key
from ..runner import Context, FetchError, Item, Listing, is_changed
from ._repos import RepoSource, missing_repos, target_repos

log = logging.getLogger("collector")

SYMLINK_MODE = "120000"


class GitError(Exception):
    pass


def run_git(args: list[str], env: dict, cwd: str | None = None, timeout: float = 300) -> subprocess.CompletedProcess:
    """git 한 번 실행(바이트 입출력). 시험에서는 이 함수를 바꿔 끼운다."""
    return subprocess.run(["git", *args], cwd=cwd, env=env, capture_output=True, timeout=timeout)


def wiki_url(repo: str) -> str:
    return f"https://github.com/{ORG}/{repo}.wiki.git"


def split_key(key: str) -> tuple[str, str]:
    repo = repo_of_key(key)
    prefix = f"{ORG}/{repo}:wiki/"
    if not repo or not key.startswith(prefix) or len(key) == len(prefix):
        raise ValueError(f"위키 키가 아닙니다: {key}")
    return repo, key[len(prefix):]


def utc(iso_time: str) -> str:
    """git 의 %cI(시간대 붙은 ISO)를 UTC Z 모양으로(코드 마크다운의 GitHub API 시각과 같은 모양)."""
    return datetime.fromisoformat(iso_time).astimezone(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


class WikiSource(RepoSource):
    name = "github-wiki"

    def __init__(self, github=None, git=run_git) -> None:
        super().__init__(github)
        self.git_runner = git
        self._env: dict | None = None
        self._tmp: str | None = None
        self.clones: dict[str, str] = {}        # 레포 → bare clone 폴더
        self.new_heads: dict[str, str] = {}     # clone 한 레포의 위키 커밋(성공하면 상태에 저장)
        self.no_wiki: set[str] = set()          # 위키 저장소가 없는 레포
        self.failed_repos: set[str] = set()     # 받을 건 중 실패가 있던 레포(상태를 고치지 않는다)
        self.wikis_cloned = 0

    # git

    def _git(self, ctx: Context, args: list[str], cwd: str | None = None, timeout: float = 300) -> bytes:
        if self._env is None:
            self._env = self.gh(ctx).git_env()
        r = self.git_runner(args, self._env, cwd, timeout)
        if r.returncode != 0:
            raise GitError(f"git {args[0]} 실패({r.returncode}): {r.stderr.decode('utf-8', 'replace').strip()[:300]}")
        return r.stdout

    def _ls_remote(self, ctx: Context, repo: str) -> str | None:
        """위키 최신 커밋. 위키 저장소가 없으면 None."""
        try:
            out = self._git(ctx, ["ls-remote", wiki_url(repo), "HEAD"], timeout=60)
        except GitError as exc:
            if "not found" in str(exc).lower():
                return None
            raise
        line = out.decode().strip()
        return line.split()[0] if line else None  # 커밋이 하나도 없는 저장소도 위키 없음으로 본다

    def _clone(self, ctx: Context, repo: str) -> str:
        if repo not in self.clones:
            if self._tmp is None:
                self._tmp = tempfile.mkdtemp(prefix="shout-outz-wiki-")
            dest = str(Path(self._tmp) / f"{repo}.wiki.git")
            # 마지막 커밋(git log)을 얻으려면 기록 전체가 있어야 하므로 --depth 를 쓰지 않는다(06 문서 3-3)
            self._git(ctx, ["clone", "--bare", "--quiet", wiki_url(repo), dest])
            self.clones[repo] = dest
            self.wikis_cloned += 1
        return self.clones[repo]

    def _drop_clone(self, repo: str) -> None:
        path = self.clones.pop(repo, None)
        if path:
            shutil.rmtree(path, ignore_errors=True)

    def _drop_all(self) -> None:
        for repo in list(self.clones):
            self._drop_clone(repo)
        if self._tmp:
            shutil.rmtree(self._tmp, ignore_errors=True)
            self._tmp = None

    def _ls_tree(self, ctx: Context, clone: str) -> list[tuple[str, str]]:
        """(경로, blob sha). -z 라 한글 파일 이름도 원래 글자 그대로 온다."""
        out = self._git(ctx, ["ls-tree", "-r", "-z", "HEAD"], cwd=clone)
        files = []
        for rec in out.split(b"\0"):
            if not rec:
                continue
            meta, _, path_b = rec.partition(b"\t")
            mode, kind, sha = meta.decode().split()
            path = path_b.decode("utf-8", errors="replace")
            if kind == "blob" and mode != SYMLINK_MODE and path.lower().endswith(".md"):
                files.append((path, sha))
        return files

    # 목록

    def list_items(self, ctx: Context) -> Listing:
        repos = target_repos(self.gh(ctx), ctx)
        self.listed = {r["name"] for r in repos}
        seen_heads = ctx.state.data.get("wiki_commits", {})
        known: dict[str, list[dict]] = {}
        for e in ctx.index.alive():
            known.setdefault(repo_of_key(e["external_key"]), []).append(e)

        items: list[Item] = []
        complete: set[str] = set()
        for r in repos:
            name = r["name"]
            ctx.begin_group(name)
            try:
                head = self._ls_remote(ctx, name)
                if head is None:
                    self.no_wiki.add(name)
                elif head == seen_heads.get(name):
                    # 위키 안의 어떤 파일도 바뀌지 않았다. 아는 키를 저장된 sha 로 넣어 바뀌지 않음으로 센다
                    items += [Item(e["external_key"], {"sha": e.get("sha")}, group=name) for e in known.get(name, [])]
                else:
                    items += self._changed_wiki(ctx, name)
            except Exception as exc:
                self._drop_clone(name)
                self.repo_failed(name, exc)
                continue
            complete.add(name)
        complete |= missing_repos(ctx, self.listed)
        return Listing(items, complete)

    def _changed_wiki(self, ctx: Context, repo: str) -> list[Item]:
        clone = self._clone(ctx, repo)
        head = self._git(ctx, ["rev-parse", "HEAD"], cwd=clone).decode().strip()
        items = [Item(wiki_key(repo, path), {"sha": sha}, group=repo, data={"repo": repo, "path": path, "sha": sha})
                 for path, sha in self._ls_tree(ctx, clone)]
        self.new_heads[repo] = head
        # 받을 파일이 없으면(이미지만 바뀐 커밋 등) clone 을 바로 지운다. 있으면 한 건 받기에서 쓰고 지운다
        if not any(ctx.index.get(it.key) is None or is_changed(it, ctx.index.get(it.key)) for it in items):
            self._drop_clone(repo)
        return items

    def only_items(self, ctx: Context, values: list[str]) -> list[Item]:
        items = []
        for key in values:
            repo, path = split_key(key)
            items.append(Item(key, None, group=repo, data={"repo": repo, "path": path}))
        return items

    # 한 건 받기

    def fetch_many(self, ctx: Context, items: list[Item]):
        current = None
        try:
            for item in items:
                repo = item.data["repo"]
                if current and repo != current:
                    self._drop_clone(current)  # 목록이 레포 순서라 앞 레포는 다 받았다
                current = repo
                try:
                    record = self.fetch(ctx, item)
                except Exception as exc:
                    self.failed_repos.add(repo)
                    yield item, exc
                    continue
                yield item, record
        finally:
            self._drop_all()

    def fetch(self, ctx: Context, item: Item) -> dict:
        repo, path = item.data["repo"], item.data["path"]
        if not path.lower().endswith(".md"):  # --only 로 받아도 목록과 같이 .md 만(06 문서 D13)
            raise FetchError("not_markdown", f"https://github.com/{ORG}/{repo}/wiki")
        try:
            clone = self._clone(ctx, repo)
            commit = self._git(ctx, ["rev-parse", "HEAD"], cwd=clone).decode().strip()
            sha = item.data.get("sha")
            if sha is None:  # --only
                out = self._git(ctx, ["--literal-pathspecs", "ls-tree", "-z", "HEAD", "--", path], cwd=clone)
                meta = out.split(b"\t", 1)[0].decode().split() if out else []
                if len(meta) != 3 or meta[1] != "blob" or meta[0] == SYMLINK_MODE:
                    raise FetchError("not_in_wiki", f"https://github.com/{ORG}/{repo}/wiki")
                sha = meta[2]
            raw = self._git(ctx, ["cat-file", "blob", sha], cwd=clone)
            log_out = self._git(ctx, ["--literal-pathspecs", "log", "-1", "--format=%cI%x00%an", "HEAD", "--", path],
                                cwd=clone).decode("utf-8", errors="replace").strip()
        except GitError as exc:
            raise FetchError("git_error", wiki_url(repo)) from exc
        except subprocess.TimeoutExpired as exc:
            raise FetchError("timeout", wiki_url(repo)) from exc
        last = None
        if log_out:
            at, _, author = log_out.partition("\0")
            last = {"at": utc(at), "author_name": author}
        try:
            content = raw.decode("utf-8")
        except UnicodeDecodeError:
            content = raw.decode("utf-8", errors="replace")
        return {
            "found_in": {"repo": f"{ORG}/{repo}", "wiki_commit": commit},
            "file": {"path": path, "sha": sha, "size": len(raw), "last_commit": last},
            "content": content,
        }

    def index_fields(self, record: dict) -> dict:
        return {"sha": record["file"]["sha"]}

    # 사라짐

    def confirm_gone(self, ctx: Context, entry: dict) -> str | None:
        repo = self.group_of(entry)
        if repo in self.no_wiki:
            return "wiki_not_found"
        if repo in self.listed:
            return "not_in_wiki"  # 이번 위키 저장소에 그 파일이 없다
        return self.repo_gone_reason(ctx, repo)

    def finish(self, ctx: Context) -> None:
        self._drop_all()
        if not ctx.only:
            heads = ctx.state.data.setdefault("wiki_commits", {})
            for repo, head in self.new_heads.items():
                if repo not in self.failed_repos:
                    heads[repo] = head
            for repo in self.no_wiki:
                heads.pop(repo, None)
            ctx.state.save()
        self.report_extra(ctx, wikis_cloned=self.wikis_cloned, wikis_missing=len(self.no_wiki))
