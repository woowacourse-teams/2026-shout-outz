"""건네기 S7a(계획 1-4): 만드는 쪽 결과를 원본 저장소에 commit 하고, 넣는 쪽이 볼 완료 표시를 남긴다.

- commit 1개: `data: daily run-{run_id} (new n, changed m, deleted k)`.
- push 는 따로 승인받아야 하므로 기본은 하지 않는다(--push 또는 PIPELINE_PUSH=1 일 때만). force push 는 하지 않는다.
- push 전에 크기를 본다: .git 1GB 또는 파일 하나 50MB 를 넘으면 멈추고 알린다(S3 로 옮길 신호, 계획 1-4).
- 완료 표시: runs/run-{run_id}.json 의 handoff.status 를 "done" 으로 쓴다. 넣는 쪽은 git pull 뒤 이것을 보고 시작한다.
"""
from __future__ import annotations

import json
import os
import subprocess
from pathlib import Path

from pipeline.common import DATA, atomic_write, now

GIT_LIMIT = 1024 ** 3          # .git 1GB
FILE_LIMIT = 50 * 1024 ** 2    # 파일 하나 50MB
ADD_PATHS = ["raw", "raw_objects", "runs", "state", "derived"]


def _git(*args: str, check: bool = True) -> subprocess.CompletedProcess:
    return subprocess.run(["git", "-C", str(DATA), *args], capture_output=True, text=True, check=check)


def _dir_bytes(path: Path) -> int:
    return sum(p.stat().st_size for p in path.rglob("*") if p.is_file())


def size_check() -> dict:
    staged = _git("diff", "--cached", "--name-only").stdout.split()
    big = [f for f in staged if (DATA / f).is_file() and (DATA / f).stat().st_size > FILE_LIMIT]
    git_bytes = _dir_bytes(DATA / ".git")
    return {"git_bytes": git_bytes, "git_limit": GIT_LIMIT, "big_files": big,
            "ok": git_bytes < GIT_LIMIT and not big}


def _mark(run_file: Path, result: dict) -> None:
    if run_file.exists():
        report = json.loads(run_file.read_text())
        report["handoff"] = result
        atomic_write(run_file, (json.dumps(report, ensure_ascii=False, sort_keys=True, indent=2) + "\n").encode())


def handoff(run_id: str, delta_summary: dict, push: bool | None = None, dry_run: bool = False) -> dict:
    """완료 표시를 먼저 실행 기록에 쓰고, 그 파일까지 같은 commit 에 담는다(넣는 쪽이 pull 로 보게)."""
    push = bool(int(os.environ.get("PIPELINE_PUSH", "0"))) if push is None else push
    result = {"started_at": now(), "status": "failed", "push_requested": push}
    run_file = DATA / "runs" / f"run-{run_id}.json"
    if dry_run:
        result.update({"status": "dry_run", "note": "commit, push 하지 않음", "finished_at": now()})
        _mark(run_file, result)
        return result
    try:
        _git("add", "--", *[p for p in ADD_PATHS if (DATA / p).exists()])
        size = size_check()
        result["size"] = size
        if not size["ok"]:
            _git("reset", "-q")
            result.update({"status": "blocked_size", "note": "크기 한도 초과, commit 하지 않음(S3 이전 신호)",
                           "finished_at": now()})
            _mark(run_file, result)
            return result
        result.update({"status": "done", "finished_at": now()})  # 이 상태로 commit 에 들어간다
        _mark(run_file, result)
        _git("add", "--", str(run_file.relative_to(DATA)))
        s = delta_summary
        _git("commit", "-q", "-m", f"data: daily run-{run_id} (new {s.get('new', 0)}, changed {s.get('changed', 0)}, "
                                    f"deleted {s.get('deleted', 0)})")
        result["commit"] = _git("rev-parse", "--short", "HEAD").stdout.strip()
    except subprocess.CalledProcessError as e:
        result.update({"status": "failed", "error": (e.stderr or str(e)).strip()[:300], "finished_at": now()})
        _mark(run_file, result)  # commit 되지 않은 실패 기록(넣는 쪽은 done 이 아니면 시작하지 않음)
        return result
    if push:
        p = _git("push", "-q", "origin", "HEAD", check=False)  # force push 는 하지 않는다
        result["pushed"] = p.returncode == 0
        if p.returncode:
            result.update({"status": "push_failed", "error": p.stderr.strip()[:300]})
    return result
