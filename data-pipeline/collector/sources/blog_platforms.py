"""블로그 플랫폼 규칙(03 문서 3장). 네트워크 없이 주소와 응답 본문만 다루는 함수 모음.

- 플랫폼 판별과 블로그 첫 주소(3-1)
- 피드(RSS, Atom, JSON Feed) 읽기, sitemap 읽기, 글 주소 고르기(3-2, 3-3)
- 범용 블로그 첫 화면의 <link rel="alternate"> 찾기(3-4)
- robots.txt 읽기(5-6)
- 받아도 되는 주소인지(http, https 이고 내부 주소가 아님)
- 받은 응답이 글 페이지인지(빈 껍데기 페이지 가리기, 5-3)

외부 라이브러리 없이 표준 라이브러리(xml.etree, html.parser, urllib.robotparser)만 쓴다.
"""

from __future__ import annotations

import ipaddress
import json
import re
import socket
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from html.parser import HTMLParser
from urllib.parse import quote, unquote, urljoin, urlsplit
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
XML_BASE = "{http://www.w3.org/XML/1998/namespace}base"
SITEMAP_ROOTS = ("urlset", "sitemapindex")
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
    # 범용 블로그는 등록된 스킴과 포트를 지킨다. https 가 없는 블로그를 https 로 바꾸면 피드를 못 찾는다(PR #586 리뷰)
    scheme = parts.scheme if parts.scheme in ("http", "https") else "https"
    try:
        port = f":{parts.port}" if parts.port else ""
    except ValueError:
        port = ""
    return Platform(GENERIC, f"{scheme}://{host}{port}", host)


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
        # Atom 의 상대 주소는 xml:base(피드, 항목, 링크 순으로 겹쳐 적용)를 기준으로 푼다(RFC 4287)
        feed_base = urljoin(base_url, root.attrib.get(XML_BASE, ""))
        for entry in root:
            if _local(entry.tag) != "entry":
                continue
            entry_base = urljoin(feed_base, entry.attrib.get(XML_BASE, ""))
            for c in entry:
                if _local(c.tag) == "link" and c.attrib.get("rel", "alternate") == "alternate" and c.attrib.get("href"):
                    link_base = urljoin(entry_base, c.attrib.get(XML_BASE, ""))
                    links.append(urljoin(link_base, c.attrib["href"].strip()))
                    break
    else:
        return None
    return _dedupe(links)


def parse_sitemap(content: bytes) -> tuple[list[str], list[str]]:
    """sitemap 에서 (페이지 주소, 하위 sitemap 주소). XML 이 아니면 ET.ParseError, sitemap 이 아닌 XML 이면 ValueError.

    오류 문서 같은 다른 XML 을 빈 sitemap 으로 읽으면 "전체 목록을 받았는데 글이 0개" 가 되어 전체 목록 날짜가 잘못 남는다.
    """
    root = ET.fromstring(content)
    if _local(root.tag) not in SITEMAP_ROOTS:
        raise ValueError(f"sitemap 이 아닌 XML(루트 {_local(root.tag)})")
    child = "sitemap" if _local(root.tag) == "sitemapindex" else "url"
    locs = []
    for el in root:
        if _local(el.tag) != child:
            continue
        for c in el:  # <url> 바로 아래 <loc> 만 본다(이미지 확장의 image:loc 은 빼기 위해)
            if _local(c.tag) == "loc" and (c.text or "").strip():
                locs.append(c.text.strip())
    return (locs, []) if child == "url" else ([], locs)


def is_fetchable_url(url: str) -> bool:
    """받아도 되는 주소인지. http, https 만, localhost 와 내부(사설, 루프백, 링크 로컬 등) IP 주소는 받지 않는다.

    피드나 sitemap 에 적힌 주소를 그대로 요청하므로, 수집용 EC2 의 내부 주소(예: 169.254.169.254 메타데이터)로
    요청이 가지 않게 막는다. 도메인 이름은 DNS 를 따로 풀어 보지 않는다.
    """
    try:
        p = urlsplit(url)
        host = (p.hostname or "").lower().rstrip(".")  # 끝에 점이 붙은 이름(localhost.)도 같은 이름
        p.port  # 포트가 숫자가 아니면 여기서 ValueError
    except ValueError:
        return False
    if p.scheme not in ("http", "https") or not host:
        return False
    if host == "localhost" or host.endswith(".localhost"):
        return False
    try:
        ip = ipaddress.ip_address(host)
    except ValueError:
        # 127.1, 2130706433, 0x7f.1 처럼 줄여 쓴 IPv4 도 운영체제는 127.0.0.1 로 읽는다(PR #586 리뷰). 같은 규칙으로 읽어 본다
        if not re.fullmatch(r"[0-9a-fx.]+", host):
            return True  # IP 가 아닌 도메인
        try:
            ip = ipaddress.IPv4Address(socket.inet_aton(host))
        except OSError:
            return True
    return ip.is_global


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


OG_TITLE = re.compile(r"""<meta\b[^>]*\bproperty\s*=\s*["']og:title["']""", re.I)


def shell_page_reason(platform: str, body: str) -> str | None:
    """받은 응답이 글이 아닌 빈 껍데기 페이지면 그 까닭, 글이면 None(03 문서 5-3, 2026-11-06).

    200 이어도 글 대신 빈 페이지가 오는 일이 있다(velog: <title> 이 "velog" 뿐이고 og:title, released_at 이 없는 33KB 페이지).
    플랫폼마다 정상 글에 늘 있는 표시를 본다. 둘 중 하나만 있어도 글로 본다(정상 글을 실패로 잘못 보지 않게).
    1~7기 묶음으로 잰 값(2026-11-06): tistory 290편, velog 136편 모두 og:title 이 있다.
    - tistory: og:title 또는 article:published_time
    - velog: og:title 또는 페이지 JSON 의 released_at
    - rilog(/markdown): 앞머리(---)에 title
    - 그 밖(범용): 일반 규칙으로 가리기 어려워 보지 않는다. 빠져나온 빈 껍데기는 ② 정제가 "원본에 글이 없음" 실패로 남긴다
    """
    if platform == TISTORY:
        if not OG_TITLE.search(body) and "article:published_time" not in body:
            return "og:title, article:published_time 이 없음"
    elif platform == VELOG:
        if not OG_TITLE.search(body) and "released_at" not in body:
            return "og:title, released_at 이 없음"
    elif platform == RILOG:
        text = body.lstrip("\ufeff")
        end = text.find("\n---", 3) if text.startswith("---") else -1
        if end < 0 or not re.search(r"^title\s*:", text[3:end], re.M):
            return "앞머리에 title 이 없음"
    return None


def velog_post_url(username: str, url_slug: str) -> str:
    """velog GraphQL 의 글 주소를 RSS 와 같은 모양으로 만든다.

    GraphQL 은 slug 를 한글 그대로, RSS 는 한글만 퍼센트 인코딩해서 준다. 키는 주소를 받은 그대로 쓰므로(CLAUDE.md 6단계)
    모양이 다르면 같은 글이 키 두 개가 된다. 그래서 GraphQL 쪽을 RSS 처럼 ASCII 가 아닌 글자만 인코딩한다.
    """
    slug = "".join(ch if ord(ch) < 128 else quote(ch) for ch in url_slug)
    return f"https://velog.io/@{username}/{slug}"


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
