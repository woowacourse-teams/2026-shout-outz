"""테코톡 자막 주고받기(04 문서 5-4, 08 문서 7-3).

    subtitles script (--all-missing | --only ID ...)   집 컴퓨터에서 돌릴 스크립트(subtitles_local.py 에 영상 ID 를 넣어)
    subtitles import <결과 파일>                         스크립트 결과를 원본에 저장

import 는 매일 실행과 같은 잠금(C9)을 쓰고 보고서를 남긴다(scope.command = "subtitles import", 08 문서 4-4).
자막을 채운 영상은 같은 원본 파일을 덮어쓰고 version 을 1 늘리므로 changed 로 센다.
"""

from __future__ import annotations

import json
import logging
import re
from datetime import datetime
from pathlib import Path
from typing import Callable

from . import lock
from .config import Settings
from .index import Index
from .keys import tekotalk_key
from .report import RunReport, SourceReport, iso, new_run_id
from .store import Store

log = logging.getLogger("collector.subtitles")

SOURCE = "youtube-tekotalk"
TEMPLATE = Path(__file__).with_name("subtitles_local.py")
PLACEHOLDER = "VIDEO_IDS = []\n"
RESULT_KIND = "tekotalk-subtitles"
# YouTube 영상 ID 모양. 스크립트(파이썬 파일)에 값을 넣으므로, 이 모양이 아니면 넣지 않는다(줄바꿈 등으로 코드가 끼어들지 않게)
VIDEO_ID = re.compile(r"[A-Za-z0-9_-]{11}")


def _video_id(value: str) -> str:
    return value[3:] if value.startswith("yt:") else value


def make_script(store: Store, only: list[str] | None = None, all_missing: bool = False) -> str:
    """집 컴퓨터에서 돌릴 스크립트 글. 색인만 읽고 원본은 열지 않는다(08 문서 4-1 has_subtitle)."""
    index = Index(store, SOURCE).load()
    notes: list[str] = []
    if all_missing:
        ids = [_video_id(e["external_key"]) for e in index.alive() if not e.get("has_subtitle")]
        bad = [v for v in ids if not VIDEO_ID.fullmatch(v)]
        if bad:
            notes.append(f"# 뺌(영상 ID 모양이 아님): {len(bad)}편")
        ids = sorted(v for v in ids if VIDEO_ID.fullmatch(v))
    else:
        ids = []
        for v in dict.fromkeys(only or []):
            vid = _video_id(v.strip())
            if not VIDEO_ID.fullmatch(vid):
                raise ValueError(f"영상 ID 모양이 아닙니다(영문, 숫자, -, _ 11자): {vid!r}")
            entry = index.get(tekotalk_key(vid))
            if entry is None:
                notes.append(f"# 뺌(색인에 없음, 매일 수집이 아직 받지 않은 영상): {vid}")
            elif entry.get("has_subtitle"):
                notes.append(f"# 뺌(이미 자막을 받음): {vid}")
            else:
                ids.append(vid)

    text = TEMPLATE.read_text(encoding="utf-8")
    if text.count(PLACEHOLDER) != 1:
        raise RuntimeError(f"스크립트 본에 '{PLACEHOLDER.strip()}' 줄이 한 번만 있어야 합니다: {TEMPLATE}")
    lines = [f"# 영상 {len(ids)}편({'아직 자막을 안 받은 영상 전부' if all_missing else '고른 영상'})", *notes]
    lines.append("VIDEO_IDS = [")
    lines += [f"    {json.dumps(v)}," for v in ids]
    lines.append("]")
    return text.replace(PLACEHOLDER, "\n".join(lines) + "\n")


def _valid(sub) -> bool:
    return (isinstance(sub, dict) and isinstance(sub.get("vtt"), str) and sub["vtt"].strip() != ""
            and isinstance(sub.get("lang"), str) and sub.get("kind") in ("manual", "auto"))


def _load_result(path: str) -> dict:
    data = json.loads(Path(path).read_text(encoding="utf-8"))
    if not isinstance(data, dict) or data.get("kind") != RESULT_KIND or not isinstance(data.get("results"), list):
        raise ValueError(f"자막 결과 파일이 아닙니다(kind 가 {RESULT_KIND!r} 이고 results 가 있어야 함)")
    return data


def _fill(store: Store, index: Index, rep: SourceReport, skipped: list[dict], results: list, at: str) -> None:
    """결과 한 줄마다: 색인에 있고 아직 자막이 없는 영상만 원본의 subtitle 을 채우고 version 을 1 늘린다."""
    for row in results:
        vid = row.get("video_id") if isinstance(row, dict) else None
        if not isinstance(vid, str) or not vid:
            skipped.append({"external_key": None, "reason": "no_video_id"})
            continue
        key = tekotalk_key(_video_id(vid))
        entry = index.get(key)
        if entry is None:
            skipped.append({"external_key": key, "reason": "not_in_index"})
            continue
        if entry.get("has_subtitle"):
            skipped.append({"external_key": key, "reason": "already_has_subtitle"})
            continue
        sub = row.get("subtitle")
        if not _valid(sub):
            skipped.append({"external_key": key, "reason": "invalid_subtitle"})
            continue
        try:
            record = store.read_json(entry["path"])
        except ValueError:  # 깨진 원본 한 건 때문에 뒤의 자막까지 멈추지 않게, 그 줄만 실패로 남긴다
            rep.fail(key, "raw_unreadable")
            continue
        if not isinstance(record, dict):
            rep.fail(key, "raw_missing" if record is None else "raw_unreadable")
            continue
        version = entry.get("version", 0) + 1
        record["version"] = version
        record["fetched_at"] = at  # 이 버전을 만든 시각. 자막을 받은 시각은 subtitle.fetched_at
        record["subtitle"] = {"lang": sub["lang"], "kind": sub["kind"], "format": sub.get("format") or "vtt",
                              "fetched_at": sub.get("fetched_at"), "vtt": sub["vtt"]}
        store.write_json(entry["path"], record)  # 원본 먼저(C4)
        index.put({**entry, "version": version, "fetched_at": record["fetched_at"], "has_subtitle": True})
        rep.add("changed")


def import_result(settings: Settings, store: Store, path: str,
                  clock: Callable[[], datetime] = lock.now_utc) -> RunReport:
    from .runner import code_version

    started = clock()
    run_id = new_run_id(started, settings.host)
    scope = {"command": "subtitles import", "file": Path(path).name}
    run = RunReport(store, run_id, settings.trigger, settings.host, code_version(), scope, started)

    got, held = lock.acquire(store, run_id, settings.host, started)
    if not got:
        log.warning("다른 실행이 잠금을 잡고 있어 건너뜀: %s", held)
        run.scope["lock_held_by"] = held
        run.finish(clock(), "skipped")
        return run
    try:
        now = clock()
        rep = SourceReport(SOURCE, now)
        run.sources[SOURCE] = rep
        run.write()
        index = Index(store, SOURCE).load()
        skipped: list[dict] = []
        rep.extra["skipped"] = skipped
        try:
            data = _load_result(path)
        except Exception as exc:
            log.exception("자막 결과 파일 읽기 실패: %s", path)
            rep.error(exc, "bad_file", clock())
            data = {"results": []}
        rep.extra["script_skipped"] = data.get("skipped") or []  # 집 컴퓨터에서 못 받은 영상(봇 확인, 자막 없음 등)

        try:
            _fill(store, index, rep, skipped, data["results"], iso(now))
        except Exception as exc:  # 예상 못 한 오류도 보고서에 남기고, 그때까지 채운 것은 색인에 저장한다
            log.exception("자막 저장 실패")
            rep.error(exc, "unexpected", clock())

        rep.extra["without_subtitle"] = sum(1 for e in index.alive() if not e.get("has_subtitle"))
        index.save()
        rep.finished = clock()
        log.info("자막 저장 끝: %s, 건너뜀 %d", rep.counts, len(skipped))
        run.finish(clock())
    finally:
        lock.release(store, run_id)
    return run
