"""8기 이후 팀 블로그 등록부(계획 3장, 7장 5번).

팀이 서비스에서 등록한 블로그(blog_sources, PR #512)를 서비스 공개 API 로 매일 읽는다. DB 에 접속하지 않으므로
만드는 쪽이 어느 서버에 있어도 운영 등록부를 읽을 수 있다(계획 원칙 1).
- 프로젝트 목록 `GET /api/v1/projects?size=50&cursor=` 로 slug 를 모으고,
  상세 `GET /api/v1/projects/@{slug}` 의 `teamBlogUrls`(확정된 주소만), `githubRepositoryUrl` 을 쓴다.
- config/team_blogs.json 은 보조: 서비스에 없는 팀의 블로그를 운영자가 직접 넣는 곳. 레포가 같으면 주소를 합친다.
- API 가 실패하면 빈 등록부로 착각하지 않도록 예외를 던진다(그날 블로그 단계는 실패, 이미 받은 글은 그대로).

서비스 주소는 SERVICE_API_BASE(기본 운영 https://api.shout-ou.tz).
"""
from __future__ import annotations

import json
import os
import time

import httpx

from pipeline.common import CONFIG, DATA, atomic_write, now

API_BASE = os.environ.get("SERVICE_API_BASE", "https://api.shout-ou.tz").rstrip("/")
MANUAL = CONFIG / "team_blogs.json"
DELAY = 0.1


def _get(c: httpx.Client, url: str) -> dict:
    for attempt in range(4):
        try:
            r = c.get(url)
        except httpx.TransportError:
            if attempt == 3:
                raise
            time.sleep(2 ** attempt)
            continue
        if r.status_code >= 500 and attempt < 3:
            time.sleep(2 ** attempt)
            continue
        r.raise_for_status()
        return r.json()
    raise RuntimeError(f"응답 없음: {url}")


def _repo(url: str | None) -> str | None:
    if not url:
        return None
    path = url.split("github.com/", 1)[-1].strip("/")
    return path if path.count("/") == 1 else None


def from_service(api_base: str = API_BASE) -> list[dict]:
    with httpx.Client(timeout=20, headers={"User-Agent": "shout-outz-data-pipeline"}) as c:
        slugs, cursor = [], None
        while True:
            q = f"{api_base}/api/v1/projects?size=50" + (f"&cursor={cursor}" if cursor else "")
            d = _get(c, q)
            slugs += [p["slug"] for p in d["data"]]
            meta = d.get("meta") or {}
            if not meta.get("hasNext"):
                break
            cursor = meta["nextCursor"]
        out, missing_field = [], 0
        for slug in slugs:
            d = _get(c, f"{api_base}/api/v1/projects/@{slug}")["data"]
            if "teamBlogUrls" not in d:
                missing_field += 1
            out.append({"name": d.get("title"), "slug": slug, "repo": _repo(d.get("githubRepositoryUrl")),
                        "in_service": True, "blogs": d.get("teamBlogUrls") or [], "from": "service"})
            time.sleep(DELAY)
    if slugs and missing_field == len(slugs):
        raise RuntimeError(f"{api_base} 응답에 teamBlogUrls 칸이 없다(팀 블로그 등록 기능이 아직 배포되지 않음)")
    return out


def load(api_base: str = API_BASE) -> dict:
    """서비스 등록부 + 보조 파일을 합친 오늘의 등록부. 원본 저장소 state/blog_registry.json 에도 남긴다."""
    service = from_service(api_base)
    manual = json.loads(MANUAL.read_text()).get("projects", []) if MANUAL.exists() else []
    by_key: dict[str, dict] = {}
    for p in service + [{**m, "from": "manual", "repo": m.get("repo") or None} for m in manual]:
        if not p["blogs"]:
            continue
        key = (p["repo"] or "").lower() or f"name:{p['name']}"  # 비교만 소문자로, 저장은 원래 표기
        if key in by_key:  # 같은 레포: 주소를 합친다(서비스 이름, 정보 우선)
            by_key[key]["blogs"] = list(dict.fromkeys(by_key[key]["blogs"] + p["blogs"]))
            by_key[key]["from"] = "service+manual" if by_key[key]["from"] != p["from"] else p["from"]
        else:
            by_key[key] = dict(p)
    reg = {"updated_at": now(), "api_base": api_base, "service_projects": len(service),
           "service_with_blogs": sum(bool(p["blogs"]) for p in service), "manual_projects": len(manual),
           "projects": sorted(by_key.values(), key=lambda p: (p["repo"] or "", p["name"] or ""))}
    atomic_write(DATA / "state" / "blog_registry.json", (json.dumps(reg, ensure_ascii=False, indent=1) + "\n").encode())
    return reg
