"""매일 파이프라인 진입점(계획 2장).

실행(지금은 만드는 쪽 S1 만):
  uv run python -m pipeline.daily make --only S1 [--sources teamdoc wiki tekotalk] [--scope daily|monthly|all]
                                     [--repos 이름 ...] [--run-id ID] [--tekotalk-limit N]

- 원본 저장소 경로는 RAW_DATA_REPO, GitHub 토큰은 GITHUB_TOKEN(없으면 예전 토큰 파일).
- 같은 서버에서 두 번 동시에 돌지 않게 잠근다(계획 1-5).
- 결과: runs/run-{run_id}.json(통합 보고서), runs/delta-{run_id}.jsonl(이번에 new, changed, deleted, restored 된 것).
"""
from __future__ import annotations

import argparse
import fcntl
import json
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))  # 기존 수집 스크립트(teamdocs.py 등)가 레포 최상위에 있다

import tekotalk  # noqa: E402
import teamdocs  # noqa: E402
import wiki  # noqa: E402
from teamdocs import DATA, STATE, atomic_write, now, stamp  # noqa: E402

SOURCES = ("teamdoc", "wiki", "tekotalk")


def target_repos(scope: str) -> list[str]:
    """계획 4장: 진행 중 기수(2026-*)는 매일, 지난 기수는 매월 1일."""
    repos = json.loads((STATE / "org_repos.json").read_text())
    if scope == "daily":
        return [r for r in repos if r.startswith("2026-")]
    if scope == "monthly":
        return [r for r in repos if not r.startswith("2026-")]
    return repos


def run_s1(args, run_id: str) -> dict:
    repos = args.repos or target_repos(args.scope)
    out = {"repos": len(repos), "scope": "repos" if args.repos else args.scope, "sources": {}}
    changes: list[dict] = []
    for name in args.sources:
        t0 = time.time()
        try:
            if name == "teamdoc":
                rep, stores = teamdocs.collect(repos, run_id)
            elif name == "wiki":
                rep, stores = wiki.collect(repos, run_id)
            else:
                rep, stores = tekotalk.collect(run_id, limit=args.tekotalk_limit)
        except Exception as e:  # noqa: BLE001 — 한 원천이 실패해도 나머지는 돈다(계획 1-5)
            out["sources"][name] = {"status": "FAILED", "error": f"{type(e).__name__}: {e}"[:300],
                                    "seconds": round(time.time() - t0, 1)}
            continue
        for st in stores:
            changes += st.changes
        out["sources"][name] = {"status": "PARTIAL" if rep.get("failures") or rep.get("stopped") else "OK",
                                "seconds": round(time.time() - t0, 1), "counts": rep["counts"],
                                "failures": rep.get("failures", [])[:20], "stopped": rep.get("stopped"),
                                "api_calls": rep.get("api_calls"), "raw_calls": rep.get("raw_calls")}
    delta = DATA / "runs" / f"delta-{run_id}.jsonl"
    atomic_write(delta, "".join(json.dumps(c, ensure_ascii=False, sort_keys=True) + "\n" for c in changes).encode())
    out["delta"] = {"file": str(delta.relative_to(DATA)), "rows": len(changes),
                    "by_status": {s: sum(c["status"] == s for c in changes) for s in ("new", "changed", "deleted", "restored")}}
    return out


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("command", choices=["make"])
    ap.add_argument("--only", choices=["S1"], default="S1")
    ap.add_argument("--sources", nargs="+", choices=SOURCES, default=list(SOURCES))
    ap.add_argument("--scope", choices=["daily", "monthly", "all"], default="daily")
    ap.add_argument("--repos", nargs="+")
    ap.add_argument("--run-id")
    ap.add_argument("--tekotalk-limit", type=int)
    args = ap.parse_args()

    STATE.mkdir(exist_ok=True)
    lock = open(STATE / "daily.lock", "w")
    try:
        fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
    except BlockingIOError:
        print(json.dumps({"status": "LOCKED", "message": "이미 다른 실행이 돌고 있다"}, ensure_ascii=False))
        return 2

    run_id = args.run_id or "daily-" + stamp(now())
    report = {"run_id": run_id, "command": args.command, "stage": args.only, "data_repo": str(DATA),
              "started_at": now(), "status": "RUNNING"}
    report["S1"] = run_s1(args, run_id)
    statuses = {s["status"] for s in report["S1"]["sources"].values()}
    report["status"] = "FAILED" if statuses == {"FAILED"} else ("PARTIAL" if statuses - {"OK"} else "OK")
    report["finished_at"] = now()
    atomic_write(DATA / "runs" / f"run-{run_id}.json",
                 (json.dumps(report, ensure_ascii=False, sort_keys=True, indent=2) + "\n").encode())
    print(json.dumps({"run_id": run_id, "status": report["status"], "delta": report["S1"]["delta"],
                      **{k: {kk: v.get(kk) for kk in ("status", "seconds", "counts")} for k, v in report["S1"]["sources"].items()}},
                     ensure_ascii=False, indent=1))
    return 0 if report["status"] == "OK" else 1


if __name__ == "__main__":
    raise SystemExit(main())
