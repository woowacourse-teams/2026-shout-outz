"""디스커션 github-discussion. 가짜 GraphQL 로 네트워크 없이 돌린다(쪽 나눔도 흉내 낸다)."""

from datetime import datetime, timedelta, timezone

from collector.config import Settings
from collector.github import GitHubError
from collector.index import Index
from collector.keys import discussion_key, raw_path
from collector.runner import collect
from collector.sources import discussion as m
from collector.sources.discussion import DiscussionSource
from collector.store import LocalStore

T0 = datetime(2026, 11, 5, 3, 0, tzinfo=timezone.utc)
REPO = "2026-jarihana"


def disc(number, updated="2026-10-28T06:25:57Z", comments=()):
    """comments: [(updatedAt, [답글 updatedAt, ...]), ...]"""
    return {"id": f"D{number}", "number": number, "title": f"t{number}", "body": "본문",
            "url": f"https://github.com/woowacourse-teams/{REPO}/discussions/{number}",
            "createdAt": "2026-10-25T15:55:25Z", "updatedAt": updated, "upvoteCount": 2,
            "category": {"name": "제안"}, "author": {"login": "u"}, "answer": None,
            "comments": [{"id": f"C{number}-{i}", "author": {"login": "c"}, "body": f"댓글{i}", "createdAt": "2026-10-26T00:00:00Z",
                          "updatedAt": cu, "isAnswer": False,
                          "replies": [{"author": {"login": "r"}, "body": f"답글{j}", "createdAt": "2026-10-26T00:00:00Z",
                                       "updatedAt": ru} for j, ru in enumerate(replies)]}
                         for i, (cu, replies) in enumerate(comments)]}


def page(nodes, after, size):
    start = int(after or 0)
    chunk = nodes[start:start + size]
    more = start + size < len(nodes)
    return {"pageInfo": {"hasNextPage": more, "endCursor": str(start + size) if more else None}, "nodes": chunk}


def comment_view(c):
    out = {k: v for k, v in c.items() if k != "replies"}
    out["replies"] = page(c["replies"], None, m.REPLIES_PAGE)
    return out


def disc_view(d):
    out = {k: v for k, v in d.items() if k != "comments"}
    out["comments"] = page([comment_view(c) for c in d["comments"]], None, m.COMMENTS_PAGE)
    return out


class FakeGitHub:
    def __init__(self):
        self.repos = [{"name": REPO, "has_discussions": True}]
        self.world: dict[str, list[dict]] = {REPO: []}
        self.exists = {}
        self.queries: list[str] = []
        self.broken = False

    def org_repos(self):
        return list(self.repos)

    def rest_json(self, path, params=None, ok404=False):
        return self.exists.get(path.rsplit("/", 1)[1])

    def _find(self, kind, node_id):
        for ds in self.world.values():
            for d in ds:
                if kind == "D" and d["id"] == node_id:
                    return d
                for c in d["comments"]:
                    if kind == "C" and c["id"] == node_id:
                        return c

    def graphql(self, query, variables=None):
        v = variables or {}
        name = {m.LIST_QUERY: "list", m.ONE_QUERY: "one", m.EXISTS_QUERY: "exists",
                m.MORE_COMMENTS: "comments", m.MORE_REPLIES: "replies"}[query]
        self.queries.append(name)
        if self.broken and name == "list":
            raise GitHubError(502, "bad gateway")
        if name == "list":
            ds = self.world.get(v["name"])
            if ds is None:
                return {"repository": None}
            return {"repository": {"discussions": page([disc_view(d) for d in ds], v["after"], m.DISCUSSIONS_PAGE)}}
        if name in ("one", "exists"):
            hit = [d for d in self.world.get(v["name"], []) if d["number"] == v["number"]]
            node = (disc_view(hit[0]) if name == "one" else {"number": hit[0]["number"]}) if hit else None
            return {"repository": {"discussion": node}}
        if name == "comments":
            d = self._find("D", v["id"])
            return {"node": {"comments": page([comment_view(c) for c in d["comments"]], v["after"], 100)}}
        c = self._find("C", v["id"])
        return {"node": {"replies": page(c["replies"], v["after"], 100)}}


_DAY = [0]


def run(tmp_path, gh, **kw):
    _DAY[0] += 1
    box = [T0 + timedelta(days=_DAY[0])]

    def clock():
        box[0] += timedelta(seconds=1)
        return box[0]
    store = LocalStore(str(tmp_path))
    rep = collect(Settings(store_url=str(tmp_path)), ["github-discussion"], store=store,
                  make_source=lambda n: DiscussionSource(gh), clock=clock, **kw)
    return store, rep, rep.sources["github-discussion"]


def counts(sr):
    return {k: v for k, v in sr.counts.items() if v}


def test_first_run_record_and_markers(tmp_path):
    gh = FakeGitHub()
    gh.world[REPO] = [disc(453, comments=[("2026-10-28T06:41:21Z", ["2026-10-27T00:00:00Z"])]), disc(454)]
    store, rep, sr = run(tmp_path, gh)
    assert counts(sr) == {"new": 2}
    key = discussion_key(REPO, 453)
    rec = store.read_json(raw_path("github-discussion", key))
    assert rec["found_in"] == {"repo": f"woowacourse-teams/{REPO}"}
    d = rec["discussion"]
    assert "id" not in d and d["number"] == 453 and d["category"] == {"name": "제안"} and d["answer"] is None
    assert set(d) == {"number", "title", "body", "url", "createdAt", "updatedAt", "category", "author", "upvoteCount",
                      "answer", "comments"}
    c = d["comments"][0]
    assert set(c) == {"author", "body", "createdAt", "updatedAt", "isAnswer", "replies"}
    assert c["replies"][0]["body"] == "답글0" and "pageInfo" not in str(d)
    e = Index(store, "github-discussion").load().get(key)
    assert e["last_activity_at"] == "2026-10-28T06:41:21Z" and e["reply_count"] == 2
    assert gh.queries == ["list"]  # 한 건 받기에서 다시 묻지 않는다
    assert sr.extra["repos_checked"] == 1


def test_unchanged_comment_edit_and_comment_delete(tmp_path):
    gh = FakeGitHub()
    gh.world[REPO] = [disc(1, comments=[("2026-10-28T00:00:00Z", []), ("2026-10-27T00:00:00Z", [])]), disc(2)]
    run(tmp_path, gh)
    store, rep, sr = run(tmp_path, gh)
    assert counts(sr) == {"unchanged": 2}

    # 댓글만 고침: 글의 updatedAt 은 그대로지만 가장 늦은 시각이 바뀐다
    gh.world[REPO][0]["comments"][1]["updatedAt"] = "2026-10-29T00:00:00Z"
    store, rep, sr = run(tmp_path, gh)
    assert counts(sr) == {"changed": 1, "unchanged": 1}

    # 이른 댓글을 지움: 시각은 그대로지만 수가 줄었다
    del gh.world[REPO][0]["comments"][0]
    store, rep, sr = run(tmp_path, gh)
    assert counts(sr) == {"changed": 1, "unchanged": 1}
    rec = store.read_json(raw_path("github-discussion", discussion_key(REPO, 1)))
    assert rec["version"] == 3 and len(rec["discussion"]["comments"]) == 1


def test_gone_only_when_number_lookup_is_null(tmp_path):
    gh = FakeGitHub()
    gh.world[REPO] = [disc(1), disc(2)]
    run(tmp_path, gh)
    real = gh.world[REPO]
    gh.world[REPO] = [real[0]]

    orig = gh.graphql
    def still_there(query, variables=None):  # 목록에서는 빠졌지만 번호로 물으면 있다
        if query == m.EXISTS_QUERY:
            return {"repository": {"discussion": {"number": 2}}}
        return orig(query, variables)
    gh.graphql = still_there
    store, rep, sr = run(tmp_path, gh)
    assert counts(sr) == {"unchanged": 1}

    gh.graphql = orig
    store, rep, sr = run(tmp_path, gh)
    assert counts(sr) == {"unchanged": 1, "gone": 1} and sr.gone_keys == [discussion_key(REPO, 2)]
    assert Index(store, "github-discussion").load().get(discussion_key(REPO, 2))["gone"]["reason"] == "not_found"


def test_nested_paging(tmp_path):
    gh = FakeGitHub()
    many_replies = [f"2026-10-01T00:00:{i:02d}Z" for i in range(m.REPLIES_PAGE + 5)]
    comments = [("2026-10-02T00:00:00Z", [])] * (m.COMMENTS_PAGE + 3) + [("2026-10-03T00:00:00Z", many_replies)]
    gh.world[REPO] = [disc(n) for n in range(1, m.DISCUSSIONS_PAGE + 3)] + [disc(999, comments=comments)]
    for i, d in enumerate(gh.world[REPO][-1]["comments"]):
        d["id"] = f"C999-{i}"
    store, rep, sr = run(tmp_path, gh)
    assert counts(sr) == {"new": m.DISCUSSIONS_PAGE + 3}
    d = store.read_json(raw_path("github-discussion", discussion_key(REPO, 999)))["discussion"]
    assert len(d["comments"]) == m.COMMENTS_PAGE + 4
    assert len(d["comments"][-1]["replies"]) == m.REPLIES_PAGE + 5
    assert gh.queries.count("list") == 2 and "comments" in gh.queries and "replies" in gh.queries


def test_disabled_repo_queried_only_with_known_keys(tmp_path):
    gh = FakeGitHub()
    gh.repos = [{"name": REPO, "has_discussions": False}]
    gh.world[REPO] = [disc(1)]
    store, rep, sr = run(tmp_path, gh)
    assert counts(sr) == {} and gh.queries == [] and sr.extra["repos_checked"] == 0

    gh.repos = [{"name": REPO, "has_discussions": True}]
    run(tmp_path, gh)
    gh.repos = [{"name": REPO, "has_discussions": False}]  # 꺼졌지만 아는 키가 있다
    gh.world[REPO] = []
    gh.queries.clear()
    store, rep, sr = run(tmp_path, gh)
    assert gh.queries[0] == "list" and counts(sr) == {"gone": 1}


def test_list_failure_skips_gone(tmp_path):
    gh = FakeGitHub()
    gh.world[REPO] = [disc(1)]
    run(tmp_path, gh)
    gh.broken = True
    gh.world[REPO] = []
    store, rep, sr = run(tmp_path, gh)
    assert counts(sr) == {} and sr.extra["repos_failed"][0]["repo"] == REPO


def test_repo_removed_from_org(tmp_path):
    gh = FakeGitHub()
    gh.world[REPO] = [disc(1), disc(2)]
    run(tmp_path, gh)
    gh.repos = []
    store, rep, sr = run(tmp_path, gh)
    assert counts(sr) == {"gone": 2}
    assert {e["gone"]["reason"] for e in Index(store, "github-discussion").load().entries.values()} == {"repo_not_found"}


def test_only_by_number(tmp_path):
    gh = FakeGitHub()
    gh.world[REPO] = [disc(7, comments=[("2026-10-29T00:00:00Z", [])])]
    store, rep, sr = run(tmp_path, gh, only=[discussion_key(REPO, 7), discussion_key(REPO, 8)])
    assert counts(sr) == {"new": 1, "failed": 1} and sr.failure_reasons == {"not_found": 1}
    assert Index(store, "github-discussion").load().get(discussion_key(REPO, 7))["reply_count"] == 1


def test_markers():
    d = m.markers({"updatedAt": "b", "comments": [{"updatedAt": "a", "replies": [{"updatedAt": "c"}]}]})
    assert d == {"last_activity_at": "c", "reply_count": 2}
