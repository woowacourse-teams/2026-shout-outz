"""코드 마크다운 github-teamdoc(06 문서 3-2, 5-4, 5-7, 6장).

목록: 레포마다 기본 브랜치 → 커밋 → 트리 1번(recursive). .md 파일(확장자 대소문자 무시)만, 바로 가기(mode 120000)는 뺀다.
바뀜 표시: 파일 blob sha.
한 건 받기: 트리를 받은 커밋에 고정한 raw 본문 + 그 파일의 마지막 커밋(REST 1번).
사라짐: 트리가 잘리지 않은 레포에서 경로가 없으면 not_in_tree. 레포가 없어지면 repo_not_found.
"""

from __future__ import annotations

from urllib.parse import quote

from ..config import ORG
from ..keys import repo_of_key, teamdoc_key
from ..runner import Context, FetchError, Item, Listing
from ._repos import RepoSource, missing_repos, target_repos, to_fetch_error

SYMLINK_MODE = "120000"


def is_markdown(entry: dict) -> bool:
    return entry.get("type") == "blob" and entry.get("mode") != SYMLINK_MODE and entry["path"].lower().endswith(".md")


def decode(raw: bytes) -> str:
    try:
        return raw.decode("utf-8")
    except UnicodeDecodeError:
        return raw.decode("utf-8", errors="replace")


def split_key(key: str) -> tuple[str, str]:
    repo = repo_of_key(key)
    prefix = f"{ORG}/{repo}:"
    if not repo or not key.startswith(prefix) or len(key) == len(prefix):
        raise ValueError(f"코드 마크다운 키가 아닙니다: {key}")
    return repo, key[len(prefix):]


class TeamdocSource(RepoSource):
    name = "github-teamdoc"

    def __init__(self, github=None) -> None:
        super().__init__(github)
        self.trees_truncated: list[str] = []
        self._heads: dict[str, tuple[str, str] | None] = {}  # --only 용 레포별 (기본 브랜치, 커밋)

    # 목록

    def list_items(self, ctx: Context) -> Listing:
        gh = self.gh(ctx)
        repos = target_repos(gh, ctx)
        self.listed = {r["name"] for r in repos}
        items: list[Item] = []
        complete: set[str] = set()
        for r in repos:
            name = r["name"]
            try:
                got = self._repo_items(gh, name, r.get("default_branch"))
            except Exception as exc:
                self.repo_failed(name, exc)
                continue
            if got is None:  # 빈 레포(브랜치 없음): 파일이 하나도 없는 것으로 본다
                complete.add(name)
                continue
            repo_items, truncated = got
            items += repo_items
            if truncated:
                self.trees_truncated.append(name)  # 새 문서, 바뀐 문서만 처리하고 사라짐 판단은 건너뛴다(06 문서 7장)
            else:
                complete.add(name)
        complete |= missing_repos(ctx, self.listed)
        return Listing(items, complete)

    def _head(self, gh, repo: str, branch: str | None) -> str | None:
        if not branch:
            return None
        b = gh.rest_json(f"/repos/{ORG}/{repo}/branches/{quote(branch)}", ok404=True)
        return None if b is None else b["commit"]["sha"]

    def _repo_items(self, gh, repo: str, branch: str | None):
        commit = self._head(gh, repo, branch)
        if commit is None:
            return None
        tree = gh.rest_json(f"/repos/{ORG}/{repo}/git/trees/{commit}", {"recursive": "1"})
        items = []
        for e in tree.get("tree", []):
            if not is_markdown(e):
                continue
            data = {"repo": repo, "branch": branch, "commit": commit, "path": e["path"], "sha": e["sha"], "size": e.get("size")}
            items.append(Item(teamdoc_key(repo, e["path"]), {"sha": e["sha"]}, group=repo, data=data))
        return items, bool(tree.get("truncated"))

    def only_items(self, ctx: Context, values: list[str]) -> list[Item]:
        items = []
        for key in values:
            repo, path = split_key(key)
            items.append(Item(key, None, group=repo, data={"repo": repo, "path": path}))
        return items

    # 한 건 받기

    def fetch(self, ctx: Context, item: Item) -> dict:
        gh = self.gh(ctx)
        d = item.data
        if "commit" not in d:  # --only: 목록 없이 받으므로 기본 브랜치 커밋과 파일 sha 를 여기서 알아낸다
            d = self._resolve(gh, d["repo"], d["path"])
        repo, commit, path = d["repo"], d["commit"], d["path"]
        raw_url = f"https://raw.githubusercontent.com/{ORG}/{repo}/{commit}/{quote(path)}"
        try:
            raw = gh.raw(repo, commit, path)
        except Exception as exc:
            raise to_fetch_error(exc, raw_url) from exc
        try:
            commits = gh.rest_json(f"/repos/{ORG}/{repo}/commits", {"sha": commit, "path": path, "per_page": 1})
        except Exception as exc:
            raise to_fetch_error(exc, f"https://api.github.com/repos/{ORG}/{repo}/commits?path={quote(path)}") from exc
        return {
            "found_in": {"repo": f"{ORG}/{repo}", "default_branch": d["branch"], "commit": commit},
            "file": {"path": path, "sha": d["sha"], "size": d["size"] if d.get("size") is not None else len(raw),
                     "last_commit": last_commit(commits)},
            "content": decode(raw),
        }

    def _resolve(self, gh, repo: str, path: str) -> dict:
        try:
            if repo not in self._heads:
                info = gh.rest_json(f"/repos/{ORG}/{repo}", ok404=True)
                branch = info and info.get("default_branch")
                commit = self._head(gh, repo, branch) if info else None
                self._heads[repo] = (branch, commit) if commit else None
            head = self._heads[repo]
            if head is None:
                raise FetchError("repo_not_found", f"https://github.com/{ORG}/{repo}")
            branch, commit = head
            meta = gh.rest_json(f"/repos/{ORG}/{repo}/contents/{quote(path)}", {"ref": commit}, ok404=True)
        except Exception as exc:
            raise to_fetch_error(exc) from exc
        if not isinstance(meta, dict) or meta.get("type") != "file":  # 없음, 폴더, 바로 가기
            raise FetchError("not_in_tree", f"https://github.com/{ORG}/{repo}/blob/{commit}/{quote(path)}")
        return {"repo": repo, "branch": branch, "commit": commit, "path": path, "sha": meta["sha"], "size": meta.get("size")}

    def index_fields(self, record: dict) -> dict:
        return {"sha": record["file"]["sha"]}

    # 사라짐

    def confirm_gone(self, ctx: Context, entry: dict) -> str | None:
        repo = self.group_of(entry)
        if repo in self.listed:
            return "not_in_tree"  # 잘리지 않은 전체 트리에 경로가 없다(묶음이 complete 일 때만 불린다)
        return self.repo_gone_reason(ctx, repo)

    def finish(self, ctx: Context) -> None:
        self.report_extra(ctx, trees_truncated=self.trees_truncated)


def last_commit(commits) -> dict | None:
    """커밋 API 응답에서 시각(committer date), 작성자 이름(author name), GitHub 아이디(author.login 또는 null)."""
    if not commits:
        return None
    c = commits[0]
    return {"at": c["commit"]["committer"]["date"], "author_name": c["commit"]["author"]["name"],
            "author_login": (c.get("author") or {}).get("login")}
