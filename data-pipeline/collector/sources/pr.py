"""PR 수집(github-pr, 07 문서).

    1. 대상 레포: 조직 레포 목록(--repos 면 그 레포만)
    2. 레포마다 머지된 PR 의 번호와 updatedAt 만 100개씩 끝까지(3-2). 끝까지 받은 레포만 사라짐을 판단한다(5-6)
    3. 새 PR, updatedAt 이 바뀐 PR 만 내용을 받는다. 같은 레포의 PR 을 25개씩 별칭으로 묶어 한 질의로(5-4)
    4. 100개 넘는 연결(댓글, 리뷰, 리뷰 스레드, 스레드 코멘트, 바뀐 파일, 라벨)은 그 연결만 끝까지 이어 받는다(P6)
    5. 사라짐: 레포가 조직 목록에서 빠졌고 GET /repos 가 404 면 repo_not_found,
       아니면 번호로 다시 물어 없을 때 not_found(5-6)

묶음 질의가 통째로 실패하면(아주 큰 PR 때문에 GitHub 이 시간 초과를 내는 경우 등) 그 묶음만 한 건씩 다시 받는다.
실패한 PR 하나 때문에 같은 묶음의 나머지가 실패로 남지 않게 하기 위해서다(08 문서 8장).
"""

from __future__ import annotations

import logging

from ..config import ORG
from ..github import GitHubError
from ..keys import pr_key, repo_of_key
from ..runner import Context, FetchError, Item, Listing, Source

log = logging.getLogger("collector.pr")

BATCH = 25         # 한 질의에 묶는 PR 수(07 문서 5-4, 25개 14점 측정)
THREAD_PAGE = 50   # 리뷰 스레드 한 쪽 크기. 100 이면 25개 묶음이 26점, 50 이면 14점(2026-11-04 dryRun 측정). 넘는 것은 이어 받는다

PAGE = "pageInfo { hasNextPage endCursor }"
COMMENT = "author { login __typename } body createdAt updatedAt"
REVIEW = "author { login __typename } state body submittedAt updatedAt"
THREAD_COMMENT = "author { login __typename } body diffHunk createdAt updatedAt"
FILE = "path additions deletions changeType"

# PR 안의 연결: (이름, 한 쪽 크기, 노드 항목). 저장할 때는 nodes 만 한 목록으로 합친다
CONNECTIONS = [
    ("labels", 100, "name"),
    ("comments", 100, COMMENT),
    ("reviews", 100, REVIEW),
    ("reviewThreads", THREAD_PAGE, f"id path isResolved comments(first: 100) {{ {PAGE} nodes {{ {THREAD_COMMENT} }} }}"),
    ("files", 100, FILE),
]

PR_FIELDS = ("number title body url author { login __typename } createdAt mergedAt updatedAt baseRefName headRefName "
             "additions deletions changedFiles "
             + " ".join(f"{name}(first: {size}) {{ {PAGE} nodes {{ {nodes} }} }}" for name, size, nodes in CONNECTIONS))

LIST_QUERY = f"""query($owner: String!, $name: String!, $cursor: String) {{
  rateLimit {{ cost }}
  repository(owner: $owner, name: $name) {{
    name
    pullRequests(first: 100, after: $cursor, states: MERGED, orderBy: {{field: UPDATED_AT, direction: DESC}}) {{
      {PAGE}
      nodes {{ number updatedAt }}
    }}
  }}
}}"""

LOOKUP_QUERY = """query($owner: String!, $name: String!, $number: Int!) {
  rateLimit { cost }
  repository(owner: $owner, name: $name) { pullRequest(number: $number) { number } }
}"""

THREAD_COMMENTS_QUERY = f"""query($id: ID!, $cursor: String) {{
  rateLimit {{ cost }}
  node(id: $id) {{ ... on PullRequestReviewThread {{ comments(first: 100, after: $cursor) {{ {PAGE} nodes {{ {THREAD_COMMENT} }} }} }} }}
}}"""


def batch_query(numbers: list[int]) -> str:
    parts = " ".join(f"p{i}: pullRequest(number: {n}) {{ ...pr }}" for i, n in enumerate(numbers))
    return (f"query($owner: String!, $name: String!) {{ rateLimit {{ cost }} "
            f"repository(owner: $owner, name: $name) {{ {parts} }} }} "
            f"fragment pr on PullRequest {{ {PR_FIELDS} }}")


def connection_query(name: str) -> str:
    size, nodes = next((s, n) for c, s, n in CONNECTIONS if c == name)
    return (f"query($owner: String!, $name: String!, $number: Int!, $cursor: String) {{ rateLimit {{ cost }} "
            f"repository(owner: $owner, name: $name) {{ pullRequest(number: $number) {{ "
            f"{name}(first: {size}, after: $cursor) {{ {PAGE} nodes {{ {nodes} }} }} }} }} }}")


def parse_key(key: str) -> tuple[str, int]:
    repo = repo_of_key(key)
    head, _, num = key.rpartition("#")
    if not repo or head != f"{ORG}/{repo}" or not num.isdigit():
        raise ValueError(f"PR 키 형식이 아닙니다(woowacourse-teams/{{레포}}#{{번호}}): {key}")
    return repo, int(num)


class PrSource(Source):
    name = "github-pr"

    def __init__(self, github=None) -> None:
        self._github = github          # 시험에서 가짜를 넣는다. 없으면 ctx.github
        self.repos_checked = 0
        self.repos_failed: list[dict] = []
        self.missing_repos: set[str] = set()   # 조직 목록을 끝까지 받았는데 거기 없는, 색인에만 있는 레포
        self._repo_exists: dict[str, str | None] = {}  # 조직 목록에서 빠진 레포 → 사라짐 이유(그대로 있으면 None)

    def gh(self, ctx: Context):
        return self._github or ctx.github

    def gql(self, ctx: Context, query: str, variables: dict) -> dict:
        """GraphQL 한 번. 한도 초과(RATE_LIMITED)는 GitHub 도구가 초기화 시각까지 기다린 뒤라서 한 번 더 보낸다."""
        try:
            return self.gh(ctx).graphql(query, variables)
        except GitHubError as exc:
            if "RATE_LIMITED" not in str(exc):
                raise
            log.warning("GraphQL 한도 초과, 다시 보냄")
            return self.gh(ctx).graphql(query, variables)

    # 1, 2. 목록

    def list_items(self, ctx: Context) -> Listing:
        if ctx.repos:
            names = list(ctx.repos)
        else:
            names = [r["name"] for r in self.gh(ctx).org_repos()]
            # 조직 목록을 끝까지 받았으므로, 색인에만 있는 레포는 "목록 0건으로 끝까지 받은 레포" 로 본다(5-6)
            listed = set(names)
            self.missing_repos = {repo_of_key(e["external_key"]) for e in ctx.index.alive()} - listed - {None}
        items: list[Item] = []
        complete: set[str] = set(self.missing_repos)
        for name in names:
            ctx.begin_group(name)
            try:
                repo, numbers = self.list_repo(ctx, name)
            except Exception as exc:  # 이 레포만 건너뛴다. 받은 쪽까지의 PR 도 쓰지 않는다(사라짐 판단도 하지 않음)
                log.warning("%s PR 목록 받기 실패: %r", name, exc)
                self.repos_failed.append({"repo": name, "message": f"{type(exc).__name__}: {exc}"[:300]})
                continue
            if repo is None:
                log.warning("%s 레포를 찾을 수 없음", name)
                self.repos_failed.append({"repo": name, "message": "repository not found"})
                continue
            self.repos_checked += 1
            complete.add(repo)
            for number, updated_at in numbers:
                items.append(Item(pr_key(repo, number), {"updated_at": updated_at}, group=repo, data=(repo, number)))
        return Listing(items, complete)

    def list_repo(self, ctx: Context, name: str) -> tuple[str | None, list[tuple[int, str]]]:
        """머지된 PR 의 (번호, updatedAt) 전체. 레포 이름은 GitHub 표기(응답의 name)로 돌려준다."""
        out: list[tuple[int, str]] = []
        cursor, repo = None, None
        while True:
            data = self.gql(ctx, LIST_QUERY, {"owner": ORG, "name": name, "cursor": cursor})
            r = data.get("repository")
            if r is None:
                return None, []
            repo = r["name"]
            conn = r["pullRequests"]
            out += [(n["number"], n["updatedAt"]) for n in conn["nodes"]]
            if not conn["pageInfo"]["hasNextPage"]:
                return repo, out
            cursor = conn["pageInfo"]["endCursor"]

    def only_items(self, ctx: Context, values: list[str]) -> list[Item]:
        items = []
        for key in values:
            repo, number = parse_key(key)
            items.append(Item(key, None, group=repo, data=(repo, number)))
        return items

    # 3, 4. 내용

    def fetch_many(self, ctx: Context, items: list[Item]):
        """같은 레포의 이어진 PR 을 BATCH 개씩 묶어 받고, 묶음마다 바로 내준다(메모리에 모으지 않음)."""
        chunk: list[Item] = []
        for item in items:
            if chunk and (len(chunk) >= BATCH or chunk[0].data[0] != item.data[0]):
                yield from self.fetch_batch(ctx, chunk)
                chunk = []
            chunk.append(item)
        if chunk:
            yield from self.fetch_batch(ctx, chunk)

    def fetch(self, ctx: Context, item: Item) -> dict:
        _, got = next(self.fetch_batch(ctx, [item], split=False))
        if isinstance(got, BaseException):
            raise got
        return got

    def fetch_batch(self, ctx: Context, chunk: list[Item], split: bool = True):
        repo = chunk[0].data[0]
        try:
            data = self.gql(ctx, batch_query([it.data[1] for it in chunk]), {"owner": ORG, "name": repo})
        except Exception as exc:
            if split and len(chunk) > 1:
                log.warning("%s 묶음(%d건) 받기 실패, 한 건씩 다시 받음: %r", repo, len(chunk), exc)
                for it in chunk:
                    yield from self.fetch_batch(ctx, [it], split=False)
                return
            for it in chunk:
                yield it, _fetch_error(exc)
            return
        r = data.get("repository")
        for i, it in enumerate(chunk):
            pr = (r or {}).get(f"p{i}")
            if r is None:
                yield it, FetchError("repo_not_found")
            elif pr is None:
                yield it, FetchError("not_found")
            elif pr.get("mergedAt") is None:
                yield it, FetchError("not_merged")  # --only 로 머지되지 않은 PR 을 고른 경우(P2)
            else:
                try:
                    yield it, {"found_in": {"repo": f"{ORG}/{repo}"}, "pull_request": self.complete(ctx, repo, pr)}
                except Exception as exc:
                    yield it, _fetch_error(exc)

    def complete(self, ctx: Context, repo: str, pr: dict) -> dict:
        """한 쪽에 다 못 받은 연결을 끝까지 이어 받고, 연결을 nodes 목록으로 바꾼다."""
        for name, _, _ in CONNECTIONS:
            q = connection_query(name)
            pr[name] = self.all_nodes(pr[name], lambda cur: self.gql(
                ctx, q, {"owner": ORG, "name": repo, "number": pr["number"], "cursor": cur})["repository"]["pullRequest"][name])
        for thread in pr["reviewThreads"]:
            tid = thread.pop("id")
            thread["comments"] = self.all_nodes(thread["comments"], lambda cur: self.gql(
                ctx, THREAD_COMMENTS_QUERY, {"id": tid, "cursor": cur})["node"]["comments"])
        return pr

    @staticmethod
    def all_nodes(conn: dict, next_page) -> list:
        nodes = list(conn["nodes"])
        page = conn["pageInfo"]
        while page["hasNextPage"]:
            conn = next_page(page["endCursor"])
            nodes += conn["nodes"]
            page = conn["pageInfo"]
        return nodes

    def index_fields(self, record: dict) -> dict:
        return {"updated_at": record["pull_request"]["updatedAt"]}

    # 5. 사라짐

    def group_of(self, entry: dict) -> str | None:
        return repo_of_key(entry["external_key"])

    def confirm_gone(self, ctx: Context, entry: dict) -> str | None:
        repo, number = parse_key(entry["external_key"])
        if repo in self.missing_repos:
            # 조직 목록에서 빠진 레포를 직접 물어 본다. 404 면 repo_not_found. 이름을 바꿨거나 옮겼으면 GitHub 이 새 레포로
            # 넘겨 줘 이름이 다르게 오므로 repo_renamed(07 문서 7장, 다른 GitHub 원천의 _repos.repo_gone_reason 과 같은 판단)
            if repo not in self._repo_exists:
                data = self.gh(ctx).rest_json(f"/repos/{ORG}/{repo}", ok404=True)
                if data is None:
                    self._repo_exists[repo] = "repo_not_found"
                elif data.get("name") != repo or (data.get("owner") or {}).get("login", ORG) != ORG:
                    self._repo_exists[repo] = "repo_renamed"
                else:
                    self._repo_exists[repo] = None
            if self._repo_exists[repo]:
                return self._repo_exists[repo]
        data = self.gql(ctx, LOOKUP_QUERY, {"owner": ORG, "name": repo, "number": number})
        if ((data.get("repository") or {}).get("pullRequest")) is None:
            return "not_found"
        return None

    def finish(self, ctx: Context) -> None:
        gh = self._github or ctx._github  # 한 번도 쓰지 않았으면 만들지 않는다(토큰 없이 끝나는 경우)
        ctx.report.extra.update({
            "repos_checked": self.repos_checked,
            "repos_failed": self.repos_failed,
            "graphql_points": getattr(gh, "graphql_points", 0),
            "waited_sec": round(getattr(gh, "waited_sec", 0.0), 1),
        })


def _fetch_error(exc: BaseException) -> FetchError:
    if isinstance(exc, FetchError):
        return exc
    if isinstance(exc, GitHubError):
        return FetchError(f"graphql_{exc.status}")
    return FetchError(f"error_{type(exc).__name__}")
