"""잠금 state/lock.json(08 문서 C9, 4-4).

"없을 때만 만들기" 로 잡는다. 이미 있으면 새 실행은 바로 끝난다(보고서 skipped).
12시간 넘은 잠금은 멈춘 실행이 남긴 것으로 보고 풀어 준 뒤 한 번 더 잡는다.
"""

from __future__ import annotations

import json
from datetime import datetime, timezone

from .config import LOCK_STALE_SECONDS
from .store import Store

LOCK_PATH = "state/lock.json"


def _parse(ts: str | None) -> datetime | None:
    if not ts:
        return None
    try:
        return datetime.fromisoformat(ts.replace("Z", "+00:00"))
    except ValueError:
        return None


def acquire(store: Store, run_id: str, host: str, now: datetime) -> tuple[bool, dict | None]:
    body = {"run_id": run_id, "host": host, "started_at": now.strftime("%Y-%m-%dT%H:%M:%SZ")}
    data = json.dumps(body, ensure_ascii=False).encode("utf-8")
    if store.create_exclusive(LOCK_PATH, data):
        return True, None
    held = store.read_json(LOCK_PATH) or {}
    started = _parse(held.get("started_at"))
    if started and (now - started).total_seconds() > LOCK_STALE_SECONDS:
        store.delete(LOCK_PATH)
        if store.create_exclusive(LOCK_PATH, data):
            return True, held
    return False, held


def release(store: Store, run_id: str) -> None:
    held = store.read_json(LOCK_PATH) or {}
    if held.get("run_id") == run_id:
        store.delete(LOCK_PATH)


def now_utc() -> datetime:
    return datetime.now(timezone.utc)
