"""조직 레포 목록 원본(github-repo, 계획 3장, 매주 월요일).

woowacourse-teams 의 공개 레포(포크 제외)를 받아 레포마다 원본을 남기고, 지난 목록과 비교해
새 레포, 바뀐 설정(기본 브랜치, 보관, 위키 사용 등), 사라진 레포(비공개 전환, 삭제)를 찾는다.
- 사라진 레포는 지우지 않고 deleted_at(파생 데이터 숨김은 넣는 쪽 S5).
- `pushed_at` 은 해시에 넣지 않는다(push 마다 새 버전이 생기지 않게). 대신 따로 저장해 팀 문서가 미리 거르는 데 쓴다.
- 결과 목록: 원본 저장소 state/org_repos.json(이름), state/org_repos_pushed.json(이름 → pushed_at).

실행: uv run python -m pipeline.collectors.repos
"""
from __future__ import annotations

import json

from pipeline.common import API, DATA, GH, OWNER, atomic_write, now, stamp, token
from pipeline.store import Store

VERSION = "1.0.0"

HASH_KEYS = ("name", "default_branch", "archived", "disabled", "visibility", "description", "topics",
             "has_wiki", "has_discussions", "homepage")
KEEP_KEYS = HASH_KEYS + ("full_name", "html_url", "created_at", "updated_at", "pushed_at", "language", "size")


def collect(run_id: str) -> tuple[dict, list[Store]]:
    gh = GH(token())
    store = Store("github-repo", run_id, VERSION)
    report = {"run_id": run_id, "started_at": now(), "failures": [],
              "counts": {"listed": 0, "forks_skipped": 0, "saved_new": 0, "saved_changed": 0, "already_present": 0,
                         "deleted": 0}}
    repos, page = [], 1
    while True:
        r = gh.get(f"{API}/orgs/{OWNER}/repos?type=public&per_page=100&page={page}")
        r.raise_for_status()  # 목록이 끝까지 안 오면 사라진 레포를 잘못 판단하므로 실패로 끝낸다
        items = r.json()
        for it in items:
            if it.get("fork"):
                report["counts"]["forks_skipped"] += 1
            else:
                repos.append(it)
        if len(items) < 100:
            break
        page += 1
    report["counts"]["listed"] = len(repos)
    fetched = now()
    seen = set()
    for it in repos:
        key = f"{OWNER}/{it['name']}"
        seen.add(key)
        payload = {k: it.get(k) for k in KEEP_KEYS}
        outcome = store.save(key, key, {"fetched_at": fetched, "request": {"url": f"{API}/orgs/{OWNER}/repos"}},
                             payload, {k: it.get(k) for k in HASH_KEYS}, it.get("html_url"), it.get("pushed_at"))
        report["counts"]["saved_new" if outcome == "new" else "saved_changed" if outcome == "changed"
                         else "already_present"] += 1
    for key in [k for k in store.latest if k not in seen]:
        if store.mark_deleted(key, fetched):
            report["counts"]["deleted"] += 1
    store.flush()
    names = sorted(it["name"] for it in repos)
    atomic_write(DATA / "state" / "org_repos.json", (json.dumps(names, ensure_ascii=False, indent=1) + "\n").encode())
    atomic_write(DATA / "state" / "org_repos_pushed.json",
                 (json.dumps({it["name"]: it.get("pushed_at") for it in repos}, ensure_ascii=False, sort_keys=True,
                             indent=1) + "\n").encode())
    report["api_calls"] = gh.api_calls
    report["finished_at"] = now()
    return report, [store]


def main() -> int:
    report, _ = collect("repo-" + stamp(now()))
    atomic_write(DATA / "runs" / f"run-{report['run_id']}.json",
                 (json.dumps(report, ensure_ascii=False, sort_keys=True, indent=2) + "\n").encode())
    print(json.dumps(report["counts"], ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
