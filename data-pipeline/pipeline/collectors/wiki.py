"""Wiki 원본 최초 적재 — 모든 레포의 모든 페이지를 구분 없이 받는다(결정 기록 19번).

GitHub API 를 쓰지 않는다: `git ls-remote` 로 wiki 저장소가 있는지 · HEAD SHA 를 보고, 있으면 얕은 clone(depth 1) 후
페이지 파일을 읽는다. Home, 사이드바, 회의록, 짧은 페이지도 모두 받는다(거르기는 추출 단계).
원본 목록은 원천별 파일(raw_objects/github-wiki.jsonl).

실행: uv run python -m pipeline.collectors.wiki [--repos 이름 ...]
"""
from __future__ import annotations

import argparse
import json
import subprocess
import tempfile
from pathlib import Path

from pipeline.common import DATA, OWNER, STATE, atomic_write, now, stamp
from pipeline.store import Store

VERSION = "1.1.0"  # 1.1.0: HEAD 커서로 바뀐 wiki 만 clone, 사라진 페이지 deleted_at

PAGE_EXT = (".md", ".markdown", ".mdown", ".mkdn", ".mediawiki", ".wiki", ".asciidoc", ".adoc", ".asc", ".rst",
            ".textile", ".rdoc", ".org", ".creole", ".pod")


def git(*args: str, cwd: Path | None = None, timeout: int = 300) -> subprocess.CompletedProcess:
    env = {"GIT_TERMINAL_PROMPT": "0", "PATH": "/usr/bin:/bin:/usr/local/bin:/opt/homebrew/bin"}
    return subprocess.run(["git", *args], cwd=cwd, capture_output=True, text=True, timeout=timeout, env=env)


CURSOR = "state/cursors/github-wiki.json"  # 레포별 지난 wiki HEAD(원본 저장소 안, 매일 파이프라인 1-4)


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--repos", nargs="+")
    ap.add_argument("--full", action="store_true", help="HEAD 가 같아도 clone 한다")
    args = ap.parse_args()
    repos = args.repos or json.loads((STATE / "org_repos.json").read_text())
    report, _ = collect(repos, "wiki-" + stamp(now()), force=args.full)
    atomic_write(DATA / "runs" / f"run-{report['run_id']}.json",
                 (json.dumps(report, ensure_ascii=False, sort_keys=True, indent=2) + "\n").encode())
    print(json.dumps({"counts": report["counts"], "failures": len(report["failures"])}, ensure_ascii=False))
    return 0


def collect(repos: list[str], run_id: str, force: bool = False) -> tuple[dict, list[Store]]:
    """HEAD 가 지난번과 같은 wiki 는 clone 하지 않는다. 처음(커서 없음)에는 모두 clone."""
    cursor_path = DATA / CURSOR
    heads = json.loads(cursor_path.read_text()) if cursor_path.exists() else {}
    store = Store("github-wiki", run_id, VERSION)
    report = {"run_id": run_id, "started_at": now(), "repos": [], "failures": [],
              "counts": {"repos_with_wiki": 0, "pages": 0, "saved_new": 0, "saved_changed": 0, "already_present": 0,
                         "other_files": 0}}
    for i, repo in enumerate(repos, 1):
        url = f"https://github.com/{OWNER}/{repo}.wiki.git"
        ls = git("ls-remote", url, "HEAD", timeout=60)
        if ls.returncode != 0 or not ls.stdout.strip():
            continue  # wiki 없음(저장소가 없거나 비어 있음)
        head = ls.stdout.split()[0]
        if heads.get(repo) == head and not force:
            report["counts"]["repos_unchanged"] = report["counts"].get("repos_unchanged", 0) + 1
            continue
        with tempfile.TemporaryDirectory() as tmp:
            cl = git("clone", "--depth", "1", "--quiet", url, tmp)
            if cl.returncode != 0:
                report["failures"].append({"repo": repo, "reason": cl.stderr.strip()[:300]})
                continue
            root = Path(tmp)
            files = [p for p in root.rglob("*") if p.is_file() and ".git" not in p.relative_to(root).parts]
            pages = [p for p in files if p.suffix.lower() in PAGE_EXT]
            others = [str(p.relative_to(root)) for p in files if p.suffix.lower() not in PAGE_EXT]
            report["counts"]["repos_with_wiki"] += 1
            report["counts"]["other_files"] += len(others)
            info = {"repo": repo, "head": head, "pages": len(pages), "other_files": others[:50], "saved": 0}
            fetched = now()
            for p in pages:
                rel = str(p.relative_to(root))
                text = p.read_bytes().decode("utf-8", errors="replace")
                report["counts"]["pages"] += 1
                outcome = store.save(
                    f"{OWNER}/{repo}:wiki/{rel}", f"{OWNER}/{repo}/{rel}",
                    {"fetched_at": fetched, "request": {"url": url, "head": head}},
                    {"repo": f"{OWNER}/{repo}", "wiki_commit_sha": head, "path": rel,
                     "title": Path(rel).stem.replace("-", " "), "format": p.suffix.lower().lstrip("."),
                     "size": p.stat().st_size, "content": text,
                     "html_url": f"https://github.com/{OWNER}/{repo}/wiki/{Path(rel).stem}"},
                    {"content": text}, f"https://github.com/{OWNER}/{repo}/wiki/{Path(rel).stem}", None)
                key = {"new": "saved_new", "changed": "saved_changed"}.get(outcome, "already_present")
                report["counts"][key] += 1
                info["saved"] += outcome in ("new", "changed")
            # 이번 clone 에 없는 지난 페이지는 deleted_at
            now_keys = {f"{OWNER}/{repo}:wiki/{p.relative_to(root)}" for p in pages}
            for key in [k for k in store.latest if k.startswith(f"{OWNER}/{repo}:wiki/") and k not in now_keys]:
                if store.mark_deleted(key, fetched):
                    report["counts"]["deleted"] = report["counts"].get("deleted", 0) + 1
            heads[repo] = head
            report["repos"].append(info)
        if i % 10 == 0:
            store.flush()
            atomic_write(cursor_path, (json.dumps(heads, ensure_ascii=False, sort_keys=True, indent=1) + "\n").encode())
            print(f"[{i}/{len(repos)}] {report['counts']} 실패 {len(report['failures'])}", flush=True)
    store.flush()
    atomic_write(cursor_path, (json.dumps(heads, ensure_ascii=False, sort_keys=True, indent=1) + "\n").encode())
    report["finished_at"] = now()
    return report, [store]


if __name__ == "__main__":
    raise SystemExit(main())
