"""원본 묶음 넣기(`bundle import <파일>`).

매일 수집이 받지 않는 문서를 원본 저장소에 같은 모양으로 넣는 명령이다. 처음 쓰임은 1~7기 팀 블로그 글이다
(2026-11-06 사용자). 1~7기는 다시 수집하지 않지만(03 문서 2-1), 예전에 받아 둔 본문을 8기 글과 같은 원본 모양으로 두어
정제부터 DB 적재까지 같은 길을 타게 한다.

- 묶음 파일: 한 줄에 원본 기록 하나(JSON Lines, .gz 가능). 칸은 수집기가 저장하는 원본과 같다
  (`source`, `external_key`, 원천별 칸). 블로그는 `url`, `blog_root`, `platform`, `found_in`, `body_format`, `body`.
- 묶음을 만드는 일(예전 데이터 읽기, 모양 바꾸기)은 이 명령이 하지 않는다. 그래서 수집기 코드는 예전 데이터를 모른다.
- 키 확인: 블로그는 `external_key` 가 `url` 로 만든 키(CLAUDE.md 6단계)와 같아야 넣는다. 다르면 운영 DB 의 같은 글과
  이어지지 않으므로 넣지 않고 실패로 남긴다.
- 이미 아는 키는 건너뛴다(unchanged). 매일 수집이 받은 문서를 묶음이 덮어쓰지 않게.
- 매일 실행과 같은 잠금을 쓰고 보고서를 남긴다(scope.command = "bundle import").
"""

from __future__ import annotations

import gzip
import json
import logging
from datetime import datetime
from pathlib import Path
from typing import Callable, Iterator

from . import lock
from .config import SOURCES, Settings
from .index import Index
from .keys import blog_key, raw_path
from .report import RunReport, SourceReport, iso, new_run_id
from .store import Store

log = logging.getLogger("collector.bundle")


def read_lines(path: str) -> Iterator[dict]:
    opener = gzip.open if path.endswith(".gz") else open
    with opener(path, "rt", encoding="utf-8") as f:
        for n, line in enumerate(f, 1):
            if line.strip():
                try:
                    yield json.loads(line)
                except ValueError:
                    yield {"_bad_line": n}


def _check(record: dict) -> str | None:
    """넣을 수 없는 이유, 넣어도 되면 None."""
    if record.get("_bad_line"):
        return "bad_json"
    source, key = record.get("source"), record.get("external_key")
    if source not in SOURCES or not isinstance(key, str) or not key:
        return "bad_record"
    if source == "blog-post":
        url = record.get("url")
        if not isinstance(url, str) or not url or not record.get("blog_root") or "body" not in record:
            return "bad_record"
        if blog_key(url) != key:
            return "key_mismatch"
    return None


def import_bundle(settings: Settings, store: Store, path: str, make_source: Callable | None = None,
                  clock: Callable[[], datetime] = lock.now_utc) -> RunReport:
    from .runner import code_version
    from .sources import make as default_make
    make_source = make_source or default_make

    started = clock()
    run_id = new_run_id(started, settings.host)
    run = RunReport(store, run_id, settings.trigger, settings.host, code_version(),
                    {"command": "bundle import", "file": Path(path).name}, started)
    got, held = lock.acquire(store, run_id, settings.host, started)
    if not got:
        log.warning("다른 실행이 잠금을 잡고 있어 건너뜀: %s", held)
        run.scope["lock_held_by"] = held
        run.finish(clock(), "skipped")
        return run
    indexes: dict[str, Index] = {}
    makers: dict[str, object] = {}
    try:
        run.write()
        for record in read_lines(path):
            source = record.get("source")
            why = _check(record)
            if source in SOURCES and source not in run.sources:
                run.sources[source] = SourceReport(source, clock())
            rep = run.sources.get(source)
            if why:
                if rep is None:  # 원천을 알 수 없는 줄은 보고서 맨 앞 원천에 모아 둔다
                    rep = run.sources.setdefault("blog-post", SourceReport("blog-post", clock()))
                rep.fail(record.get("external_key") or f"line:{record.get('_bad_line', '?')}", why, record.get("url"))
                continue
            index = indexes.get(source)
            if index is None:
                index = indexes[source] = Index(store, source).load()
            key = record["external_key"]
            if index.get(key):
                rep.add("unchanged")
                continue
            body = {k: v for k, v in record.items() if k not in ("source", "external_key", "version", "fetched_at")}
            out = {"source": source, "external_key": key, "version": 1,
                   "fetched_at": record.get("fetched_at") or iso(clock()), **body}
            dest = raw_path(source, key)
            store.write_json(dest, out)  # 원본 먼저(C4)
            index.put({"external_key": key, "path": dest, "version": 1, "fetched_at": out["fetched_at"],
                       "gone": None, **makers.setdefault(source, make_source(source)).index_fields(out)})
            rep.add("new")
        for index in indexes.values():
            index.save()
        for rep in run.sources.values():
            rep.finished = clock()
        run.finish(clock())
    except BaseException as exc:
        for index in indexes.values():  # 받은 데까지 색인을 남긴다(원본을 먼저 썼으므로 어긋나지 않음)
            try:
                index.save()
            except Exception:
                log.exception("색인 저장 실패")
        for rep in run.sources.values():
            if rep.finished is None:
                rep.error(exc, "interrupted" if not isinstance(exc, Exception) else "unexpected", clock())
                rep.finished = clock()
        run.finish(clock(), "failure")
        if not isinstance(exc, Exception):
            raise
    finally:
        lock.release(store, run_id)
    return run
