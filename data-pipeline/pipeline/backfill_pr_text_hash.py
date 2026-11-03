"""기존 PR 원본 목록에 재수집 판단용 해시(text_hash, pr-v2)를 한 번 채운다(2026-11-03 사용자 결정, B 안).

- 원본 파일(raw/github-pr/...json.gz)과 기존 content_hash 는 건드리지 않는다. 목록 파일 raw_objects/github-pr.jsonl 의
  각 행에 `text_hash`, `hash_scheme` 칸만 더한다. 이미 채운 행은 건너뛴다(다시 실행해도 안전).
- 로컬 계산만 한다(API 0회).

실행: uv run python -m pipeline.backfill_pr_text_hash [--dry-run]
"""
from __future__ import annotations

import argparse
import gzip
import json
import time
from concurrent.futures import ProcessPoolExecutor

from pipeline.common import DATA, atomic_write
from pipeline.pr_hash import SCHEME, text_hash

LIST = DATA / "raw_objects" / "github-pr.jsonl"


def _one(s3_key: str) -> tuple[str, str | None, str | None]:
    try:
        p = json.loads(gzip.decompress((DATA / s3_key).read_bytes()))["payload"]
        return s3_key, text_hash(p["pull"], p.get("reviews") or [], p.get("review_threads") or []), None
    except Exception as e:  # noqa: BLE001 — 실패는 건수와 이유로 보고
        return s3_key, None, f"{type(e).__name__}: {e}"[:200]


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--dry-run", action="store_true", help="계산만 하고 목록 파일은 쓰지 않는다")
    args = ap.parse_args()
    t0 = time.time()
    rows = [json.loads(line) for line in LIST.read_text().splitlines() if line.strip()]
    todo = [r for r in rows if r.get("hash_scheme") != SCHEME]
    results: dict[str, tuple[str | None, str | None]] = {}
    with ProcessPoolExecutor() as ex:
        for key, h, err in ex.map(_one, [r["s3_key"] for r in todo], chunksize=200):
            results[key] = (h, err)
    failures = []
    for r in todo:
        h, err = results[r["s3_key"]]
        if err:
            failures.append({"external_key": r["external_key"], "reason": err})
            continue
        r["text_hash"], r["hash_scheme"] = h, SCHEME
    report = {"rows": len(rows), "already_done": len(rows) - len(todo), "filled": len(todo) - len(failures),
              "failures": len(failures), "failure_examples": failures[:5],
              "distinct_text_hash": len({r.get("text_hash") for r in rows if r.get("text_hash")}),
              "seconds": round(time.time() - t0, 1), "dry_run": args.dry_run}
    if not args.dry_run and not failures:
        data = "".join(json.dumps(r, ensure_ascii=False, sort_keys=True, separators=(",", ":")) + "\n" for r in rows)
        atomic_write(LIST, data.encode())
    elif failures:
        report["note"] = "실패가 있어 목록 파일을 쓰지 않았다"
    print(json.dumps(report, ensure_ascii=False, indent=1))
    return 0 if not failures else 1


if __name__ == "__main__":
    raise SystemExit(main())
