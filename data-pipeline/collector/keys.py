"""키와 경로 규칙.

- 원본 파일 경로: raw/{source}/{external_key 의 sha1}.json(08 문서 C2)
- 블로그 글 키: ../CLAUDE.md 의 주소 정규화 6단계와 03 문서 4-2, 4-3(경로 인코딩 풀기, tistory /m 떼기)
- GitHub 원천 키: ../CLAUDE.md 형식. 레포 이름은 API 의 name 그대로(대소문자 포함)
"""

from __future__ import annotations

import hashlib
from urllib.parse import unquote, urlsplit, urlunsplit

from .config import ORG

DROP_QUERY = {"utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content", "fbclid", "gclid", "category"}


def sha1(text: str) -> str:
    return hashlib.sha1(text.encode("utf-8")).hexdigest()


def raw_path(source: str, external_key: str) -> str:
    return f"raw/{source}/{sha1(external_key)}.json"


def normalize_blog_url(url: str) -> str:
    s = url.strip()
    parts = urlsplit(s)
    host = (parts.hostname or "").lower()
    path = unquote(parts.path)
    if host.endswith(".tistory.com") and (path == "/m" or path.startswith("/m/")):
        path = path[2:] or "/"
    if host.startswith("www."):
        host = host[4:]
    netloc = host + (f":{parts.port}" if parts.port else "")
    path = path.rstrip("/") or "/"
    pairs = [p for p in parts.query.split("&") if p]
    pairs = [p for p in pairs if p.split("=", 1)[0] not in DROP_QUERY]
    pairs.sort(key=lambda p: p.split("=", 1)[0])
    return urlunsplit(("https", netloc, path, "&".join(pairs), ""))


def blog_key(url: str) -> str:
    return "post:" + sha1(normalize_blog_url(url))


def tekotalk_key(video_id: str) -> str:
    return f"yt:{video_id}"


def teamdoc_key(repo: str, path: str) -> str:
    return f"{ORG}/{repo}:{path}"


def wiki_key(repo: str, path: str) -> str:
    return f"{ORG}/{repo}:wiki/{path}"


def discussion_key(repo: str, number: int) -> str:
    return f"{ORG}/{repo}/discussions/{number}"


def pr_key(repo: str, number: int) -> str:
    return f"{ORG}/{repo}#{number}"


def repo_of_key(external_key: str) -> str | None:
    """GitHub 원천 키에서 레포 이름을 꺼낸다(사라짐 확인을 레포별로 묶을 때).

    레포 이름에는 ':', '#', '/' 가 들어갈 수 없으므로 셋 중 가장 먼저 나오는 곳까지가 레포다.
    """
    prefix = ORG + "/"
    if not external_key.startswith(prefix):
        return None
    rest = external_key[len(prefix):]
    cut = [i for i in (rest.find(":"), rest.find("#"), rest.find("/")) if i > 0]
    return rest[: min(cut)] if cut else None
