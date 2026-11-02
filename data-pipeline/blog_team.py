"""8기 이후 팀 블로그 원본 수집(결정 기록 18번, 26번).

등록부(config/team_blogs.json)의 블로그마다 플랫폼별로 **글 목록 전체**를 받고, 아직 없는 글만 본문을 받아
1~7기와 같은 `blog-post` 형식으로 저장한다. 이미 받은 글은 지우지 않는다(18-5).

- Rilog: 공개 API(블로그 글 목록, 글 본문 블록) → 본문은 블록에서 마크다운으로, 원문 HTML 대신 API 응답을 보관.
- velog: 공개 GraphQL 로 글 목록 전체 → 본문은 페이지를 받아 추출(1~7기와 같은 방식).
- 티스토리: sitemap.xml 의 글 주소 → 본문은 페이지 추출.
- 그 밖: 페이지의 RSS 링크(자동 탐지) → 본문은 페이지 추출.

실행: uv run python blog_team.py [--projects 이름 ...]
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import time
from pathlib import Path
from urllib.parse import urlparse

import httpx
import trafilatura

import blog_1_7_archive as base

HERE = Path(__file__).resolve().parent
REGISTRY = HERE / "config" / "team_blogs.json"
RILOG_API = "https://api.rilog.kr/v1"
VELOG_GQL = "https://v2.velog.io/graphql"
SOURCE = "team-blog-registry"
_EXCL = json.loads((HERE / "config" / "exclusions.json").read_text()) if (HERE / "config" / "exclusions.json").exists() else {}
EXCLUDED_URLS = set(_EXCL.get("urls", [])) | {base.canonical(u) for u in _EXCL.get("urls", [])}


# ---- 플랫폼별 글 목록 -----------------------------------------------------------------
def platform_of(url: str) -> tuple[str, str]:
    p = urlparse(url)
    host = p.netloc.lower()
    if host.endswith("rilog.kr"):
        return "rilog", re.match(r"/@([^/]+)", p.path).group(1)
    if host.endswith("velog.io"):
        return "velog", re.match(r"/@([^/]+)", p.path).group(1)
    if host.endswith("tistory.com"):
        return "tistory", f"{p.scheme}://{host}"
    return "other", f"{p.scheme}://{host}"


def list_rilog(c: httpx.Client, slug: str) -> list[dict]:
    out, page = [], 0
    while True:
        d = c.get(f"{RILOG_API}/blogs/{slug}/posts", params={"page": page, "size": 50}).json().get("data") or {}
        for p in d.get("posts") or []:
            if p.get("visibility", "PUBLIC") == "PUBLIC":
                out.append({"url": f"https://www.rilog.kr/@{slug}/posts/{p['postId']}", "id": p["postId"], "meta": p})
        if not d.get("hasNext"):
            return out
        page += 1
        time.sleep(base.DELAY)


def list_velog(c: httpx.Client, user: str) -> list[dict]:
    q = ("query Posts($username:String,$cursor:ID){posts(username:$username,cursor:$cursor,limit:50)"
         "{id title url_slug released_at}}")
    out, cursor = [], None
    while True:
        posts = c.post(VELOG_GQL, json={"query": q, "variables": {"username": user, "cursor": cursor}}).json()["data"]["posts"] or []
        out += [{"url": f"https://velog.io/@{user}/{p['url_slug']}", "id": p["id"], "meta": p} for p in posts]
        if len(posts) < 50:
            return out
        cursor = posts[-1]["id"]
        time.sleep(base.DELAY)


def list_tistory(c: httpx.Client, root: str) -> list[dict]:
    xml = c.get(f"{root}/sitemap.xml").text
    locs = re.findall(r"<loc>(.*?)</loc>", xml)
    posts = [u for u in locs if re.search(r"/(entry/[^/]+|\d+)$", urlparse(u).path)]
    if not posts:  # sitemap 이 없으면 RSS(최근 글만)
        posts = re.findall(r"<item>.*?<link>(.*?)</link>", c.get(f"{root}/rss").text, re.S)
    return [{"url": u, "id": u, "meta": {}} for u in posts]


def list_other(c: httpx.Client, root: str) -> list[dict]:
    html = c.get(root).text
    feed = re.search(r'<link[^>]+type="application/(?:rss|atom)\+xml"[^>]+href="([^"]+)"', html)
    if not feed:
        return []
    xml = c.get(httpx.URL(root).join(feed.group(1))).text
    links = re.findall(r"<item>.*?<link>(.*?)</link>", xml, re.S) or re.findall(r'<entry>.*?<link[^>]+href="([^"]+)"', xml, re.S)
    return [{"url": u.strip(), "id": u.strip(), "meta": {}} for u in links]


# ---- Rilog 본문(BlockNote 블록 → 마크다운) --------------------------------------------
def inline(content) -> str:
    out = []
    for n in content or []:
        if isinstance(n, str):
            out.append(n)
        elif n.get("type") == "link":
            out.append(f"[{inline(n.get('content'))}]({n.get('href', '')})")
        else:
            t, st = n.get("text", ""), n.get("styles") or {}
            if st.get("code"):
                t = f"`{t}`"
            if st.get("bold"):
                t = f"**{t}**"
            out.append(t)
    return "".join(out)


def blocks_md(blocks, depth: int = 0) -> list[str]:
    lines = []
    for b in blocks or []:
        t, props, pad = b.get("type"), b.get("props") or {}, "  " * depth
        text = inline(b.get("content")) if isinstance(b.get("content"), list) else ""
        if t == "heading":
            lines.append("\n" + "#" * int(props.get("level", 2)) + " " + text)
        elif t == "codeBlock":
            lines.append(f"\n```{props.get('language', '')}\n{text}\n```")
        elif t == "bulletListItem":
            lines.append(f"{pad}- {text}")
        elif t == "numberedListItem":
            lines.append(f"{pad}1. {text}")
        elif t == "checkListItem":
            lines.append(f"{pad}- [{'x' if props.get('checked') else ' '}] {text}")
        elif t == "quote":
            lines.append(f"> {text}")
        elif t == "table":
            for row in (b.get("content") or {}).get("rows", []):
                lines.append("| " + " | ".join(inline(cell) if isinstance(cell, list) else inline(cell.get("content"))
                                                for cell in row.get("cells", [])) + " |")
        elif t == "image":
            if props.get("caption"):
                lines.append(f"(이미지: {props['caption']})")
        elif text:
            lines.append(f"{pad}{text}")
        lines += blocks_md(b.get("children"), depth + 1)
    return lines


def rilog_body(c: httpx.Client, slug: str, post_id) -> dict:
    d = c.get(f"{RILOG_API}/blogs/{slug}/posts/{post_id}").json().get("data") or {}
    content = d.get("content")
    if isinstance(content, str) and content.startswith("["):
        content = json.loads(content)
    text = re.sub(r"\n{3,}", "\n\n", "\n".join(blocks_md(content if isinstance(content, list) else []))).strip()
    return {"detail": d, "text": text}


# ---- 본 실행 -----------------------------------------------------------------------
def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--projects", nargs="+")
    args = ap.parse_args()
    report, _ = collect("blog-team-" + base.stamp(base.now()), projects=args.projects)
    base.atomic_write(base.DATA / "runs" / f"run-{report['run_id']}.json",
                      (json.dumps(report, ensure_ascii=False, sort_keys=True, indent=2) + "\n").encode())
    print(json.dumps({"blogs": len(report["blogs"]), **report["counts"], "errors": len(report["errors"])}, ensure_ascii=False))
    return 0


def collect(run_id: str, projects: list[str] | None = None) -> tuple[dict, list]:
    """등록부의 블로그마다 글 목록을 받고, 이미 받은 글 주소는 접속하지 않는다(처음이면 전부)."""
    reg = json.loads(REGISTRY.read_text())
    base.COLLECTOR.update({"name": "blog-team", "version": "1.0.0"})
    store = base.Store(run_id)
    stored = {r["external_key"] for r in store.rows if r["source"] == "blog-post"}
    report = {"run_id": run_id, "started_at": base.now(), "registry_updated_at": reg.get("updated_at"),
              "blogs": [], "skipped": [], "errors": []}
    with httpx.Client(timeout=25, follow_redirects=True, headers={"User-Agent": base.UA}) as c:
        robots = base.Robots(c)
        for proj in reg["projects"]:
            if projects and proj["name"] not in projects:
                continue
            for blog in proj["blogs"]:
                plat, key = platform_of(blog)
                info = {"project": proj["name"], "repo": proj["repo"], "blog": blog, "platform": plat,
                        "listed": 0, "saved": 0, "already_present": 0, "skipped": 0}
                try:
                    posts = {"rilog": list_rilog, "velog": list_velog, "tistory": list_tistory,
                             "other": list_other}[plat](c, key)
                except (httpx.HTTPError, KeyError, ValueError, TypeError) as e:
                    report["errors"].append({"blog": blog, "reason": f"목록: {type(e).__name__}: {e}"[:300]})
                    report["blogs"].append(info)
                    continue
                info["listed"] = len(posts)
                via = [{"source": SOURCE, "blog": blog, "project": proj["name"], "repo": proj["repo"],
                        "in_service": proj["in_service"]}]
                for post in posts:
                    curl = base.canonical(post["url"])
                    if curl in EXCLUDED_URLS or post["url"] in EXCLUDED_URLS:  # 원작자 제외 요청
                        info["skipped"] += 1
                        report["skipped"].append({"url": post["url"], "reason": "제외 요청"})
                        continue
                    key_id = hashlib.sha1(curl.encode()).hexdigest()
                    if f"post:{key_id}" in stored:
                        info["already_present"] += 1
                        continue
                    try:
                        if plat == "rilog":
                            body = rilog_body(c, key, post["id"])
                            d, text = body["detail"], body["text"]
                            payload = {"requested_url": post["url"], "final_url": post["url"], "canonical_url": curl,
                                       "http_status": 200, "content_type": "application/json", "platform": "rilog",
                                       "fetch_state": "ok", "title": d.get("title") or post["meta"].get("title"),
                                       "author_display": ((d.get("author") or {}).get("nickname")),
                                       "published_at": d.get("publishedAt"), "text": text, "text_chars": len(text),
                                       "html": None, "rilog_api": d,
                                       "extractor": {"name": "rilog-api+blocknote", "version": "1.0.0", "format": "markdown"},
                                       "discovered_via": via}
                        else:
                            if not robots.allowed(post["url"]):
                                info["skipped"] += 1
                                report["skipped"].append({"url": post["url"], "reason": "robots.txt 금지"})
                                continue
                            r = c.get(post["url"])
                            state, reason = base.classify(post["url"], r, None)
                            if state != "ok":
                                info["skipped"] += 1
                                report["skipped"].append({"url": post["url"], "reason": reason})
                                continue
                            text = trafilatura.extract(r.text, url=str(r.url), output_format="markdown",
                                                       include_formatting=True, include_links=True, include_tables=True,
                                                       favor_recall=True) or ""
                            meta = trafilatura.extract_metadata(r.text, default_url=str(r.url))
                            payload = {"requested_url": post["url"], "final_url": str(r.url), "canonical_url": curl,
                                       "http_status": r.status_code, "content_type": r.headers.get("content-type"),
                                       "platform": plat, "fetch_state": "ok",
                                       "title": (meta.title if meta else None) or post["meta"].get("title"),
                                       "author_display": meta.author if meta else None,
                                       "published_at": (meta.date if meta else None) or post["meta"].get("released_at"),
                                       "text": text, "text_chars": len(text), "html": r.text,
                                       "extractor": base.EXTRACTOR, "discovered_via": via}
                        if not payload["text"].strip():
                            info["skipped"] += 1
                            report["skipped"].append({"url": post["url"], "reason": "본문 비어 있음"})
                            continue
                        outcome, _ = store.save("blog-post", f"post:{key_id}", key_id,
                                                {"fetched_at": base.now(), "request": {"url": post["url"]}},
                                                payload, {"canonical_url": curl, "text": payload["text"], "title": payload["title"]},
                                                curl, payload["published_at"])
                        stored.add(f"post:{key_id}")
                        info["saved"] += outcome == "new"
                    except (httpx.HTTPError, KeyError, ValueError, TypeError) as e:
                        report["errors"].append({"url": post["url"], "reason": f"{type(e).__name__}: {e}"[:300]})
                    time.sleep(base.DELAY)
                store.flush()
                report["blogs"].append(info)
                print(json.dumps(info, ensure_ascii=False), flush=True)
    report["finished_at"] = base.now()
    report["counts"] = {k: sum(b[k] for b in report["blogs"]) for k in ("listed", "saved", "already_present", "skipped")}
    report["failures"] = report["errors"]
    return report, [store]


if __name__ == "__main__":
    raise SystemExit(main())
