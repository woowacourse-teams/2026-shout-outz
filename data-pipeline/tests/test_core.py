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


def test_interrupted_run_finishes_report_saves_index_and_releases_lock(tmp_path):
    class Interrupted(FakeSource):
        def fetch(self, ctx, item):
            if item.key == "k2":
                raise KeyboardInterrupt("signal 15")
            return super().fetch(ctx, item)

    store = LocalStore(str(tmp_path))
    try:
        run(tmp_path, Interrupted({"k1": "t1", "k2": "t2", "k3": "t3"}))
        assert False
    except KeyboardInterrupt:
        pass
    reports = store.list("reports/")
    data = store.read_json(reports[-1])
    assert data["status"] == "failure" and "finished_at" in data
    assert data["sources"]["github-pr"]["errors"][0]["type"] == "interrupted"
    assert store.read_bytes("state/lock.json") is None
    assert Index(store, "github-pr").load().get("k1") is not None  # 받은 데까지 색인에 남는다
    _, rep = run(tmp_path, FakeSource({"k1": "t1", "k2": "t2", "k3": "t3"}))
    assert rep.sources["github-pr"].counts["new"] == 2 and rep.sources["github-pr"].counts["unchanged"] == 1


# 모니터링 칸(phase, current, progress, timing, requests, waits, slowest, peak_rss, log_tail, 중간 저장)

class ManualClock:
    """원천이 직접 시간을 넘기는 시계(부를 때마다 흐르지 않는다)."""

    def __init__(self, t):
        self.t = t

    def __call__(self):
        return self.t

    def advance(self, sec):
        self.t += timedelta(seconds=sec)


class SnapshotStore(LocalStore):
    """보고서를 쓸 때마다 그 모양을 남긴다. fail 이 참을 돌려주면 그 보고서 쓰기를 실패시킨다."""

    def __init__(self, root, fail=None):
        super().__init__(root)
        self.snaps, self.fail = [], fail

    def write_bytes(self, path, data, content_type="application/json"):
        if path.startswith("reports/"):
            snap = json.loads(data.decode("utf-8"))
            if self.fail and self.fail(snap):
                raise OSError("S3 PutObject 500")
            self.snaps.append(snap)
        super().write_bytes(path, data, content_type)


def run_mon(tmp_path, src, clock, store=None, **kw):
    _RUNS[0] += 1
    store = store or SnapshotStore(str(tmp_path))
    rep = collect(Settings(store_url=str(tmp_path)), ["github-pr"], store=store, make_source=lambda n: src,
                  clock=clock, **kw)
    return store, rep, rep.sources["github-pr"]


class TimedSource(FakeSource):
    """묶음마다 목록에 list_sec, 한 건 받기에 fetch_sec 초가 걸리는 원천. groups: {묶음: [키...]}."""

    def __init__(self, clock, groups, list_sec=10, fetch_sec=5, gone_sec=0, finish_sec=0):
        super().__init__({k: "t1" for ks in groups.values() for k in ks})
        self.clock, self.groups = clock, groups
        self.list_sec, self.fetch_sec, self.gone_sec, self.finish_sec = list_sec, fetch_sec, gone_sec, finish_sec
        self.group_of_key = {k: g for g, ks in groups.items() for k in ks}

    def list_items(self, ctx):
        items = []
        for g, keys in self.groups.items():
            ctx.begin_group(g)
            self.clock.advance(self.list_sec(g) if callable(self.list_sec) else self.list_sec)
            items += [Item(k, {"updated_at": "t1"}, group=g) for k in keys]
        return Listing(items, set(self.groups))

    def fetch(self, ctx, item):
        self.clock.advance(self.fetch_sec)
        return super().fetch(ctx, item)

    def group_of(self, entry):
        return self.group_of_key.get(entry["external_key"], "gone-repo")

    def confirm_gone(self, ctx, entry):
        self.clock.advance(self.gone_sec)
        return None

    def finish(self, ctx):
        self.clock.advance(self.finish_sec)


def test_report_saved_on_phase_change_and_every_60_seconds(tmp_path):
    clock = ManualClock(T0 + timedelta(days=_RUNS[0] + 1))
    src = TimedSource(clock, {"r1": [], "r2": [f"k{i}" for i in range(10)]}, list_sec=70, fetch_sec=25)
    store, rep, sr = run_mon(tmp_path, src, clock)
    snaps = [s["sources"]["github-pr"] for s in store.snaps if "github-pr" in s["sources"]]
    phases = [s["phase"] for s in snaps]
    # 단계가 바뀔 때마다 저장하고, 끝나면 done
    for p in ("list", "fetch", "gone", "finish"):
        assert p in phases
    assert phases[-1] == "done" and snaps[-1]["current"] is None
    assert phases == sorted(phases, key=["list", "fetch", "gone", "finish", "done"].index)
    # 목록 받기 중: r1 이 70초 걸려 r2 를 시작할 때 저장되고, current 는 r2
    assert any(s["phase"] == "list" and s["current"] == "r2" for s in snaps)
    # 받기 중: 25초씩 10건(250초). 60초가 지날 때마다만 저장(3건마다 = 75초)
    mid = [s for s in store.snaps if s["sources"] and s["sources"]["github-pr"]["phase"] == "fetch"]
    fetched = [s["sources"]["github-pr"]["progress"]["fetched"] for s in mid]
    assert fetched == [0, 3, 6, 9]
    times = [datetime.fromisoformat(s["updated_at"].replace("Z", "+00:00")) for s in mid]
    assert all(b - a >= timedelta(seconds=60) for a, b in zip(times, times[1:]))
    assert mid[1]["sources"]["github-pr"]["current"] == "r2"
    assert snaps[-1]["progress"] == {"listed": 10, "to_fetch": 10, "fetched": 10}


def test_report_not_saved_more_often_than_60_seconds(tmp_path):
    clock = ManualClock(T0 + timedelta(days=_RUNS[0] + 1))
    src = TimedSource(clock, {"r": [f"k{i}" for i in range(30)]}, list_sec=1, fetch_sec=1)  # 받기 30초
    store, _, _ = run_mon(tmp_path, src, clock)
    # 원천 끝(done)과 실행 끝(status 확정)에 한 번씩 쓰는 것은 원래대로
    assert [s["sources"]["github-pr"]["phase"] for s in store.snaps if s["sources"]] == ["list", "fetch", "gone", "finish", "done", "done"]


def test_timing_adds_up_to_duration(tmp_path):
    clock = ManualClock(T0 + timedelta(days=_RUNS[0] + 1))
    src = TimedSource(clock, {"a": ["k1", "k2"], "b": ["k3"]}, list_sec=10, fetch_sec=5, gone_sec=7, finish_sec=3)
    run_mon(tmp_path, src, clock)
    src.groups = {"a": ["k1"], "b": ["k3"]}  # k2 가 목록에서 빠져 사라짐 확인
    src.world = {"k1": "t1", "k3": "t1"}
    store, rep, sr = run_mon(tmp_path, src, clock)
    data = store.read_json(f"reports/{rep.run_id}.json")["sources"]["github-pr"]
    assert data["timing"] == {"list_sec": 20.0, "fetch_sec": 0.0, "gone_sec": 7.0, "finish_sec": 3.0}
    assert abs(sum(data["timing"].values()) - data["duration_sec"]) <= 1.0
    # 첫 실행: 받기 3건
    first = store.read_json(sorted(store.list("reports/"))[-2])["sources"]["github-pr"]
    assert first["timing"]["fetch_sec"] == 15.0
    assert abs(sum(first["timing"].values()) - first["duration_sec"]) <= 1.0


def test_requests_and_waits_from_http_and_github(tmp_path):
    from types import SimpleNamespace as NS
    http = NS(calls=5, polite_sec=1.5, retry_sec=2.0)
    gh_http = NS(calls=10, polite_sec=0.0, retry_sec=1.0)
    gh = NS(rest_calls=3, graphql_points=7, waited_sec=4.0, http=gh_http)

    class WithClients(FakeSource):
        def __init__(self, world):
            super().__init__(world)
            self._http, self._github = http, gh

        def list_items(self, ctx):
            ctx._http = http  # 원천과 Context 가 같은 객체를 가져도 한 번만 센다
            ctx._github = gh
            return super().list_items(ctx)

    _, _, sr = run_mon(tmp_path, WithClients({"k1": "t1"}), clock_from(T0 + timedelta(days=_RUNS[0] + 1)))
    data = sr.to_json()
    assert data["requests"] == {"http": 15, "github_rest": 3, "github_graphql_points": 7}
    assert data["waits"] == {"rate_limit_sec": 4.0, "polite_sec": 1.5, "retry_sec": 3.0}

    # 가짜에 칸이 없거나 숫자가 아니면 0
    _, _, sr = run_mon(tmp_path, FakeSource({"k1": "t1"}), clock_from(T0 + timedelta(days=_RUNS[0] + 1)))
    assert sr.to_json()["requests"] == {"http": 0, "github_rest": 0, "github_graphql_points": 0}
    assert sr.to_json()["waits"] == {"rate_limit_sec": 0.0, "polite_sec": 0.0, "retry_sec": 0.0}


def test_http_counts_polite_and_retry_waits():
    from collector.http import Http

    class R:
        def __init__(self, status):
            self.status_code, self.headers = status, {}
        def close(self):
            pass

    class Session:
        def __init__(self, statuses):
            self.headers, self.statuses = {}, list(statuses)
        def request(self, method, url, **kw):
            return R(self.statuses.pop(0))

    slept = []
    h = Http(host_delay=1.0, sleep=slept.append, session=Session([200, 200]))
    h.get("https://a.example/1")
    h.get("https://a.example/2")
    assert h.calls == 2 and 0.9 < h.polite_sec <= 1.0 and h.retry_sec == 0.0

    h = Http(sleep=slept.append, session=Session([503, 502, 200]))
    assert h.get("https://b.example/").status_code == 200
    assert h.calls == 3 and h.retry_sec == 2 + 4 and h.polite_sec == 0.0


def test_slowest_top_10_by_group(tmp_path):
    clock = ManualClock(T0 + timedelta(days=_RUNS[0] + 1))
    groups = {f"r{i:02d}": [f"k{i}"] for i in range(12)}
    src = TimedSource(clock, groups, list_sec=lambda g: int(g[1:]), fetch_sec=100)
    src.groups["r00"] = ["k0", "k0b", "k0c"]  # r00 은 목록 0초, 받기 3건 300초
    src.world.update({"k0b": "t1", "k0c": "t1"})
    src.group_of_key.update({"k0b": "r00", "k0c": "r00"})
    _, _, sr = run_mon(tmp_path, src, clock)
    top = sr.to_json()["slowest"]
    assert len(top) == 10
    assert top[0] == {"group": "r00", "sec": 300.0, "docs": 3}
    assert top[1] == {"group": "r11", "sec": 111.0, "docs": 1}
    assert [t["sec"] for t in top] == sorted((t["sec"] for t in top), reverse=True)
    assert {"r01", "r02"}.isdisjoint(t["group"] for t in top)


def test_log_tail_keeps_last_50_warnings(tmp_path):
    import logging
    lg = logging.getLogger("collector.test")

    class Noisy(FakeSource):
        def list_items(self, ctx):
            lg.info("정보는 남기지 않음")
            for i in range(60):
                lg.warning("경고 %d", i)
            lg.warning("긴 경고 " + "가" * 1000)
            try:
                raise ValueError("나쁜 값")
            except ValueError:
                lg.exception("목록 일부 실패")
            return super().list_items(ctx)

    _, _, sr = run_mon(tmp_path, Noisy({"k1": "t1"}), clock_from(T0 + timedelta(days=_RUNS[0] + 1)))
    tail = sr.to_json()["log_tail"]
    assert len(tail) == 50
    assert tail[0]["message"] == "경고 12" and tail[0]["level"] == "WARNING"
    assert len(tail[-2]["message"]) == 500
    last = tail[-1]
    assert last["level"] == "ERROR" and last["message"].startswith("목록 일부 실패 | ")
    assert "ValueError: 나쁜 값" in last["message"] and "test_core.py" in last["message"]
    assert len(last["at"]) == 20 and last["at"].endswith("Z")
    # 다음 원천, 다음 실행에는 섞이지 않는다
    _, _, sr = run_mon(tmp_path, FakeSource({"k1": "t1"}), clock_from(T0 + timedelta(days=_RUNS[0] + 1)))
    assert sr.to_json()["log_tail"] == []


def test_peak_rss_and_run_fields(tmp_path):
    store, rep, sr = run_mon(tmp_path, FakeSource({"k1": "t1"}), clock_from(T0 + timedelta(days=_RUNS[0] + 1)))
    data = store.read_json(f"reports/{rep.run_id}.json")
    s = data["sources"]["github-pr"]
    assert s["peak_rss_mb"] > 0 and s["peak_rss_children_mb"] >= 0
    assert data["peak_rss_mb"] > 0 and data["updated_at"] >= data["finished_at"]
    assert {"phase", "current", "progress", "timing", "requests", "waits", "slowest", "peak_rss_mb",
            "peak_rss_children_mb", "log_tail"} <= set(s)


def test_mid_run_save_failure_does_not_stop_collection(tmp_path):
    clock = ManualClock(T0 + timedelta(days=_RUNS[0] + 1))
    src = TimedSource(clock, {"r": [f"k{i}" for i in range(6)]}, list_sec=1, fetch_sec=30)
    store = SnapshotStore(str(tmp_path), fail=lambda s: (s["sources"].get("github-pr") or {}).get("phase") in ("fetch", "gone", "finish"))
    store, rep, sr = run_mon(tmp_path, src, clock, store=store)
    assert sr.counts["new"] == 6 and rep.status == "success"
    final = store.read_json(f"reports/{rep.run_id}.json")["sources"]["github-pr"]
    assert final["phase"] == "done"
    assert any("보고서 중간 저장 실패" in r["message"] and "S3 PutObject 500" in r["message"] for r in final["log_tail"])
