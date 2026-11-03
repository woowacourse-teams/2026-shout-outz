"""매일 파이프라인 진입점(계획 2장).

실행(지금은 만드는 쪽 S1 만):
  uv run python -m pipeline.daily make --only S1 [--sources ...] [--scope daily|monthly|all]
                                     [--repos 이름 ...] [--refresh-repos] [--run-id ID] [--tekotalk-limit N]

- 원본 저장소 경로는 RAW_DATA_REPO, GitHub 토큰은 GITHUB_TOKEN(없으면 예전 토큰 파일).
- 같은 서버에서 두 번 동시에 돌지 않게 잠근다(계획 1-5).
- 레포 목록(repos)은 월요일이거나 --refresh-repos 이거나 목록이 아직 없을 때만 다시 받는다(계획 4장).
- 커밋 정보(teamdoc-commit, wiki-commit)는 아직 그 기준 커밋으로 받지 않은 문서만 본다(실패해도 다음 날 다시 잡힘).
- 결과: runs/run-{run_id}.json(통합 보고서), runs/delta-{run_id}.jsonl(new, changed, deleted, restored).
"""
from __future__ import annotations

import argparse
import datetime as dt
import fcntl
import json
import time

from pipeline.collectors import blog_team, discussions, pr, teamdoc_commits, tekotalk, wiki, wiki_commits
from pipeline.collectors import repos as org_repos
from pipeline.collectors import teamdoc as teamdocs
from pipeline import handoff as handoff_mod
from pipeline import notify
from pipeline.common import CONFIG, DATA, STATE, atomic_write, now, stamp

# 실행 순서. 레포 목록이 먼저, 커밋 정보는 그 문서를 받은 뒤.
SOURCES = ("repos", "pr", "teamdoc", "wiki", "discussion", "blog", "tekotalk", "teamdoc-commit", "wiki-commit")
EXCLUSIONS = CONFIG / "exclusions.json"


def excluded_repos() -> set[str]:
    return set(json.loads(EXCLUSIONS.read_text()).get("repos", [])) if EXCLUSIONS.exists() else set()


def target_repos(scope: str) -> list[str]:
    """계획 4장: 진행 중 기수(2026-*)는 매일, 지난 기수는 매월 1일. 제외 요청 레포는 뺀다."""
    path = DATA / "state" / "org_repos.json"
    if not path.exists():
        path = STATE / "org_repos.json"  # 레포 목록을 아직 원본 저장소에 만들지 않은 경우(최초 수집기 사본)
    names = [r for r in json.loads(path.read_text()) if r not in excluded_repos()]
    if scope == "daily":
        return [r for r in names if r.startswith("2026-")]
    if scope == "monthly":
        return [r for r in names if not r.startswith("2026-")]
    return names


def run_one(name: str, args, run_id: str, changed: dict[str, set[str]]) -> tuple[dict, list]:
    if name == "repos":
        return org_repos.collect(run_id)
    if name == "tekotalk":
        return tekotalk.collect(run_id, limit=args.tekotalk_limit, recheck_no_subtitle=args.weekly)
    if name == "blog":
        return blog_team.collect(run_id)
    if name == "teamdoc-commit":
        return teamdoc_commits.collect(run_id)  # 기준 커밋이 다른 문서만(전날 실패분도 다시 잡힘, 원칙 3)
    if name == "wiki-commit":
        return wiki_commits.collect(run_id)
    repos = args.repos or target_repos(args.scope)
    if name == "pr":
        return pr.collect(repos, run_id, force=args.full_pr, window_days=args.full_pr_days)
    if name == "discussion":
        return discussions.collect(repos, run_id, force=args.monthly)  # 매월 1일 전체(지워진 글, 수정 확인)
    return {"teamdoc": teamdocs.collect, "wiki": wiki.collect}[name](repos, run_id)


def run_s1(args, run_id: str) -> dict:
    sources = list(args.sources)
    is_monday = dt.datetime.now(dt.UTC).astimezone(dt.timezone(dt.timedelta(hours=9))).weekday() == 0
    if "repos" in sources and not (args.refresh_repos or is_monday or not (DATA / "state" / "org_repos.json").exists()):
        sources.remove("repos")  # 레포 목록은 주 1회
    out = {"scope": "repos" if args.repos else args.scope, "sources": {}}
    changes: list[dict] = []
    changed: dict[str, set[str]] = {}  # source → 이번 실행에서 new, changed 된 키
    for name in sources:
        t0 = time.time()
        try:
            rep, stores = run_one(name, args, run_id, changed)
        except Exception as e:  # noqa: BLE001 — 한 원천이 실패해도 나머지는 돈다(계획 1-5)
            out["sources"][name] = {"status": "FAILED", "error": f"{type(e).__name__}: {e}"[:300],
                                    "seconds": round(time.time() - t0, 1)}
            continue
        for st in stores:
            changes += st.changes
            for c in st.changes:
                if c["status"] in ("new", "changed"):
                    changed.setdefault(c["source"], set()).add(c["external_key"])
        out["sources"][name] = {"status": "PARTIAL" if rep.get("failures") or rep.get("stopped") else "OK",
                                "seconds": round(time.time() - t0, 1), "counts": rep.get("counts"),
                                "failures": (rep.get("failures") or [])[:20], "stopped": rep.get("stopped"),
                                "api_calls": rep.get("api_calls"), "raw_calls": rep.get("raw_calls"),
                                "graphql_calls": rep.get("graphql_calls"), "graphql_points": rep.get("graphql_points")}
    delta = DATA / "runs" / f"delta-{run_id}.jsonl"
    atomic_write(delta, "".join(json.dumps(c, ensure_ascii=False, sort_keys=True) + "\n" for c in changes).encode())
    out["delta"] = {"file": str(delta.relative_to(DATA)), "rows": len(changes),
                    "by_status": {s: sum(c["status"] == s for c in changes) for s in ("new", "changed", "deleted", "restored")},
                    "by_source": {s: sum(c["source"] == s for c in changes) for s in sorted({c["source"] for c in changes})}}
    return out


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("command", choices=["make"])
    ap.add_argument("--only", choices=["S1"], default="S1")
    ap.add_argument("--sources", nargs="+", choices=SOURCES, default=list(SOURCES))
    ap.add_argument("--scope", choices=["daily", "monthly", "all"], default="daily")
    ap.add_argument("--repos", nargs="+")
    ap.add_argument("--refresh-repos", action="store_true", help="월요일이 아니어도 레포 목록을 다시 받는다")
    ap.add_argument("--full-pr", action="store_true", help="PR 을 커서와 상관없이 전부 확인(매월 1일 자동)")
    ap.add_argument("--full-pr-days", type=int, help="PR 전체 확인 때 이 기간(일) 안에 수정된 PR 까지만 본다")
    ap.add_argument("--monthly", action="store_true", help="매월 1일 동작(지난 기수 포함, PR, Discussions 전체 확인)")
    ap.add_argument("--weekly", action="store_true", help="매주 월요일 동작(레포 목록, 테코톡 자막 없던 영상 다시 확인)")
    ap.add_argument("--dry-run", action="store_true", help="수집은 하되 원본 저장소 commit, push, 알림 전송은 하지 않음")
    ap.add_argument("--push", action="store_true", help="commit 뒤 push(따로 승인받은 경우만, PIPELINE_PUSH=1 과 같음)")
    ap.add_argument("--run-id")
    ap.add_argument("--tekotalk-limit", type=int)
    args = ap.parse_args()
    args.sources = [s for s in SOURCES if s in args.sources]  # 입력 순서와 상관없이 정해진 순서로
    today = dt.datetime.now(dt.timezone(dt.timedelta(hours=9)))
    args.monthly = args.monthly or today.day == 1
    args.weekly = args.weekly or today.weekday() == 0
    if args.monthly:
        args.full_pr = True      # 리뷰 수정은 PR updatedAt 을 올리지 않는다(PR #568 실험)
        args.scope = "all"       # 지난 기수 레포도 매월 1일
    if args.weekly:
        args.refresh_repos = True

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
    report["flags"] = {"monthly": args.monthly, "weekly": args.weekly, "full_pr": args.full_pr, "dry_run": args.dry_run}
    atomic_write(DATA / "runs" / f"run-{run_id}.json",
                 (json.dumps(report, ensure_ascii=False, sort_keys=True, indent=2) + "\n").encode())
    # S7a 건네기: 완료 표시를 실행 기록에 쓰고 같은 commit 에 담는다. push 는 승인받은 경우만.
    report["handoff"] = handoff_mod.handoff(run_id, report["S1"]["delta"]["by_status"],
                                            push=args.push or None, dry_run=args.dry_run)
    report["notify"] = notify.send(run_id, notify.alerts_for(report), dry_run=args.dry_run)
    print(json.dumps({"run_id": run_id, "status": report["status"], "delta": report["S1"]["delta"],
                      "handoff": {k: report["handoff"].get(k) for k in ("status", "commit", "pushed", "note")},
                      **{k: {kk: v.get(kk) for kk in ("status", "seconds", "counts", "error")}
                         for k, v in report["S1"]["sources"].items()}},
                     ensure_ascii=False, indent=1))
    return 0 if report["status"] == "OK" else 1


if __name__ == "__main__":
    raise SystemExit(main())
