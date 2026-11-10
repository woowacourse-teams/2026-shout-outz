"""블로그 blog-post(09 문서 6장 R5, 2-3 제목, 2-3-2 작성 시각, 2-3-4 레포, R13-1).

본문 뽑기(6-1)
- markdown(rilog 등): 앞머리를 빼고 그대로
- text(Notion 등): 그대로
- html tistory: 본문 자리(contents_style, tt_article_useless_p_margin, blogview_content, article_view)를 마크다운으로.
  없으면 범용 라이브러리로 뽑고 끝의 "카테고리의 다른 글" 이하를 지운다
- html velog: 페이지 안 JSON(__APOLLO_STATE__)에서 주소의 글 이름(url_slug)과 같은 글의 원본 마크다운. 없으면 범용 라이브러리
- html 범용: trafilatura 로 마크다운(이미지, 링크, 표 포함)
빈 껍데기(6-2): tistory, velog 는 전용 본문을 못 찾았고 페이지 제목이 없거나 사이트 이름뿐이면, 범용은 라이브러리 결과가
비고 페이지 제목도 없으면 정제 실패 "원본에 글이 없음"(짧은 글로 제외하지 않는다).
머리말과 꼬리말을 떼지 않는다(2-3). 작성자는 받지 않는다(2-3-3). 거르는 것은 200자 미만 하나뿐이다(R13-1).
1~7기 글(원본 origin 이 archive- 로 시작)은 meta.origin 으로 옮겨 고정 대상임을 알린다(R18-11).
"""

from __future__ import annotations

import json
import re
from urllib.parse import unquote, urlsplit

from ..model import Draft, RefineFailure
from ..text import clean_markdown, clean_title, front_matter_title, front_matter_value, readable_chars, to_utc
from . import Refiner

SITE_NAMES = {"velog", "tistory", "medium", "naver blog", "네이버 블로그"}
TISTORY_CLASSES = ("contents_style", "tt_article_useless_p_margin", "blogview_content", "article_view")
TISTORY_TAIL = re.compile(r"\n[^\n]*카테고리의 다른 글.*\Z", re.S)
APOLLO = re.compile(r"window\.__APOLLO_STATE__\s*=\s*(\{.*?\})\s*;?\s*</script>", re.S)
RELEASED_AT = re.compile(r'"released_at"\s*:\s*"([^"]+)"')
DATE_PUBLISHED = re.compile(r'"datePublished"\s*:\s*"([^"]+)"')
REPO_URL = re.compile(r"github\.com/([^/\s]+)/([^/\s#?]+)", re.I)


def _html(body: str):
    from lxml import html as lh
    try:
        return lh.fromstring(body)
    except Exception:  # 빈 글, 깨진 HTML
        return None


def _meta(doc, prop: str) -> str | None:
    if doc is None:
        return None
    for attr in ("property", "name"):
        got = doc.xpath(f'//meta[@{attr}="{prop}"]/@content')
        if got and got[0].strip():
            return got[0].strip()
    return None


def _title_tag(doc) -> str | None:
    if doc is None:
        return None
    got = doc.xpath("//title")
    return got[0].text_content().strip() if got and got[0].text_content().strip() else None


def _to_markdown(el) -> str:
    from lxml import html as lh
    from markdownify import markdownify
    for bad in el.xpath(".//script|.//style|.//noscript"):
        bad.getparent().remove(bad)
    return markdownify(lh.tostring(el, encoding="unicode"), heading_style="ATX")


def _by_class(doc, names) -> tuple[object, str | None]:
    if doc is None:
        return None, None
    for name in names:
        got = doc.xpath(f"//*[contains(concat(' ', normalize-space(@class), ' '), ' {name} ')]")
        if got:
            return got[0], name
    return None, None


def _velog_markdown(body: str, url: str) -> str | None:
    m = APOLLO.search(body)
    if not m:
        return None
    try:
        state = json.loads(m.group(1))
    except ValueError:
        return None
    slug = unquote(urlsplit(url).path.rstrip("/").split("/")[-1])
    for v in state.values():
        if isinstance(v, dict) and v.get("__typename") == "Post" and v.get("url_slug") == slug and v.get("body"):
            return v["body"]
    return None


def _trafilatura(body: str) -> str:
    import trafilatura
    return trafilatura.extract(body, output_format="markdown", include_images=True, include_links=True,
                               include_tables=True) or ""


def repo_from(found_in) -> tuple[str | None, dict | None]:
    """found_in[].github_repository_url 의 레포. 여럿이면 이름순 첫 번째를 쓰고 후보를 선정 기록에 남긴다(2-3-4)."""
    repos = []
    for f in found_in or []:
        m = REPO_URL.search((f or {}).get("github_repository_url") or "")
        if m:
            r = f"{m.group(1)}/{re.sub(r'[.]git$', '', m.group(2))}"
            if r not in repos:
                repos.append(r)
    if not repos:
        return None, None
    chosen = sorted(repos)[0]
    return chosen, ({"candidates": sorted(repos), "chosen": chosen} if len(repos) > 1 else None)


class BlogRefiner(Refiner):
    name = "blog-post"
    rules = ("short_body",)

    def build(self, raw: dict, entry: dict) -> Draft:
        fmt = raw.get("body_format") or "html"
        platform = raw.get("platform") or "generic"
        body = raw.get("body") or ""
        url = raw.get("url") or ""
        archive = raw.get("archive") or {}
        tried: list[dict] = []
        p_tried: list[dict] = []

        def pick_title(cands: list[tuple[str, str | None, bool]]) -> str:
            for step, value, entities in cands:
                t = clean_title(value, entities=entities)
                if t:
                    tried.append({"step": step, "result": "used"})
                    return t
                tried.append({"step": step, "result": "empty" if value else "missing"})
            tried.append({"step": "url", "result": "used"})
            return clean_title(url) or url

        def pick_time(cands: list[tuple[str, str | None, str]]) -> tuple[str | None, str | None, str | None]:
            for step, value, naive_tz in cands:
                if not value:
                    p_tried.append({"step": step, "result": "missing"})
                    continue
                v, assumed = to_utc(value, naive_tz)
                if v:
                    p_tried.append({"step": step, "result": "used"})
                    return v, value, assumed
                p_tried.append({"step": step, "result": "unreadable"})
            return None, None, None

        how = None
        if fmt == "markdown":
            title = pick_title([("front_matter.title", front_matter_title(body), False),
                                ("archive.title", archive.get("title"), False)])
            published, praw, assumed = pick_time([("front_matter.publishedAt", front_matter_value(body, "publishedAt"), "UTC"),
                                                  ("archive.published_at", archive.get("published_at"), "Asia/Seoul")])
            text = clean_markdown(body, front_matter=True)
            how = "markdown"
        elif fmt == "text":
            title = pick_title([("archive.title", archive.get("title"), False)])
            published, praw, assumed = pick_time([("archive.published_at", archive.get("published_at"), "Asia/Seoul")])
            text = clean_markdown(body)
            how = "text"
        else:
            doc = _html(body)
            og, tag = _meta(doc, "og:title"), _title_tag(doc)
            title = pick_title([("og:title", og, True), ("<title>", tag, True), ("archive.title", archive.get("title"), False)])
            if platform == "velog":
                m = RELEASED_AT.search(body)
                times = [("released_at", m.group(1) if m else None, "UTC")]
            elif platform == "tistory":
                times = [("article:published_time", _meta(doc, "article:published_time"), "Asia/Seoul")]
            else:
                m = DATE_PUBLISHED.search(body)
                times = [("article:published_time", _meta(doc, "article:published_time"), "Asia/Seoul"),
                         ("JSON-LD datePublished", m.group(1) if m else None, "Asia/Seoul")]
            published, praw, assumed = pick_time(times + [("archive.published_at", archive.get("published_at"), "Asia/Seoul")])
            page_title = (og or tag or "").strip()
            has_title = bool(page_title) and page_title.lower() not in SITE_NAMES
            md, special = "", False
            if platform == "tistory":
                el, cls = _by_class(doc, TISTORY_CLASSES)
                if el is not None:
                    md, special, how = _to_markdown(el), True, f"tistory {cls}"
            elif platform == "velog":
                v = _velog_markdown(body, url)
                if v:
                    md, special, how = v, True, "velog JSON"
            if platform in ("tistory", "velog") and not special and not has_title:
                # 전용 본문을 못 찾았고 페이지 제목도 없다: 빈 껍데기(라이브러리가 바닥글 몇 글자를 뽑아도 같다, 6-2)
                raise RefineFailure("no_content", f"{platform} 본문 자리와 페이지 제목이 없음")
            if not md.strip():
                md = _trafilatura(body)
                if platform == "tistory":
                    md = TISTORY_TAIL.sub("", md)
                how = "trafilatura" + ("(대체)" if platform in ("tistory", "velog") else "")
                if not readable_chars(md)[0] and not has_title:
                    raise RefineFailure("no_content", "범용 라이브러리 결과와 페이지 제목이 없음")
            text = clean_markdown(md)

        repo, repo_trace = repo_from(raw.get("found_in"))
        trace = {"title": {"tried": tried, "removed": [], "no_title": None},
                 "published_at": {"tried": p_tried, "raw": praw, "assumed_tz": assumed},
                 "body": {"method": how}}
        if repo_trace:
            trace["repo"] = repo_trace
        return Draft(
            title=title, original_title=title, url=url, published_at=published, author=None, repo=repo,
            meta={"platform": platform, "blog_root": raw.get("blog_root"), "body_format": fmt,
                  "found_in": raw.get("found_in") or [], "origin": raw.get("origin")},
            parts=[{"kind": "body", "author": None, "at": None, "text": text}] if text else [],
            trace=trace,
        )
