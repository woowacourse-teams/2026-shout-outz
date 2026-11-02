"""팀 문서(github-teamdoc) 파일별 커밋 정보 수집(결정 기록 25번: 게시 시각, 작성자를 채우기 위함).

받은 팀 문서 최신 버전마다, 그 문서를 받은 시점의 커밋(`commit_sha`)까지 그 파일을 바꾼 커밋 목록을 묻는다.
- 마지막 커밋: 최근 수정 시각, 수정한 사람
- 커밋 수: 응답 Link 헤더의 마지막 페이지 번호(한 페이지에 1개씩 물어서)
- 첫 커밋: 커밋이 2개 이상일 때만 마지막 페이지를 한 번 더 받아 처음 쓴 시각, 처음 쓴 사람
이메일 주소는 개인정보라 저장하지 않는다. 이미 같은 기준 커밋으로 받은 문서는 다시 묻지 않는다.

실행: uv run python teamdoc_commits.py [--limit N]
"""
from __future__ import annotations

import argparse
import json
import re
import time
from urllib.parse import quote

import teamdocs as base

SOURCE = "github-teamdoc-commit"
EMAIL = re.compile(r"[\w.+-]+@[\w-]+(\.[\w-]+)+")
base.COLLECTOR.update({"name": SOURCE, "version": "1.0.0"})


def slim(commit: dict) -> dict:
    """커밋 응답에서 이메일을 뺀 나머지를 남긴다."""
    c = commit.get("commit") or {}
    def person(p):
        return None if not p else {"name": p.get("name"), "date": p.get("date")}
    def account(a):
        return None if not a else {"login": a.get("login"), "id": a.get("id"), "type": a.get("type")}
    message = EMAIL.sub("<email>", c.get("message") or "")  # Co-authored-by 줄 등의 이메일
    return {"sha": commit.get("sha"), "html_url": commit.get("html_url"),
            "message": message,"author": person(c.get("author")), "committer": person(c.get("committer")),
            "author_account": account(commit.get("author")), "committer_account": account(commit.get("committer")),
            "parents": [p.get("sha") for p in commit.get("parents") or []]}


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--limit", type=int)
    args = ap.parse_args()
    report, _ = collect("teamdoc-commit-" + base.stamp(base.now()), limit=args.limit)
    base.atomic_write(base.DATA / "runs" / f"run-{report['run_id']}.json",
                      (json.dumps(report, ensure_ascii=False, sort_keys=True, indent=2) + "\n").encode())
    print(json.dumps({k: v if not isinstance(v, list) else len(v) for k, v in report.items()}, ensure_ascii=False))
    return 1 if report["errors"] else 0


def collect(run_id: str, only_keys: set[str] | None = None, limit: int | None = None) -> tuple[dict, list]:
    """only_keys 가 있으면 그 문서(오늘 바뀐 팀 문서)만 본다. 없으면 모든 최신 문서(같은 기준 커밋은 건너뜀)."""
    docs = base.Store("github-teamdoc", run_id)
    store = base.Store(SOURCE, run_id)
    gh = base.GH(base.token())
    done_refs = {}
    for r in store.latest.values():
        done_refs[r["external_key"]] = r.get("ref_commit_sha")
    report = {"run_id": run_id, "started_at": base.now(), "docs": 0, "saved": 0, "already_present": 0,
              "skipped_same_ref": 0, "not_found": [], "errors": [], "commit_count_hist": {}}
    targets = sorted((r for r in docs.latest.values()
                      if not r.get("deleted_at") and (only_keys is None or r["external_key"] in only_keys)),
                     key=lambda r: r["external_key"])
    if limit:
        targets = targets[:limit]
    t0 = time.time()
    for i, row in enumerate(targets, 1):
        report["docs"] += 1
        key = row["external_key"]
        p = json.loads(base.gzip.decompress((base.DATA / row["s3_key"]).read_bytes()))["payload"]
        repo, path, ref = p["repo"], p["path"], p["commit_sha"]
        if done_refs.get(key) == ref:
            report["skipped_same_ref"] += 1
            continue
        try:
            url = f"{base.API}/repos/{repo}/commits?path={quote(path)}&sha={ref}&per_page=1"
            r = gh.get(url)
            if r.status_code != 200:
                report["not_found" if r.status_code in (404, 409, 422) else "errors"].append(
                    {"key": key, "status": r.status_code})
                continue
            items = r.json()
            if not items:
                report["not_found"].append({"key": key, "status": 200, "reason": "커밋 없음"})
                continue
            last = slim(items[0])
            m = re.search(r'[?&]page=(\d+)>; rel="last"', r.headers.get("link", ""))
            count = int(m.group(1)) if m else 1
            first = last
            if count > 1:
                fr = gh.get(f"{url}&page={count}")
                if fr.status_code == 200 and fr.json():
                    first = slim(fr.json()[0])
                else:
                    report["errors"].append({"key": key, "status": fr.status_code, "reason": "첫 커밋"})
                    continue
                time.sleep(base.DELAY)
            payload = {"repo": repo, "path": path, "ref_commit_sha": ref, "commit_count": count,
                       "last_commit": last, "first_commit": first,
                       "request": {"endpoint": "GET /repos/{repo}/commits", "per_page": 1}}
            fetched = base.now()
            outcome = store.save(key, f"{repo}/{path}", {"fetched_at": fetched, "request": {"url": url}},
                                 payload, {"ref": ref, "last": last["sha"], "first": first["sha"], "count": count},
                                 row.get("canonical_url"), (last.get("committer") or {}).get("date"))
            if outcome != "already_present":
                store.latest[key]["ref_commit_sha"] = ref
            report["saved" if outcome != "already_present" else "already_present"] += 1
            b = "1" if count == 1 else "2-5" if count <= 5 else "6-20" if count <= 20 else "21+"
            report["commit_count_hist"][b] = report["commit_count_hist"].get(b, 0) + 1
        except (base.httpx.HTTPError, ValueError, KeyError) as e:
            report["errors"].append({"key": key, "reason": f"{type(e).__name__}: {e}"[:200]})
        time.sleep(base.DELAY)
        if i % 200 == 0:
            store.flush()
            print(f"[{base.now()}] {i}/{len(targets)} saved={report['saved']} api={gh.api_calls} "
                  f"errors={len(report['errors'])} {time.time() - t0:.0f}s", flush=True)
    store.flush()
    report.update({"finished_at": base.now(), "api_calls": gh.api_calls, "elapsed_seconds": round(time.time() - t0)})
    report["failures"] = report["errors"]
    report["counts"] = {k: report[k] for k in ("docs", "saved", "already_present", "skipped_same_ref")}
    return report, [store]


if __name__ == "__main__":
    raise SystemExit(main())
