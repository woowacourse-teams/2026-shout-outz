"""실행 보고서 reports/{run_id}.json(08 문서 C7, 4-3, 4-4).

실행이 시작되면 status "running" 으로 먼저 쓰고, 원천 하나가 끝날 때마다 덮어쓴다.
"""

from __future__ import annotations

import secrets
import traceback
from datetime import datetime

from .store import Store


def iso(t: datetime) -> str:
    return t.strftime("%Y-%m-%dT%H:%M:%SZ")


def new_run_id(now: datetime, host: str) -> str:
    return f"{now.strftime('%Y-%m-%dT%H-%M-%SZ')}-{host}-{secrets.token_hex(2)}"


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
        tb = traceback.extract_tb(exc.__traceback__)
        where = f"{tb[-1].filename.rsplit('/', 1)[-1]}:{tb[-1].lineno} {tb[-1].name}" if tb else ""
        self.errors.append({"type": kind, "message": f"{type(exc).__name__}: {exc}"[:500], "where": where, "at": iso(now)})

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
        out.update(self.extra)
        return out


class RunReport:
    def __init__(self, store: Store, run_id: str, trigger: str, host: str, code_version: str, scope: dict, started: datetime) -> None:
        self.store, self.run_id, self.trigger, self.host = store, run_id, trigger, host
        self.code_version, self.scope, self.started = code_version, scope, started
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
        out["sources"] = {k: v.to_json() for k, v in self.sources.items()}
        return out

    def write(self) -> None:
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
