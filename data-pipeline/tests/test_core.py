import hashlib
import json
from datetime import datetime, timedelta, timezone

from collector import lock
from collector.config import Settings
from collector.index import Index
from collector.keys import blog_key, pr_key, raw_path, repo_of_key, teamdoc_key, wiki_key, discussion_key
from collector.runner import FetchError, Item, Listing, Source, collect
from collector.store import LocalStore

T0 = datetime(2026, 11, 5, 3, 0, tzinfo=timezone.utc)


def clock_from(t):
    box = [t]
    def tick():
        box[0] += timedelta(seconds=1)
        return box[0]
    return tick


class FakeSource(Source):
    """목록 world 를 그대로 돌려주는 가짜 원천. world: {key: marker}."""
    name = "github-pr"

    def __init__(self, world, gone=(), broken=()):
        self.world, self.gone, self.broken = world, set(gone), set(broken)

    def list_items(self, ctx):
        return Listing([Item(k, {"updated_at": m}, group="r") for k, m in self.world.items()], {"r"})

    def only_items(self, ctx, values):
        return [Item(v, None, group="r") for v in values]

    def fetch(self, ctx, item):
        if item.key in self.broken:
            raise FetchError("http_503")
        return {"pull_request": {"updatedAt": self.world.get(item.key, "x")}}

    def index_fields(self, record):
        return {"updated_at": record["pull_request"]["updatedAt"]}

    def group_of(self, entry):
        return "r"

    def confirm_gone(self, ctx, entry):
        return "not_found" if entry["external_key"] in self.gone else None


_RUNS = [0]


def run(tmp_path, src, **kw):
    """실행마다 하루씩 뒤의 시각에서 시작한다(보고서 이름이 시각 순서가 되도록)."""
    _RUNS[0] += 1
    store = LocalStore(str(tmp_path))
    settings = Settings(store_url=str(tmp_path))
    rep = collect(settings, ["github-pr"], store=store, make_source=lambda n: src,
                  clock=clock_from(T0 + timedelta(days=_RUNS[0])), **kw)
    return store, rep


def test_paths_and_keys():
    assert raw_path("github-pr", "a#1") == "raw/github-pr/" + hashlib.sha1("a#1".encode()).hexdigest() + ".json"
    assert pr_key("2026-Chalkak", 900) == "woowacourse-teams/2026-Chalkak#900"
    assert teamdoc_key("2026-Chalkak", "README.md") == "woowacourse-teams/2026-Chalkak:README.md"
    assert wiki_key("2026-Harudle", "Home.md") == "woowacourse-teams/2026-Harudle:wiki/Home.md"
    assert discussion_key("2024-corea", 350) == "woowacourse-teams/2024-corea/discussions/350"
    for k in (pr_key("2026-Chalkak", 1), teamdoc_key("2026-Chalkak", "a/b.md"), wiki_key("2026-Chalkak", "x.md"),
              discussion_key("2026-Chalkak", 2)):
        assert repo_of_key(k) == "2026-Chalkak"


def test_blog_key_rules():
    sha = lambda u: "post:" + hashlib.sha1(u.encode()).hexdigest()
    assert blog_key("  http://WWW.Example.com/a/b/?z=1&utm_source=x&a=2&category=dev#top ") == sha("https://example.com/a/b?a=2&z=1")
    assert blog_key("https://example.com") == sha("https://example.com/")
    assert blog_key("https://velog.io/@t/%EB%AA%A9%EB%A1%9D") == sha("https://velog.io/@t/목록")
    assert blog_key("https://abc.tistory.com/m/6") == blog_key("https://abc.tistory.com/6")
    assert blog_key("https://example.com/m/6") == sha("https://example.com/m/6")


def test_first_run_then_daily(tmp_path):
    src = FakeSource({"k1": "t1", "k2": "t1"})
    store, rep = run(tmp_path, src)
    assert rep.status == "success"
    assert rep.sources["github-pr"].counts == {"new": 2, "changed": 0, "unchanged": 0, "gone": 0, "failed": 0}
    rec = store.read_json(raw_path("github-pr", "k1"))
    assert rec["version"] == 1 and rec["source"] == "github-pr" and rec["external_key"] == "k1"

    # 다음 날: k1 바뀜, k2 그대로, k3 새로, 사라진 키 없음
    src.world = {"k1": "t2", "k2": "t1", "k3": "t1"}
    store, rep = run(tmp_path, src)
    assert rep.sources["github-pr"].counts == {"new": 1, "changed": 1, "unchanged": 1, "gone": 0, "failed": 0}
    assert store.read_json(raw_path("github-pr", "k1"))["version"] == 2
    idx = Index(store, "github-pr").load()
    assert idx.get("k1")["updated_at"] == "t2" and idx.get("k1")["version"] == 2


def test_gone_only_when_confirmed_and_comes_back(tmp_path):
    src = FakeSource({"k1": "t1", "k2": "t1"})
    run(tmp_path, src)
    src.world, src.gone = {"k1": "t1"}, {"k2"}
    store, rep = run(tmp_path, src)
    sr = rep.sources["github-pr"]
    assert sr.counts["gone"] == 1 and sr.gone_keys == ["k2"]
    assert Index(store, "github-pr").load().get("k2")["gone"]["reason"] == "not_found"
    # 다시 나타나면 gone 을 지운다
    src.world, src.gone = {"k1": "t1", "k2": "t1"}, set()
    store, rep = run(tmp_path, src)
    assert Index(store, "github-pr").load().get("k2")["gone"] is None


def test_not_gone_if_confirm_says_still_there(tmp_path):
    src = FakeSource({"k1": "t1", "k2": "t1"})
    run(tmp_path, src)
    src.world = {"k1": "t1"}  # 목록에서 빠졌지만 확인하니 아직 있다
    store, rep = run(tmp_path, src)
    assert rep.sources["github-pr"].counts["gone"] == 0


def test_failures_and_consecutive(tmp_path):
    src = FakeSource({"k1": "t1"}, broken={"k1"})
    for n in (1, 2, 3):
        store, rep = run(tmp_path, src)
        f = rep.sources["github-pr"].failures[0]
        assert f["reason"] == "http_503" and f["consecutive"] == n
    assert rep.status == "partial_failure"
    assert rep.sources["github-pr"].failure_reasons == {"http_503": 1}
    assert Index(store, "github-pr").load().get("k1") is None  # 실패한 건은 아는 키가 아니다


def test_only_skips_listing_and_gone(tmp_path):
    src = FakeSource({"k1": "t1", "k2": "t1"})
    run(tmp_path, src)
    src.world, src.gone = {"k1": "t2"}, {"k2"}
    store, rep = run(tmp_path, src, only=["k1"])
    sr = rep.sources["github-pr"]
    assert sr.counts["changed"] == 1 and sr.counts["gone"] == 0
    assert rep.scope["only"] == ["k1"]


def test_list_failure_is_source_error(tmp_path):
    class Broken(FakeSource):
        def list_items(self, ctx):
            raise RuntimeError("목록 API 500")
    store, rep = run(tmp_path, Broken({}))
    sr = rep.sources["github-pr"]
    assert sr.errors and sr.errors[0]["type"] == "list_failed" and "500" in sr.errors[0]["message"]
    assert rep.status == "failure"


def test_lock_blocks_second_run_and_is_released(tmp_path):
    store = LocalStore(str(tmp_path))
    ok, _ = lock.acquire(store, "other", "ec2", T0 + timedelta(days=_RUNS[0] + 1))
    assert ok
    _, rep = run(tmp_path, FakeSource({"k1": "t1"}))
    assert rep.status == "skipped" and rep.scope["lock_held_by"]["run_id"] == "other"
    lock.release(store, "other")
    _, rep = run(tmp_path, FakeSource({"k1": "t1"}))
    assert rep.status == "success"
    assert store.read_bytes("state/lock.json") is None  # 끝나면 지운다


def test_stale_lock_is_taken_over(tmp_path):
    store = LocalStore(str(tmp_path))
    lock.acquire(store, "old", "ec2", T0 + timedelta(days=_RUNS[0] + 1) - timedelta(hours=13))
    _, rep = run(tmp_path, FakeSource({"k1": "t1"}))
    assert rep.status == "success"


def test_report_shape(tmp_path):
    store, rep = run(tmp_path, FakeSource({"k1": "t1"}))
    data = store.read_json(f"reports/{rep.run_id}.json")
    assert {"run_id", "trigger", "host", "code_version", "scope", "started_at", "finished_at", "duration_sec", "status", "sources"} <= set(data)
    s = data["sources"]["github-pr"]
    assert {"started_at", "finished_at", "duration_sec", "new", "changed", "unchanged", "gone", "failed",
            "gone_keys", "failures", "failure_reasons", "errors"} <= set(s)


def test_index_checkpoint(tmp_path):
    store = LocalStore(str(tmp_path))
    idx = Index(store, "github-pr", checkpoint=2).load()
    idx.put({"external_key": "a"})
    assert store.read_bytes("index/github-pr.jsonl") is None
    idx.put({"external_key": "b"})
    lines = store.read_bytes("index/github-pr.jsonl").decode().splitlines()
    assert [json.loads(l)["external_key"] for l in lines] == ["a", "b"]


def test_fetch_many_batches(tmp_path):
    calls = []

    class Batched(FakeSource):
        def fetch_many(self, ctx, items):
            for i in range(0, len(items), 2):
                chunk = items[i:i + 2]
                calls.append([it.key for it in chunk])
                for it in chunk:
                    yield it, {"pull_request": {"updatedAt": self.world[it.key]}}

    _, rep = run(tmp_path, Batched({"a": "1", "b": "1", "c": "1"}))
    assert calls == [["a", "b"], ["c"]]
    assert rep.sources["github-pr"].counts["new"] == 3


def test_partial_group_failure_makes_partial_status(tmp_path):
    class OneRepoFailed(FakeSource):
        def finish(self, ctx):
            ctx.report.extra["repos_failed"] = [{"repo": "x", "reason": "timeout"}]
    _, rep = run(tmp_path, OneRepoFailed({"k1": "t1"}))
    assert rep.status == "partial_failure"


def test_previous_failures_skips_command_reports(tmp_path):
    from collector.report import previous_failures
    store = LocalStore(str(tmp_path))
    store.write_json("reports/2026-11-01T00-00-00Z-ec2-aaaa.json",
                     {"scope": {}, "sources": {"youtube-tekotalk": {"failures": [{"external_key": "yt:a", "consecutive": 2}]}}})
    store.write_json("reports/2026-11-02T00-00-00Z-home-bbbb.json",
                     {"scope": {"command": "subtitles import"}, "sources": {"youtube-tekotalk": {"failures": []}}})
    assert previous_failures(store, "youtube-tekotalk") == {"yt:a": 2}


def test_graphql_waits_on_rate_limited_200():
    from collector.github import GitHub

    class R:
        def __init__(self, body, headers):
            self.status_code, self._b, self.headers, self.text = 200, body, headers, ""
        def json(self):
            return self._b

    class FakeHttp:
        def __init__(self):
            self.n = 0
        def request(self, method, url, **kw):
            self.n += 1
            if self.n == 1:
                return R({"errors": [{"type": "RATE_LIMITED", "message": "x"}]}, {"X-RateLimit-Reset": "130"})
            return R({"data": {"ok": 1}}, {})

    slept = []
    gh = GitHub("t", http=FakeHttp(), sleep=slept.append, clock=lambda: 100)
    assert gh.graphql("{ ok }") == {"ok": 1}
    assert slept == [31.0] and gh.waited_sec == 31.0


def test_unreadable_lock_skips_and_is_kept(tmp_path):
    store = LocalStore(str(tmp_path))
    store.write_bytes("state/lock.json", b"{broken")
    _, rep = run(tmp_path, FakeSource({"k1": "t1"}))
    assert rep.status == "skipped" and "unreadable" in rep.scope["lock_held_by"]
    assert store.read_bytes("state/lock.json") == b"{broken"  # 사람이 확인하도록 지우지 않는다


def test_local_create_exclusive_leaves_no_temp(tmp_path):
    store = LocalStore(str(tmp_path))
    assert store.create_exclusive("state/lock.json", b"{}")
    assert not store.create_exclusive("state/lock.json", b"{}")
    assert store.list("state") == ["state/lock.json"]
    assert [p.name for p in (tmp_path / "state").iterdir()] == ["lock.json"]


def test_broken_index_is_source_error_and_run_finishes(tmp_path):
    store = LocalStore(str(tmp_path))
    store.write_bytes("index/github-pr.jsonl", b"not json\n")
    _, rep = run(tmp_path, FakeSource({"k1": "t1"}))
    sr = rep.sources["github-pr"]
    assert sr.errors and sr.errors[0]["type"] == "unexpected"
    assert rep.status == "failure" and rep.finished is not None
    assert store.read_bytes("state/lock.json") is None


def test_index_saved_before_finish(tmp_path):
    seen = {}

    class Checks(FakeSource):
        def finish(self, ctx):
            seen["lines"] = (ctx.store.read_bytes("index/github-pr.jsonl") or b"").decode().count("\n")
            raise RuntimeError("상태 저장 실패")

    _, rep = run(tmp_path, Checks({"k1": "t1", "k2": "t2"}))
    assert seen["lines"] == 2
    assert rep.sources["github-pr"].errors[0]["type"] == "finish_failed"


def test_retry_after_accepts_http_date():
    from collector.http import retry_after_seconds
    now = datetime(2026, 11, 5, 3, 0, 0, tzinfo=timezone.utc)
    assert retry_after_seconds("120", now) == 120
    assert retry_after_seconds("Thu, 05 Nov 2026 03:00:30 GMT", now) == 30
    assert retry_after_seconds("Thu, 05 Nov 2026 02:00:00 GMT", now) == 0
    assert retry_after_seconds("soon", now) is None and retry_after_seconds(None, now) is None


def test_secondary_limit_without_retry_after_waits_then_gives_up():
    from collector.github import GitHub, GitHubError, SECONDARY_RETRIES

    class R:
        status_code, headers, text = 403, {}, '{"message": "You have exceeded a secondary rate limit"}'

    class Plain403(R):
        text = '{"message": "Resource not accessible"}'

    class FakeHttp:
        def __init__(self, resp):
            self.resp, self.n = resp, 0
        def request(self, method, url, **kw):
            self.n += 1
            return self.resp

    slept = []
    http = FakeHttp(R())
    gh = GitHub("t", http=http, sleep=slept.append, clock=lambda: 0)
    try:
        gh.rest("/x")
        assert False
    except GitHubError as exc:
        assert exc.status == 403
    assert slept == [60.0 * 2 ** i for i in range(SECONDARY_RETRIES)] and http.n == SECONDARY_RETRIES + 1

    slept.clear()
    http = FakeHttp(Plain403())
    gh = GitHub("t", http=http, sleep=slept.append, clock=lambda: 0)
    try:
        gh.rest("/x")
    except GitHubError:
        pass
    assert slept == [] and http.n == 1  # 보통 403 은 기다리지 않는다


def test_http_max_bytes_stops_reading():
    import requests
    from collector.http import Http, ResponseTooLarge

    def make(body, headers=None):
        r = requests.Response()
        r.status_code, r.url = 200, "https://a.example/x"
        r.headers.update(headers or {})
        r.raw = __import__("io").BytesIO(body)
        return r

    class Session:
        def __init__(self, resp):
            self.headers, self.resp, self.kw = {}, resp, None
        def request(self, method, url, **kw):
            self.kw = kw
            return self.resp

    s = Session(make(b"x" * 10))
    r = Http(session=s, max_bytes=10).get("https://a.example/x")
    assert r.content == b"x" * 10 and r.text == "x" * 10 and s.kw["stream"] is True
    for resp in (make(b"x" * 11), make(b"", {"Content-Length": "999"})):
        try:
            Http(session=Session(resp), max_bytes=10).get("https://a.example/x")
            assert False
        except ResponseTooLarge:
            pass


def test_default_sources_are_registered():
    from collector import sources
    from collector.config import SOURCES
    for name in SOURCES:
        assert sources.make(name).name == name


def test_settings_require_pipeline_store(monkeypatch):
    monkeypatch.delenv("PIPELINE_STORE", raising=False)
    try:
        Settings.from_env()
        assert False
    except RuntimeError as exc:
        assert "PIPELINE_STORE" in str(exc)
    monkeypatch.setenv("PIPELINE_STORE", "  ")
    try:
        Settings.from_env()
        assert False
    except RuntimeError:
        pass
    monkeypatch.setenv("PIPELINE_STORE", "./somewhere")
    assert Settings.from_env().store_url == "./somewhere"
