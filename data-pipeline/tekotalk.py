"""테코톡 원본 최초 적재 — 재생목록 영상의 메타데이터와 자막(결정 기록 23번).

yt-dlp 로 영상 파일은 받지 않고(skip_download) 메타데이터와 자막만 받는다. 사람이 단 한국어 자막이 있으면 그것,
없으면 자동 생성 한국어 자막. 영상 사이 DELAY 초를 쉬고, 429 · 봇 확인 요구가 나오면 즉시 멈춘다.
자막 원문은 내부 원본 전용이다(화면에는 요약만, 3-2절).
원본 목록은 원천별 파일(raw_objects/youtube-tekotalk.jsonl).

실행: uv run python tekotalk.py [--limit N]
"""
from __future__ import annotations

import argparse
import json
import random
import re
import tempfile
import time
from pathlib import Path

import yt_dlp

from teamdocs import DATA, Store, atomic_write, now, stamp

PLAYLIST = "https://www.youtube.com/playlist?list=PLgXGHBqgT2TvpJ_p9L_yZKPifgdBOzdVH"
DELAY = (4.0, 7.0)  # 영상 사이 대기(초), 무작위
META_KEYS = ["id", "title", "description", "upload_date", "timestamp", "release_timestamp", "duration", "view_count",
             "like_count", "comment_count", "tags", "categories", "chapters", "channel", "channel_id", "uploader",
             "thumbnail", "language", "availability", "live_status", "webpage_url", "age_limit"]
STOP_SIGNS = ("HTTP Error 429", "Sign in to confirm", "not a bot", "Too Many Requests")


class Quiet:
    def debug(self, m): pass
    def info(self, m): pass
    def warning(self, m): pass
    def error(self, m): pass


def vtt_to_text(vtt: str) -> str:
    """VTT 에서 시간 · 태그를 지우고 자동 자막의 겹치는 줄을 합쳐 읽을 수 있는 글로 만든다(원문 VTT 는 따로 보관)."""
    out: list[str] = []
    for line in vtt.splitlines():
        line = line.strip()
        if not line or line == "WEBVTT" or "-->" in line or re.match(r"^(Kind|Language|NOTE)\b", line) or line.isdigit():
            continue
        line = re.sub(r"<[^>]+>", "", line).strip()
        if line and (not out or out[-1] != line):
            out.append(line)
    return "\n".join(out)


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--limit", type=int)
    ap.add_argument("--offset", type=int, default=0)
    args = ap.parse_args()
    report, _ = collect("tekotalk-" + stamp(now()), limit=args.limit, offset=args.offset)
    atomic_write(DATA / "runs" / f"run-{report['run_id']}.json",
                 (json.dumps(report, ensure_ascii=False, sort_keys=True, indent=2) + "\n").encode())
    print(json.dumps({"counts": report["counts"], "failures": len(report["failures"]), "stopped": report["stopped"]},
                     ensure_ascii=False))
    return 0


def collect(run_id: str, limit: int | None = None, offset: int = 0) -> tuple[dict, list[Store]]:
    """재생목록에서 아직 받지 않은 영상만 받는다(이미 받은 영상 ID 는 접속하지 않음)."""
    store = Store("youtube-tekotalk", run_id)
    report = {"run_id": run_id, "started_at": now(), "failures": [], "stopped": None,
              "counts": {"playlist": 0, "saved_new": 0, "saved_changed": 0, "already_present": 0,
                         "sub_manual": 0, "sub_auto": 0, "sub_none": 0}}
    with yt_dlp.YoutubeDL({"extract_flat": "in_playlist", "quiet": True, "logger": Quiet()}) as y:
        entries = y.extract_info(PLAYLIST, download=False).get("entries") or []
    ids = [e["id"] for e in entries if e.get("id")]
    report["counts"]["playlist"] = len(ids)
    ids = ids[offset:]
    if limit:
        ids = ids[:limit]
    done = {r["external_key"] for r in store.rows}
    for i, vid in enumerate(ids, 1):
        if f"yt:{vid}" in done:
            report["counts"]["already_present"] += 1
            continue
        with tempfile.TemporaryDirectory() as tmp:
            opts = {"skip_download": True, "writesubtitles": True, "writeautomaticsub": True,
                    "subtitleslangs": ["ko", "ko-KR", "ko.*"], "subtitlesformat": "vtt", "quiet": True,
                    "logger": Quiet(), "outtmpl": f"{tmp}/%(id)s.%(ext)s", "noplaylist": True}
            try:
                with yt_dlp.YoutubeDL(opts) as y:
                    info = y.extract_info(f"https://www.youtube.com/watch?v={vid}", download=True)
            except Exception as e:  # noqa: BLE001 — yt-dlp 는 여러 예외를 던진다
                msg = f"{type(e).__name__}: {e}"[:400]
                if any(k in msg for k in ("Private video", "Video unavailable", "has been removed", "members-only")):
                    # 비공개 · 삭제 영상은 재생목록에만 남아 있다 → 실패가 아니라 "받을 수 없음"
                    report["counts"]["unavailable"] = report["counts"].get("unavailable", 0) + 1
                    report.setdefault("unavailable", []).append({"id": vid, "reason": msg[:120]})
                    continue
                report["failures"].append({"id": vid, "reason": msg})
                if any(s in msg for s in STOP_SIGNS):
                    report["stopped"] = f"차단 신호로 멈춤: {msg[:200]}"
                    break
                continue
            manual = (info.get("subtitles") or {})
            kind = "manual" if any(k.startswith("ko") for k in manual) else (
                "auto" if any(k.startswith("ko") for k in (info.get("automatic_captions") or {})) else None)
            vtt_files = sorted(Path(tmp).glob(f"{vid}*.vtt"))
            vtt = vtt_files[0].read_text(encoding="utf-8", errors="replace") if vtt_files else None
            if not vtt:
                kind = None
            report["counts"][f"sub_{kind or 'none'}"] += 1
            meta = {k: info.get(k) for k in META_KEYS}
            payload = {"video": meta,
                       "subtitle": None if not vtt else {
                           "kind": kind, "lang": vtt_files[0].name.split(".")[-2], "format": "vtt",
                           "vtt": vtt, "text": vtt_to_text(vtt)}}
            hash_obj = {"title": meta["title"], "description": meta["description"], "tags": meta["tags"],
                        "chapters": meta["chapters"], "duration": meta["duration"], "subtitle": vtt}
            outcome = store.save(f"yt:{vid}", vid, {"fetched_at": now(), "request": {"url": meta["webpage_url"]}},
                                 payload, hash_obj, meta["webpage_url"],
                                 None if not meta["upload_date"] else
                                 f"{meta['upload_date'][:4]}-{meta['upload_date'][4:6]}-{meta['upload_date'][6:]}T00:00:00Z")
            key = {"new": "saved_new", "changed": "saved_changed"}.get(outcome, "already_present")
            report["counts"][key] += 1
        if i % 20 == 0:
            store.flush()
            print(f"[{i}/{len(ids)}] {report['counts']} 실패 {len(report['failures'])}", flush=True)
        time.sleep(random.uniform(*DELAY))
    store.flush()
    report["finished_at"] = now()
    return report, [store]


if __name__ == "__main__":
    raise SystemExit(main())
