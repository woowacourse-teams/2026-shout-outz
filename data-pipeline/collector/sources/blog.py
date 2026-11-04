"""블로그 글 수집 `blog-post`(03 문서, 08 문서 4-1, 4-2, 7-3).

    1. 대상 블로그 읽기   서비스 DB blog_sources(읽기만, load_targets)
    2. 글 목록 받기       블로그마다: 전체 목록 받는 날이면 전체 목록, 아니면 피드(3-2)
    3. 키 만들기          keys.blog_key(인코딩 풀기, tistory /m 떼기, 정규화, sha1)
    4. 글 받기            처음 보는 키만(runner 가 아는 키를 거른다, 바뀜 표시 없음)
    5. 사라짐 확인        전체 목록을 받은 블로그에서 목록에 없는 아는 키의 주소가 404, 410 이면
    6. 상태 저장          state/blog-post.json: 블로그별 마지막 전체 목록 날짜, 찾은 피드 주소

상태 모양: {"blogs": {"<블로그 첫 주소>": {"last_full_list": "YYYY-MM-DD", "feed_url": "..."}}}
"""

from __future__ import annotations

import logging
from dataclasses import dataclass, field
from datetime import date
from urllib.parse import urlsplit

import requests

from ..config import USER_AGENT
from ..http import Http
from ..keys import blog_key
from ..runner import Context, FetchError, Item, Listing, Source
from .blog_platforms import (COMMON_FEED_PATHS, GENERIC, RILOG, RILOG_SITEMAP, TISTORY, VELOG, VELOG_GRAPHQL,
                             VELOG_PAGE, VELOG_QUERY, VELOG_RSS, Platform, detect, find_feed_links, is_rilog_post,
                             is_tistory_post, parse_feed, parse_sitemap, rilog_markdown_url, robots_from,
                             velog_post_url)

log = logging.getLogger("collector.blog")

FULL_LIST_DAYS = 7     # 마지막 전체 목록에서 이만큼 지났으면 전체 목록(03 문서 3-2)
HOST_DELAY = 1.0       # 같은 호스트 1초에 1번 이하(03 문서 5-6)
MAX_SITEMAPS = 50      # sitemap 묶음(sitemapindex)에서 따라갈 하위 sitemap 수 상한
MAX_VELOG_PAGES = 200  # GraphQL 페이지 상한(50편 x 200 = 1만 편). 커서가 돌면 멈추려고

# 목록 1: 서비스에 등록한 8기 팀(승인 상태는 보지 않음). 목록 2: 프로젝트 없는 행 중 2026- 레포(03 문서 2-1, 2-3)
TARGETS_SQL = """
SELECT bs.id, bs.site_url, COALESCE(p.github_repository_url, bs.github_repository_url)
FROM blog_sources bs
LEFT JOIN projects p ON p.id = bs.project_id
WHERE bs.status = 'CONFIRMED'
  AND (
    (bs.project_id IS NOT NULL AND p.cohort = 8 AND p.deleted_at IS NULL)
    OR (bs.project_id IS NULL AND bs.github_repository_url LIKE %s)
  )
ORDER BY bs.id
"""
UNREGISTERED_REPO_PATTERN = "https://github.com/woowacourse-teams/2026-%"


@dataclass
class Target:
    """blog_sources 한 행."""
    id: int
    site_url: str
    repo_url: str | None


@dataclass
class Blog:
    """블로그 첫 주소가 같은 대상 행 묶음. 같은 블로그가 행 두 곳에 있어도 한 번만 받는다(03 문서 4-3)."""
    platform: Platform
    targets: list[Target] = field(default_factory=list)

    @property
    def root(self) -> str:
        return self.platform.root

    def found_in(self) -> list[dict]:
        return [{"blog_source_id": t.id, "github_repository_url": t.repo_url} for t in self.targets]


class ListError(Exception):
    """블로그 하나의 목록 받기 실패. 그 블로그만 건너뛰고 보고서 notices 에 남긴다."""


def load_targets(settings) -> list[Target]:
    """서비스 DB 에서 대상 행을 읽는다(읽기 전용). 비밀번호가 없으면 RDS IAM 인증 토큰을 쓴다(03 문서 2-2)."""
    import psycopg

    db = settings.db or {}
    if not db.get("host") or not db.get("dbname") or not db.get("user"):
        raise RuntimeError("서비스 DB 접속 정보(SERVICE_DB_HOST, SERVICE_DB_NAME, SERVICE_DB_USER)가 없음")
    password, sslmode = db.get("password"), "prefer"
    if not password:
        import boto3
        region = db.get("region") or "ap-northeast-2"
        password = boto3.client("rds", region_name=region).generate_db_auth_token(
            DBHostname=db["host"], Port=int(db.get("port") or 5432), DBUsername=db["user"], Region=region)
        sslmode = "require"  # RDS IAM 인증은 SSL 접속에서만 된다
    with psycopg.connect(host=db["host"], port=int(db.get("port") or 5432), dbname=db["dbname"], user=db["user"],
                         password=password, sslmode=sslmode, connect_timeout=10) as conn:
        conn.read_only = True
        with conn.cursor() as cur:
            cur.execute(TARGETS_SQL, (UNREGISTERED_REPO_PATTERN,))
            return [Target(int(r[0]), r[1], r[2]) for r in cur.fetchall()]


def group_blogs(targets: list[Target]) -> dict[str, Blog]:
    """대상 행을 블로그 첫 주소로 묶는다. 지원 안 하는 블로그도 함께 돌려준다(platform.unsupported)."""
    blogs: dict[str, Blog] = {}
    for t in targets:
        p = detect(t.site_url)
        blog = blogs.setdefault(p.root, Blog(p))
        if all(x.id != t.id for x in blog.targets):
            blog.targets.append(t)
    return blogs


def decode_text(r) -> str:
    """응답 본문 글. charset 이 없으면 utf-8 로 먼저 읽는다(requests 는 text/* 를 ISO-8859-1 로 읽어 한글이 깨진다)."""
    ctype = (r.headers.get("Content-Type") or "").lower()
    if "charset=" not in ctype:
        try:
            return r.content.decode("utf-8")
        except UnicodeDecodeError:
            pass
    return r.text


class BlogSource(Source):
    name = "blog-post"

    def __init__(self, http=None, targets_loader=None) -> None:
        self._http = http
        self._loader = targets_loader
        self._robots: dict[str, tuple] = {}   # origin → (RobotFileParser, 못 받았으면 True)
        self._rilog: list[str] | Exception | None = None  # 한 실행에 rilog sitemap 은 한 번만 받는다
        self.blogs: dict[str, Blog] = {}
        self.notices: list[dict] = []
        self.full_list_blogs: list[str] = []
        self.feed_only_blogs: list[str] = []
        self.unsupported: set[str] = set()

    # ---- 도구 ----

    @property
    def http(self) -> Http:
        if self._http is None:
            self._http = Http(host_delay=HOST_DELAY)
        return self._http

    def _targets(self, ctx: Context) -> list[Target]:
        return (self._loader or load_targets)(ctx.settings)

    def _notice(self, kind: str, blog: str, text: str) -> None:
        self.notices.append({"type": kind, "blog": blog, "text": text})

    def _robots_block(self, url: str) -> str | None:
        """robots.txt 가 막으면 이유(robots_disallow, robots_unavailable), 아니면 None(03 문서 5-6)."""
        p = urlsplit(url)
        origin = f"{p.scheme}://{p.netloc}"
        if origin not in self._robots:
            try:
                r = self.http.get(origin + "/robots.txt")
                rp, broken = robots_from(r.status_code, decode_text(r) if r.status_code == 200 else ""), r.status_code >= 500
            except requests.RequestException:
                rp, broken = robots_from(None), True
            self._robots[origin] = (rp, broken)
        rp, broken = self._robots[origin]
        if rp.can_fetch(USER_AGENT, url):
            return None
        return "robots_unavailable" if broken else "robots_disallow"

    def _get(self, url: str):
        """목록용 GET. robots 가 막거나 200 이 아니면 ListError."""
        why = self._robots_block(url)
        if why:
            raise ListError(f"{why}: {url}")
        try:
            r = self.http.get(url)
        except requests.RequestException as exc:
            raise ListError(f"{type(exc).__name__}: {url}") from exc
        if r.status_code != 200:
            raise ListError(f"http_{r.status_code}: {url}")
        return r

    def _feed(self, url: str) -> list[str]:
        r = self._get(url)
        links = parse_feed(r.content, r.url or url)
        if links is None:
            raise ListError(f"피드가 아님: {url}")
        return links

    def _sitemap(self, url: str) -> list[str]:
        """sitemap 의 페이지 주소. sitemapindex 면 하위 sitemap 을 따라간다."""
        try:
            pages, children = parse_sitemap(self._get(url).content)
        except ListError:
            raise
        except Exception as exc:
            raise ListError(f"sitemap 을 읽지 못함: {url}: {exc}") from exc
        for child in children[:MAX_SITEMAPS]:
            try:
                pages += parse_sitemap(self._get(child).content)[0]
            except Exception as exc:
                raise ListError(f"하위 sitemap 을 읽지 못함: {child}: {exc}") from exc
        return pages

    # ---- 플랫폼별 목록 ----

    def _velog_all(self, username: str) -> list[str]:
        """velog GraphQL posts(username, cursor, limit) 로 전체 글. 빈 페이지가 올 때까지 커서로 넘긴다."""
        why = self._robots_block(VELOG_GRAPHQL)
        if why:
            raise ListError(f"{why}: {VELOG_GRAPHQL}")
        urls: list[str] = []
        cursor, seen = None, set()
        for _ in range(MAX_VELOG_PAGES):
            body = {"query": VELOG_QUERY, "variables": {"username": username, "cursor": cursor, "limit": VELOG_PAGE}}
            try:
                r = self.http.post(VELOG_GRAPHQL, json=body)
                data = r.json() if r.status_code == 200 else None
            except (requests.RequestException, ValueError) as exc:
                raise ListError(f"GraphQL 요청 실패: {type(exc).__name__}") from exc
            if data is None:
                raise ListError(f"GraphQL http_{r.status_code}")
            posts = (data.get("data") or {}).get("posts") if isinstance(data, dict) else None
            if data.get("errors") or not isinstance(posts, list):
                raise ListError(f"GraphQL 응답 오류: {str(data.get('errors'))[:200]}")
            if not posts:
                return urls
            try:
                for post in posts:
                    user = (post.get("user") or {}).get("username") or username
                    urls.append(velog_post_url(user, post["url_slug"]))
                cursor = posts[-1]["id"]
            except (AttributeError, KeyError, TypeError) as exc:  # 공식 API 가 아니라 모양이 바뀔 수 있다(6장)
                raise ListError(f"GraphQL 응답 모양이 다름: {exc!r}") from exc
            if cursor in seen:
                return urls
            seen.add(cursor)
        raise ListError("GraphQL 페이지가 너무 많음")

    def _rilog_sitemap(self) -> list[str]:
        if self._rilog is None:
            try:
                self._rilog = self._sitemap(RILOG_SITEMAP)
            except Exception as exc:
                self._rilog = exc
        if isinstance(self._rilog, Exception):
            raise ListError(f"rilog sitemap 실패: {self._rilog}")
        return self._rilog

    def _discover_feed(self, root: str) -> str | None:
        """범용 블로그 피드 찾기(03 문서 3-4): 첫 화면 <link rel="alternate">, 그다음 흔한 경로."""
        candidates: list[str] = []
        try:
            r = self._get(root + "/")
            candidates += find_feed_links(decode_text(r), r.url or root + "/")
        except ListError as exc:
            log.info("첫 화면을 받지 못해 흔한 경로만 봄: %s", exc)
        candidates += [root + p for p in COMMON_FEED_PATHS]
        tried: set[str] = set()
        for url in candidates:
            if url in tried:
                continue
            tried.add(url)
            try:
                self._feed(url)
                return url
            except ListError:
                continue
        return None

    def _list_blog(self, blog: Blog, st: dict, today: date) -> tuple[list[str], bool]:
        """블로그 하나의 글 주소와, 전체 목록을 받았는지(사라짐 확인 대상인지)."""
        p = blog.platform
        try:
            last = date.fromisoformat(st["last_full_list"])
        except (KeyError, TypeError, ValueError):  # 기록이 없으면(첫 실행, 새 블로그) 전체 목록
            last = None
        full_day = last is None or (today - last).days >= FULL_LIST_DAYS

        if p.name == RILOG:  # sitemap 이 전체 목록이라 매일 전체 목록(3-3)
            urls = [u for u in self._rilog_sitemap() if is_rilog_post(u, p.ident)]
            st["last_full_list"] = today.isoformat()
            return urls, True

        if p.name == TISTORY:
            if full_day:
                try:
                    urls = [u for u in self._sitemap(blog.root + "/sitemap.xml") if is_tistory_post(u)]
                    st["last_full_list"] = today.isoformat()
                    return urls, True
                except ListError as exc:
                    self._notice("full_list_failed", blog.root, f"sitemap 을 받지 못해 피드만 봄({exc})")
            return self._feed(blog.root + "/rss"), False

        if p.name == VELOG:
            if full_day:
                try:
                    urls = self._velog_all(p.ident)
                    st["last_full_list"] = today.isoformat()
                    return urls, True
                except ListError as exc:  # 그 주는 RSS 만(3-2). 날짜를 남기지 않으므로 다음 날 다시 시도한다
                    self._notice("full_list_failed", blog.root, f"velog GraphQL 실패로 RSS 만 봄({exc})")
            return self._feed(VELOG_RSS.format(id=p.ident)), False

        # 범용: 피드만. 찾은 피드 주소는 상태에 남겨 매일 다시 찾지 않는다(08 문서 4-2)
        feed_url = st.get("feed_url")
        urls = None
        if feed_url:
            try:
                urls = self._feed(feed_url)
            except ListError as exc:
                log.info("남겨 둔 피드를 받지 못해 다시 찾음: %s", exc)
        if urls is None:
            feed_url = self._discover_feed(blog.root)
            if not feed_url:
                st.pop("feed_url", None)
                self.unsupported.add(blog.root)
                self._notice("unsupported", blog.root, "피드를 찾지 못해 지원 안 함")
                return [], False
            st["feed_url"] = feed_url
            urls = self._feed(feed_url)
        self.feed_only_blogs.append(blog.root)  # 범용은 피드에 보이는 글만 받는다(3-4 한계)
        if full_day:  # 범용은 목록이 피드뿐이라, 7일마다 피드에 없는 아는 키를 확인한다(5-5)
            st["last_full_list"] = today.isoformat()
        return urls, full_day

    # ---- Source 계약 ----

    def list_items(self, ctx: Context) -> Listing:
        self.blogs = group_blogs(self._targets(ctx))
        states = ctx.state.data.setdefault("blogs", {})
        today = ctx.now.date()
        items: dict[str, Item] = {}
        complete: set[str] = set()
        for root, blog in self.blogs.items():
            if blog.platform.unsupported:
                self.unsupported.add(root)
                self._notice("unsupported", root, blog.platform.unsupported)
                continue
            st = states.setdefault(root, {})
            try:
                urls, full = self._list_blog(blog, st, today)
            except Exception as exc:  # 블로그 하나가 막혀도 나머지는 계속한다(08 문서 8장)
                log.warning("%s 목록 받기 실패: %r", root, exc)
                self._notice("list_failed", root, f"목록을 받지 못함({exc})")
                continue
            if full:
                complete.add(root)
                self.full_list_blogs.append(root)
            for url in urls:
                self._add(items, url, blog)
        return Listing(list(items.values()), complete)

    def _add(self, items: dict[str, Item], url: str, blog: Blog) -> None:
        key = blog_key(url)
        item = items.get(key)
        if item is None:
            items[key] = Item(key, None, group=blog.root, data={
                "url": url, "blog_root": blog.root, "platform": blog.platform.name, "found_in": blog.found_in()})
            return
        have = {f["blog_source_id"] for f in item.data["found_in"]}
        item.data["found_in"] += [f for f in blog.found_in() if f["blog_source_id"] not in have]

    def only_items(self, ctx: Context, values: list[str]) -> list[Item]:
        """--only 글 주소. 출처는 매일 실행처럼 서비스 DB 대상에서 그 글의 블로그 첫 주소로 찾는다(08 문서 7-3)."""
        self.blogs = group_blogs(self._targets(ctx))
        items: dict[str, Item] = {}
        for value in values:
            url = value.strip()
            p = detect(url)
            blog = self.blogs.get(p.root)
            if blog is None:
                self._notice("only_not_in_targets", p.root, f"대상 블로그에 없는 주소라 출처 없이 받음: {url}")
                blog = Blog(Platform(p.name if not p.unsupported else GENERIC, p.root, p.ident))
            self._add(items, url, blog)
        return list(items.values())

    def fetch(self, ctx: Context, item: Item) -> dict:
        d = item.data
        markdown = d["platform"] == RILOG
        target = rilog_markdown_url(d["url"]) if markdown else d["url"]
        why = self._robots_block(target)
        if why:
            raise FetchError(why, url=d["url"])
        try:
            r = self.http.get(target)
        except requests.Timeout:
            raise FetchError("timeout", url=d["url"])
        except requests.RequestException as exc:
            raise FetchError(f"error_{type(exc).__name__}", url=d["url"])
        if r.status_code != 200:
            raise FetchError(f"http_{r.status_code}", url=d["url"])
        # 받은 그대로 저장한다. 본문 뽑기는 ② 정제에서(03 문서 5-3)
        return {"url": d["url"], "blog_root": d["blog_root"], "platform": d["platform"], "found_in": d["found_in"],
                "body_format": "markdown" if markdown else "html", "body": decode_text(r)}

    def index_fields(self, record: dict) -> dict:
        return {"url": record["url"], "blog_root": record["blog_root"]}

    def group_of(self, entry: dict) -> str | None:
        return entry.get("blog_root")

    def confirm_gone(self, ctx: Context, entry: dict) -> str | None:
        """응답 코드만 본다. 404, 410 이면 사라짐(03 문서 5-5). robots 가 막으면 확인하지 않는다."""
        url = entry.get("url")
        if not url or self._robots_block(url):
            return None
        r = self.http.get(url)
        return f"http_{r.status_code}" if r.status_code in (404, 410) else None

    def finish(self, ctx: Context) -> None:
        if not ctx.only:
            ctx.state.save()
        ctx.report.extra.update({
            "targets": len(self.blogs),
            "target_rows": sum(len(b.targets) for b in self.blogs.values()),
            "full_list_blogs": sorted(self.full_list_blogs),
            "feed_only_blogs": sorted(self.feed_only_blogs),
            "unsupported": len(self.unsupported),
            "notices": self.notices,
        })
