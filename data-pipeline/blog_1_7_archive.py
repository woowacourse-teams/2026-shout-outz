"""1~7기 블로그 원본 1회 적재.

① blog-1_7-archive : 1~7기 블로그 목록(모아온 API 응답, 글마다 1파일, 그대로)
② blog-post  : 원문 블로그 본문(정규화 URL 마다 1파일) — 8기 블로그도 같은 형식을 쓴다.

결정 기록 17번. 사이트가 없거나 글이 지워진 경우(도메인 없음 · 연결 불가 · 404/410 · '없는 페이지' 안내 ·
메인으로 튕김)는 저장하지 않고 보고서에만 남긴다. robots.txt 를 지키고 요청 사이 1초 이상 쉰다.

실행: uv run python blog_1_7_archive.py [--limit N]
"""
from __future__ import annotations

import argparse
import gzip
import hashlib
import json
import os
import re
import tempfile
import time
import urllib.robotparser
from datetime import UTC, datetime
from pathlib import Path
from urllib.parse import parse_qsl, urlencode, urlparse, urlunparse

import httpx
import trafilatura

DATA = Path(os.environ.get("RAW_DATA_REPO") or Path.home() / "Desktop" / "woowacourse-raw-data")  # 매일 파이프라인 1-5
STATE = Path(__file__).resolve().parent / "state" / "blog_1_7_archive"
CACHE_ARTICLES = Path.home() / "Desktop" / "senior-trace" / "data" / "raw" / "moaon" / "articles.json"
MOAON_API = "https://api.moaon.co.kr"
UA = "shout-outz-raw-collector/0.1 (woowacourse alumni blog archive; internal research)"
SOURCE = "blog-1_7-archive"  # 1~7기 블로그 목록(제공: 모아온 API). 사용자 요청으로 moaon 대신 이 이름을 쓴다
COLLECTOR = {"name": SOURCE, "version": "1.0.0"}
EXTRACTOR = {"name": "trafilatura", "version": trafilatura.__version__, "format": "markdown"}
DELAY = 1.0
MIN_TEXT = 200  # 이보다 짧으면 본문을 못 뽑은 것으로 본다

# 글이 지워졌을 때 200 으로 돌려주는 안내 문구(소프트 404)
GONE_MARKERS = [
    "존재하지 않는 페이지", "페이지를 찾을 수 없습니다", "삭제된 게시글", "존재하지 않는 게시글", "비공개 글",
    "포스트를 찾을 수 없습니다", "This page could not be found", "Page not found", "404 Not Found",
    "There isn't a GitHub Pages site here", "블로그가 존재하지 않습니다", "접근 권한이 없는",
]
TRACKING = {"utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content", "fbclid", "gclid", "category"}


def now() -> str:
    return datetime.now(UTC).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def stamp(iso: str) -> str:
    return iso.replace("-", "").replace(":", "").replace("Z", "Z")


def sha256(obj) -> str:
    return "sha256:" + hashlib.sha256(json.dumps(obj, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode()).hexdigest()


def canonical(url: str) -> str:
    p = urlparse(url.strip())
    q = urlencode(sorted((k, v) for k, v in parse_qsl(p.query) if k not in TRACKING))
    path = p.path.rstrip("/") or "/"
    return urlunparse(("https", p.netloc.lower().removeprefix("www."), path, "", q, ""))


def fetch_url(url: str) -> str:
    """네이버 블로그는 겉 페이지가 iframe 이라 본문이 비어 나온다 → 본문 주소(PostView)로 바꿔 받는다."""
    m = re.match(r"https?://(?:m\.)?blog\.naver\.com/([^/?#]+)/(\d+)", url)
    if m:
        return f"https://blog.naver.com/PostView.naver?blogId={m.group(1)}&logNo={m.group(2)}"
    return url


def platform(host: str) -> str:
    for key in ("tistory", "velog", "notion", "oopy", "medium", "naver", "github"):
        if key in host:
            return key
    return "other"


def atomic_write(path: Path, data: bytes) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    fd, tmp = tempfile.mkstemp(dir=path.parent, prefix=".tmp-")
    with os.fdopen(fd, "wb") as f:
        f.write(data)
    os.replace(tmp, path)


class Store:
    def __init__(self, run_id: str):
        self.run_id = run_id
        # 원본 목록은 원천별 파일(raw_objects/{source}.jsonl) — 다른 수집기와 동시에 돌아도 안전(결정 기록 17-6)
        self.dir = DATA / "raw_objects"
        self.rows = []
        for src in ("blog-1_7-archive", "blog-post"):
            f = self.dir / f"{src}.jsonl"
            if f.exists():
                self.rows += [json.loads(l) for l in f.read_text().splitlines() if l.strip()]
        self.keys = {(r["source"], r["external_key"], r["content_hash"]) for r in self.rows}
        self.changes: list[dict] = []  # 이번 실행에서 새로 받은 글(매일 파이프라인 변경 목록)

    def save(self, source: str, key: str, key_path: str, envelope_extra: dict, payload: dict,
             hash_obj, canonical_url: str | None, source_updated_at: str | None) -> tuple[str, str]:
        h = sha256(hash_obj)
        if (source, key, h) in self.keys:
            return "already_present", ""
        fetched = envelope_extra["fetched_at"]
        rel = f"raw/{source}/{key_path}/v1_{stamp(fetched)}.json.gz"
        env = {"source": source, "external_key": key, "version": 1, "source_updated_at": source_updated_at,
               "collector": {**COLLECTOR, "run_id": self.run_id}, "content_hash": h, "payload": payload, **envelope_extra}
        atomic_write(DATA / rel, gzip.compress(json.dumps(env, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode()))
        self.rows.append({"canonical_url": canonical_url, "content_hash": h, "deleted_at": None, "external_key": key,
                          "fetched_at": fetched, "is_latest": True, "last_seen_at": fetched, "run_id": self.run_id,
                          "s3_key": rel, "source": source, "source_updated_at": source_updated_at, "version": 1})
        self.keys.add((source, key, h))
        self.changes.append({"source": source, "external_key": key, "version": 1, "status": "new"})
        return "new", rel

    def flush(self) -> None:
        for src in ("blog-1_7-archive", "blog-post"):
            data = "".join(json.dumps(r, ensure_ascii=False, sort_keys=True, separators=(",", ":")) + "\n"
                           for r in self.rows if r["source"] == src)
            atomic_write(self.dir / f"{src}.jsonl", data.encode())


def fetch_moaon_api(client: httpx.Client) -> tuple[list[dict], list[dict]]:
    """API 전체를 다시 받는다. (글 목록, 페이지별 요청 정보) 를 돌려준다."""
    items, pages, cursor, n = [], [], None, 0
    while True:
        params = {"limit": 100} | ({"cursor": cursor} if cursor else {})
        r = client.get(f"{MOAON_API}/articles", params=params)
        r.raise_for_status()
        page, n = r.json(), n + 1
        at = now()
        for a in page["contents"]:
            items.append(a)
            pages.append({"url": str(r.url), "page": n, "fetched_at": at})
        if not page.get("hasNext") or not page.get("nextCursor"):
            return items, pages
        cursor = page["nextCursor"]
        time.sleep(DELAY)


class Robots:
    def __init__(self, client: httpx.Client):
        self.client, self.cache = client, {}

    def allowed(self, url: str) -> bool:
        p = urlparse(url)
        base = f"{p.scheme}://{p.netloc}"
        if base not in self.cache:
            rp = urllib.robotparser.RobotFileParser()
            try:
                r = self.client.get(base + "/robots.txt", timeout=15)
                rp.parse(r.text.splitlines() if r.status_code == 200 else [])
            except httpx.HTTPError:
                rp.parse([])
            self.cache[base] = rp
        return self.cache[base].can_fetch(UA, url)


def classify(requested: str, resp: httpx.Response | None, error: str | None) -> tuple[str, str]:
    """('ok' | 'gone' | 'error', 이유)"""
    if error:
        gone = any(k in error for k in ("ConnectError", "nodename", "Name or service", "getaddrinfo", "SSL", "Certificate"))
        return ("gone" if gone else "error"), error
    if resp.status_code in (404, 410):
        return "gone", f"HTTP {resp.status_code}"
    if resp.status_code != 200:
        return "error", f"HTTP {resp.status_code}"
    req, fin = urlparse(requested), urlparse(str(resp.url))
    if req.path.strip("/") and not fin.path.strip("/") and not fin.query:
        return "gone", "메인으로 튕김"
    head = resp.text[:20000]
    title = re.search(r"<title[^>]*>(.*?)</title>", head, re.S | re.I)
    probe = (title.group(1) if title else "") + " " + head[:5000]
    for m in GONE_MARKERS:
        if m in probe:
            return "gone", f"없는 페이지 안내: {m}"
    return "ok", ""


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--limit", type=int)
    args = ap.parse_args()
    STATE.mkdir(parents=True, exist_ok=True)
    run_id = "blog-1_7-archive-" + stamp(now())
    store = Store(run_id)
    report = {"run_id": run_id, "started_at": now(), "api": {}, "posts": {}, "skipped": [], "errors": []}

    with httpx.Client(timeout=25, follow_redirects=True, headers={"User-Agent": UA}) as client:
        # ① 모아온 API
        try:
            items, reqs = fetch_moaon_api(client)
            report["api"]["source"] = "live"
        except httpx.HTTPError as e:
            items = json.loads(CACHE_ARTICLES.read_text())
            at = datetime.fromtimestamp(CACHE_ARTICLES.stat().st_mtime, UTC).replace(microsecond=0).isoformat().replace("+00:00", "Z")
            reqs = [{"url": "prototype-cache", "page": None, "fetched_at": at}] * len(items)
            report["api"]["source"] = f"prototype-cache ({type(e).__name__})"
        cache_ids = {a["id"] for a in json.loads(CACHE_ARTICLES.read_text())}
        live_ids = {a["id"] for a in items}
        report["api"].update({"articles": len(items), "cache_articles": len(cache_ids),
                              "only_in_cache": sorted(cache_ids - live_ids), "only_in_live": sorted(live_ids - cache_ids)})
        if args.limit:
            items, reqs = items[: args.limit], reqs[: args.limit]
        api_new = 0
        for a, rq in zip(items, reqs):
            hash_obj = {k: v for k, v in a.items() if k != "clicks"}
            outcome, _ = store.save(SOURCE, f"archive:{a['id']}", str(a["id"]),
                                    {"fetched_at": rq["fetched_at"], "request": {"url": rq["url"], "page": rq["page"]}},
                                    {"provider": "moaon", "article": a}, hash_obj, canonical(a["url"]) if a.get("url") else None, None)
            api_new += outcome == "new"
        store.flush()
        report["api"]["new_files"] = api_new

        # ② 원문 본문
        robots = Robots(client)
        stored_posts = {r["external_key"] for r in store.rows if r["source"] == "blog-post"}
        by_url: dict[str, list[dict]] = {}
        for a in items:
            if a.get("url"):
                by_url.setdefault(canonical(a["url"]), []).append(a)
        counts = {"ok": 0, "js_required": 0, "short_text": 0, "gone": 0, "error": 0, "robots_disallow": 0, "already_present": 0}
        for i, (curl, arts) in enumerate(by_url.items(), 1):
            a = arts[0]
            key_id = hashlib.sha1(curl.encode()).hexdigest()
            if f"post:{key_id}" in stored_posts:  # 이미 받은 글은 다시 접속하지 않는다(이어 받기)
                counts["already_present"] += 1
                continue
            url = fetch_url(a["url"].strip())
            via = [{"source": SOURCE, "key": f"archive:{x['id']}", "project": x.get("projectTitle")} for x in arts]
            if not robots.allowed(url):
                counts["robots_disallow"] += 1
                report["skipped"].append({"url": url, "reason": "robots.txt 금지", "archive": [v["key"] for v in via]})
                continue
            resp, err = None, None
            for attempt in range(3):
                try:
                    resp = client.get(url)
                    err = None
                    if resp.status_code >= 500 and attempt < 2:
                        time.sleep(3 * (attempt + 1))
                        continue
                    break
                except httpx.HTTPError as e:
                    err = f"{type(e).__name__}: {e}"[:300]
                    if any(k in err for k in ("ConnectError", "nodename", "getaddrinfo")):
                        break
                    time.sleep(3 * (attempt + 1))
            fetched = now()
            state, reason = classify(url, resp, err)
            if state != "ok":
                counts[state] += 1
                (report["skipped"] if state == "gone" else report["errors"]).append(
                    {"url": url, "reason": reason, "archive": [v["key"] for v in via]})
                time.sleep(DELAY)
                continue
            html = resp.text
            text = trafilatura.extract(html, url=str(resp.url), output_format="markdown", include_formatting=True,
                                       include_links=True, include_tables=True, favor_recall=True) or ""
            meta = trafilatura.extract_metadata(html, default_url=str(resp.url))
            plat = platform(urlparse(str(resp.url)).netloc)
            fetch_state = "ok" if len(text) >= MIN_TEXT else ("js_required" if plat in ("notion", "oopy") else "short_text")
            payload = {
                "requested_url": url, "final_url": str(resp.url), "canonical_url": curl, "http_status": resp.status_code,
                "content_type": resp.headers.get("content-type"), "platform": plat, "fetch_state": fetch_state,
                "title": (meta.title if meta else None) or a.get("title"), "author_display": meta.author if meta else None,
                "published_at": meta.date if meta else None, "text": text, "text_chars": len(text), "html": html,
                "extractor": EXTRACTOR, "discovered_via": via,
            }
            if fetch_state != "ok":
                # 노션처럼 자바스크립트로 그려지는 페이지 · 본문을 못 뽑은 페이지는 빈 껍데기라 원본으로 저장하지 않는다.
                # 목록 메타(blog-1_7-archive)는 따로 저장되므로 잃는 것은 없고, 나중에 다른 방법으로 받는다.
                counts["js_required" if fetch_state == "js_required" else "short_text"] += 1
                report.setdefault("needs_other_method", []).append(
                    {"url": url, "platform": plat, "state": fetch_state, "chars": len(text), "archive": [v["key"] for v in via]})
                time.sleep(DELAY)
                continue
            key_id = hashlib.sha1(curl.encode()).hexdigest()
            outcome, _ = store.save("blog-post", f"post:{key_id}", key_id,
                                    {"fetched_at": fetched, "request": {"url": url, "etag": resp.headers.get("etag")}},
                                    payload, {"canonical_url": curl, "text": text, "title": payload["title"]},
                                    curl, payload["published_at"])
            counts["already_present" if outcome == "already_present" else "ok"] += 1
            if i % 50 == 0:
                store.flush()
                print(f"[{i}/{len(by_url)}] {counts}", flush=True)
            time.sleep(DELAY)
        store.flush()

    report["posts"] = {"unique_urls": len(by_url), **counts}
    report["finished_at"] = now()
    atomic_write(DATA / "runs" / f"run-{run_id}.json",
                 (json.dumps(report, ensure_ascii=False, sort_keys=True, indent=2) + "\n").encode())
    print(json.dumps({"api": {k: v for k, v in report["api"].items() if not isinstance(v, list)}, "posts": report["posts"]},
                     ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
