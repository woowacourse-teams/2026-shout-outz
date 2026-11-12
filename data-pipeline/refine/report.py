"""정제 실행 보고서 refine/reports/{run_id}.json(09 문서 20장 R20-8).

실행이 시작되면 status "running" 으로 먼저 쓰고, 원천이 도는 동안 15초마다, 원천 하나가 끝날 때마다 덮어쓴다.
키 목록은 하나에 KEY_LIST_CAP 개까지 남기고 넘는 것은 수만 센다(첫 정제는 수만 건이라, 전체는 정제 색인에 있다).
"""

from __future__ import annotations

import logging
import time
from collections import Counter, defaultdict, deque
from datetime import datetime, timezone
from typing import Callable

from collector.report import LogTail, iso, peak_rss_mb, where
from collector.store import Store

from .config import KEY_LIST_CAP, REFINE_DIR, SAVE_EVERY_SEC


class Capped(list):
    """KEY_LIST_CAP 개까지만 담고 전체 수를 센다."""

    def __init__(self) -> None:
        super().__init__()
        self.total = 0

    def add(self, item) -> None:
        self.total += 1
        if len(self) < KEY_LIST_CAP:
            self.append(item)

    def to_json(self) -> list | dict:
        return list(self) if self.total <= len(self) else {"items": list(self), "total": self.total}


class SourceStats:
    def __init__(self, name: str, started: datetime) -> None:
        self.name = name
        self.started = started
        self.finished: datetime | None = None
        self.counts = {"new": 0, "changed": 0, "ignored": 0, "unchanged": 0, "failed": 0}
        self.changed_by = Counter()          # raw(원본이 바뀜), rules(정제 규칙이 바뀜), hash_rule(해시 규칙이 바뀜)
        self.failed_keys = Capped()
        self.failure_reasons = Counter()
        self.excluded_by_rule = Counter()
        self.newly_excluded = Capped()
        self.unexcluded = Capped()
        self.gone_keys: dict[str, Capped] = defaultdict(Capped)
        self.returned_keys = Capped()
        self.moved = Capped()
        self.masked = Counter()
        self.waiting = 0
        self.waiting_keys = Capped()
        self.waiting_max_days: int | None = None
        self.bot_parts_removed = 0
        self.bot_chars_removed = 0
        self.title_from = Counter()
        self.title_fallback = Capped()
        self.title_changed = Capped()
        self.title_none = Capped()
        self.published_at_from = Counter()
        self.published_at_missing = Capped()
        self.blog_same_title_pairs: Capped | None = None
        self.remaining = 0
        self.skipped = Capped()               # --only 로 받았지만 정제하지 않은 키(사라짐, 고정, 색인에 없음)
        self.totals: dict = {}
        self.errors: list[dict] = []
        self.phase = "plan"
        self.progress = {"selected": 0, "done": 0}
        self.timing = Counter()
        self.peak_rss_mb = 0.0
        self.log_tail: deque = deque(maxlen=50)  # 이 원천이 도는 동안의 WARNING 이상 로그
        self._t0 = time.monotonic()

    def fail(self, key: str, reason: str, message: str, consecutive: int) -> None:
        self.counts["failed"] += 1
        self.failure_reasons[reason] += 1
        self.failed_keys.add({"external_key": key, "reason": reason, "message": message, "consecutive": consecutive})

    def error(self, exc: BaseException, kind: str, now: datetime) -> None:
        self.errors.append({"type": kind, "message": f"{type(exc).__name__}: {exc}"[:500], "where": where(exc), "at": iso(now)})

    def set_phase(self, phase: str) -> None:
        now = time.monotonic()
        self.timing[f"{self.phase}_sec"] += now - self._t0
        self._t0 = now
        self.phase = phase

    def to_json(self) -> dict:
        out = {"started_at": iso(self.started)}
        if self.finished:
            out["finished_at"] = iso(self.finished)
            out["duration_sec"] = round((self.finished - self.started).total_seconds(), 1)
        out.update(self.counts)
        out.update({
            "changed_by": dict(self.changed_by),
            "failed_keys": self.failed_keys.to_json(),
            "failure_reasons": dict(self.failure_reasons),
            "excluded_by_rule": dict(self.excluded_by_rule),
            "newly_excluded": self.newly_excluded.to_json(),
            "unexcluded": self.unexcluded.to_json(),
            "totals": self.totals,
            "gone_keys": {k: v.to_json() for k, v in self.gone_keys.items()},
            "returned_keys": self.returned_keys.to_json(),
            "moved": self.moved.to_json(),
            "masked": dict(self.masked),
            "bot_parts_removed": self.bot_parts_removed,
            "bot_chars_removed": self.bot_chars_removed,
            "title_from": dict(self.title_from),
            "title_fallback": self.title_fallback.to_json(),
            "title_changed": self.title_changed.to_json(),
            "title_none": self.title_none.to_json(),
            "published_at_from": dict(self.published_at_from),
            "published_at_missing": self.published_at_missing.to_json(),
            "remaining": self.remaining,
            "skipped": self.skipped.to_json(),
            "errors": self.errors,
            "phase": "done" if self.finished else self.phase,
            "progress": dict(self.progress),
            "timing": {k: round(v, 1) for k, v in self.timing.items()},
            "peak_rss_mb": self.peak_rss_mb,
            "log_tail": list(self.log_tail),
        })
        if self.name == "youtube-tekotalk":
            out.update({"waiting": self.waiting, "waiting_keys": self.waiting_keys.to_json(),
                        "waiting_max_days": self.waiting_max_days})
        if self.blog_same_title_pairs is not None:
            out["blog_same_title_pairs"] = self.blog_same_title_pairs.to_json()
        return out


def _utc_now() -> datetime:
    return datetime.now(timezone.utc)


class RefineReport:
    def __init__(self, store: Store, run_id: str, trigger: str, host: str, code_version: str, scope: dict, started: datetime,
                 clock: Callable[[], datetime] = _utc_now) -> None:
        self.store, self.run_id, self.trigger, self.host = store, run_id, trigger, host
        self.code_version, self.scope, self.started, self.clock = code_version, scope, started, clock
        self.status = "running"
        self.finished: datetime | None = None
        self.updated: datetime | None = None
        self.sources: dict[str, SourceStats] = {}
        self.path = f"{REFINE_DIR}/reports/{run_id}.json"
        self._saved_mono: float | None = None
        self.log = logging.getLogger("refine")

    def to_json(self) -> dict:
        out = {"run_id": self.run_id, "trigger": self.trigger, "host": self.host, "code_version": self.code_version,
               "scope": self.scope, "started_at": iso(self.started), "status": self.status}
        if self.finished:
            out["finished_at"] = iso(self.finished)
            out["duration_sec"] = round((self.finished - self.started).total_seconds(), 1)
        if self.updated:
            out["updated_at"] = iso(self.updated)
        out["max_rss_mb"] = peak_rss_mb()
        out["sources"] = {k: v.to_json() for k, v in self.sources.items()}
        return out

    def write(self) -> None:
        self.updated = self.clock()
        self._saved_mono = time.monotonic()
        self.store.write_json(self.path, self.to_json())

    def tick(self) -> None:
        """마지막 저장 뒤 15초가 지났으면 저장한다. 저장이 실패해도(S3 오류 등) 정제는 계속한다."""
        if self._saved_mono is None or time.monotonic() - self._saved_mono >= SAVE_EVERY_SEC:
            try:
                self.write()
            except Exception:
                self.log.warning("정제 보고서 중간 저장 실패", exc_info=True)

    def finish(self, now: datetime, status: str | None = None) -> None:
        self.finished = now
        if status:
            self.status = status
        else:
            srcs = list(self.sources.values())
            broken = [s for s in srcs if s.errors]
            if srcs and len(broken) == len(srcs):
                self.status = "failure"
            elif broken or any(s.counts["failed"] for s in srcs):
                self.status = "partial_failure"
            else:
                self.status = "success"
        self.write()


__all__ = ["Capped", "LogTail", "RefineReport", "SourceStats"]
