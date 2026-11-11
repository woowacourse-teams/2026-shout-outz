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

보고서는 원천이 도는 동안에도 단계가 바뀔 때(list, fetch, gone, finish)와 마지막 저장 뒤 SAVE_EVERY(15초)가 지났을 때
덮어쓴다(Context.set_phase, Context.tick). 원천은 레포나 블로그 목록을 받기 시작할 때 ctx.begin_group 을 부른다.
"""

from __future__ import annotations

import logging
import subprocess
from dataclasses import dataclass, field
from datetime import datetime, timedelta
from pathlib import Path
from typing import Any, Callable

from . import lock
from .config import SOURCES, Settings
from .http import Http
from .index import Index
from .keys import raw_path
from .report import LogTail, RunReport, SourceReport, iso, new_run_id, peak_rss_mb, previous_failures
from .state import State
from .store import Store, open_store

log = logging.getLogger("collector")

SAVE_EVERY = timedelta(seconds=15)  # 원천이 도는 동안 보고서를 덮어쓰는 최소 간격. 관리 웹이 10초마다 다시 그리므로 15초(2026-11-06, 처음 60초)


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
    clock: Callable[[], datetime] = lock.now_utc
    save_report: Callable[[], None] | None = None  # 보고서 중간 저장(실패해도 예외를 내지 않는 함수)
    _source: Any = None
    _phase_t: datetime | None = None   # 지금 단계를 시작한 시각
    _mark: datetime | None = None      # 묶음 시간을 마지막으로 잰 시각
    _group: str | None = None          # 목록 받기에서 지금 열려 있는 묶음
    _saved_t: datetime | None = None   # 보고서를 마지막으로 저장한 시각

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
            self._github.on_wait = self._on_wait
            self._github.on_wait_done = self._on_wait_done
        return self._github

    def _on_wait(self, until: datetime, seconds: float) -> None:
        """GitHub 한도 대기를 시작함. 보고서에 적어 관리 웹이 멈춘 것이 아니라 기다리는 중임을 보이게 한다."""
        self.report.waiting = {"reason": "github_rate_limit", "until": iso(until), "seconds": round(seconds)}
        self.save()

    def _on_wait_done(self) -> None:
        self.report.waiting = None
        self.save()

    def now_iso(self) -> str:
        return iso(self.now)

    # 모니터링(보고서 phase, current, progress, timing, slowest, 중간 저장)

    def begin_group(self, name: str) -> None:
        """원천이 묶음(레포, 블로그) 하나의 목록을 받기 시작할 때 부른다. 앞 묶음의 시간을 닫고 current 를 바꾼다."""
        t = self.clock()
        self._spend(t, self._group)
        self._group = name
        self.report.current = name
        self.tick(t)

    def fetched(self, item: "Item") -> None:
        """받기 한 건을 처리함(성공, 실패 모두). 지난 처리 뒤 걸린 시간을 그 건의 묶음에 더한다."""
        t = self.clock()
        self.report.progress["fetched"] += 1
        self._spend(t, item.group, 1)
        if item.group is not None:
            self.report.current = item.group
        self.tick(t)

    def set_phase(self, phase: str) -> None:
        """단계를 바꾸고 보고서를 저장한다."""
        t = self.clock()
        self._close(t)
        self.report.phase = phase
        self.report.current = None
        self.save(t)

    def end(self) -> None:
        """원천이 끝남(오류, 멈춤 포함). 지금 단계 시간을 닫고 요청 수, 메모리를 모은다. 저장은 collect 가 한다."""
        if self._phase_t is not None:
            self._close(self.clock())
            self._phase_t = None
        self.refresh_usage()

    def tick(self, t: datetime | None = None) -> None:
        """마지막 저장 뒤 SAVE_EVERY 가 지났으면 저장한다."""
        t = t or self.clock()
        if self._saved_t is None or t - self._saved_t >= SAVE_EVERY:
            self.save(t)

    def save(self, t: datetime | None = None) -> None:
        self.refresh_usage()
        self._saved_t = t or self.clock()
        if self.save_report:
            self.save_report()

    def _start(self, source: "Source") -> None:
        t = self.clock()
        self._source = source
        self._phase_t = self._mark = self._saved_t = t
        self.report.phase = "list"

    def _spend(self, t: datetime, group: str | None, docs: int = 0) -> None:
        if group is not None and self._mark is not None:
            self.report.add_group(group, (t - self._mark).total_seconds(), docs)
        self._mark = t

    def _close(self, t: datetime) -> None:
        if self.report.phase == "list":
            self._spend(t, self._group)  # 목록 받기에서 마지막으로 열린 묶음
        self._group = None
        self._mark = t
        if self._phase_t is not None:
            self.report.add_time(self.report.phase, (t - self._phase_t).total_seconds())
        self._phase_t = t

    def refresh_usage(self) -> None:
        """이 원천이 쓴 Http, GitHub 객체에서 요청 수와 대기 시간을 모은다. 같은 객체는 한 번만 센다."""
        src = self._source
        ghs = _unique([self._github, getattr(src, "_gh", None), getattr(src, "_github", None)])
        https = _unique([self._http, getattr(src, "_http", None), *(getattr(g, "http", None) for g in ghs)])
        rep = self.report
        rep.requests = {"http": sum(_num(h, "calls") for h in https),
                        "github_rest": sum(_num(g, "rest_calls") for g in ghs),
                        "github_graphql_points": sum(_num(g, "graphql_points") for g in ghs)}
        rep.waits = {"rate_limit_sec": float(sum(_num(g, "waited_sec") for g in ghs)),
                     "polite_sec": float(sum(_num(h, "polite_sec") for h in https)),
                     "retry_sec": float(sum(_num(h, "retry_sec") for h in https))}
        rep.peak_rss_mb = peak_rss_mb()
        rep.peak_rss_children_mb = peak_rss_mb(children=True)


def _unique(objs) -> list:
    out, seen = [], set()
    for o in objs:
        if o is not None and id(o) not in seen:
            seen.add(id(o))
            out.append(o)
    return out


def _num(obj, name: str) -> float:
    """숫자 칸이면 그 값, 없거나 숫자가 아니면 0(시험의 가짜 객체는 calls 가 목록이기도 하다)."""
    v = getattr(obj, name, 0)
    return v if isinstance(v, (int, float)) and not isinstance(v, bool) else 0


class Source:
    name = ""
    # --only 로 받은 아는 문서를 바뀜 표시가 같아도 새 버전으로 덮어쓸지. 바뀜 표시가 내용을 따라가지 않는 원천(블로그)만 켠다
    only_overwrites = False

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

    def confirm_gone(self, ctx: Context, entry: dict) -> str | dict | None:
        """목록에 없는 아는 키가 정말 사라졌는지. 사라졌으면 이유(예: http_404), 아니면 None.

        이유에 값을 더 남길 때는 dict(예: {"reason": "repo_renamed", "renamed_to": "2026-New"}).
        """
        return None

    def recheck_gone(self, ctx: Context) -> None:
        """사라짐으로 기록한 문서를 다시 열어 봐 되살아났으면 사라짐 표시를 지운다. 기본은 하지 않음(블로그만, R18-9)."""

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
    ctx._start(source)
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
    rep.progress["listed"] = len(items)

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
    rep.progress["to_fetch"] = len(to_fetch)

    ctx.set_phase("fetch")
    for item, got in source.fetch_many(ctx, to_fetch):
        ctx.fetched(item)
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
        if (entry and ctx.only and not source.only_overwrites and not entry.get("gone") and fields
                and all(entry.get(k) == v for k, v in fields.items())):
            rep.add("unchanged")  # --only 로 받아 봤지만 바뀜 표시가 같다
            continue
        version = (entry or {}).get("version", 0) + 1
        record = {"source": source.name, "external_key": item.key, "version": version, "fetched_at": ctx.now_iso(), **body}
        path = raw_path(source.name, item.key)
        ctx.store.write_json(path, record)  # 원본 먼저(C4)
        index.put({"external_key": item.key, "path": path, "version": version,
                   "fetched_at": record["fetched_at"], "gone": None, **fields})
        rep.add("changed" if entry else "new")

    ctx.set_phase("gone")
    if not ctx.only:
        for entry in list(index.alive()):
            key = entry["external_key"]
            group = source.group_of(entry)
            if key in seen or group not in complete:
                continue
            rep.current = group
            ctx.tick()
            try:
                reason = source.confirm_gone(ctx, entry)
            except Exception as exc:
                log.exception("%s 사라짐 확인 실패: %s", source.name, key)
                rep.fail(key, f"gone_check_{type(exc).__name__}")
                continue
            if reason:
                extra = dict(reason) if isinstance(reason, dict) else {"reason": reason}
                index.mark_gone(key, ctx.now_iso(), extra.pop("reason"), **extra)
                rep.add("gone")
                rep.gone_keys.append(key)
        try:
            source.recheck_gone(ctx)
        except Exception as exc:  # 다시 확인은 덤이라 실패해도 원천 전체를 오류로 만들지 않는다
            log.exception("%s 사라진 문서 다시 확인 실패", source.name)
            rep.fail("(gone_recheck)", f"gone_recheck_{type(exc).__name__}")

    # 색인을 먼저 저장한다. finish 가 상태(위키 커밋 등)를 먼저 저장했는데 색인 저장이 실패하면,
    # 다음 실행이 그 상태를 보고 받은 문서를 다시 보지 않게 된다(08 문서 4-5)
    ctx.set_phase("finish")
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
    run = RunReport(store, run_id, settings.trigger, settings.host, code_version(), scope, started, clock=clock)

    def save_quietly() -> None:
        # 중간 저장이 실패해도(S3 오류 등) 수집은 계속한다. 원천이 끝날 때 다시 쓴다
        try:
            run.write()
        except Exception:
            log.warning("보고서 중간 저장 실패", exc_info=True)

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
            tail = LogTail(rep.log_tail)
            logging.getLogger().addHandler(tail)  # 이 원천이 도는 동안의 WARNING 이상 로그(라이브러리 로그 포함)
            ctx = None
            save_quietly()  # 원천이 시작됐음을 바로 보이게
            try:
                # 색인, 상태 읽기도 원천별 try 안에서 한다. 깨진 파일이 있어도 그 원천만 오류로 남기고 보고서를 끝까지 쓴다
                ctx = Context(settings=settings, store=store, index=Index(store, name).load(),
                              state=State(store, name).load(), report=rep, now=now, only=only, repos=repos,
                              clock=clock, save_report=save_quietly)
                run_source(make_source(name), ctx)
            except Exception as exc:
                log.exception("%s 실패", name)
                rep.error(exc, "unexpected", clock())
            finally:
                logging.getLogger().removeHandler(tail)
                if ctx is not None:
                    ctx.end()
                else:
                    rep.peak_rss_mb, rep.peak_rss_children_mb = peak_rss_mb(), peak_rss_mb(children=True)
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
