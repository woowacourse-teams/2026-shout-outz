"""블로그 수집(03 문서) 시험. 네트워크 없이 가짜 Http 로 돌린다."""

import json
from datetime import datetime, timedelta, timezone

import requests

from collector.config import Settings
from collector.index import Index
from collector.keys import blog_key, raw_path
from collector.runner import collect
from collector.sources import blog as blog_mod
from collector.sources.blog import BlogSource, Target, group_blogs
from collector.sources.blog_platforms import (detect, shell_page_reason, find_feed_links, is_rilog_post, is_tistory_post, parse_feed,
                                              parse_sitemap)
from collector.store import LocalStore

DAY0 = datetime(2026, 11, 5, 3, 0, tzinfo=timezone.utc)
SRC = "blog-post"


class Resp:
    def __init__(self, status=200, body=b"", url="", headers=None):
        self.status_code = status
        self.content = body.encode("utf-8") if isinstance(body, str) else body
        self.url = url
        self.headers = headers or {}

    @property
    def text(self):
        return self.content.decode("utf-8")

    def json(self):
        return json.loads(self.content)


class FakeHttp:
    """주소 → 응답. 값은 Resp, 글(200 본문), 함수(요청 본문을 받아 Resp), 예외. 없는 주소는 404."""

    def __init__(self, routes):
        self.routes = dict(routes)
        self.calls = []

    def _answer(self, method, url, payload=None):
        self.calls.append((method, url))
        v = self.routes.get(url)
        if v is None:
            return Resp(404, "", url)
        if isinstance(v, Exception):
            raise v
        if callable(v):
            v = v(payload)
        if isinstance(v, Resp):
            v.url = v.url or url
            return v
        return Resp(200, v, url)

    def get(self, url, **kw):
        return self._answer("GET", url)

    def post(self, url, json=None, **kw):
        return self._answer("POST", url, json)

    def count(self, url, method="GET"):
        return sum(1 for m, u in self.calls if m == method and u == url)


def rss(*links):
    items = "".join(f"<item><title>t</title><link>{u}</link></item>" for u in links)
    return f'<?xml version="1.0" encoding="UTF-8"?><rss version="2.0"><channel><title>b</title>{items}</channel></rss>'


def sitemap(*locs):
    urls = "".join(f"<url><loc>{u}</loc><lastmod>2026-11-01</lastmod></url>" for u in locs)
    return f'<?xml version="1.0" encoding="UTF-8"?><urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">{urls}</urlset>'


def run(tmp_path, http, targets, day, only=None):
    store = LocalStore(str(tmp_path))
    src = BlogSource(http=http, targets_loader=lambda settings: targets)
    t = [DAY0 + timedelta(days=day)]

    def clock():
        t[0] += timedelta(seconds=1)
        return t[0]

    rep = collect(Settings(store_url=str(tmp_path)), [SRC], only=only, store=store, make_source=lambda n: src, clock=clock)
    return store, rep.sources[SRC], rep


def state(store):
    return store.read_json("state/blog-post.json")["blogs"]


# ---- 플랫폼 규칙 ----

def test_detect_platform_and_root():
    assert detect("https://abc.tistory.com/m/6").root == "https://abc.tistory.com"
    v = detect("velog.io/@dev/글")
    assert (v.name, v.root, v.ident) == ("velog", "https://velog.io/@dev", "dev")
    r = detect("https://rilog.kr/@team/posts/3")
    assert (r.name, r.root, r.ident) == ("rilog", "https://www.rilog.kr/@team", "team")
    assert detect("https://www.rilog.kr/@team").root == "https://www.rilog.kr/@team"
    g = detect("https://Team.github.io/about/")
    assert (g.name, g.root, g.unsupported) == ("generic", "https://team.github.io", None)
    assert detect("https://team.notion.site/x").unsupported
    assert detect("https://medium.com/@me").unsupported
    assert detect("https://velog.io/").unsupported


def test_feed_and_sitemap_parsers():
    atom = ('<feed xmlns="http://www.w3.org/2005/Atom"><entry><link rel="alternate" href="/p/1"/></entry>'
            '<entry><link href="https://b.io/p/2"/></entry></feed>')
    assert parse_feed(atom.encode(), "https://b.io/atom.xml") == ["https://b.io/p/1", "https://b.io/p/2"]
    jf = json.dumps({"version": "https://jsonfeed.org/version/1.1", "items": [{"url": "https://b.io/p/3"}]})
    assert parse_feed(jf.encode(), "https://b.io/feed.json") == ["https://b.io/p/3"]
    assert parse_feed(b"<html><body>not a feed</body></html>", "https://b.io/") is None
    assert parse_feed(b'{"name": "wp-json"}', "https://b.io/") is None
    pages, kids = parse_sitemap(sitemap("https://a.tistory.com/1", "https://a.tistory.com/category").encode())
    assert pages == ["https://a.tistory.com/1", "https://a.tistory.com/category"] and kids == []
    assert [is_tistory_post(u) for u in ("https://a.tistory.com/12", "https://a.tistory.com/entry/%EA%B8%80",
                                         "https://a.tistory.com/category/x", "https://a.tistory.com/tag/y",
                                         "https://a.tistory.com/guestbook")] == [True, True, False, False, False]
    assert is_rilog_post("https://www.rilog.kr/@team/posts/3", "team")
    assert not is_rilog_post("https://www.rilog.kr/@other/posts/3", "team")
    assert not is_rilog_post("https://www.rilog.kr/@team", "team")
    html = ('<html><head><link rel="stylesheet" href="/a.css">'
            '<link rel="alternate" type="application/rss+xml" href="/feed.xml"></head></html>')
    assert find_feed_links(html, "https://b.io/") == ["https://b.io/feed.xml"]


# ---- tistory: 전체 목록 받는 날, 새 글만, 출처, /m 과 인코딩 ----

TIS = "https://abc.tistory.com"
TARGETS_TIS = [Target(1, "https://abc.tistory.com/m/6", "https://github.com/woowacourse-teams/2026-a"),
               Target(2, "abc.tistory.com", "https://github.com/woowacourse-teams/2026-a")]


def page(s):
    """글 페이지(og:title 이 있는 HTML). 빈 껍데기 확인(03 문서 5-3)을 지나도록."""
    return f'<html><head><meta property="og:title" content="t"></head>{s}</html>'


def tistory_http(sitemap_posts, rss_posts, gone=()):
    routes = {
        f"{TIS}/sitemap.xml": sitemap(*(sitemap_posts + [f"{TIS}/category", f"{TIS}/tag/x", f"{TIS}/guestbook"])),
        f"{TIS}/rss": rss(*rss_posts),
    }
    for u in set(sitemap_posts) | set(rss_posts):
        routes[u] = page(u)
    for u in gone:
        routes[u] = Resp(404)
    return FakeHttp(routes)


def test_full_list_day_rule_and_new_posts_only(tmp_path):
    posts = [f"{TIS}/1", f"{TIS}/2"]
    http = tistory_http(posts, [f"{TIS}/2"])
    store, sr, _ = run(tmp_path, http, TARGETS_TIS, day=0)
    # 기록 없음 → 전체 목록(sitemap), 카테고리, 태그 주소는 고르지 않는다
    assert http.count(f"{TIS}/sitemap.xml") == 1 and http.count(f"{TIS}/rss") == 0
    assert sr.counts["new"] == 2 and sr.extra["full_list_blogs"] == [TIS]
    assert state(store)[TIS]["last_full_list"] == "2026-11-05"
    rec = store.read_json(raw_path(SRC, blog_key(f"{TIS}/1")))
    assert rec["body_format"] == "html" and rec["platform"] == "tistory" and rec["body"] == page(f"{TIS}/1")
    # 같은 블로그가 두 행에 있다 → 한 번만 받고 출처 둘
    assert [f["blog_source_id"] for f in rec["found_in"]] == [1, 2]
    assert http.count(f"{TIS}/1") == 1
    entry = Index(store, SRC).load().get(blog_key(f"{TIS}/1"))
    assert entry["url"] == f"{TIS}/1" and entry["blog_root"] == TIS

    # 3일 뒤 → 피드만. 이미 받은 글(2)은 다시 받지 않는다
    http = tistory_http(posts, [f"{TIS}/2", f"{TIS}/3"])
    store, sr, _ = run(tmp_path, http, TARGETS_TIS, day=3)
    assert http.count(f"{TIS}/sitemap.xml") == 0 and http.count(f"{TIS}/rss") == 1
    assert sr.counts["new"] == 1 and sr.counts["unchanged"] == 1
    assert http.count(f"{TIS}/2") == 0 and http.count(f"{TIS}/1") == 0
    assert sr.extra["full_list_blogs"] == []
    assert state(store)[TIS]["last_full_list"] == "2026-11-05"

    # 8일 뒤 → 다시 전체 목록
    http = tistory_http(posts + [f"{TIS}/3"], [])
    store, sr, _ = run(tmp_path, http, TARGETS_TIS, day=8)
    assert http.count(f"{TIS}/sitemap.xml") == 1
    assert sr.counts["unchanged"] == 3 and sr.counts["new"] == 0
    assert state(store)[TIS]["last_full_list"] == "2026-11-13"


def test_gone_only_on_full_list_day_with_404(tmp_path):
    run(tmp_path, tistory_http([f"{TIS}/1", f"{TIS}/2", f"{TIS}/3"], []), TARGETS_TIS, day=0)
    # 3일 뒤(피드 날): 2 가 목록에 없고 404 여도 사라짐 확인을 하지 않는다
    http = tistory_http([], [f"{TIS}/1"], gone=[f"{TIS}/2"])
    store, sr, _ = run(tmp_path, http, TARGETS_TIS, day=3)
    assert sr.counts["gone"] == 0 and http.count(f"{TIS}/2") == 0
    # 8일 뒤(전체 목록 날): 2 는 404 → 사라짐, 3 은 목록에서 빠졌지만 200 → 그대로
    http = tistory_http([f"{TIS}/1"], [], gone=[f"{TIS}/2"])
    http.routes[f"{TIS}/3"] = "<html>still</html>"
    store, sr, _ = run(tmp_path, http, TARGETS_TIS, day=8)
    assert sr.gone_keys == [blog_key(f"{TIS}/2")]
    idx = Index(store, SRC).load()
    assert idx.get(blog_key(f"{TIS}/2"))["gone"]["reason"] == "http_404"
    assert idx.get(blog_key(f"{TIS}/3"))["gone"] is None


def test_blog_dropped_from_targets_is_not_checked(tmp_path):
    run(tmp_path, tistory_http([f"{TIS}/1"], []), TARGETS_TIS, day=0)
    http = tistory_http([], [], gone=[f"{TIS}/1"])
    store, sr, _ = run(tmp_path, http, [], day=8)  # 팀이 주소를 지웠다(DISABLED)
    assert sr.counts["gone"] == 0 and http.count(f"{TIS}/1") == 0


# ---- velog ----

VEL = "https://velog.io/@dev"
TARGETS_VEL = [Target(3, "https://velog.io/@dev/posts", None)]
ENC = "%EB%AA%A9%EB%A1%9D"  # "목록"


def velog_http(graphql_ok=True):
    def graphql(payload):
        if not graphql_ok:
            return Resp(200, json.dumps({"errors": [{"message": "boom"}]}))
        cursor = payload["variables"]["cursor"]
        pages = {None: [{"id": "a", "url_slug": "목록", "user": {"username": "dev"}},
                        {"id": "b", "url_slug": "two", "user": {"username": "dev"}}],
                 "b": []}
        return Resp(200, json.dumps({"data": {"posts": pages[cursor]}}))
    return FakeHttp({
        "https://v2.velog.io/graphql": graphql,
        "https://v2.velog.io/rss/@dev": rss(f"{VEL}/{ENC}", f"{VEL}/three"),
        f"{VEL}/목록": page(1), f"{VEL}/{ENC}": page(1),
        f"{VEL}/two": page(2), f"{VEL}/three": page(3),
    })


def test_velog_graphql_then_rss_dedupes_encoded_url(tmp_path):
    http = velog_http()
    store, sr, _ = run(tmp_path, http, TARGETS_VEL, day=0)
    assert http.count("https://v2.velog.io/graphql", "POST") == 2  # 빈 페이지가 올 때까지
    assert sr.counts["new"] == 2 and sr.extra["notices"] == []
    # 3일 뒤 RSS 는 한글 주소를 인코딩해서 준다 → 같은 키라 다시 받지 않는다
    http = velog_http()
    store, sr, _ = run(tmp_path, http, TARGETS_VEL, day=3)
    assert http.count("https://v2.velog.io/graphql", "POST") == 0
    assert sr.counts == {"new": 1, "changed": 0, "unchanged": 1, "gone": 0, "failed": 0}
    assert http.count(f"{VEL}/{ENC}") == 0


def test_velog_graphql_failure_uses_rss_with_notice(tmp_path):
    http = velog_http(graphql_ok=False)
    store, sr, _ = run(tmp_path, http, TARGETS_VEL, day=0)
    assert http.count("https://v2.velog.io/rss/@dev") == 1
    assert sr.counts["new"] == 2
    assert [n["type"] for n in sr.extra["notices"]] == ["full_list_failed"]
    assert sr.extra["notices"][0]["blog"] == VEL
    assert sr.extra["full_list_blogs"] == []
    assert "last_full_list" not in state(store)[VEL]  # 다음 실행이 다시 전체 목록을 시도한다


# ---- rilog ----

RIL = "https://www.rilog.kr/@team"


def test_rilog_daily_sitemap_markdown_and_gone(tmp_path):
    targets = [Target(4, "https://rilog.kr/@team", "https://github.com/woowacourse-teams/2026-r")]
    routes = {
        "https://www.rilog.kr/sitemap.xml": sitemap(f"{RIL}/posts/1", f"{RIL}/posts/2", "https://www.rilog.kr/@other/posts/9",
                                                    RIL, "https://www.rilog.kr/about"),
        f"{RIL}/posts/1/markdown": '---\ntitle: "제목"\n---\n본문',
        f"{RIL}/posts/2/markdown": '---\ntitle: "둘"\n---\n',
    }
    http = FakeHttp(routes)
    store, sr, _ = run(tmp_path, http, targets, day=0)
    assert sr.counts["new"] == 2
    rec = store.read_json(raw_path(SRC, blog_key(f"{RIL}/posts/1")))
    assert rec["body_format"] == "markdown" and rec["body"] == '---\ntitle: "제목"\n---\n본문' and rec["url"] == f"{RIL}/posts/1"
    # 다음 날도 전체 목록(매일). 2 가 sitemap 에서 빠지고 404 → 사라짐
    routes["https://www.rilog.kr/sitemap.xml"] = sitemap(f"{RIL}/posts/1")
    routes[f"{RIL}/posts/2"] = Resp(410)
    http = FakeHttp(routes)
    store, sr, _ = run(tmp_path, http, targets, day=1)
    assert sr.extra["full_list_blogs"] == [RIL]
    assert sr.gone_keys == [blog_key(f"{RIL}/posts/2")]
    assert Index(store, SRC).load().get(blog_key(f"{RIL}/posts/2"))["gone"]["reason"] == "http_410"


# ---- 범용 ----

GEN = "https://team.github.io"


def generic_routes():
    return {
        f"{GEN}/": '<html><head><link rel="alternate" type="application/atom+xml" href="/atom.xml"></head></html>',
        f"{GEN}/atom.xml": ('<feed xmlns="http://www.w3.org/2005/Atom"><entry><link href="/2026/a/"/></entry></feed>'),
        f"{GEN}/2026/a/": "<html>a</html>",
    }


def test_generic_feed_discovery_saved_in_state(tmp_path):
    targets = [Target(5, "https://team.github.io/about", None)]
    http = FakeHttp(generic_routes())
    store, sr, _ = run(tmp_path, http, targets, day=0)
    assert sr.counts["new"] == 1
    assert state(store)[GEN] == {"feed_url": f"{GEN}/atom.xml", "last_full_list": "2026-11-05"}
    assert sr.extra["feed_only_blogs"] == [GEN] and sr.extra["full_list_blogs"] == [GEN]
    # 다음 날: 남겨 둔 피드 주소를 바로 쓴다(첫 화면을 다시 보지 않는다), 7일이 안 지나 사라짐 확인도 안 한다
    http = FakeHttp(generic_routes())
    store, sr, _ = run(tmp_path, http, targets, day=1)
    assert http.count(f"{GEN}/") == 0 and http.count(f"{GEN}/atom.xml") == 1
    assert sr.extra["full_list_blogs"] == []


def test_unsupported_generic_and_notion(tmp_path):
    targets = [Target(6, "https://nofeed.example.com", None), Target(7, "https://team.notion.site/blog", None)]
    http = FakeHttp({"https://nofeed.example.com/": "<html><head></head></html>"})
    store, sr, rep = run(tmp_path, http, targets, day=0)
    assert sr.extra["unsupported"] == 2 and sr.extra["targets"] == 2
    assert sorted(n["type"] for n in sr.extra["notices"]) == ["unsupported", "unsupported"]
    # 흔한 경로를 모두 봤다
    for p in ("/feed", "/rss", "/rss.xml", "/feed.xml", "/atom.xml", "/index.xml", "/feed.json"):
        assert http.count("https://nofeed.example.com" + p) == 1
    assert not any("notion" in u for _, u in http.calls)
    assert sr.counts["new"] == 0 and rep.status == "success"


# ---- 예의, 실패 ----

def test_robots_disallow_blocks_fetch(tmp_path):
    http = tistory_http([f"{TIS}/1", f"{TIS}/2"], [])
    http.routes[f"{TIS}/robots.txt"] = "User-agent: *\nDisallow: /2\n"
    store, sr, _ = run(tmp_path, http, TARGETS_TIS, day=0)
    assert http.count(f"{TIS}/robots.txt") == 1  # 호스트마다 한 번
    assert sr.counts["new"] == 1 and sr.counts["failed"] == 1
    assert sr.failures[0]["reason"] == "robots_disallow" and sr.failures[0]["url"] == f"{TIS}/2"
    assert http.count(f"{TIS}/2") == 0


def test_fetch_failure_reasons(tmp_path):
    http = tistory_http([f"{TIS}/1", f"{TIS}/2"], [])
    http.routes[f"{TIS}/1"] = Resp(403)
    http.routes[f"{TIS}/2"] = requests.Timeout("slow")
    store, sr, _ = run(tmp_path, http, TARGETS_TIS, day=0)
    assert sr.failure_reasons == {"http_403": 1, "timeout": 1}
    assert Index(store, SRC).load().get(blog_key(f"{TIS}/1")) is None  # 실패한 글은 다음 실행이 다시 받는다


def test_one_blog_listing_failure_does_not_stop_others(tmp_path):
    targets = TARGETS_TIS + [Target(9, "https://broken.tistory.com", None)]
    http = tistory_http([f"{TIS}/1"], [])  # broken 블로그는 sitemap, rss 모두 404
    store, sr, _ = run(tmp_path, http, targets, day=0)
    assert sr.counts["new"] == 1
    types = [n["type"] for n in sr.extra["notices"]]
    assert types == ["full_list_failed", "list_failed"]


def test_same_post_from_two_blogs_rows_merges_found_in(tmp_path):
    # 서로 다른 행이 같은 블로그를 다른 주소 모양으로 등록(목록 1과 목록 2)
    targets = [Target(1, "https://abc.tistory.com/m/6", "https://github.com/woowacourse-teams/2026-a"),
               Target(2, "https://abc.tistory.com/category/dev", "https://github.com/woowacourse-teams/2026-b")]
    blogs = group_blogs(targets)
    assert list(blogs) == [TIS] and [t.id for t in blogs[TIS].targets] == [1, 2]
    http = tistory_http([f"{TIS}/1"], [])
    store, sr, _ = run(tmp_path, http, targets, day=0)
    rec = store.read_json(raw_path(SRC, blog_key(f"{TIS}/1")))
    assert rec["found_in"] == [{"blog_source_id": 1, "github_repository_url": "https://github.com/woowacourse-teams/2026-a"},
                               {"blog_source_id": 2, "github_repository_url": "https://github.com/woowacourse-teams/2026-b"}]


# ---- --only ----

def test_only_url_resolves_found_in_from_targets(tmp_path):
    http = tistory_http([f"{TIS}/1"], [f"{TIS}/1"])
    http.routes[f"{TIS}/m/7"] = page(7)  # 받는 주소도 키도 준 그대로(CLAUDE.md 6단계는 /m 을 떼지 않는다)
    store, sr, rep = run(tmp_path, http, TARGETS_TIS, day=0, only=[f"{TIS}/m/7"])
    assert http.count(f"{TIS}/sitemap.xml") == 0 and http.count(f"{TIS}/rss") == 0  # 목록은 받지 않는다
    assert sr.counts["new"] == 1
    rec = store.read_json(raw_path(SRC, blog_key(f"{TIS}/m/7")))
    assert [f["blog_source_id"] for f in rec["found_in"]] == [1, 2]
    assert store.read_json("state/blog-post.json") is None  # --only 는 상태를 바꾸지 않는다
    # 대상에 없는 블로그 주소는 출처 없이 받고 알림
    http.routes["https://other.tistory.com/1"] = page("o")
    store, sr, _ = run(tmp_path, http, TARGETS_TIS, day=1, only=["https://other.tistory.com/1"])
    rec = store.read_json(raw_path(SRC, blog_key("https://other.tistory.com/1")))
    assert rec["found_in"] == [] and sr.extra["notices"][0]["type"] == "only_not_in_targets"


def test_default_loader_is_module_function(tmp_path, monkeypatch):
    monkeypatch.setattr(blog_mod, "load_targets", lambda settings: TARGETS_TIS)
    store = LocalStore(str(tmp_path))
    http = tistory_http([f"{TIS}/1"], [])
    rep = collect(Settings(store_url=str(tmp_path)), [SRC], store=store,
                  make_source=lambda n: BlogSource(http=http), clock=lambda: DAY0)
    assert rep.sources[SRC].counts["new"] == 1 and rep.sources[SRC].extra["target_rows"] == 2


# ---- 리뷰 반영(2026-11-05) ----

def test_generic_feed_is_requested_once_when_discovered(tmp_path):
    http = FakeHttp(generic_routes())
    run(tmp_path, http, [Target(5, "https://team.github.io/about", None)], day=0)
    assert http.count(f"{GEN}/atom.xml") == 1


def test_parsers_sitemap_root_and_atom_xml_base():
    try:
        parse_sitemap(b"<error><message>blocked</message></error>")
        assert False
    except ValueError:
        pass
    idx = (b'<sitemapindex xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">'
           b'<sitemap><loc>https://a.tistory.com/s1.xml</loc></sitemap></sitemapindex>')
    assert parse_sitemap(idx) == ([], ["https://a.tistory.com/s1.xml"])
    feed = (b'<feed xmlns="http://www.w3.org/2005/Atom" xml:base="https://ex.org/blog/">'
            b'<entry xml:base="posts/"><link href="one"/></entry><entry><link href="two"/></entry></feed>')
    assert parse_feed(feed, "https://ex.org/atom.xml") == ["https://ex.org/blog/posts/one", "https://ex.org/blog/two"]


def sitemap_index(*locs):
    items = "".join(f"<sitemap><loc>{u}</loc></sitemap>" for u in locs)
    return f'<sitemapindex xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">{items}</sitemapindex>'


def test_nested_sitemap_index_is_followed(tmp_path):
    http = tistory_http([], [])
    http.routes[f"{TIS}/sitemap.xml"] = sitemap_index(f"{TIS}/s-a.xml")
    http.routes[f"{TIS}/s-a.xml"] = sitemap_index(f"{TIS}/s-b.xml")
    http.routes[f"{TIS}/s-b.xml"] = sitemap(f"{TIS}/1", f"{TIS}/category")
    http.routes[f"{TIS}/1"] = page(1)
    store, sr, _ = run(tmp_path, http, TARGETS_TIS, day=0)
    assert sr.counts["new"] == 1 and sr.extra["full_list_blogs"] == [TIS]


def test_non_sitemap_xml_is_full_list_failure(tmp_path):
    http = tistory_http([], [f"{TIS}/1"])
    http.routes[f"{TIS}/sitemap.xml"] = "<error>blocked</error>"
    store, sr, _ = run(tmp_path, http, TARGETS_TIS, day=0)
    assert [n["type"] for n in sr.extra["notices"]] == ["full_list_failed"]
    assert http.count(f"{TIS}/rss") == 1 and sr.counts["new"] == 1
    assert "last_full_list" not in state(store)[TIS]


def test_velog_odd_graphql_shapes_fall_back_to_rss(tmp_path):
    for answer in ([], None):
        def graphql(payload, answer=answer):
            if answer is None:  # 커서가 반복된다: 같은 페이지만 계속 온다
                return Resp(200, json.dumps({"data": {"posts": [{"id": "a", "url_slug": "x", "user": {"username": "dev"}}]}}))
            return Resp(200, json.dumps(answer))
        http = velog_http()
        http.routes["https://v2.velog.io/graphql"] = graphql
        store, sr, _ = run(tmp_path / str(answer), http, TARGETS_VEL, day=0)
        assert [n["type"] for n in sr.extra["notices"]] == ["full_list_failed"]
        assert http.count("https://v2.velog.io/rss/@dev") == 1
        assert "last_full_list" not in state(store)[VEL]


def test_internal_addresses_in_feed_are_not_fetched(tmp_path):
    http = tistory_http([f"{TIS}/1", "http://169.254.169.254/latest/meta-data", "file:///etc/passwd",
                         "http://localhost:8080/x"], [])
    store, sr, _ = run(tmp_path, http, TARGETS_TIS, day=0)
    assert sr.counts["new"] == 1
    assert not any("169.254" in u or "localhost" in u for _, u in http.calls)
    assert sum(1 for n in sr.extra["notices"] if n["type"] == "url_rejected") == 0  # tistory 는 글 주소 모양만 고른다
    # 범용 피드에 내부 주소가 있으면 빼고 알린다
    routes = generic_routes()
    routes[f"{GEN}/atom.xml"] = ('<feed xmlns="http://www.w3.org/2005/Atom"><entry><link href="/2026/a/"/></entry>'
                                 '<entry><link href="http://127.0.0.1:9000/secret"/></entry></feed>')
    http = FakeHttp(routes)
    store, sr, _ = run(tmp_path / "gen", http, [Target(5, "https://team.github.io", None)], day=0)
    assert sr.counts["new"] == 1 and not any("127.0.0.1" in u for _, u in http.calls)
    assert [n["type"] for n in sr.extra["notices"]] == ["url_rejected"]


def test_only_url_without_scheme(tmp_path):
    http = tistory_http([], [])
    http.routes[f"{TIS}/7"] = page(7)
    store, sr, _ = run(tmp_path, http, TARGETS_TIS, day=0, only=["abc.tistory.com/7"])
    assert sr.counts["new"] == 1 and http.count(f"{TIS}/7") == 1


def test_failed_post_on_full_list_day_is_retried_next_day(tmp_path):
    posts = [f"{TIS}/1", f"{TIS}/2", f"{TIS}/3"]
    http = tistory_http(posts, [])
    http.routes[f"{TIS}/2"] = Resp(503)
    http.routes[f"{TIS}/3"] = Resp(404)  # 404 는 다시 받지 않는다
    store, sr, _ = run(tmp_path, http, TARGETS_TIS, day=0)
    assert sr.counts["new"] == 1 and sr.counts["failed"] == 2
    assert state(store)[TIS]["retry_urls"] == [f"{TIS}/2"]
    # 다음 날(피드 날): 피드에 없는 옛 글 2 를 다시 받는다
    http = tistory_http([], [f"{TIS}/4"])
    http.routes[f"{TIS}/2"] = page(2)
    store, sr, _ = run(tmp_path, http, TARGETS_TIS, day=1)
    assert http.count(f"{TIS}/2") == 1 and http.count(f"{TIS}/3") == 0
    assert sr.counts["new"] == 2 and sr.extra["retried"] == 1
    assert "retry_urls" not in state(store)[TIS]
    assert state(store)[TIS]["last_full_list"] == "2026-11-05"
    # 그다음 날에는 다시 받지 않는다
    http = tistory_http([], [])
    store, sr, _ = run(tmp_path, http, TARGETS_TIS, day=2)
    assert http.count(f"{TIS}/2") == 0 and sr.extra["retried"] == 0


def test_too_large_post_is_a_failure(tmp_path):
    from collector.http import ResponseTooLarge
    http = tistory_http([f"{TIS}/1"], [])
    http.routes[f"{TIS}/1"] = ResponseTooLarge("big")
    store, sr, _ = run(tmp_path, http, TARGETS_TIS, day=0)
    assert sr.failure_reasons == {"too_large": 1}


def test_retry_urls_kept_when_targets_cannot_be_read(tmp_path):
    """서비스 DB 에서 대상 목록을 못 읽은 날에 다시 받을 글 주소를 지우지 않는다."""
    http = tistory_http([f"{TIS}/1", f"{TIS}/2"], [])
    http.routes[f"{TIS}/2"] = Resp(503)
    store, sr, _ = run(tmp_path, http, TARGETS_TIS, day=0)
    assert state(store)[TIS]["retry_urls"] == [f"{TIS}/2"]

    store = LocalStore(str(tmp_path))
    def broken(settings):
        raise RuntimeError("DB 접속 실패")
    src = BlogSource(http=tistory_http([], []), targets_loader=broken)
    collect(Settings(store_url=str(tmp_path)), [SRC], store=store, make_source=lambda n: src,
            clock=lambda: DAY0 + timedelta(days=1))
    assert state(store)[TIS]["retry_urls"] == [f"{TIS}/2"]


# ---- 빈 껍데기 페이지(03 문서 5-3, 2026-11-06) ----

def test_shell_page_reason_per_platform():
    velog_shell = "<html><head><title>velog</title></head><body><div id=root></div></body></html>"
    assert shell_page_reason("velog", velog_shell)
    assert shell_page_reason("velog", page("글")) is None
    assert shell_page_reason("velog", '<script>{"released_at":"2026-11-01T00:00:00Z"}</script>') is None
    assert shell_page_reason("velog", "<meta content='t' property='og:title'>") is None  # 속성 순서, 따옴표가 달라도
    assert shell_page_reason("tistory", "<html></html>")
    assert shell_page_reason("tistory", '<meta property="article:published_time" content="2026">') is None
    assert shell_page_reason("rilog", "# 제목\n본문")                       # 앞머리 없음
    assert shell_page_reason("rilog", '---\nauthor: "a"\n---\n본문')         # 앞머리에 title 없음
    assert shell_page_reason("rilog", '---\ntitle: "t"\n---\n본문') is None
    assert shell_page_reason("generic", "<html></html>") is None              # 범용은 보지 않는다(② 정제의 안전망)


def test_shell_page_is_a_failure_and_retried_next_day(tmp_path):
    http = tistory_http([f"{TIS}/1", f"{TIS}/2"], [])
    http.routes[f"{TIS}/2"] = "<html><head><title>tistory</title></head></html>"
    store, sr, _ = run(tmp_path, http, TARGETS_TIS, day=0)
    assert sr.counts["new"] == 1 and sr.failure_reasons == {"not_a_post": 1}
    assert store.read_json(raw_path(SRC, blog_key(f"{TIS}/2"))) is None      # 저장하지 않는다
    assert state(store)[TIS]["retry_urls"] == [f"{TIS}/2"]
    http = tistory_http([], [])
    http.routes[f"{TIS}/2"] = page(2)
    store, sr, _ = run(tmp_path, http, TARGETS_TIS, day=1)
    assert sr.counts["new"] == 1 and "retry_urls" not in state(store)[TIS]


def test_only_overwrites_known_blog_post(tmp_path):
    """이미 저장된 글(예: 확인이 생기기 전에 저장된 빈 껍데기)을 --only 로 다시 받으면 새 버전으로 덮어쓴다."""
    http = tistory_http([f"{TIS}/1"], [])
    store, sr, _ = run(tmp_path, http, TARGETS_TIS, day=0)
    key = blog_key(f"{TIS}/1")
    assert store.read_json(raw_path(SRC, key))["version"] == 1
    http.routes[f"{TIS}/1"] = page("고친 글")
    store, sr, _ = run(tmp_path, http, TARGETS_TIS, day=1, only=[f"{TIS}/1"])
    assert sr.counts["changed"] == 1 and sr.counts["unchanged"] == 0
    rec = store.read_json(raw_path(SRC, key))
    assert rec["version"] == 2 and rec["body"] == page("고친 글")
    assert Index(store, SRC).load().get(key)["version"] == 2
    # --only 로 받은 것도 빈 껍데기면 덮어쓰지 않는다
    http.routes[f"{TIS}/1"] = "<html></html>"
    store, sr, _ = run(tmp_path, http, TARGETS_TIS, day=2, only=[f"{TIS}/1"])
    assert sr.failure_reasons == {"not_a_post": 1}
    assert store.read_json(raw_path(SRC, key))["version"] == 2


# ---- 사라진 글 다시 확인(09 문서 R18-9, 2026-11-10) ----

def gone_setup(tmp_path):
    """0일에 글 1, 2 를 받고, 8일(전체 목록 날)에 2 가 목록에서 빠지고 404 라 사라짐."""
    run(tmp_path, tistory_http([f"{TIS}/1", f"{TIS}/2"], []), TARGETS_TIS, day=0)
    store, sr, _ = run(tmp_path, tistory_http([f"{TIS}/1"], [], gone=[f"{TIS}/2"]), TARGETS_TIS, day=8)
    assert sr.gone_keys == [blog_key(f"{TIS}/2")] and sr.extra["gone_rechecked"] == 0  # 사라진 그 실행에서는 다시 보지 않는다
    return blog_key(f"{TIS}/2")


def test_gone_post_that_opens_again_is_restored(tmp_path):
    key = gone_setup(tmp_path)
    http = tistory_http([], [])
    http.routes[f"{TIS}/2"] = page(2)          # 다시 열린다
    store, sr, _ = run(tmp_path, http, TARGETS_TIS, day=9)
    assert sr.extra["gone_rechecked"] == 1 and sr.extra["returned_keys"] == [key]
    assert Index(store, SRC).load().get(key)["gone"] is None
    assert sr.counts["new"] == 0               # 본문은 다시 받지 않는다


def test_gone_post_recheck_schedule(tmp_path):
    key = gone_setup(tmp_path)
    def day(n, http=None):
        http = http or tistory_http([], [], gone=[f"{TIS}/2"])
        store, sr, _ = run(tmp_path, http, TARGETS_TIS, day=n)
        return store, sr, http
    for n in (9, 10, 11):                        # 사라진 뒤 3일은 실행마다
        store, sr, http = day(n)
        assert sr.extra["gone_rechecked"] == 1 and http.count(f"{TIS}/2") == 1
    gone = Index(store, SRC).load().get(key)["gone"]
    assert gone["reason"] == "http_404" and gone["checked_at"].startswith("2026-11-16")
    _, sr, http = day(14)                         # 그 뒤로는 마지막 확인에서 7일이 지나야
    assert sr.extra["gone_rechecked"] == 0 and http.count(f"{TIS}/2") == 0
    _, sr, http = day(18)
    assert sr.extra["gone_rechecked"] == 1
    # 빈 껍데기로 열리면 되살리지 않는다
    http = tistory_http([], [])
    http.routes[f"{TIS}/2"] = "<html></html>"
    _, sr, _ = day(25, http)
    assert sr.extra["returned_keys"] == []


def test_gone_post_of_other_blog_is_not_rechecked(tmp_path):
    """대상 블로그가 아닌 글(1~7기 묶음 등)은 다시 보지 않는다."""
    key = gone_setup(tmp_path)
    other = [Target(9, "https://other.tistory.com", None)]
    http = FakeHttp({"https://other.tistory.com/sitemap.xml": sitemap(), "https://other.tistory.com/rss": rss()})
    http.routes[f"{TIS}/2"] = page(2)
    store, sr, _ = run(tmp_path, http, other, day=9)
    assert sr.extra["gone_rechecked"] == 0 and Index(store, SRC).load().get(key)["gone"]
