"""명령 입구(09 문서 R20-4). 수집기와 같은 모양이라 관리 웹이 같은 방식으로 부른다.

    python -m refine run [--source S ...] [--only KEY ...] [--limit N] [--after-collect]
    python -m refine preview --source S [--limit N]

- run: 매일 실행. 정제 색인과 원본 색인을 견줘 고른 문서만 정제한다(R19-1). 한 번에 최대 1만 건(R20-6, --limit 로 바꿈)
- --only: 고른 키만(아침 다시 돌리기, R20-7). --source 하나와 함께 쓴다(코드 마크다운과 위키의 키 모양이 겹칠 수 있어서)
- --after-collect: 가장 최근 수집 실행과 같은 run_id 로 묶는다(R20-1). 매일 실행(systemd)과 아침 다시 돌리기에서 쓴다
- preview: 정제 문서를 건드리지 않고 지금 규칙으로 정제만 해 본 결과를 refine/previews/ 에 남긴다(R19-4)
"""

from __future__ import annotations

import argparse
import json
import logging
import signal
import sys

from collector.config import Settings

from .config import SOURCES


def _stop(signum, frame):
    """systemd 가 멈출 때(SIGTERM) Ctrl+C 와 같이 끝내, 보고서와 잠금을 정리하게 한다."""
    raise KeyboardInterrupt(f"signal {signum}")


def main(argv: list[str] | None = None) -> int:
    signal.signal(signal.SIGTERM, _stop)
    p = argparse.ArgumentParser(prog="python -m refine", description="샤라웃 정제(②)")
    sub = p.add_subparsers(dest="cmd", required=True)
    r = sub.add_parser("run", help="정제(매일 실행도 이것)")
    r.add_argument("--source", nargs="+", choices=SOURCES, help="원천(여러 개 가능). 없으면 모두, 작은 원천부터")
    r.add_argument("--only", nargs="+", metavar="키", help="그 문서만 다시 정제(아침 다시 돌리기). --source 하나와 함께")
    r.add_argument("--limit", type=int, metavar="건수", help="이번 실행에 정제할 최대 문서 수(기본 1만)")
    r.add_argument("--after-collect", action="store_true", help="가장 최근 수집 실행과 같은 run_id 로 묶는다")
    v = sub.add_parser("preview", help="규칙을 고치기 전 미리 보기(정제 문서를 건드리지 않음)")
    v.add_argument("--source", required=True, choices=SOURCES)
    v.add_argument("--limit", type=int, metavar="건수", help="앞에서부터 이만큼만 미리 본다")

    a = p.parse_args(argv)
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s %(message)s", stream=sys.stderr)
    try:
        settings = Settings.from_env()
    except RuntimeError as exc:  # PIPELINE_STORE 없음
        p.error(str(exc))

    if a.cmd == "preview":
        from .preview import run_preview
        got = run_preview(settings, a.source, a.limit)
        print(json.dumps({"path": got["path"], "docs": got["docs"], "counts": got["counts"],
                          "excluded_by_rule": got["excluded_by_rule"]}, ensure_ascii=False))
        return 0

    if a.only and (not a.source or len(a.source) != 1):
        p.error("--only 는 --source 하나와 함께 쓴다")
    if a.limit is not None and a.limit < 0:
        p.error("--limit 는 0 이상")
    from .runner import run_refine
    rep = run_refine(settings, a.source, a.only, a.limit, a.after_collect)
    print(json.dumps({"run_id": rep.run_id, "status": rep.status,
                      "sources": {k: {**v.counts, "remaining": v.remaining} for k, v in rep.sources.items()}},
                     ensure_ascii=False))
    return 0 if rep.status in ("success", "skipped") else 1


if __name__ == "__main__":
    raise SystemExit(main())
