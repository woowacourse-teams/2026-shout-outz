"""Wiki 페이지별 커밋 정보 수집(결정 기록 25번: 게시 시각, 작성자를 채우기 위함).

GitHub API 를 쓰지 않는다. wiki 저장소를 파일 내용 없이 기록만 clone(`--filter=blob:none --no-checkout`)하고,
원본을 받은 시점의 wiki 커밋(`wiki_commit_sha`)까지의 `git log --name-only` 한 번으로 페이지마다
첫 커밋, 마지막 커밋, 커밋 수를 센다. 이메일은 저장하지 않는다.
이름을 바꾼 페이지는 바뀐 이름으로 처음 나온 커밋이 첫 커밋이 된다(이름 추적은 하지 않음).

실행: uv run python -m pipeline.collectors.wiki_commits [--repos 2022-dallog ...]
"""
from __future__ import annotations

import argparse
import codecs
import gzip
import json
import re
import tempfile
import time
from collections import defaultdict
from pathlib import Path

from pipeline import common as base
from pipeline.store import Store
from pipeline.collectors.wiki import git

SOURCE = "github-wiki-commit"
EMAIL = re.compile(r"[\w.+-]+@[\w-]+(\.[\w-]+)+")
VERSION = "1.1.0"  # 1.1.0: 오늘 바뀐 문서만(only_keys)


def history(root: Path, ref: str) -> dict[str, list[dict]]:
    """경로마다 그 경로를 바꾼 커밋 목록(최신순)."""
    out = git("-c", "core.quotePath=false", "log", ref, "--name-only",
              "--format=%x1e%H%x1f%an%x1f%aI%x1f%cn%x1f%cI%x1f%s", cwd=root, timeout=600)
    if out.returncode != 0:
        raise RuntimeError(out.stderr.strip()[:200])
    paths: dict[str, list[dict]] = defaultdict(list)
    for record in out.stdout.split("\x1e")[1:]:
        head, _, names = record.partition("\n")
        sha, an, ad, cn, cd, subject = head.split("\x1f")
        commit = {"sha": sha, "author": {"name": an, "date": ad}, "committer": {"name": cn, "date": cd},
                  "subject": EMAIL.sub("<email>", subject)}
        for name in filter(None, (n.strip() for n in names.splitlines())):
            if len(name) > 1 and name[0] == name[-1] == '"':
                # 제어 문자가 든 이름은 git 이 C 문자열처럼 따옴표로 감싸 내보낸다.
                name = codecs.escape_decode(name[1:-1].encode())[0].decode("utf-8", errors="replace")
            paths[name].append(commit)
    return paths


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--repos", nargs="+")
    args = ap.parse_args()
    report, _ = collect("wiki-commit-" + base.stamp(base.now()), repos=args.repos)
    base.atomic_write(base.DATA / "runs" / f"run-{report['run_id']}.json",
                      (json.dumps(report, ensure_ascii=False, sort_keys=True, indent=2) + "\n").encode())
    print(json.dumps({k: v if not isinstance(v, list) else len(v) for k, v in report.items()}, ensure_ascii=False))
    return 1 if report["errors"] else 0


def collect(run_id: str, only_keys: set[str] | None = None, repos: list[str] | None = None) -> tuple[dict, list]:
    """only_keys 가 있으면 그 페이지(오늘 바뀐 wiki 페이지)가 있는 wiki 만 clone 한다."""
    pages = Store("github-wiki", run_id)
    store = Store(SOURCE, run_id, VERSION)
    by_repo: dict[str, list[dict]] = defaultdict(list)
    for row in pages.latest.values():
        if row.get("deleted_at") or (only_keys is not None and row["external_key"] not in only_keys):
            continue
        p = json.loads(gzip.decompress((base.DATA / row["s3_key"]).read_bytes()))["payload"]
        if repos and p["repo"].split("/")[1] not in repos:
            continue
        by_repo[p["repo"]].append({"key": row["external_key"], "path": p["path"], "ref": p["wiki_commit_sha"],
                                   "url": row.get("canonical_url")})
    report = {"run_id": run_id, "started_at": base.now(), "repos": 0, "pages": 0, "saved": 0,
              "already_present": 0, "no_history": [], "errors": [], "commit_count_hist": {}}
    t0 = time.time()
    for repo, items in sorted(by_repo.items()):
        report["repos"] += 1
        url = f"https://github.com/{repo}.wiki.git"
        with tempfile.TemporaryDirectory() as tmp:
            cl = git("clone", "--filter=blob:none", "--no-checkout", "--quiet", url, tmp, timeout=600)
            if cl.returncode != 0:
                report["errors"].append({"repo": repo, "reason": cl.stderr.strip()[:200]})
                continue
            refs = {i["ref"] for i in items}
            hist = {}
            try:
                for ref in refs:
                    hist[ref] = history(Path(tmp), ref)
            except (RuntimeError, ValueError) as e:
                report["errors"].append({"repo": repo, "reason": str(e)})
                continue
            fetched = base.now()
            for item in items:
                report["pages"] += 1
                commits = hist[item["ref"]].get(item["path"])
                if not commits:
                    report["no_history"].append(item["key"])
                    continue
                last, first, count = commits[0], commits[-1], len(commits)
                payload = {"repo": repo, "path": item["path"], "ref_wiki_commit_sha": item["ref"],
                           "commit_count": count, "last_commit": last, "first_commit": first,
                           "request": {"method": "git clone --filter=blob:none, git log --name-only"}}
                outcome = store.save(item["key"], f"{repo}/{item['path']}",
                                     {"fetched_at": fetched, "request": {"url": url, "ref": item["ref"]}},
                                     payload, {"ref": item["ref"], "last": last["sha"], "first": first["sha"],
                                               "count": count},
                                     item["url"], last["committer"]["date"])
                report["saved" if outcome != "already_present" else "already_present"] += 1
                b = "1" if count == 1 else "2-5" if count <= 5 else "6-20" if count <= 20 else "21+"
                report["commit_count_hist"][b] = report["commit_count_hist"].get(b, 0) + 1
        store.flush()
        print(f"[{base.now()}] {repo} pages={len(items)} total_saved={report['saved']} "
              f"{time.time() - t0:.0f}s", flush=True)
    store.flush()
    report.update({"finished_at": base.now(), "elapsed_seconds": round(time.time() - t0)})
    report["failures"] = report["errors"]
    report["counts"] = {k: report[k] for k in ("repos", "pages", "saved", "already_present")}
    return report, [store]


if __name__ == "__main__":
    raise SystemExit(main())
