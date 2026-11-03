"""기존 원본으로 커서를 미리 채운다(트러블슈팅: PR 첫 실행과 매월 전체 확인이 73분 걸리는 문제).

커서는 수집기가 레포를 끝까지 본 뒤에야 처음 생겨서, 이미 다 받아 둔 레포도 첫 실행에서는 처음부터 다시 확인했다.
원본 목록에 이미 있는 값으로 레포별 커서를 한 번 만들어 두면 첫 실행부터 증분으로 돈다.
- PR: 레포별 가장 늦은 PR 수정 시각(raw_objects/github-pr.jsonl 의 source_updated_at)
- Discussions: 레포별 가장 늦은 글 수정 시각(raw_objects/github-discussion.jsonl 의 source_updated_at)
- Wiki: 레포별 마지막으로 받은 wiki HEAD(최신 원본 봉투의 request.head)
이미 있는 커서는 덮어쓰지 않는다(--force 면 덮어씀). 로컬 계산만 한다(API 0회).

실행: uv run python -m pipeline.seed_cursors [--force] [--dry-run]
"""
from __future__ import annotations

import argparse
import gzip
import json
import time

from pipeline.common import DATA, atomic_write


def _rows(source: str) -> list[dict]:
    path = DATA / "raw_objects" / f"{source}.jsonl"
    if not path.exists():
        return []
    return [r for r in (json.loads(l) for l in path.read_text().splitlines() if l.strip()) if r.get("is_latest")]


def _latest_by_repo(rows: list[dict], repo_of) -> dict[str, str]:
    out: dict[str, str] = {}
    for r in rows:
        ts, repo = r.get("source_updated_at"), repo_of(r["external_key"])
        if ts and repo and (repo not in out or ts > out[repo]):
            out[repo] = ts
    return out


def pr_cursors() -> dict[str, str]:  # woowacourse-teams/2026-x#12 → 2026-x
    return _latest_by_repo(_rows("github-pr"), lambda k: k.split("/", 1)[1].split("#", 1)[0])


def discussion_cursors() -> dict[str, str]:  # woowacourse-teams/2026-x/discussions/3 → 2026-x
    return _latest_by_repo(_rows("github-discussion"), lambda k: k.split("/")[1])


def wiki_cursors() -> dict[str, str]:  # woowacourse-teams/2026-x:wiki/Home.md → 2026-x, 레포마다 최신 원본 하나에서 HEAD
    newest: dict[str, dict] = {}
    for r in _rows("github-wiki"):
        repo = r["external_key"].split("/", 1)[1].split(":", 1)[0]
        if repo not in newest or r["fetched_at"] > newest[repo]["fetched_at"]:
            newest[repo] = r
    out = {}
    for repo, r in newest.items():
        env = json.loads(gzip.decompress((DATA / r["s3_key"]).read_bytes()))
        head = (env.get("request") or {}).get("head") or (env.get("payload") or {}).get("wiki_commit_sha")
        if head:
            out[repo] = head
    return out


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--force", action="store_true", help="이미 있는 커서도 덮어쓴다")
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()
    t0, report = time.time(), {}
    for name, fn in (("github-pr", pr_cursors), ("github-discussion", discussion_cursors), ("github-wiki", wiki_cursors)):
        path = DATA / "state" / "cursors" / f"{name}.json"
        current = json.loads(path.read_text()) if path.exists() else {}
        seeded = fn()
        merged = dict(seeded) | ({} if args.force else current)  # 기본: 이미 있는 값이 우선
        report[name] = {"repos_from_raw": len(seeded), "already_had": len(current),
                        "added": len(set(merged) - set(current)), "total": len(merged)}
        if not args.dry_run:
            atomic_write(path, (json.dumps(merged, ensure_ascii=False, sort_keys=True, indent=1) + "\n").encode())
    report["seconds"] = round(time.time() - t0, 1)
    report["dry_run"] = args.dry_run
    print(json.dumps(report, ensure_ascii=False, indent=1))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
