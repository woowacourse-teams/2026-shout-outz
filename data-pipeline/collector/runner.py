"""여섯 원천이 함께 쓰는 흐름(08 문서 7-2).

원천 파일(sources/*.py)은 Source 를 상속해 "목록 받기"(list_items), "한 건 받기"(fetch),
"사라짐 확인"(confirm_gone), 색인 칸(index_fields)만 채운다. 아는 키인가, 바뀌었나, 저장, 색인, 보고서는 여기 한 곳에만 있다.

    1. 잠금 잡기, 보고서 running 쓰기           (collect)
    2. 색인 읽기(아는 키)                        (run_source)
    3. 목록 받기                                 Source.list_items
    4. 색인과 비교: 새 / 바뀐 / 같은 / 목록에 없는 키
    5. 새 키, 바뀐 키: 한 건 받기 → raw 쓰기 → 색인 고치기(200건마다 중간 저장)
    6. 목록을 끝까지 받은 묶음에서 목록에 없는 키: 사라짐 확인 → 색인 gone
    7. 색인, 상태 저장, 보고서 쓰기, 잠금 풀기
"""

from __future__ import annotations

import logging
import subprocess
from dataclasses import dataclass, field
from datetime import datetime
from pathlib import Path
from typing import Any, Callable

from . import lock
from .config import SOURCES, Settings
from .http import Http
from .index import Index
from .keys import raw_path
from .report import RunReport, SourceReport, iso, new_run_id, previous_failures
from .state import State
from .store import Store, open_store

log = logging.getLogger("collector")


@dataclass
class Item:
    """목록에서 나온 한 건."""
    key: str
    marker: dict | None = None  # 바뀜 표시(색인 칸 이름과 값). None 이면 아는 키는 다시 받지 않는다(블로그, 테코톡)
    group: str | None = None    # 사라짐 확인 묶음(레포, 블로그 첫 주소 등)
    data: Any = None            # 한 건 받기에 원천이 쓰는 값


@dataclass
class Listing:
    items: list[Item]
    # 목록을 끝까지 받은 묶음. 이 묶음의 아는 키 중 이번 목록에 없는 것만 사라짐을 확인한다(06 문서 5-7, 07 문서 5-6)
    complete_groups: set[str] = field(default_factory=set)


class FetchError(Exception):
    """한 건 받기 실패. reason 은 보고서 failure_reasons 의 종류(예: http_404, timeout)."""

    def __init__(self, reason: str, url: str | None = None) -> None:
        super().__init__(reason)
        self.reason = reason
        self.url = url


@dataclass
class Context:
    settings: Settings
    store: Store
    index: Index
    state: State
    report: SourceReport
    now: datetime
    only: list[str] | None = None
    repos: list[str] | None = None
    _http: Http | None = None
    _github: Any = None

    @property
    def http(self) -> Http:
        if self._http is None:
            self._http = Http()
        return self._http

    @property
    def github(self):
        if self._github is None:
            from .github import GitHub
            self._github = GitHub(self.settings.github_token)
        return self._github

    def now_iso(self) -> str:
        return iso(self.now)


class Source:
    name = ""

    def list_items(self, ctx: Context) -> Listing:
        raise NotImplementedError

    def only_items(self, ctx: Context, values: list[str]) -> list[Item]:
        """--only 값으로 목록 없이 받을 건을 만든다(블로그 글 주소, 테코톡 영상 ID, GitHub 원천 키)."""
        raise NotImplementedError

    def fetch(self, ctx: Context, item: Item) -> dict:
        """저장할 기록. source, external_key, version, fetched_at 은 runner 가 채운다. 실패는 FetchError."""
        raise NotImplementedError

    def fetch_many(self, ctx: Context, items: list[Item]):
        """받을 건들을 받아 (item, 기록 또는 예외) 를 차례로 내준다. 기본은 한 건씩 fetch.

        여러 건을 한 요청으로 묶어 받는 원천(PR, 07 문서 5-4)은 이것을 바꿔 쓴다. 메모리에 모아 두지 말고 묶음마다 내준다.
        """
        for item in items:
            try:
                yield item, self.fetch(ctx, item)
            except Exception as exc:  # FetchError 와 그 밖의 예외를 runner 가 보고서에 남긴다
                yield item, exc

    def index_fields(self, record: dict) -> dict:
        """색인 원천별 칸(바뀜 표시 포함, 08 문서 4-1). 값은 기록 안의 값을 그대로 옮긴다."""
        return {}

    def group_of(self, entry: dict) -> str | None:
        """색인 한 줄이 속한 사라짐 확인 묶음."""
        return None

    def confirm_gone(self, ctx: Context, entry: dict) -> str | None:
        """목록에 없는 아는 키가 정말 사라졌는지. 사라졌으면 이유(예: http_404), 아니면 None."""
        return None

    def finish(self, ctx: Context) -> None:
        """상태 저장, 보고서 원천별 칸 채우기 등."""


def is_changed(item: Item, entry: dict) -> bool:
    if item.marker is None:
        return False
    return any(entry.get(k) != v for k, v in item.marker.items())


def run_source(source: Source, ctx: Context) -> None:
    try:
        _run_source(source, ctx)
    except BaseException:
        # Ctrl+C, systemd 의 멈춤(SIGTERM) 등으로 중간에 끝나도 받은 데까지 색인을 남긴다.
        # 원본을 먼저 쓰고 색인을 고치므로(C4) 색인에 있는 키는 원본도 있다. 다음 실행은 나머지만 받는다
        try:
            ctx.index.save()
        except Exception:
            log.exception("%s 멈춘 뒤 색인 저장 실패", source.name)
        raise


def _run_source(source: Source, ctx: Context) -> None:
    rep, index = ctx.report, ctx.index
    seen: set[str] = set()
    complete: set[str] = set()
    try:
        if ctx.only:
            items = source.only_items(ctx, ctx.only)
        else:
            listing = source.list_items(ctx)
            items, complete = listing.items, listing.complete_groups
    except Exception as exc:  # 목록 받기 자체가 막힘: 원천 전체 오류(4-3 errors)
        log.exception("%s 목록 받기 실패", source.name)
        rep.error(exc, "list_failed", ctx.now)
        items = []

    to_fetch: list[Item] = []
    for item in items:
        if item.key in seen:  # 한 실행에 같은 키는 한 번(03 문서 4-3)
            continue
        seen.add(item.key)
        entry = index.get(item.key)
        if entry and entry.get("gone"):
            index.clear_gone(item.key)  # 다시 나타나면 gone 을 지운다(C6)
        if entry and not ctx.only and not is_changed(item, entry):
            rep.add("unchanged")
            continue
        to_fetch.append(item)

    for item, got in source.fetch_many(ctx, to_fetch):
        if isinstance(got, FetchError):
            rep.fail(item.key, got.reason, got.url)
            continue
        if isinstance(got, BaseException):
            log.error("%s 한 건 받기 실패: %s: %r", source.name, item.key, got)
            rep.fail(item.key, f"error_{type(got).__name__}")
            continue
        body = got
        entry = index.get(item.key)
        fields = source.index_fields(body)
        if entry and ctx.only and not entry.get("gone") and fields and all(entry.get(k) == v for k, v in fields.items()):
            rep.add("unchanged")  # --only 로 받아 봤지만 바뀜 표시가 같다
            continue
        version = (entry or {}).get("version", 0) + 1
        record = {"source": source.name, "external_key": item.key, "version": version, "fetched_at": ctx.now_iso(), **body}
        path = raw_path(source.name, item.key)
        ctx.store.write_json(path, record)  # 원본 먼저(C4)
        index.put({"external_key": item.key, "path": path, "version": version,
                   "fetched_at": record["fetched_at"], "gone": None, **fields})
        rep.add("changed" if entry else "new")

    if not ctx.only:
        for entry in list(index.alive()):
            key = entry["external_key"]
            if key in seen or source.group_of(entry) not in complete:
                continue
            try:
                reason = source.confirm_gone(ctx, entry)
            except Exception as exc:
                log.exception("%s 사라짐 확인 실패: %s", source.name, key)
                rep.fail(key, f"gone_check_{type(exc).__name__}")
                continue
            if reason:
                index.mark_gone(key, ctx.now_iso(), reason)
                rep.add("gone")
                rep.gone_keys.append(key)

    # 색인을 먼저 저장한다. finish 가 상태(위키 커밋 등)를 먼저 저장했는데 색인 저장이 실패하면,
    # 다음 실행이 그 상태를 보고 받은 문서를 다시 보지 않게 된다(08 문서 4-5)
    index.save()
    try:
        source.finish(ctx)
    except Exception as exc:
        rep.error(exc, "finish_failed", ctx.now)
    if index._pending:
        index.save()  # finish 가 색인을 고쳤으면 한 번 더


def code_version() -> str:
    try:
        out = subprocess.run(["git", "rev-parse", "--short", "HEAD"], cwd=Path(__file__).parent,
                             capture_output=True, text=True, timeout=5)
        return out.stdout.strip() or "unknown"
    except Exception:
        return "unknown"


def collect(settings: Settings, sources: list[str] | None = None, only: list[str] | None = None,
            repos: list[str] | None = None, store: Store | None = None,
            make_source: Callable[[str], Source] | None = None, clock: Callable[[], datetime] = lock.now_utc) -> RunReport:
    from .sources import make as default_make
    make_source = make_source or default_make
    store = store or open_store(settings.store_url)
    names = [s for s in SOURCES if s in sources] if sources else list(SOURCES)
    started = clock()
    run_id = new_run_id(started, settings.host)
    scope = {"sources": names, "only": only, "repos": repos}
    run = RunReport(store, run_id, settings.trigger, settings.host, code_version(), scope, started)

    got, held = lock.acquire(store, run_id, settings.host, started)
    if not got:
        log.warning("다른 실행이 잠금을 잡고 있어 건너뜀: %s", held)
        run.scope["lock_held_by"] = held
        run.finish(clock(), "skipped")
        return run
    rep = None
    try:
        run.write()
        for name in names:
            now = clock()
            rep = SourceReport(name, now, previous_failures(store, name, exclude_run=run_id))
            run.sources[name] = rep
            log.info("%s 시작", name)
            try:
                # 색인, 상태 읽기도 원천별 try 안에서 한다. 깨진 파일이 있어도 그 원천만 오류로 남기고 보고서를 끝까지 쓴다
                ctx = Context(settings=settings, store=store, index=Index(store, name).load(),
                              state=State(store, name).load(), report=rep, now=now, only=only, repos=repos)
                run_source(make_source(name), ctx)
            except Exception as exc:
                log.exception("%s 실패", name)
                rep.error(exc, "unexpected", clock())
            rep.finished = clock()
            log.info("%s 끝: %s", name, rep.counts)
            run.write()  # 원천 하나가 끝날 때마다(4-3)
        run.finish(clock())
    except BaseException as exc:
        # Ctrl+C, SIGTERM 으로 멈춤: 보고서가 running 으로 남지 않게 failure 와 멈춘 원천의 interrupted 오류를 남긴다
        if rep is not None and rep.finished is None:
            rep.error(exc, "interrupted", clock())
            rep.finished = clock()
        try:
            run.finish(clock(), "failure")
        except Exception:
            log.exception("멈춘 실행의 보고서 쓰기 실패")
        raise
    finally:
        lock.release(store, run_id)
    return run
