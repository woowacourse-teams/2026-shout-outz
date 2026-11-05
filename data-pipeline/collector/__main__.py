"""명령 입구(08 문서 7-3).

    python -m collector collect [--source S ...] [--only V ...] [--repos R ...]
    python -m collector subtitles script (--all-missing | --only ID ...)   > subtitles_local.py
    python -m collector subtitles import <결과 파일>
"""

from __future__ import annotations

import argparse
import json
import logging
import signal
import sys

from .config import GITHUB_SOURCES, SOURCES, Settings


def _stop(signum, frame):
    """systemd 가 멈출 때(SIGTERM, 13시간 넘김 포함) Ctrl+C 와 같이 끝내, 보고서와 잠금을 정리하게 한다."""
    raise KeyboardInterrupt(f"signal {signum}")


def main(argv: list[str] | None = None) -> int:
    signal.signal(signal.SIGTERM, _stop)
    p = argparse.ArgumentParser(prog="python -m collector", description="샤라웃 원본 수집기")
    sub = p.add_subparsers(dest="cmd", required=True)

    c = sub.add_parser("collect", help="원본 수집(매일 실행도 이것)")
    c.add_argument("--source", nargs="+", choices=SOURCES, help="원천(여러 개 가능). 없으면 모두, 정해진 순서로")
    c.add_argument("--only", nargs="+", metavar="값", help="그 건만 받기: 블로그 글 주소, 테코톡 영상 ID, GitHub 원천 키")
    c.add_argument("--repos", nargs="+", metavar="레포", help="GitHub 원천을 레포 몇 개로 좁히기(시험용)")

    s = sub.add_parser("subtitles", help="테코톡 자막(04 문서 5-4)")
    ssub = s.add_subparsers(dest="sub", required=True)
    sc = ssub.add_parser("script", help="집 컴퓨터에서 돌릴 자막 스크립트를 표준 출력으로 낸다")
    g = sc.add_mutually_exclusive_group(required=True)
    g.add_argument("--all-missing", action="store_true", help="아직 자막을 안 받은 영상 전부")
    g.add_argument("--only", nargs="+", metavar="영상ID")
    si = ssub.add_parser("import", help="스크립트 결과 파일을 원본에 저장")
    si.add_argument("file")

    a = p.parse_args(argv)
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s %(message)s", stream=sys.stderr)
    try:
        settings = Settings.from_env()
    except RuntimeError as exc:  # PIPELINE_STORE 없음
        p.error(str(exc))

    if a.cmd == "collect":
        if a.only and (not a.source or len(a.source) != 1):
            p.error("--only 는 --source 하나와 함께 쓴다")
        if a.repos and (not a.source or not set(a.source) <= GITHUB_SOURCES):
            p.error("--repos 는 GitHub 원천(--source github-...)과 함께 쓴다")
        from .runner import collect
        run = collect(settings, a.source, a.only, a.repos)
        print(json.dumps({"run_id": run.run_id, "status": run.status,
                          "sources": {k: v.counts for k, v in run.sources.items()}}, ensure_ascii=False))
        return 0 if run.status in ("success", "skipped") else 1

    from . import subtitles
    from .store import open_store
    store = open_store(settings.store_url)
    if a.sub == "script":
        try:
            text = subtitles.make_script(store, only=a.only, all_missing=a.all_missing)
        except ValueError as exc:  # 영상 ID 모양이 아님
            p.error(str(exc))
        sys.stdout.write(text)
        return 0
    run = subtitles.import_result(settings, store, a.file)
    print(json.dumps({"run_id": run.run_id, "status": run.status,
                      "sources": {k: v.counts for k, v in run.sources.items()}}, ensure_ascii=False))
    return 0 if run.status in ("success", "skipped") else 1


if __name__ == "__main__":
    raise SystemExit(main())
