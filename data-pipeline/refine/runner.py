"""정제 공통 흐름(09 문서 R4 저장, R18 사라진 원본, R19 다시 정제, R20 실행).

    1. 잠금 잡기(수집기와 같은 state/lock.json, R20-3), 보고서 running 쓰기
    2. 원천마다(작은 원천부터, R20-6)
       2-1. 고르기(R19-1): 원본 색인과 정제 색인을 견준다. 원본 파일을 열지 않는다
            처음 보는 키, 원본 버전이 오른 키, 지난번 실패한 키, 규칙 버전이 바뀐 원천의 키 → 정제
            사라짐 표시가 생긴 키 → gone 만 고침, 지워진 키 → 되살림(원본도 바뀌었으면 다시 정제)
            1~7기 블로그(고정, R18-11)와 사라진 문서(R18-6)는 정제하지 않는다. 자막을 받지 않은 테코톡은 기다림(R8)
       2-2. 정제(한 번 실행에 최대 1만 건, R20-6): 원본 한 건 읽기 → 원천별 정리 → 공통(가리기, 해시, 본문, 제외 규칙)
            → 정제 문서 쓰기 → 정제 색인(200건마다 중간 저장, R20-5) → 변경 기록. 한 건씩 흘려 처리한다(R20-9)
       2-3. 사라짐, 돌아옴, 옮겨짐(R18-2, R18-5, R18-8)
    3. 같은 레포 사본 다시 정하기(R17, R19-2): 이번에 문서가 바뀐 레포만. 팀 문서 세 원천을 함께, PR 은 따로
    4. 제외 바뀜(새로 제외, 제외 풀림), 규칙별 제외 수, 원천별 현황을 보고서와 변경 기록에, 잠금 풀기
"""

from __future__ import annotations

import io
import json
import logging
import subprocess
from collections import Counter, defaultdict
from dataclasses import dataclass, field
from datetime import datetime, timedelta
from pathlib import Path
from typing import Callable

from collector import lock
from collector.config import Settings
from collector.index import Index
from collector.keys import repo_of_key, sha1
from collector.report import iso, new_run_id, peak_rss_mb
from collector.store import Store, open_store

from . import rules as R
from .config import (AFTER_COLLECT_HOURS, CHANGE_TEXT_CAP, HASH_VERSION, MAX_PER_RUN, REFINE_DIR, RULES_VERSION, SOURCES,
                     TEAMDOC_SOURCES)
from .dedupe import norm_text, pr_duplicates, teamdoc_duplicates
from .model import RefineFailure, Waiting, duplicate_hit, finish, hash_texts
from .report import LogTail, RefineReport, SourceStats
from .sources import Refiner, make

log = logging.getLogger("refine")

NEW = object()  # touched 에서 "이번 실행 전에는 정제 문서가 없었다" 표시
MOVE_REASONS_SAME_REPO = {"not_in_tree", "not_in_wiki"}  # 같은 레포 안에서 파일만 옮겼을 수 있는 사라짐(R18-8 ②)


def doc_path(source: str, key: str) -> str:
    return f"{REFINE_DIR}/docs/{source}/{sha1(key)}.json"


class RefineIndex(Index):
    """정제 색인 refine/index/{source}.jsonl(5장). 수집기 색인과 같은 모양의 파일, 키마다 한 줄."""

    def __init__(self, store: Store, source: str) -> None:
        super().__init__(store, source)
        self.path = f"{REFINE_DIR}/index/{source}.jsonl"

    def save(self) -> None:
        """줄마다 바로 바이트로 써 모은다. 줄 목록과 이은 글자를 따로 만들지 않아, 4만 5천 줄 PR 색인을 200건마다
        저장해도 메모리가 덜 든다(1GB EC2, R20-9)."""
        buf = io.BytesIO()
        for k in sorted(self.entries):
            buf.write(json.dumps(self.entries[k], ensure_ascii=False).encode("utf-8"))
            buf.write(b"\n")
        self.store.write_bytes(self.path, buf.getvalue(), "application/x-ndjson")
        self._pending = 0


def code_version() -> str:
    try:
        out = subprocess.run(["git", "rev-parse", "--short", "HEAD"], cwd=Path(__file__).parent,
                             capture_output=True, text=True, timeout=5)
        return out.stdout.strip() or "unknown"
    except Exception:
        return "unknown"


def _parse(ts: str | None) -> datetime | None:
    if not ts:
        return None
    try:
        return datetime.fromisoformat(ts.replace("Z", "+00:00"))
    except ValueError:
        return None


def pick_run_id(store: Store, now: datetime, host: str) -> tuple[str, str | None]:
    """--after-collect: 가장 최근 수집 실행의 run_id 를 같은 실행 묶음으로 쓴다(R20-1, R20-7).
    AFTER_COLLECT_HOURS 안에 시작했고 아직 그 run_id 의 정제 보고서가 없을 때만. 아니면 새 run_id."""
    for path in reversed(store.list("reports/")):
        try:
            rep = store.read_json(path) or {}
        except ValueError:
            continue
        if (rep.get("scope") or {}).get("command"):  # 자막 저장, 묶음 넣기는 수집 실행이 아니다
            continue
        started = _parse(rep.get("started_at"))
        run_id = rep.get("run_id")
        if not run_id or not started or now - started > timedelta(hours=AFTER_COLLECT_HOURS):
            break
        if store.read_bytes(f"{REFINE_DIR}/reports/{run_id}.json") is not None:
            break
        return run_id, run_id
    return new_run_id(now, host), None


def history_line(doc: dict, run_id: str, at: str) -> dict:
    """이력 한 줄(2-4, 2-5 의 3). 제외됐으면 그때의 detail 과 evidence 도 적는다(나중에 왜 뺐는지가 이력에 남게)."""
    ex = doc.get("excluded") or {}
    tried = ((doc.get("trace") or {}).get("title") or {}).get("tried") or []
    used = next((t["step"] for t in tried if t.get("result") == "used"), None)
    line = {"at": at, "run_id": run_id, "raw_version": doc.get("raw_version"), "rules_version": doc.get("rules_version"),
            "title": doc.get("title"), "title_from": used,
            "removed": ((doc.get("trace") or {}).get("title") or {}).get("removed") or [],
            "no_title": ((doc.get("trace") or {}).get("title") or {}).get("no_title"),
            "excluded_rule": ex.get("rule"), "content_hash": doc.get("content_hash"), "gone": doc.get("gone")}
    if ex:
        line["excluded_detail"] = ex.get("detail")
        line["excluded_evidence"] = ex.get("evidence")
    return line


HISTORY_KEYS = ("title", "title_from", "content_hash", "excluded_rule", "gone")


def add_history(doc: dict, prev: list | None, run_id: str, at: str) -> None:
    """제목, 제목 출처, 내용 해시, 대표 제외 규칙, 사라짐 가운데 하나라도 바뀌면 한 줄을 더한다. 같으면 쌓지 않는다."""
    history = list(prev or [])
    line = history_line(doc, run_id, at)
    last = history[-1] if history else None
    if last is None or any(last.get(k) != line.get(k) for k in HISTORY_KEYS):
        history.append(line)
    doc["history"] = history


def _cap(t: str) -> str:
    return t if len(t) <= CHANGE_TEXT_CAP else t[:CHANGE_TEXT_CAP] + f"…(잘림, 전체 {len(t)}자)"


def _hash_content(doc: dict) -> dict:
    return {"title": doc.get("original_title"), "texts": [_cap(t) for t in hash_texts(doc.get("parts") or [])]}


def _changed_fields(prev: dict, doc: dict) -> list[str]:
    skip = {"history", "refined_at", "run_id", "raw_version", "trace"}
    return sorted(k for k in set(prev) | set(doc) if k not in skip and prev.get(k) != doc.get(k))


@dataclass
class Run:
    settings: Settings
    store: Store
    report: RefineReport
    clock: Callable[[], datetime]
    run_id: str
    budget: int
    changes: list = field(default_factory=list)
    touched: dict = field(default_factory=lambda: defaultdict(dict))   # source → {key: 이번 실행 전 대표 제외 규칙 또는 NEW}
    refined: dict = field(default_factory=lambda: defaultdict(set))    # source → 이번에 정제한 키
    dup_repos: dict = field(default_factory=lambda: defaultdict(set))  # "team" / "pr" → 사본을 다시 정할 레포
    indexes: dict = field(default_factory=dict)                        # source → RefineIndex

    def now_iso(self) -> str:
        return iso(self.clock())

    def index(self, source: str) -> RefineIndex:
        if source not in self.indexes:
            self.indexes[source] = RefineIndex(self.store, source).load()
        return self.indexes[source]

    def stats(self, source: str) -> SourceStats:
        if source not in self.report.sources:
            self.report.sources[source] = SourceStats(source, self.clock())
        return self.report.sources[source]

    def touch(self, source: str, key: str, line: dict | None) -> None:
        if key not in self.touched[source]:
            self.touched[source][key] = NEW if not (line and line.get("content_hash")) else line.get("excluded_rule")

    def change(self, kind: str, source: str, key: str, **more) -> None:
        self.changes.append({"type": kind, "source": source, "external_key": key, "at": self.now_iso(), **more})

    def save_changes(self) -> None:
        if not self.changes:
            return
        data = "\n".join(json.dumps(c, ensure_ascii=False) for c in self.changes) + "\n"
        self.store.write_bytes(f"{REFINE_DIR}/changes/{self.run_id}.jsonl", data.encode("utf-8"), "application/x-ndjson")

    def dup_group(self, source: str) -> str | None:
        return "team" if source in TEAMDOC_SOURCES else ("pr" if source == "github-pr" else None)


# 고르기 -------------------------------------------------------------------------------------------------------

@dataclass
class Plan:
    refine: list = field(default_factory=list)    # (키, 까닭)
    gone: list = field(default_factory=list)      # 사라짐 표시가 새로 생긴 키
    returned: list = field(default_factory=list)  # 사라짐 표시가 지워졌고 원본은 같은 키
    returned_changed: set = field(default_factory=set)  # 돌아왔는데 원본도 바뀐 키(다시 정제)
    waiting: list = field(default_factory=list)
    unchanged: int = 0


def plan(run: Run, refiner: Refiner, raw: Index, idx: RefineIndex, only: list[str] | None, stats: SourceStats) -> Plan:
    pl = Plan()
    rules_version = RULES_VERSION[refiner.name]
    keys = list(dict.fromkeys(only)) if only else sorted(raw.entries)
    for key in keys:
        e = raw.get(key)
        line = idx.get(key)
        if e is None:
            stats.skipped.add({"external_key": key, "why": "원본 색인에 없음"})
            continue
        if line and line.get("frozen"):  # 1~7기 블로그는 고정(R18-11): 다시 정제, 사라짐 표시 모두 하지 않는다
            if only:
                stats.skipped.add({"external_key": key, "why": "고정된 1~7기 블로그"})
            continue
        has_doc = bool(line and line.get("content_hash"))
        if e.get("gone"):
            if has_doc and not line.get("gone"):
                pl.gone.append(key)
            if only:
                stats.skipped.add({"external_key": key, "why": "원천에서 사라진 문서"})
            continue  # 사라진 문서는 정제하지 않는다(R18-6)
        if not has_doc and refiner.waiting(e):
            pl.waiting.append(key)
            continue
        if not has_doc:
            pl.refine.append((key, "retry" if line and line.get("failed") else "new"))
        elif line.get("gone"):
            if e.get("version") != line.get("raw_version"):
                pl.refine.append((key, "raw"))
                pl.returned_changed.add(key)
            else:
                pl.returned.append(key)
        elif only:
            pl.refine.append((key, "only"))
        elif e.get("version") != line.get("raw_version"):
            pl.refine.append((key, "raw"))
        elif line.get("failed"):
            pl.refine.append((key, "retry"))
        elif line.get("rules_version") != rules_version:
            pl.refine.append((key, "rules"))
        elif not str(line.get("content_hash") or "").startswith(HASH_VERSION + ":"):
            pl.refine.append((key, "hash_rule"))
        else:
            pl.unchanged += 1
    return pl


def renamed_key(old_key: str, renamed_to: str) -> str | None:
    """레포 이름이 바뀐 키의 새 키(같은 조직, 같은 경로나 번호, R18-8 ①). 다른 조직으로 옮겼으면 None."""
    repo = repo_of_key(old_key)
    if not repo or not renamed_to or "/" in renamed_to:
        return None
    prefix = f"woowacourse-teams/{repo}"
    return f"woowacourse-teams/{renamed_to}{old_key[len(prefix):]}" if old_key.startswith(prefix) else None


# 원천 하나 -------------------------------------------------------------------------------------------------------

def refine_source(run: Run, name: str, only: list[str] | None) -> None:
    refiner = make(name)
    stats = run.stats(name)
    raw = Index(run.store, name).load()  # 수집기 색인(읽기만 한다)
    idx = run.index(name)
    pl = plan(run, refiner, raw, idx, only, stats)
    stats.counts["unchanged"] += pl.unchanged
    take = pl.refine[:max(0, run.budget)]
    stats.remaining = len(pl.refine) - len(take)
    run.budget -= len(take)
    stats.progress["selected"] = len(take)

    # 기다림(R8-2): 수, 키, 가장 오래 기다린 날 수(원본을 처음 저장한 날부터)
    now = run.clock()
    for key in pl.waiting:
        stats.waiting += 1
        stats.waiting_keys.add(key)
        fetched = _parse((raw.get(key) or {}).get("fetched_at"))
        if fetched:
            days = (now - fetched).days
            stats.waiting_max_days = max(stats.waiting_max_days or 0, days)

    # 옮겨짐 후보(R18-8): 이름이 바뀐 레포의 새 키 → 옛 키, 같은 레포에서 사라진 파일의 (레포, 본문 해시) → 옛 키
    renamed_new: dict[str, str] = {}
    for key, e in raw.entries.items():
        g = e.get("gone") or {}
        line = idx.get(key)
        if g.get("reason") == "repo_renamed" and line and line.get("content_hash"):
            nk = renamed_key(key, g.get("renamed_to"))
            if nk:
                renamed_new.setdefault(nk, key)
    file_moved: dict[tuple, str] = {}
    for key in pl.gone:
        g = (raw.get(key) or {}).get("gone") or {}
        line = idx.get(key) or {}
        if g.get("reason") in MOVE_REASONS_SAME_REPO and line.get("body_hash"):
            file_moved.setdefault((line.get("repo"), line["body_hash"]), key)
    moved_old: dict[str, str] = {}  # 옛 키 → 새 키

    stats.set_phase("refine")
    for key, why in take:
        refine_one(run, refiner, raw, idx, key, why, stats, renamed_new, file_moved, moved_old,
                   returned=key in pl.returned_changed)
        stats.progress["done"] += 1
        stats.peak_rss_mb = peak_rss_mb()
        run.report.tick()

    stats.set_phase("gone")
    for key in pl.returned:
        _set_gone(run, refiner, idx, key, None, stats)
        stats.returned_keys.add(key)
        run.change("returned", name, key)
    for key in pl.gone:
        g = dict(raw.get(key)["gone"])
        _set_gone(run, refiner, idx, key, g, stats)
        new_key = moved_old.get(key)
        if new_key is None and g.get("reason") == "repo_renamed":
            nk = renamed_key(key, g.get("renamed_to"))
            nline = idx.get(nk) if nk else None
            if nline and nline.get("content_hash") and not nline.get("gone"):
                new_key = nk
                _set_moved_from(run, idx, nk, key)
        if new_key:
            stats.moved.add({"from": key, "to": new_key})
            run.change("moved", name, key, to=new_key, reason=g.get("reason"))
        else:
            stats.gone_keys[g.get("reason") or "unknown"].add(key)
            run.change("gone", name, key, reason=g.get("reason"), gone=g)
    stats.set_phase("finish")
    idx.save()


def refine_one(run: Run, refiner: Refiner, raw: Index, idx: RefineIndex, key: str, why: str, stats: SourceStats,
               renamed_new: dict, file_moved: dict, moved_old: dict, returned: bool = False) -> None:
    name = refiner.name
    e = raw.get(key)
    line = idx.get(key)
    prev_fail = (line or {}).get("failed") or {}
    at = run.now_iso()
    try:
        try:
            record = run.store.read_json(e["path"])
        except ValueError as exc:
            raise RefineFailure("raw_unreadable", f"{type(exc).__name__}") from exc
        if record is None:
            raise RefineFailure("raw_missing", e.get("path"))
        draft = refiner.build(record, e)
        dup_of = (line or {}).get("dup_of")
        fin = finish(refiner, key, draft, e.get("version"), RULES_VERSION[name], dup_of)
    except Waiting:
        stats.waiting += 1
        stats.waiting_keys.add(key)
        return
    except Exception as exc:
        if isinstance(exc, RefineFailure):
            reason, message = exc.reason, exc.message
        else:
            log.exception("%s 정제 실패: %s", name, key)
            reason, message = f"error_{type(exc).__name__}", str(exc)[:300]
        count = int(prev_fail.get("count") or 0) + 1
        new_line = dict(line or {"external_key": key, "path": doc_path(name, key), "raw_version": None, "rules_version": None,
                                 "content_hash": None, "body_hash": None, "excluded_rule": None, "refined_at": None,
                                 "gone": None})
        new_line["failed"] = {"reason": reason, "message": message, "at": at, "count": count}
        idx.put(new_line)  # 정제 문서는 쓰지 않는다. 있던 것은 그대로(5장)
        stats.fail(key, reason, message, count)
        return

    doc = fin.doc
    has_prev = bool(line and line.get("content_hash"))
    prev_doc = None
    if has_prev:
        try:
            prev_doc = run.store.read_json(line["path"])
        except ValueError:
            prev_doc = None
    # 옮겨짐(R18-8): 처음 정제하는 키가 이름이 바뀐 레포의 같은 경로이거나, 같은 레포에서 사라진 파일과 본문이 같으면 옛 키를 잇는다
    moved_from = ((prev_doc or {}).get("meta") or {}).get("moved_from")
    if not has_prev:
        old = renamed_new.get(key)
        if old is None and name in ("github-teamdoc", "github-wiki"):
            old = file_moved.pop((doc.get("repo"), doc.get("body_hash")), None)
        if old:
            moved_from = old
            moved_old[old] = key
    if moved_from:
        doc["meta"] = {**doc["meta"], "moved_from": moved_from}

    doc = {"source": doc["source"], "external_key": key, "run_id": run.run_id, "refined_at": at,
           **{k: v for k, v in doc.items() if k not in ("source", "external_key")}}
    add_history(doc, (prev_doc or {}).get("history"), run.run_id, at)
    path = doc_path(name, key)
    run.touch(name, key, line)
    run.store.write_json(path, doc)  # 정제 문서 먼저, 색인은 뒤(5장)

    frozen = name == "blog-post" and str((doc["meta"] or {}).get("origin") or "").startswith("archive-")
    new_line = {"external_key": key, "path": path, "raw_version": doc["raw_version"], "rules_version": doc["rules_version"],
                "content_hash": doc["content_hash"], "body_hash": doc["body_hash"],
                "excluded_rule": (doc["excluded"] or {}).get("rule"), "failed": None, "refined_at": at, "gone": None,
                **fin.index}
    if frozen:
        new_line["frozen"] = True
    if (line or {}).get("dup_of") and fin.index.get("dup_candidate"):
        new_line["dup_of"] = line["dup_of"]
    idx.put(new_line)
    run.refined[name].add(key)
    group = run.dup_group(name)
    if group:
        run.dup_repos[group].add(doc.get("repo"))
        if line and line.get("repo") and line.get("repo") != doc.get("repo"):
            run.dup_repos[group].add(line["repo"])

    # 변경 기록과 보고서(4-8)
    if returned:
        stats.returned_keys.add(key)
        run.change("returned", name, key)
    if not has_prev:
        stats.counts["new"] += 1
        run.change("new", name, key, content_hash=doc["content_hash"], **({"moved_from": moved_from} if moved_from else {}))
        if moved_from and moved_old.get(moved_from) == key:
            pass  # 옛 키의 "옮겨짐" 은 사라짐을 적을 때 함께 남긴다
    elif line.get("content_hash") != doc["content_hash"]:
        if line.get("raw_version") != doc["raw_version"]:
            cause = "raw"
        elif not str(line.get("content_hash") or "").startswith(HASH_VERSION + ":"):
            cause = "hash_rule"
        else:
            cause = "rules"
        stats.counts["changed"] += 1
        stats.changed_by[cause] += 1
        run.change("changed", name, key, cause=cause, before_hash=line.get("content_hash"), after_hash=doc["content_hash"],
                   raw_version=[line.get("raw_version"), doc["raw_version"]],
                   rules_version=[line.get("rules_version"), doc["rules_version"]],
                   before=_hash_content(prev_doc) if prev_doc else None, after=_hash_content(doc))
    elif line.get("raw_version") != doc["raw_version"]:
        stats.counts["ignored"] += 1  # 원본은 새 버전인데 해시가 같다
        fields = _changed_fields(prev_doc, doc) if prev_doc else []
        run.change("ignored", name, key, raw_version=[line.get("raw_version"), doc["raw_version"]],
                   changed_fields=fields or ["원본에서만 바뀜"])
    else:
        stats.counts["unchanged"] += 1
    if prev_doc and prev_doc.get("title") != doc["title"]:
        stats.title_changed.add({"external_key": key, "before": prev_doc.get("title"), "after": doc["title"],
                                 "why": "raw" if line.get("raw_version") != doc["raw_version"] else "rules"})

    tried = doc["trace"].get("title", {}).get("tried") or []
    used = next((t for t in tried if t.get("result") == "used"), None)
    if used:
        stats.title_from[used["step"]] += 1
        if used["step"] in ("url", "path"):
            stats.title_fallback.add(key)
    if (doc["excluded"] or {}).get("rule") == "no_title":
        stats.title_none.add(key)
    ptried = (doc["trace"].get("published_at") or {}).get("tried") or []
    pused = next((t["step"] for t in ptried if t.get("result") == "used"), None)
    stats.published_at_from[pused or "none"] += 1
    if not doc.get("published_at"):
        stats.published_at_missing.add(key)
    stats.masked.update(fin.masked)
    stats.bot_parts_removed += int(draft.stats.get("bot_parts_removed", 0))
    stats.bot_chars_removed += int(draft.stats.get("bot_chars_removed", 0))


def _set_gone(run: Run, refiner: Refiner, idx: RefineIndex, key: str, gone: dict | None, stats: SourceStats) -> None:
    """정제 문서와 색인의 gone 만 고친다(본문은 다시 정제하지 않는다, R18-2, R19-1)."""
    line = idx.get(key)
    run.touch(refiner.name, key, line)
    try:
        doc = run.store.read_json(line["path"])
    except ValueError:
        doc = None
    at = run.now_iso()
    if doc is not None:
        doc["gone"] = gone
        add_history(doc, doc.get("history"), run.run_id, at)
        run.store.write_json(line["path"], doc)
    line = dict(line)
    line["gone"] = gone
    idx.put(line)
    group = run.dup_group(refiner.name)
    if group and line.get("repo"):
        run.dup_repos[group].add(line["repo"])  # 남긴 문서가 사라지면 사본이 대신 남는다(R18-7)


def _set_moved_from(run: Run, idx: RefineIndex, key: str, old_key: str) -> None:
    line = idx.get(key)
    try:
        doc = run.store.read_json(line["path"])
    except ValueError:
        return
    if doc is None or (doc.get("meta") or {}).get("moved_from") == old_key:
        return
    doc["meta"] = {**(doc.get("meta") or {}), "moved_from": old_key}
    run.store.write_json(line["path"], doc)


# 같은 레포 사본(R17, R19-2) -------------------------------------------------------------------------------------------

def dup_pass(run: Run, group: str) -> None:
    sources = list(TEAMDOC_SOURCES) if group == "team" else ["github-pr"]
    repos = {r for r in run.dup_repos.get(group, set()) if r}
    if not repos:
        return
    by_repo: dict[str, list[tuple[str, dict]]] = defaultdict(list)
    for s in sources:
        for line in run.index(s).entries.values():
            if line.get("repo") in repos:
                by_repo[line["repo"]].append((s, line))
    for repo in sorted(repos):
        rows = by_repo.get(repo, [])
        cands = [{"key": ln["external_key"], "source": s, "body_hash": ln.get("body_hash"), "minhash": ln.get("minhash"),
                  "readable": ln.get("readable"), "dup_key": ln.get("dup_key"), "merged_at": ln.get("merged_at")}
                 for s, ln in rows
                 if ln.get("content_hash") and not ln.get("gone") and not ln.get("frozen") and ln.get("dup_candidate")]
        if group == "team":
            src_of = {c["key"]: c["source"] for c in cands}
            result = teamdoc_duplicates(cands, lambda c: _load_text(run, src_of[c["key"]], c["key"]))
        else:
            result = pr_duplicates(cands)
        for s, ln in rows:
            key = ln["external_key"]
            if not ln.get("content_hash"):
                continue
            new = result.get(key)
            if not ln.get("dup_candidate") or ln.get("gone"):
                new = None
            if (new or None) != (ln.get("dup_of") or None):
                _apply_dup(run, s, key, new)


def _load_text(run: Run, source: str, key: str) -> str | None:
    line = run.index(source).get(key)
    try:
        doc = run.store.read_json(line["path"]) if line else None
    except ValueError:
        return None
    return norm_text(hash_texts(doc.get("parts") or [])) if doc else None


def _apply_dup(run: Run, source: str, key: str, dup: dict | None) -> None:
    idx = run.index(source)
    line = idx.get(key)
    run.touch(source, key, line)
    try:
        doc = run.store.read_json(line["path"])
    except ValueError:
        doc = None
    at = run.now_iso()
    rule = None
    if doc is not None:
        matched = [m for m in (doc.get("trace") or {}).get("rules") or [] if m.get("rule") != "duplicate"]
        if dup:
            matched.append(duplicate_hit(dup))
        doc["excluded"] = R.excluded_from(matched)
        doc.setdefault("trace", {})["rules"] = sorted(matched, key=lambda m: R.RANK[m["rule"]])
        add_history(doc, doc.get("history"), run.run_id, at)
        run.store.write_json(line["path"], doc)
        rule = (doc["excluded"] or {}).get("rule")
    line = dict(line)
    line["excluded_rule"] = rule
    if dup:
        line["dup_of"] = dup
    else:
        line.pop("dup_of", None)
    idx.put(line)


# 마무리 -----------------------------------------------------------------------------------------------------------

def finalize_source(run: Run, name: str) -> None:
    """이번 실행 전과 뒤의 대표 제외 규칙을 견줘 새로 제외, 제외 풀림을 남기고(R19-5) 원천별 현황을 센다(2-5 totals)."""
    stats = run.stats(name)
    idx = run.index(name)
    for key in sorted(run.refined.get(name, ())):
        rule = (idx.get(key) or {}).get("excluded_rule")
        if rule:
            stats.excluded_by_rule[rule] += 1
    for key, before in sorted(run.touched.get(name, {}).items(), key=lambda kv: kv[0]):
        line = idx.get(key) or {}
        after = line.get("excluded_rule")
        if before is NEW or line.get("gone"):
            continue
        if before is None and after:
            stats.newly_excluded.add({"external_key": key, "rule": after})
            run.change("excluded", name, key, rule=after)
        elif before and not after:
            stats.unexcluded.add({"external_key": key, "was": before})
            run.change("unexcluded", name, key, was=before)
    totals = {"docs": 0, "passed": 0, "excluded": Counter(), "gone": 0, "failed": 0}
    for line in idx.entries.values():
        if line.get("failed"):
            totals["failed"] += 1
        if not line.get("content_hash"):
            continue
        if line.get("gone"):
            totals["gone"] += 1
            continue
        totals["docs"] += 1
        if line.get("excluded_rule"):
            totals["excluded"][line["excluded_rule"]] += 1
        else:
            totals["passed"] += 1
    totals["excluded"] = dict(totals["excluded"])
    if name == "youtube-tekotalk":
        totals["waiting"] = stats.waiting
    stats.totals = totals
    if name == "blog-post":
        stats.blog_same_title_pairs = same_title_pairs(idx)


def same_title_pairs(idx: RefineIndex):
    """같은 레포의 블로그 글 중 원래 제목이 같은 쌍(R6, 크로스포스트 후보). 묶지 않고 보고서에만 남긴다."""
    from .report import Capped
    groups = defaultdict(list)
    for line in idx.entries.values():
        if line.get("content_hash") and not line.get("gone") and line.get("repo") and line.get("title"):
            groups[(line["repo"].lower(), line["title"].strip().lower())].append(line["external_key"])
    pairs = Capped()
    for (repo, title), keys in sorted(groups.items()):
        keys = sorted(keys)
        for i in range(len(keys)):
            for j in range(i + 1, len(keys)):
                pairs.add({"repo": repo, "title": title, "keys": [keys[i], keys[j]]})
    return pairs


def run_refine(settings: Settings, sources: list[str] | None = None, only: list[str] | None = None, limit: int | None = None,
               after_collect: bool = False, store: Store | None = None,
               clock: Callable[[], datetime] = lock.now_utc) -> RefineReport:
    store = store or open_store(settings.store_url)
    names = [s for s in SOURCES if not sources or s in sources]
    started = clock()
    if after_collect:
        run_id, joined = pick_run_id(store, started, settings.host)
    else:
        run_id, joined = new_run_id(started, settings.host), None
    scope = {"command": "refine run", "sources": names, "only": len(only) if only else None,
             "only_keys": list(only[:100]) if only else None, "limit": limit, "after_collect": after_collect,
             "joined_collect_run": joined}
    report = RefineReport(store, run_id, settings.trigger, settings.host, code_version(), scope, started, clock=clock)
    got, held = lock.acquire(store, run_id, settings.host, started)
    if not got:
        log.warning("다른 실행이 잠금을 잡고 있어 건너뜀: %s", held)
        report.scope["lock_held_by"] = held
        report.finish(clock(), "skipped")
        return report
    run = Run(settings, store, report, clock, run_id, MAX_PER_RUN if limit is None else max(0, int(limit)))
    team_in_scope = [n for n in names if n in TEAMDOC_SOURCES]
    current = None
    try:
        report.write()
        for name in names:
            current = run.stats(name)
            tail = LogTail(current.log_tail)
            logging.getLogger().addHandler(tail)
            log.info("%s 정제 시작", name)
            try:
                refine_source(run, name, only)
                # 같은 레포 사본은 팀 문서 세 원천을 다 정제한 뒤 함께, PR 은 PR 다음에 다시 정한다(R19-2)
                if team_in_scope and name == team_in_scope[-1]:
                    current.set_phase("duplicate")
                    dup_pass(run, "team")
                if name == "github-pr":
                    current.set_phase("duplicate")
                    dup_pass(run, "pr")
            except Exception as exc:
                log.exception("%s 정제 실패", name)
                current.error(exc, "unexpected", clock())
            finally:
                logging.getLogger().removeHandler(tail)
                current.peak_rss_mb = peak_rss_mb()
                for s, idx in run.indexes.items():
                    try:
                        idx.save()
                    except Exception as exc:
                        log.exception("%s 정제 색인 저장 실패", s)
                        run.stats(s).error(exc, "index_save_failed", clock())
            if name in TEAMDOC_SOURCES and name != team_in_scope[-1]:
                continue  # 팀 문서는 사본까지 정한 뒤 함께 마무리한다
            done = [n for n in TEAMDOC_SOURCES if n in team_in_scope or run.touched.get(n)] if name in TEAMDOC_SOURCES else [name]
            for s in done:
                finalize_source(run, s)
                run.stats(s).finished = clock()
            run.save_changes()
            report.write()
            log.info("%s 정제 끝: %s", name, current.counts)
        report.finish(clock())
    except BaseException as exc:
        if current is not None and current.finished is None:
            current.error(exc, "interrupted", clock())
            current.finished = clock()
        for idx in run.indexes.values():
            try:
                idx.save()
            except Exception:
                log.exception("멈춘 뒤 정제 색인 저장 실패")
        try:
            run.save_changes()
            report.finish(clock(), "failure")
        except Exception:
            log.exception("멈춘 정제의 보고서 쓰기 실패")
        raise
    finally:
        lock.release(store, run_id)
    return report
