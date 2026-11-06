"""실행 보고서 reports/{run_id}.json(08 문서 C7, 4-3, 4-4).

실행이 시작되면 status "running" 으로 먼저 쓰고, 원천 하나가 끝날 때마다 덮어쓴다.
원천이 도는 동안에도 단계가 바뀔 때와 60초마다 덮어쓴다(모니터링 칸: phase, current, progress, timing 등).
"""

from __future__ import annotations

import logging
import secrets
import sys
import traceback
from collections import deque
from datetime import datetime, timezone
from typing import Callable

from .store import Store

PHASES = ("list", "fetch", "gone", "finish")
LOG_TAIL = 50     # 보고서에 남길 WARNING 이상 로그 수
SLOWEST = 10      # 보고서에 남길 오래 걸린 묶음 수


def iso(t: datetime) -> str:
    return t.strftime("%Y-%m-%dT%H:%M:%SZ")


def new_run_id(now: datetime, host: str) -> str:
    return f"{now.strftime('%Y-%m-%dT%H-%M-%SZ')}-{host}-{secrets.token_hex(2)}"


def peak_rss_mb(children: bool = False) -> float:
    """프로세스(또는 끝난 자식 프로세스) 최대 메모리 MB. ru_maxrss 는 Linux 는 KB, macOS 는 바이트다."""
    try:
        import resource
    except ImportError:  # Windows
        return 0.0
    v = resource.getrusage(resource.RUSAGE_CHILDREN if children else resource.RUSAGE_SELF).ru_maxrss
    return round(v / (1024 * 1024 if sys.platform == "darwin" else 1024), 1)


def where(exc: BaseException) -> str:
    """예외가 난 마지막 자리(파일:줄 함수)."""
    tb = traceback.extract_tb(exc.__traceback__)
    return f"{tb[-1].filename.rsplit('/', 1)[-1]}:{tb[-1].lineno} {tb[-1].name}" if tb else ""


class LogTail(logging.Handler):
    """원천이 도는 동안 WARNING 이상 로그를 보고서 log_tail 에 모은다(마지막 LOG_TAIL 개)."""

    def __init__(self, rows: deque) -> None:
        super().__init__(logging.WARNING)
        self.rows = rows

    def emit(self, record: logging.LogRecord) -> None:
        try:
            msg = record.getMessage()
        except Exception:
            msg = str(record.msg)
        exc = record.exc_info[1] if record.exc_info else None
        if exc is not None:
            msg += f" | {where(exc)} {type(exc).__name__}: {exc}"
        at = datetime.fromtimestamp(record.created, timezone.utc)
        self.rows.append({"at": iso(at), "level": "ERROR" if record.levelno >= logging.ERROR else "WARNING",
                          "message": msg[:500]})


class SourceReport:
    def __init__(self, name: str, started: datetime, prev_failures: dict[str, int] | None = None) -> None:
        self.name = name
        self.started = started
        self.finished: datetime | None = None
        self.counts = {"new": 0, "changed": 0, "unchanged": 0, "gone": 0, "failed": 0}
        self.gone_keys: list[str] = []
        self.failures: list[dict] = []
        self.failure_reasons: dict[str, int] = {}
        self.errors: list[dict] = []
        self.extra: dict = {}  # 원천별 칸(4-3, 4-4)
        self._prev_failures = prev_failures or {}
        # 모니터링 칸. runner 의 Context 가 채운다. 자막 저장(subtitles import)은 기본값 그대로 쓴다
        self.phase = "list"
        self.current: str | None = None
        self.progress = {"listed": 0, "to_fetch": 0, "fetched": 0}
        self.timing = {f"{p}_sec": 0.0 for p in PHASES}
        self.requests = {"http": 0, "github_rest": 0, "github_graphql_points": 0}
        self.waits = {"rate_limit_sec": 0.0, "polite_sec": 0.0, "retry_sec": 0.0}
        self.groups: dict[str, list] = {}  # 묶음 → [걸린 초, 받기를 처리한 건수]
        self.peak_rss_mb = 0.0
        self.peak_rss_children_mb = 0.0
        self.log_tail: deque = deque(maxlen=LOG_TAIL)

    def add(self, kind: str, n: int = 1) -> None:
        self.counts[kind] += n

    def fail(self, key: str, reason: str, url: str | None = None) -> None:
        self.counts["failed"] += 1
        row = {"external_key": key, "reason": reason, "consecutive": self._prev_failures.get(key, 0) + 1}
        if url:
            row["url"] = url
        self.failures.append(row)
        self.failure_reasons[reason] = self.failure_reasons.get(reason, 0) + 1

    def error(self, exc: BaseException, kind: str, now: datetime) -> None:
        self.errors.append({"type": kind, "message": f"{type(exc).__name__}: {exc}"[:500], "where": where(exc), "at": iso(now)})

    def add_time(self, phase: str, sec: float) -> None:
        key = f"{phase}_sec"
        if key in self.timing:
            self.timing[key] += sec

    def add_group(self, name: str, sec: float, docs: int = 0) -> None:
        g = self.groups.setdefault(name, [0.0, 0])
        g[0] += sec
        g[1] += docs

    def slowest(self) -> list[dict]:
        top = sorted(self.groups.items(), key=lambda kv: kv[1][0], reverse=True)[:SLOWEST]
        return [{"group": k, "sec": round(v[0], 1), "docs": v[1]} for k, v in top]

    def partial(self) -> bool:
        """원천 전체는 돌았지만 묶음 일부(레포 하나, 블로그 하나)의 목록을 받지 못했나(원천별 칸으로 알림)."""
        if self.extra.get("repos_failed"):
            return True
        return any(str(n.get("type", "")).endswith("_failed") for n in self.extra.get("notices") or [])

    def to_json(self) -> dict:
        out = {"started_at": iso(self.started)}
        if self.finished:
            out["finished_at"] = iso(self.finished)
            out["duration_sec"] = round((self.finished - self.started).total_seconds(), 1)
        out.update(self.counts)
        out.update({"gone_keys": self.gone_keys, "failures": self.failures,
                    "failure_reasons": self.failure_reasons, "errors": self.errors})
        out.update({
            "phase": "done" if self.finished else self.phase,
            "current": None if self.finished else self.current,
            "progress": dict(self.progress),
            "timing": {k: round(v, 1) for k, v in self.timing.items()},
            "requests": dict(self.requests),
            "waits": {k: round(v, 1) for k, v in self.waits.items()},
            "slowest": self.slowest(),
            "peak_rss_mb": self.peak_rss_mb,
            "peak_rss_children_mb": self.peak_rss_children_mb,
            "log_tail": list(self.log_tail),
        })
        out.update(self.extra)
        return out


def _utc_now() -> datetime:
    return datetime.now(timezone.utc)


class RunReport:
    def __init__(self, store: Store, run_id: str, trigger: str, host: str, code_version: str, scope: dict, started: datetime,
                 clock: Callable[[], datetime] = _utc_now) -> None:
        self.store, self.run_id, self.trigger, self.host = store, run_id, trigger, host
        self.code_version, self.scope, self.started = code_version, scope, started
        self.clock = clock  # updated_at 시각(시험에서 바꿀 수 있게)
        self.updated: datetime | None = None
        self.finished: datetime | None = None
        self.status = "running"
        self.sources: dict[str, SourceReport] = {}
        self.path = f"reports/{run_id}.json"

    def to_json(self) -> dict:
        out = {"run_id": self.run_id, "trigger": self.trigger, "host": self.host, "code_version": self.code_version,
               "scope": self.scope, "started_at": iso(self.started), "status": self.status}
        if self.finished:
            out["finished_at"] = iso(self.finished)
            out["duration_sec"] = round((self.finished - self.started).total_seconds(), 1)
        if self.updated:
            out["updated_at"] = iso(self.updated)  # 관리 웹이 멈춘 실행을 알아보는 데 쓴다
        out["peak_rss_mb"] = peak_rss_mb()
        out["sources"] = {k: v.to_json() for k, v in self.sources.items()}
        return out

    def write(self) -> None:
        self.updated = self.clock()
        self.store.write_json(self.path, self.to_json())

    def finish(self, now: datetime, status: str | None = None) -> None:
        self.finished = now
        if status:
            self.status = status
        else:
            srcs = list(self.sources.values())
            broken = [s for s in srcs if s.errors]
            if srcs and len(broken) == len(srcs):
                self.status = "failure"
            elif broken or any(s.counts["failed"] or s.partial() for s in srcs):
                self.status = "partial_failure"
            else:
                self.status = "success"
        self.write()


def previous_failures(store: Store, source: str, exclude_run: str | None = None) -> dict[str, int]:
    """바로 전 실행 보고서에서 그 원천의 실패 키별 이어서 실패한 횟수(3번이면 보고서에 따로 모은다, 08 문서 8장)."""
    for path in reversed(store.list("reports/")):
        if exclude_run and path.endswith(f"{exclude_run}.json"):
            continue
        rep = store.read_json(path) or {}
        if (rep.get("scope") or {}).get("command"):  # 자막 결과 저장 같은 따로 된 명령은 수집 실행이 아니다
            continue
        src = (rep.get("sources") or {}).get(source)
        if src is not None:
            return {f["external_key"]: int(f.get("consecutive", 1)) for f in src.get("failures", [])}
    return {}
