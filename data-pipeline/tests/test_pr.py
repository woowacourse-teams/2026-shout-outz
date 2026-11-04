"""PR 수집(github-pr) 시험. 네트워크 없이 가짜 GitHub 로 돌린다."""

import re
from datetime import datetime, timedelta, timezone

from collector.config import Settings
from collector.github import GitHubError
from collector.index import Index
from collector.keys import pr_key, raw_path
from collector.runner import collect
from collector.sources.pr import LIST_QUERY, PrSource
from collector.store import LocalStore

T0 = datetime(2026, 11, 5, 3, 0, tzinfo=timezone.utc)
ORG = "woowacourse-teams"


def clock_from(t):
    box = [t]
    def tick():
        box[0] += timedelta(seconds=1)
        return box[0]
    return tick


def make_pr(number, updated="t1", files=1, comments=1, threads=1, thread_comments=1, merged=True):
    return {
        "number": number, "title": f"PR {number}", "body": "본문", "url": f"https://github.com/{ORG}/r/pull/{number}",
        "author": {"login": "a"}, "createdAt": "c", "mergedAt": "m" if merged else None, "updatedAt": updated,
        "baseRefName": "develop", "headRefName": "feat", "additions": 1, "deletions": 0, "changedFiles": files,
        "labels": [{"name": "BE"}],
        "comments": [{"author": None, "body": f"댓글{i}", "createdAt": "c", "updatedAt": "u"} for i in range(comments)],
        "reviews": [{"author": {"login": "b"}, "state": "APPROVED", "body": "", "submittedAt": "s", "updatedAt": "u"}],
        "reviewThreads": [
            {"id": f"T{number}-{t}", "path": "a.py", "isResolved": True,
             "comments": [{"author": {"login": "b"}, "body": f"코멘트{i}", "diffHunk": "@@ -1 +1 @@", "createdAt": "c",
                           "updatedAt": "u"} for i in range(thread_comments)]}
            for t in range(threads)],
        "files": [{"path": f"f{i}.py", "additions": 1, "deletions": 0, "changeType": "MODIFIED"} for i in range(files)],
    }


def page(items, size, cursor):
    start = int(cursor or 0)
    end = start + size
    return {"pageInfo": {"hasNextPage": end < len(items), "endCursor": str(end)}, "nodes": items[start:end]}


class FakeGitHub:
    """world: {레포: {번호: PR}}. graphql() 은 질의 모양을 보고 가짜 응답을 만든다."""

    def __init__(self, world, existing=None):
        self.world = world
        self.existing = set(world) if existing is None else set(existing)
        self.queries = []
        self.graphql_points = 0
        self.waited_sec = 0.0
        self.fail_batches = 0

    def org_repos(self):
        return [{"name": n} for n in self.world]

    def rest_json(self, path, params=None, ok404=False):
        name = path.rsplit("/", 1)[-1]
        return {"name": name} if name in self.existing else None

    def _thread(self, t):
        return {"id": t["id"], "path": t["path"], "isResolved": t["isResolved"], "comments": page(t["comments"], 100, None)}

    def _pr(self, pr):
        out = {k: v for k, v in pr.items() if not isinstance(v, list)}
        for name, size in (("labels", 100), ("comments", 100), ("reviews", 100), ("files", 100)):
            out[name] = page(pr[name], size, None)
        out["reviewThreads"] = page([self._thread(t) for t in pr["reviewThreads"]], 50, None)
        return out

    def graphql(self, query, variables=None):
        v = variables or {}
        self.queries.append((query, v))
        self.graphql_points += 1
        if "node(id: $id)" in query:
            tid = v["id"]
            for prs in self.world.values():
                for pr in prs.values():
                    for t in pr["reviewThreads"]:
                        if t["id"] == tid:
                            return {"node": {"comments": page(t["comments"], 100, v["cursor"])}}
            return {"node": None}
        prs = self.world.get(v["name"]) if v["name"] in self.existing else None
        if prs is None:
            return {"rateLimit": {"cost": 1}, "repository": None}
        if "pullRequests(" in query:
            assert "states: MERGED" in query
            merged = [{"number": n, "updatedAt": p["updatedAt"]} for n, p in prs.items() if p["mergedAt"]]
            return {"repository": {"name": v["name"], "pullRequests": page(merged, 100, v["cursor"])}}
        aliases = re.findall(r"(p\d+): pullRequest\(number: (\d+)\)", query)
        if aliases:
            if self.fail_batches and len(aliases) > 1:
                self.fail_batches -= 1
                raise GitHubError(502, "timeout")
            return {"repository": {a: (self._pr(prs[int(n)]) if int(n) in prs else None) for a, n in aliases}}
        pr = prs.get(v["number"])
        if pr is None:
            return {"repository": {"pullRequest": None}}
        m = re.search(r"pullRequest\(number: \$number\) \{ (\w+)\(first: (\d+)", query)
        if not m:  # 사라짐 확인용 번호 조회
            return {"repository": {"pullRequest": {"number": pr["number"]}}}
        name, size = m.group(1), int(m.group(2))
        nodes = [self._thread(t) for t in pr[name]] if name == "reviewThreads" else pr[name]
        return {"repository": {"pullRequest": {name: page(nodes, size, v["cursor"])}}}

    def batch_queries(self):
        return [q for q, _ in self.queries if "p0: pullRequest" in q]


_RUNS = [0]


def run(tmp_path, gh, **kw):
    _RUNS[0] += 1
    store = LocalStore(str(tmp_path))
    rep = collect(Settings(store_url=str(tmp_path)), ["github-pr"], store=store, make_source=lambda n: PrSource(gh),
                  clock=clock_from(T0 + timedelta(days=_RUNS[0])), **kw)
    return store, rep, rep.sources["github-pr"]


def counts(sr):
    return {k: v for k, v in sr.counts.items() if v}


def test_first_run_all_new_then_unchanged(tmp_path):
    gh = FakeGitHub({"2026-A": {1: make_pr(1), 2: make_pr(2, merged=False), 3: make_pr(3)}, "2026-B": {7: make_pr(7)}})
    store, rep, sr = run(tmp_path, gh)
    assert rep.status == "success"
    assert counts(sr) == {"new": 3}  # 머지되지 않은 2번은 목록에 없다
    rec = store.read_json(raw_path("github-pr", pr_key("2026-A", 1)))
    assert rec["found_in"] == {"repo": f"{ORG}/2026-A"} and rec["version"] == 1
    p = rec["pull_request"]
    assert p["labels"] == [{"name": "BE"}] and p["comments"][0]["author"] is None
    assert p["reviewThreads"] == [{"path": "a.py", "isResolved": True, "comments": [
        {"author": {"login": "b"}, "body": "코멘트0", "diffHunk": "@@ -1 +1 @@", "createdAt": "c", "updatedAt": "u"}]}]
    assert isinstance(p["files"], list) and "pageInfo" not in str(p)
    assert Index(store, "github-pr").load().get(pr_key("2026-B", 7))["updated_at"] == "t1"
    assert sr.extra["repos_checked"] == 2 and sr.extra["graphql_points"] == len(gh.queries)
    assert len(gh.batch_queries()) == 2  # 레포마다 한 묶음
    assert all("states: MERGED" in q for q, _ in gh.queries if q == LIST_QUERY)

    gh.queries.clear()
    _, _, sr = run(tmp_path, gh)
    assert counts(sr) == {"unchanged": 3}
    assert gh.batch_queries() == []


def test_changed_updated_at_refetches(tmp_path):
    gh = FakeGitHub({"2026-A": {1: make_pr(1), 2: make_pr(2)}})
    run(tmp_path, gh)
    gh.world["2026-A"][2] = make_pr(2, updated="t2", comments=3)
    gh.queries.clear()
    store, _, sr = run(tmp_path, gh)
    assert counts(sr) == {"changed": 1, "unchanged": 1}
    assert re.findall(r"pullRequest\(number: (\d+)\)", gh.batch_queries()[0]) == ["2"]
    rec = store.read_json(raw_path("github-pr", pr_key("2026-A", 2)))
    assert rec["version"] == 2 and len(rec["pull_request"]["comments"]) == 3
    assert Index(store, "github-pr").load().get(pr_key("2026-A", 2))["updated_at"] == "t2"


def test_batches_of_25(tmp_path):
    gh = FakeGitHub({"2026-A": {n: make_pr(n) for n in range(1, 31)}})
    run(tmp_path, gh)
    for n in range(1, 31):
        gh.world["2026-A"][n]["updatedAt"] = "t2"
    gh.queries.clear()
    _, _, sr = run(tmp_path, gh)
    assert counts(sr) == {"changed": 30}
    sizes = [len(re.findall(r"p\d+: pullRequest", q)) for q in gh.batch_queries()]
    assert sizes == [25, 5]


def test_listing_pages_over_100(tmp_path):
    gh = FakeGitHub({"2026-A": {n: make_pr(n, files=0, comments=0, threads=0) for n in range(1, 251)}})
    _, _, sr = run(tmp_path, gh)
    assert counts(sr) == {"new": 250}
    assert sum(1 for q, _ in gh.queries if q == LIST_QUERY) == 3


def test_nested_pagination(tmp_path):
    gh = FakeGitHub({"2026-A": {1: make_pr(1, files=275, comments=130, threads=60, thread_comments=120)}})
    store, _, sr = run(tmp_path, gh)
    assert counts(sr) == {"new": 1}
    p = store.read_json(raw_path("github-pr", pr_key("2026-A", 1)))["pull_request"]
    assert [f["path"] for f in p["files"]] == [f"f{i}.py" for i in range(275)]
    assert len(p["comments"]) == 130 and len(p["reviewThreads"]) == 60
    assert all(len(t["comments"]) == 120 and "id" not in t for t in p["reviewThreads"])
    file_pages = [v["cursor"] for q, v in gh.queries if "files(first: 100, after: $cursor)" in q]
    assert file_pages == ["100", "200"]


def test_batch_failure_falls_back_to_single(tmp_path):
    gh = FakeGitHub({"2026-A": {1: make_pr(1), 2: make_pr(2), 3: make_pr(3)}})
    gh.fail_batches = 1
    _, rep, sr = run(tmp_path, gh)
    assert counts(sr) == {"new": 3} and rep.status == "success"


def test_gone_via_null_lookup(tmp_path):
    gh = FakeGitHub({"2026-A": {1: make_pr(1), 2: make_pr(2)}})
    run(tmp_path, gh)
    del gh.world["2026-A"][2]
    store, _, sr = run(tmp_path, gh)
    assert counts(sr) == {"unchanged": 1, "gone": 1} and sr.gone_keys == [pr_key("2026-A", 2)]
    assert Index(store, "github-pr").load().get(pr_key("2026-A", 2))["gone"]["reason"] == "not_found"


def test_missing_from_list_but_lookup_finds_is_not_gone(tmp_path):
    gh = FakeGitHub({"2026-A": {1: make_pr(1), 2: make_pr(2)}})
    run(tmp_path, gh)
    real = gh.graphql

    def hide_2(query, variables=None):  # 목록에서만 빠짐(쪽을 넘기는 사이 고쳐진 경우 등)
        out = real(query, variables)
        if query == LIST_QUERY:
            out["repository"]["pullRequests"]["nodes"] = [n for n in out["repository"]["pullRequests"]["nodes"] if n["number"] != 2]
        return out
    gh.graphql = hide_2
    _, _, sr = run(tmp_path, gh)
    assert counts(sr) == {"unchanged": 1}


def test_repo_removed_is_repo_not_found(tmp_path):
    gh = FakeGitHub({"2026-A": {1: make_pr(1)}, "2026-B": {7: make_pr(7), 8: make_pr(8)}})
    run(tmp_path, gh)
    del gh.world["2026-B"]
    gh.existing.discard("2026-B")
    store, _, sr = run(tmp_path, gh)
    assert counts(sr) == {"unchanged": 1, "gone": 2}
    idx = Index(store, "github-pr").load()
    assert idx.get(pr_key("2026-B", 7))["gone"]["reason"] == "repo_not_found"
    assert sum(1 for q, _ in gh.queries if "pullRequest(number: $number)" in q) == 0


def test_repo_list_failure_skips_gone(tmp_path):
    gh = FakeGitHub({"2026-A": {1: make_pr(1), 2: make_pr(2)}})
    run(tmp_path, gh)
    real = gh.graphql

    def broken(query, variables=None):
        if query == LIST_QUERY:
            raise GitHubError(502, "bad gateway")
        return real(query, variables)
    gh.graphql = broken
    _, _, sr = run(tmp_path, gh)
    assert counts(sr) == {} and sr.extra["repos_failed"][0]["repo"] == "2026-A"


def test_repos_narrowing_uses_github_name(tmp_path):
    gh = FakeGitHub({"2026-A": {1: make_pr(1)}, "2026-B": {7: make_pr(7)}})
    run(tmp_path, gh)
    del gh.world["2026-B"][7]
    _, _, sr = run(tmp_path, gh, repos=["2026-A"])
    assert counts(sr) == {"unchanged": 1}  # 좁힌 실행에서는 다른 레포의 사라짐을 판단하지 않는다
    assert sr.extra["repos_checked"] == 1


def test_only(tmp_path):
    gh = FakeGitHub({"2026-A": {1: make_pr(1), 2: make_pr(2), 3: make_pr(3, merged=False)}})
    run(tmp_path, gh)
    gh.world["2026-A"][1]["updatedAt"] = "t2"
    gh.queries.clear()
    store, _, sr = run(tmp_path, gh, only=[pr_key("2026-A", 1), pr_key("2026-A", 2), pr_key("2026-A", 3)])
    assert counts(sr) == {"changed": 1, "unchanged": 1, "failed": 1}
    assert sr.failures[0]["reason"] == "not_merged"
    assert not any(q == LIST_QUERY for q, _ in gh.queries)
