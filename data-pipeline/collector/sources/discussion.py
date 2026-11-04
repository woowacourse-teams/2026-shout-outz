"""디스커션 github-discussion(06 문서 3-4, 5-3, 5-6, 5-7, 6장).

목록: GraphQL repository.discussions 로 글, 댓글, 답글 전체. 대상은 Discussions 가 켜진 레포와, 꺼졌지만 아는 키가 있는 레포(D19).
바뀜 표시: last_activity_at(글, 댓글, 답글 updatedAt 중 가장 늦은 값), reply_count(댓글 수 + 답글 수).
한 건 받기: 목록 응답에 내용이 모두 있으므로 다시 묻지 않고 그대로 저장한다.
사라짐: 목록에 없고 repository.discussion(number:) 도 null 이면 not_found. 레포가 없어지면 repo_not_found.

쪽 크기: 글 50, 댓글 20, 답글 10.
  - 100, 100, 100 은 GitHub 한 질의 노드 한도(50만)를 넘는다(100만, 2026-11-04 확인).
  - GraphQL 점수는 실제 개수가 아니라 요청한 최대 개수로 매긴다. 50/20/10 은 11점, 100/20/10 은 21점, 100/30/10 은 31점(dryRun 확인).
    켜진 레포 68개를 매일 묻기 때문에 레포당 한 번의 점수가 작은 쪽을 고른다.
  - 넘치는 댓글, 답글은 node(id:) 로 100개씩 이어 받는다. 표본(2026-jarihana 44건)에서 댓글 최대 5개, 답글 최대 7개라 거의 생기지 않는다.
"""

from __future__ import annotations

from ..config import ORG
from ..keys import discussion_key, repo_of_key
from ..runner import Context, FetchError, Item, Listing
from ._repos import RepoSource, known_repos, missing_repos, target_repos, to_fetch_error

DISCUSSIONS_PAGE, COMMENTS_PAGE, REPLIES_PAGE = 50, 20, 10

REPLY_FIELDS = "author { login } body createdAt updatedAt"
COMMENT_FIELDS = f"""id author {{ login }} body createdAt updatedAt isAnswer
  replies(first: {REPLIES_PAGE}) {{ pageInfo {{ hasNextPage endCursor }} nodes {{ {REPLY_FIELDS} }} }}"""
DISCUSSION_FIELDS = f"""id number title body url createdAt updatedAt upvoteCount
  category {{ name }} author {{ login }} answer {{ author {{ login }} createdAt url }}
  comments(first: {COMMENTS_PAGE}) {{ pageInfo {{ hasNextPage endCursor }} nodes {{ {COMMENT_FIELDS} }} }}"""

LIST_QUERY = f"""query($owner: String!, $name: String!, $after: String) {{
  rateLimit {{ cost }}
  repository(owner: $owner, name: $name) {{
    discussions(first: {DISCUSSIONS_PAGE}, after: $after, orderBy: {{field: CREATED_AT, direction: ASC}}) {{
      pageInfo {{ hasNextPage endCursor }}
      nodes {{ {DISCUSSION_FIELDS} }}
    }}
  }}
}}"""

ONE_QUERY = f"""query($owner: String!, $name: String!, $number: Int!) {{
  rateLimit {{ cost }}
  repository(owner: $owner, name: $name) {{ discussion(number: $number) {{ {DISCUSSION_FIELDS} }} }}
}}"""

EXISTS_QUERY = """query($owner: String!, $name: String!, $number: Int!) {
  rateLimit { cost }
  repository(owner: $owner, name: $name) { discussion(number: $number) { number } }
}"""

MORE_COMMENTS = f"""query($id: ID!, $after: String) {{
  rateLimit {{ cost }}
  node(id: $id) {{ ... on Discussion {{
    comments(first: 100, after: $after) {{ pageInfo {{ hasNextPage endCursor }} nodes {{ {COMMENT_FIELDS} }} }}
  }} }}
}}"""

MORE_REPLIES = f"""query($id: ID!, $after: String) {{
  rateLimit {{ cost }}
  node(id: $id) {{ ... on DiscussionComment {{
    replies(first: 100, after: $after) {{ pageInfo {{ hasNextPage endCursor }} nodes {{ {REPLY_FIELDS} }} }}
  }} }}
}}"""


def split_key(key: str) -> tuple[str, int]:
    repo = repo_of_key(key)
    prefix = f"{ORG}/{repo}/discussions/"
    if not repo or not key.startswith(prefix) or not key[len(prefix):].isdigit():
        raise ValueError(f"디스커션 키가 아닙니다: {key}")
    return repo, int(key[len(prefix):])


def markers(discussion: dict) -> dict:
    """바뀜 표시 두 값(06 문서 5-3). 저장한 discussion 에서 계산한다."""
    times = [discussion.get("updatedAt") or ""]
    count = 0
    for c in discussion.get("comments", []):
        times.append(c.get("updatedAt") or "")
        count += 1
        for r in c.get("replies", []):
            times.append(r.get("updatedAt") or "")
            count += 1
    return {"last_activity_at": max(times), "reply_count": count}


class DiscussionSource(RepoSource):
    name = "github-discussion"

    def __init__(self, github=None) -> None:
        super().__init__(github)
        self.repos_queried = 0

    def _q(self, gh, query: str, **variables) -> dict:
        return gh.graphql(query, variables)

    def _more_comments(self, gh, d: dict) -> list[dict]:
        """한 디스커션의 댓글 전체(쪽 나눔 이어 받기), 댓글마다 답글 전체."""
        conn = d["comments"]
        comments = list(conn["nodes"])
        page = conn["pageInfo"]
        while page["hasNextPage"]:
            conn = self._q(gh, MORE_COMMENTS, id=d["id"], after=page["endCursor"])["node"]["comments"]
            comments += conn["nodes"]
            page = conn["pageInfo"]
        out = []
        for c in comments:
            rconn = c["replies"]
            replies = list(rconn["nodes"])
            rpage = rconn["pageInfo"]
            while rpage["hasNextPage"]:
                rconn = self._q(gh, MORE_REPLIES, id=c["id"], after=rpage["endCursor"])["node"]["replies"]
                replies += rconn["nodes"]
                rpage = rconn["pageInfo"]
            out.append({"author": c["author"], "body": c["body"], "createdAt": c["createdAt"], "updatedAt": c["updatedAt"],
                        "isAnswer": c["isAnswer"], "replies": replies})
        return out

    def _shape(self, gh, d: dict) -> dict:
        """GraphQL 응답을 저장 모양으로: 쪽 나눔 표시와 id 를 빼고 댓글, 답글을 한 목록으로(06 문서 6장)."""
        return {"number": d["number"], "title": d["title"], "body": d["body"], "url": d["url"],
                "createdAt": d["createdAt"], "updatedAt": d["updatedAt"], "category": d["category"],
                "author": d["author"], "upvoteCount": d["upvoteCount"], "answer": d["answer"],
                "comments": self._more_comments(gh, d)}

    # 목록

    def list_items(self, ctx: Context) -> Listing:
        gh = self.gh(ctx)
        repos = target_repos(gh, ctx)
        self.listed = {r["name"] for r in repos}
        known = known_repos(ctx)
        items: list[Item] = []
        complete: set[str] = set()
        for r in repos:
            name = r["name"]
            if not r.get("has_discussions") and name not in known:
                continue
            self.repos_queried += 1
            try:
                repo_items = self._repo_items(gh, name)
            except Exception as exc:
                self.repo_failed(name, exc)  # 한 레포 안에서 일부만 받은 글은 버린다(바뀜 표시가 틀릴 수 있어서)
                continue
            items += repo_items
            complete.add(name)
        complete |= missing_repos(ctx, self.listed)
        return Listing(items, complete)

    def _repo_items(self, gh, repo: str) -> list[Item]:
        items, after = [], None
        while True:
            data = self._q(gh, LIST_QUERY, owner=ORG, name=repo, after=after)
            if not data.get("repository"):
                raise RuntimeError("repository 가 null")
            conn = data["repository"]["discussions"]
            for d in conn["nodes"]:
                shaped = self._shape(gh, d)
                items.append(Item(discussion_key(repo, d["number"]), markers(shaped), group=repo,
                                  data={"repo": repo, "discussion": shaped}))
            if not conn["pageInfo"]["hasNextPage"]:
                return items
            after = conn["pageInfo"]["endCursor"]

    def only_items(self, ctx: Context, values: list[str]) -> list[Item]:
        items = []
        for key in values:
            repo, number = split_key(key)
            items.append(Item(key, None, group=repo, data={"repo": repo, "number": number}))
        return items

    # 한 건 받기

    def fetch(self, ctx: Context, item: Item) -> dict:
        d = item.data
        shaped = d.get("discussion")
        if shaped is None:  # --only: 번호로 한 건 묻는다
            gh = self.gh(ctx)
            url = f"https://github.com/{ORG}/{d['repo']}/discussions/{d['number']}"
            try:
                data = self._q(gh, ONE_QUERY, owner=ORG, name=d["repo"], number=d["number"])
                node = (data.get("repository") or {}).get("discussion")
                if node is None:
                    raise FetchError("not_found", url)
                shaped = self._shape(gh, node)
            except Exception as exc:
                raise to_fetch_error(exc, url) from exc
        return {"found_in": {"repo": f"{ORG}/{d['repo']}"}, "discussion": shaped}

    def index_fields(self, record: dict) -> dict:
        return markers(record["discussion"])

    # 사라짐

    def confirm_gone(self, ctx: Context, entry: dict) -> str | None:
        repo, number = split_key(entry["external_key"])
        if repo not in self.listed:
            return self.repo_gone_reason(ctx, repo)
        data = self._q(self.gh(ctx), EXISTS_QUERY, owner=ORG, name=repo, number=number)
        if not data.get("repository"):
            return None  # 레포를 못 찾는 것은 레포 확인이 맡는다. 여기서는 사라짐으로 기록하지 않는다
        return "not_found" if data["repository"].get("discussion") is None else None

    def finish(self, ctx: Context) -> None:
        self.report_extra(ctx)
        ctx.report.extra["repos_checked"] = self.repos_queried
