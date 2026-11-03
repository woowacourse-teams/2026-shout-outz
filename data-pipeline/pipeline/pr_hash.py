"""PR 재수집 판단용 해시(계획 2장 S1 호환성 함정 1번, 2026-11-03 사용자 결정).

`pr-v2` 재료는 사람이 쓴 글과 상태뿐이다: 상태(OPEN, CLOSED, MERGED), 제목, 본문, 사람 리뷰 (id, 본문),
리뷰 스레드 댓글 (id, 본문). 커밋, 파일, 라벨, draft, 일반 대화 댓글은 넣지 않는다.
리뷰와 스레드 댓글은 봇, 빈 승인, 이모지만 있는 것을 뺀 뒤(filters.py 규칙)의 목록을 넣는다.

원본 봉투의 `content_hash`(pr-v1, 원본 전체 지문)는 그대로 두고 LLM 결과와의 연결에 쓴다.
목록 행의 `text_hash` 가 이 해시이고, `hash_scheme` 이 "pr-v2" 다.
"""
from __future__ import annotations

import hashlib
import json
from typing import Any

SCHEME = "pr-v2"


def text_hash(pull: dict[str, Any], reviews: list[dict[str, Any]], review_threads: list[dict[str, Any]]) -> str:
    comments = []
    for thread in review_threads:
        nodes = (thread.get("comments") or {}).get("nodes", [])
        comments.extend((str(c.get("id") or ""), c.get("body")) for c in nodes)
    material = {
        "state": pull.get("state"),
        "title": pull.get("title"),
        "body": pull.get("body"),
        "reviews": sorted((str(r.get("id") or ""), r.get("body")) for r in reviews),
        "review_comments": sorted(comments),
    }
    encoded = json.dumps(material, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode()
    return "sha256:" + hashlib.sha256(encoded).hexdigest()
