"""팀 문서(레포 마크다운) 원본 최초 적재(결정 기록 21번).

레포마다 기본 브랜치의 파일 목록을 받아 `github-repo-tree` 원본으로 남기고,
막을 것(문서가 아닌 것, AI 도구 지시문)만 빼고 모든 마크다운 원문을 `github-teamdoc` 원본으로 저장한다.
원문은 커밋 SHA 고정 주소(raw.githubusercontent.com)에서 받아 GitHub API 한도를 거의 쓰지 않는다.
원본 목록은 원천별 파일(raw_objects/{source}.jsonl)이라 PR 수집과 동시에 돌려도 된다(결정 기록 17-6).

실행: uv run python teamdocs.py [--repos 이름 ...]
"""
from __future__ import annotations

import argparse
import gzip
import hashlib
import json
import os
import re
import tempfile
import time
from datetime import UTC, datetime
from pathlib import Path
from urllib.parse import quote

import httpx

# 원본 저장소 경로와 토큰은 환경 변수가 먼저다(EC2 에서 경로가 다르고 토큰을 파일로 두지 않음, 매일 파이프라인 1-5).
DATA = Path(os.environ.get("RAW_DATA_REPO") or Path.home() / "Desktop" / "woowacourse-raw-data")
STATE = Path(__file__).resolve().parent / "state"
TOKEN_FILE = Path.home() / ".config" / "woowacourse-collect-token"
OWNER = "woowacourse-teams"
API = "https://api.github.com"
RAW = "https://raw.githubusercontent.com"
COLLECTOR = {"name": "github-teamdoc", "version": "1.2.0"}  # 1.2.0: 에이전트용 파일만 제외(결정 기록 22-4)
DOC_EXT = (".md", ".mdx", ".markdown")
DELAY = 0.2

# 원본에서 빼는 것은 "에이전트(AI 도구)가 읽으려고 둔 파일"과 레포 밖에서 온 의존성 · 빌드 산출물뿐이다(결정 기록 22-4).
# 팀 문서는 최대한 유지한다: 빈 파일, 도구 기본 문서, 사본, 템플릿, 테스트 데이터도 원본에 두고 가공 단계에서 고른다.
# 팀 · 레포 이름이 아니라 AI 도구들의 공개된 관례 이름으로 판단한다.
AGENT_TOOL_DIRS = {
    ".claude", ".agents", ".agent", ".cursor", ".codex", ".gemini", ".windsurf", ".kiro", ".opencode", ".omo", ".omc",
    ".omd", ".roo", ".clinerules", ".continue", ".aider", ".sisyphus", ".junie", ".amazonq", ".trae", ".qoder", ".serena",
}
AGENT_FILES = re.compile(
    r"(^|/)(AGENTS|CLAUDE|CLAUDE\.local|GEMINI|SKILL|QWEN|CRUSH|WARP)\.md$"
    r"|(^|/)copilot-instructions\.md$|\.(prompt|instructions|chatmode)\.md$"
    r"|^\.github/(prompts|instructions|chatmodes|agents)/", re.I)
AGENT_MEMORY_DIRS = {"llm-wiki", "memory-bank"}
NOT_TEAM_FILES = re.compile(r"(^|/)(node_modules|vendor|bower_components|dist|build|out|\.next|coverage|target)/", re.I)


def now() -> str:
    return datetime.now(UTC).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def stamp(iso: str) -> str:
    return iso.replace("-", "").replace(":", "")


def sha256(obj) -> str:
    return "sha256:" + hashlib.sha256(json.dumps(obj, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode()).hexdigest()


def atomic_write(path: Path, data: bytes) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    fd, tmp = tempfile.mkstemp(dir=path.parent, prefix=".tmp-")
    with os.fdopen(fd, "wb") as f:
        f.write(data)
    os.replace(tmp, path)


def blocked(path: str) -> str | None:
    dirs = [d.lower() for d in path.split("/")[:-1]]
    if any(d in AGENT_TOOL_DIRS for d in dirs):
        return "a1_agent_tool_dir"
    if AGENT_FILES.search(path):
        return "a2_agent_instruction"
    if any(d in AGENT_MEMORY_DIRS for d in dirs):
        return "a3_agent_memory"
    if any(re.fullmatch(r"(.+[_-])?evals?", d) for d in dirs):
        return "a4_llm_eval_suite"
    if NOT_TEAM_FILES.search(path):
        return "not_team_file"
    return None


class Store:
    """원천별 목록 파일 하나를 맡는다."""

    def __init__(self, source: str, run_id: str):
        self.source, self.run_id = source, run_id
        self.changes: list[dict] = []  # 이번 실행에서 new, changed, deleted 된 것(매일 파이프라인의 변경 목록)
        self.path = DATA / "raw_objects" / f"{source}.jsonl"
        # 일지: 원본 파일을 쓴 직후 한 줄씩 덧붙인다. flush 전에 죽어도 다음 실행이 읽어 들여
        # 같은 내용을 "처음 보는 것"으로 다시 저장하지 않는다(목록에서 빠진 원본 파일이 생기지 않게).
        self.journal = DATA / "raw_objects" / f".{source}.journal.jsonl"
        self.rows = [json.loads(l) for l in self.path.read_text().splitlines() if l.strip()] if self.path.exists() else []
        self.latest = {}
        for r in self.rows:
            if r.get("is_latest"):
                self.latest[r["external_key"]] = r
        if self.journal.exists():  # 지난 실행이 flush 전에 끝남 → 일지의 행을 이어 붙인다
            for line in self.journal.read_text().splitlines():
                if not line.strip():
                    continue
                row = json.loads(line)
                prev = self.latest.get(row["external_key"])
                if prev and prev["version"] >= row["version"]:
                    continue  # 이미 목록에 들어간 행
                if prev:
                    prev["is_latest"] = False
                self.rows.append(row)
                self.latest[row["external_key"]] = row

    def save(self, key: str, key_path: str, envelope_extra: dict, payload: dict, hash_obj,
             canonical_url: str | None, source_updated_at: str | None) -> str:
        h = sha256(hash_obj)
        prev = self.latest.get(key)
        if prev and prev["content_hash"] == h:
            prev["last_seen_at"] = envelope_extra["fetched_at"]
            if prev.get("deleted_at"):  # 지워졌다가 같은 내용으로 다시 생김 → 되살림
                prev["deleted_at"] = None
                self.changes.append({"source": self.source, "external_key": key, "version": prev["version"],
                                     "status": "restored"})
                return "restored"
            return "already_present"
        version = (prev["version"] + 1) if prev else 1
        fetched = envelope_extra["fetched_at"]
        rel = f"raw/{self.source}/{key_path}/v{version}_{stamp(fetched)}.json.gz"
        env = {"source": self.source, "external_key": key, "version": version, "source_updated_at": source_updated_at,
               "collector": {**COLLECTOR, "name": self.source, "run_id": self.run_id}, "content_hash": h,
               "payload": payload, **envelope_extra}
        atomic_write(DATA / rel, gzip.compress(json.dumps(env, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode()))
        if prev:
            prev["is_latest"] = False
        row = {"canonical_url": canonical_url, "content_hash": h, "deleted_at": None, "external_key": key,
               "fetched_at": fetched, "is_latest": True, "last_seen_at": fetched, "run_id": self.run_id,
               "s3_key": rel, "source": self.source, "source_updated_at": source_updated_at, "version": version}
        self.rows.append(row)
        self.latest[key] = row
        self.journal.parent.mkdir(parents=True, exist_ok=True)  # 빈 상태에서 처음 저장할 때
        with open(self.journal, "a", encoding="utf-8") as f:
            f.write(json.dumps(row, ensure_ascii=False, sort_keys=True) + "\n")
        status = "new" if version == 1 else "changed"
        self.changes.append({"source": self.source, "external_key": key, "version": version, "status": status})
        return status

    def mark_deleted(self, key: str, at: str) -> bool:
        """원천에서 사라진 것을 지우지 않고 deleted_at 만 찍는다(원본 보존). 이미 찍혔으면 False."""
        row = self.latest.get(key)
        if not row or row.get("deleted_at"):
            return False
        row["deleted_at"] = at
        self.changes.append({"source": self.source, "external_key": key, "version": row["version"], "status": "deleted"})
        return True

    def flush(self) -> None:
        data = "".join(json.dumps(r, ensure_ascii=False, sort_keys=True, separators=(",", ":")) + "\n" for r in self.rows)
        atomic_write(self.path, data.encode())
        self.journal.unlink(missing_ok=True)  # 목록에 모두 들어갔으니 일지는 비운다


class GH:
    def __init__(self, token: str):
        self.c = httpx.Client(timeout=60, follow_redirects=True,
                              headers={"Authorization": f"Bearer {token}", "Accept": "application/vnd.github+json",
                                       "User-Agent": "shout-outz-raw-collector/0.1"})
        self.api_calls = 0
        self.raw_calls = 0

    def get(self, url: str, raw: bool = False) -> httpx.Response:
        for attempt in range(6):
            try:
                r = self.c.get(url)
            except (httpx.TransportError,) as e:
                if attempt == 5:
                    raise
                time.sleep(min(2 ** attempt, 30))
                continue
            if raw:
                self.raw_calls += 1
            else:
                self.api_calls += 1
            if r.status_code in (403, 429) and not raw:
                wait = int(r.headers.get("retry-after") or 0) or max(60, int(r.headers.get("x-ratelimit-reset", time.time() + 60)) - int(time.time()) + 5)
                time.sleep(min(wait, 3700))
                continue
            if r.status_code >= 500 and attempt < 5:
                time.sleep(min(2 ** attempt, 30))
                continue
            return r
        return r


def token() -> str:
    return os.environ.get("GITHUB_TOKEN") or TOKEN_FILE.read_text().strip()


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--repos", nargs="+")
    ap.add_argument("--full", action="store_true", help="커밋이 같아도 레포를 다시 본다(규칙을 바꿨을 때). 내용이 같은 파일은 여전히 받지 않는다")
    args = ap.parse_args()
    repos = args.repos or json.loads((STATE / "org_repos.json").read_text())
    report, _ = collect(repos, "teamdoc-" + stamp(now()), force=args.full)
    atomic_write(DATA / "runs" / f"run-{report['run_id']}.json",
                 (json.dumps(report, ensure_ascii=False, sort_keys=True, indent=2) + "\n").encode())
    print(json.dumps({"counts": report["counts"], "failures": len(report["failures"]), "api_calls": report["api_calls"],
                      "raw_calls": report["raw_calls"]}, ensure_ascii=False))
    return 0


def collect(repos: list[str], run_id: str, force: bool = False) -> tuple[dict, list[Store]]:
    """레포 목록을 받아 바뀐 팀 문서만 저장한다. 최초 적재와 매일 실행이 같이 쓴다."""
    gh = GH(token())
    trees, docs = Store("github-repo-tree", run_id), Store("github-teamdoc", run_id)
    report = {"run_id": run_id, "started_at": now(), "repos": [], "failures": [],
              "counts": {"md_total": 0, "saved_new": 0, "saved_changed": 0, "already_present": 0}}
    for i, repo in enumerate(repos, 1):
        full = f"{OWNER}/{repo}"
        try:
            meta = gh.get(f"{API}/repos/{full}")
            if meta.status_code == 404:
                report["failures"].append({"repo": repo, "reason": "레포 없음(404)"})
                continue
            meta.raise_for_status()
            branch = meta.json()["default_branch"]
            tr = gh.get(f"{API}/repos/{full}/git/trees/{quote(branch, safe='')}?recursive=1")
            if tr.status_code == 409:  # 빈 레포
                report["repos"].append({"repo": repo, "branch": branch, "empty": True})
                continue
            tr.raise_for_status()
            tree = tr.json()
            sha = tree["sha"]
            fetched = now()
            # 증분: 지난번 파일 목록 원본과 비교해, 커밋이 같으면 레포 전체를 건너뛰고
            # 다르면 blob SHA(파일 내용의 지문)가 바뀐 마크다운만 받는다(결정 기록 22-3).
            prev_tree = trees.latest.get(f"{full}@{branch}")
            prev_blobs: dict[str, str] = {}
            if prev_tree:
                prev_payload = json.loads(gzip.decompress((DATA / prev_tree["s3_key"]).read_bytes()))["payload"]
                if prev_payload.get("commit_sha") == sha and not force:
                    trees.latest[f"{full}@{branch}"]["last_seen_at"] = fetched
                    report["repos"].append({"repo": repo, "branch": branch, "commit_sha": sha, "unchanged": True})
                    report["counts"]["repos_unchanged"] = report["counts"].get("repos_unchanged", 0) + 1
                    continue
                prev_blobs = {e["path"]: e.get("sha") for e in prev_payload.get("tree", [])}
                # 지난 트리에 있던 마크다운이 이번 트리에 없으면 deleted_at(지난번 버전에는 없던 처리)
                now_paths = {e["path"] for e in tree.get("tree", [])}
                for path in prev_blobs:
                    if path not in now_paths and docs.mark_deleted(f"{full}:{path}", fetched):
                        report["counts"]["deleted"] = report["counts"].get("deleted", 0) + 1
            trees.save(f"{full}@{branch}", f"{OWNER}/{repo}/{branch}",
                       {"fetched_at": fetched, "request": {"url": f"{API}/repos/{full}/git/trees/{branch}?recursive=1",
                                                           "etag": tr.headers.get("etag")}},
                       {"repo": full, "branch": branch, "commit_sha": sha, "truncated": tree.get("truncated", False),
                        "tree": tree.get("tree", [])},
                       {"commit_sha": sha, "paths": sorted((e["path"], e.get("sha")) for e in tree.get("tree", []))},
                       f"https://github.com/{full}/tree/{branch}", None)
            mds = [e for e in tree.get("tree", []) if e.get("type") == "blob" and e["path"].lower().endswith(DOC_EXT)]
            info = {"repo": repo, "branch": branch, "commit_sha": sha, "truncated": tree.get("truncated", False),
                    "md": len(mds), "saved": 0, "blocked": {}}
            fetched_docs = []
            for e in mds:
                report["counts"]["md_total"] += 1
                why = blocked(e["path"])
                if why:
                    report["counts"][f"blocked_{why}"] = report["counts"].get(f"blocked_{why}", 0) + 1
                    info["blocked"][why] = info["blocked"].get(why, 0) + 1
                    continue
                if prev_blobs.get(e["path"]) == e.get("sha") and f"{full}:{e['path']}" in docs.latest:
                    report["counts"]["unchanged_blob"] = report["counts"].get("unchanged_blob", 0) + 1
                    docs.latest[f"{full}:{e['path']}"]["last_seen_at"] = fetched
                    continue  # 내용이 같다 → 받지 않는다
                url = f"{RAW}/{full}/{sha}/{quote(e['path'])}"
                r = gh.get(url, raw=True)
                if r.status_code != 200:
                    report["failures"].append({"repo": repo, "path": e["path"], "reason": f"HTTP {r.status_code}"})
                    continue
                text = r.content.decode("utf-8", errors="replace")
                fetched_docs.append((e, url, text))
                time.sleep(DELAY)
            for e, url, text in fetched_docs:
                outcome = docs.save(f"{full}:{e['path']}", f"{OWNER}/{repo}/{e['path']}",
                                    {"fetched_at": now(), "request": {"url": url}},
                                    {"repo": full, "branch": branch, "commit_sha": sha, "path": e["path"],
                                     "blob_sha": e.get("sha"), "size": e.get("size"), "markdown": text,
                                     "html_url": f"https://github.com/{full}/blob/{sha}/{quote(e['path'])}"},
                                    {"markdown": text}, f"https://github.com/{full}/blob/{branch}/{e['path']}", None)
                key = {"new": "saved_new", "changed": "saved_changed"}.get(outcome, "already_present")
                report["counts"][key] += 1
                info["saved"] += outcome in ("new", "changed")
            report["repos"].append(info)
        except (httpx.HTTPError, KeyError, ValueError) as e:
            report["failures"].append({"repo": repo, "reason": f"{type(e).__name__}: {e}"[:300]})
        if i % 10 == 0:
            trees.flush()
            docs.flush()
            print(f"[{i}/{len(repos)}] {report['counts']} 실패 {len(report['failures'])}", flush=True)
    trees.flush()
    docs.flush()
    report["api_calls"], report["raw_calls"] = gh.api_calls, gh.raw_calls
    report["finished_at"] = now()
    return report, [trees, docs]


if __name__ == "__main__":
    raise SystemExit(main())
