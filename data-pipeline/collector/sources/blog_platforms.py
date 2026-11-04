"""블로그 플랫폼 규칙(03 문서 3장). 네트워크 없이 주소와 응답 본문만 다루는 함수 모음.

- 플랫폼 판별과 블로그 첫 주소(3-1)
- 피드(RSS, Atom, JSON Feed) 읽기, sitemap 읽기, 글 주소 고르기(3-2, 3-3)
- 범용 블로그 첫 화면의 <link rel="alternate"> 찾기(3-4)
- robots.txt 읽기(5-6)

외부 라이브러리 없이 표준 라이브러리(xml.etree, html.parser, urllib.robotparser)만 쓴다.
"""

from __future__ import annotations

import json
import re
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from html.parser import HTMLParser
from urllib.parse import unquote, urljoin, urlsplit
from urllib.robotparser import RobotFileParser

TISTORY, VELOG, RILOG, GENERIC = "tistory", "velog", "rilog", "generic"

VELOG_RSS = "https://v2.velog.io/rss/@{id}"
VELOG_GRAPHQL = "https://v2.velog.io/graphql"
VELOG_PAGE = 50  # GraphQL 한 번에 받을 글 수
VELOG_QUERY = (
    "query Posts($username: String, $cursor: ID, $limit: Int) {"
    " posts(username: $username, cursor: $cursor, limit: $limit) { id url_slug user { username } } }"
)
RILOG_SITEMAP = "https://www.rilog.kr/sitemap.xml"
COMMON_FEED_PATHS = ["/feed", "/rss", "/rss.xml", "/feed.xml", "/atom.xml", "/index.xml", "/feed.json"]

# 지원하지 않는 호스트와 이유.
# Notion 계열은 03 문서 3-4. Medium, 네이버는 여러 사람이 한 호스트를 쓰는 곳이라, 범용 방법(호스트 첫 화면의 피드)으로 받으면
# 팀 블로그가 아니라 플랫폼 전체 글을 받게 된다. 전용 규칙을 넣을 때까지 지원 안 함으로 둔다(3-5)
UNSUPPORTED_HOSTS = {
    "notion.site": "Notion 계열은 지원하지 않음(브라우저가 필요)",
    "notion.so": "Notion 계열은 지원하지 않음(브라우저가 필요)",
    "oopy.io": "Notion 계열은 지원하지 않음(브라우저가 필요)",
    "medium.com": "Medium 은 전용 규칙이 없음",
    "blog.naver.com": "네이버 블로그는 전용 규칙이 없음",
}

TISTORY_POST = re.compile(r"/(?:\d+|entry/[^/]+)/?")
RILOG_POST = re.compile(r"/@([^/]+)/posts/(\d+)/?")
FEED_TYPES = {"application/rss+xml", "application/atom+xml", "application/rdf+xml", "application/feed+json",
              "application/json", "application/xml", "text/xml"}


@dataclass(frozen=True)
class Platform:
    name: str                       # tistory, velog, rilog, generic(지원 안 함이면 판별한 이름)
    root: str                       # 블로그 첫 주소, https:// 를 붙인 모양(08 문서 4-4)
    ident: str = ""                 # tistory 아이디, velog 아이디, rilog slug
    unsupported: str | None = None  # 지원 안 하는 이유. None 이면 지원


def _host_is(host: str, domain: str) -> bool:
    return host == domain or host.endswith("." + domain)


def detect(url: str) -> Platform:
    """등록된 주소(또는 글 주소)의 호스트로 플랫폼과 블로그 첫 주소를 알아본다(03 문서 3-1)."""
    s = url.strip()
    if "://" not in s:
        s = "https://" + s
    parts = urlsplit(s)
    host = (parts.hostname or "").lower()
    segs = [p for p in unquote(parts.path).split("/") if p]
    first = segs[0] if segs else ""
    handle = first[1:] if first.startswith("@") and len(first) > 1 else ""

    if host.endswith(".tistory.com"):
        return Platform(TISTORY, f"https://{host}", host[: -len(".tistory.com")])
    if host in ("velog.io", "www.velog.io"):
        if handle:
            return Platform(VELOG, f"https://velog.io/@{handle}", handle)
        return Platform(VELOG, "https://velog.io", unsupported="velog 주소에 @아이디가 없음")
    if host in ("rilog.kr", "www.rilog.kr"):
        if handle:
            return Platform(RILOG, f"https://www.rilog.kr/@{handle}", handle)
        return Platform(RILOG, "https://www.rilog.kr", unsupported="rilog 주소에 @slug 가 없음")
    for domain, why in UNSUPPORTED_HOSTS.items():
        if _host_is(host, domain):
            return Platform(domain.split(".")[0], f"https://{host}", unsupported=why)
    if not host:
        return Platform(GENERIC, s, unsupported="주소에서 호스트를 알 수 없음")
    return Platform(GENERIC, f"https://{host}", host)


def _local(tag) -> str:
    return tag.rsplit("}", 1)[-1].lower() if isinstance(tag, str) else ""


def _dedupe(urls: list[str]) -> list[str]:
    seen: set[str] = set()
    return [u for u in urls if u and not (u in seen or seen.add(u))]


def parse_feed(content: bytes, base_url: str) -> list[str] | None:
    """피드(RSS 2.0, RSS 1.0, Atom, JSON Feed)에서 글 주소 목록. 피드가 아니면 None."""
    head = content.lstrip()[:1]
    if head in (b"{", b"["):
        try:
            data = json.loads(content.decode("utf-8-sig"))
        except (ValueError, UnicodeDecodeError):
            return None
        if not isinstance(data, dict) or not isinstance(data.get("items"), list):
            return None
        links = [it.get("url") or it.get("external_url") for it in data["items"] if isinstance(it, dict)]
        return _dedupe([urljoin(base_url, u.strip()) for u in links if isinstance(u, str)])
    try:
        root = ET.fromstring(content)
    except ET.ParseError:
        return None
    kind = _local(root.tag)
    links: list[str] = []
    if kind in ("rss", "rdf"):
        for item in root.iter():
            if _local(item.tag) != "item":
                continue
            link = guid = None
            for c in item:
                name = _local(c.tag)
                if name == "link" and (c.text or "").strip():
                    link = c.text.strip()
                elif name == "guid" and (c.attrib.get("isPermaLink") or "true").lower() != "false":
                    guid = (c.text or "").strip()
            u = link or (guid if guid and guid.startswith("http") else None)
            if u:
                links.append(urljoin(base_url, u))
    elif kind == "feed":
        for entry in root:
            if _local(entry.tag) != "entry":
                continue
            for c in entry:
                if _local(c.tag) == "link" and c.attrib.get("rel", "alternate") == "alternate" and c.attrib.get("href"):
                    links.append(urljoin(base_url, c.attrib["href"].strip()))
                    break
    else:
        return None
    return _dedupe(links)


def parse_sitemap(content: bytes) -> tuple[list[str], list[str]]:
    """sitemap 에서 (페이지 주소, 하위 sitemap 주소). XML 이 아니면 ET.ParseError."""
    root = ET.fromstring(content)
    child = "sitemap" if _local(root.tag) == "sitemapindex" else "url"
    locs = []
    for el in root:
        if _local(el.tag) != child:
            continue
        for c in el:  # <url> 바로 아래 <loc> 만 본다(이미지 확장의 image:loc 은 빼기 위해)
            if _local(c.tag) == "loc" and (c.text or "").strip():
                locs.append(c.text.strip())
    return (locs, []) if child == "url" else ([], locs)


def is_tistory_post(url: str) -> bool:
    """tistory sitemap 에서 글 주소(/{번호}, /entry/{제목})만 고른다. 카테고리, 태그, 소개 페이지는 뺀다."""
    return bool(TISTORY_POST.fullmatch(urlsplit(url).path))


def is_rilog_post(url: str, slug: str) -> bool:
    """rilog sitemap 에서 그 블로그의 글 주소(/@{slug}/posts/{번호})만 고른다."""
    m = RILOG_POST.fullmatch(unquote(urlsplit(url).path))
    return bool(m) and m.group(1).lower() == slug.lower()


def rilog_markdown_url(post_url: str) -> str:
    """rilog 본문은 글 주소 뒤에 /markdown 을 붙여 받는다(03 문서 3-3)."""
    p = urlsplit(post_url)
    return f"https://www.rilog.kr{p.path.rstrip('/')}/markdown"


def velog_post_url(username: str, url_slug: str) -> str:
    return f"https://velog.io/@{username}/{url_slug}"


class _AlternateLinks(HTMLParser):
    def __init__(self) -> None:
        super().__init__()
        self.hrefs: list[str] = []

    def handle_starttag(self, tag, attrs):
        if tag != "link":
            return
        a = {k.lower(): (v or "") for k, v in attrs}
        rels = a.get("rel", "").lower().split()
        kind = a.get("type", "").lower().split(";")[0].strip()
        if "alternate" in rels and kind in FEED_TYPES and a.get("href", "").strip():
            self.hrefs.append(a["href"].strip())


def find_feed_links(html: str, base_url: str) -> list[str]:
    """블로그 첫 화면 HTML 의 <link rel="alternate"> 중 RSS, Atom, JSON Feed 주소(03 문서 3-4 의 1)."""
    p = _AlternateLinks()
    try:
        p.feed(html)
        p.close()
    except Exception:  # 깨진 HTML 이어도 그때까지 찾은 것은 쓴다
        pass
    return _dedupe([urljoin(base_url, h) for h in p.hrefs])


def robots_from(status: int | None, text: str = "") -> RobotFileParser:
    """robots.txt 응답으로 규칙을 만든다(RFC 9309).

    200 이면 내용대로, 4xx(없음 등)면 모두 허용, 5xx 나 접속 실패(status None)면 모두 막는다.
    """
    rp = RobotFileParser()
    if status == 200:
        rp.parse(text.splitlines())
    elif status is not None and 400 <= status < 500:
        rp.allow_all = True
    else:
        rp.disallow_all = True
    return rp
