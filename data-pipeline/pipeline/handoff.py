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
import time
from pathlib import Path

from pipeline.common import DATA, atomic_write, now

GIT_LIMIT = 1024 ** 3          # .git 1GB
FILE_LIMIT = 50 * 1024 ** 2    # 파일 하나 50MB
GC_LOOSE_LIMIT = 50 * 1024 ** 2  # 묶이지 않은 git 객체가 50MB(약 열흘치)를 넘으면 gc
ADD_PATHS = ["raw", "raw_objects", "runs", "state", "derived"]


def _git(*args: str, check: bool = True) -> subprocess.CompletedProcess:
    return subprocess.run(["git", "-C", str(DATA), *args], capture_output=True, text=True, check=check)


def loose_bytes() -> int:
    """아직 묶이지(pack) 않은 git 객체의 크기. `git count-objects -v` 의 size 는 KiB 단위다."""
    info = dict(line.split(": ", 1) for line in _git("count-objects", "-v").stdout.splitlines() if ": " in line)
    return int(info.get("size", "0")) * 1024


def maybe_gc() -> dict:
    """묶이지 않은 객체가 GC_LOOSE_LIMIT 를 넘으면 git gc 를 돌린다.

    목록 파일(raw_objects/github-pr.jsonl, 28MB)은 last_seen_at 만 바뀌어도 매일 통째로 다시 쓰여
    압축 4.9MB 짜리 새 객체가 하루 하나씩 생긴다. commit 이 부르는 `git gc --auto` 는 객체 "개수"(약 6,700개)로만
    판단해 이런 큰 객체 몇 개는 몇 주 동안 정리되지 않고, 그동안 size_check 의 .git 크기에 그대로 잡힌다.
    gc 를 돌리면 지난 버전과의 차이만 남는다(사본 시험: 7일 +38MB → gc 15초 뒤 시작보다 작아짐).
    gc 가 실패해도 건네기는 실패로 보지 않는다(이미 commit 은 끝났고, 다음 실행이 다시 시도한다).
    """
    before = loose_bytes()
    if before < GC_LOOSE_LIMIT:
        return {"ran": False, "loose_bytes": before}
    t0 = time.time()
    p = _git("gc", "--quiet", check=False)
    out = {"ran": True, "ok": p.returncode == 0, "loose_bytes_before": before, "loose_bytes_after": loose_bytes(),
           "seconds": round(time.time() - t0, 1)}
    if p.returncode:
        out["error"] = p.stderr.strip()[:200]
    return out


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
        result["gc"] = maybe_gc()  # 다음 실행의 크기 확인이 정리 안 된 객체 때문에 막히지 않게
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
