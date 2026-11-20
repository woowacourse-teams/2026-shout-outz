"""규칙을 고치기 전 미리 보기(09 문서 R19-4, R20-4).

지금 코드(고친 규칙)로 원천 하나를 정제만 해 보고, 정제 문서, 색인, 변경 기록은 건드리지 않는다. 결과는
refine/previews/{source}-{시각}.json 에 새로 제외될 문서, 제외가 풀릴 문서, 대표 규칙이 바뀔 문서, 해시가 바뀔 문서,
제목이 바뀔 문서의 수와 목록을 남긴다. 사용자가 보고 나서 규칙 버전을 올린다(R19-3).
같은 레포 사본도 미리 본 결과로 다시 정한다(팀 문서는 다른 두 원천의 지금 정제 색인과 함께).
잠금은 잡지 않는다(원본을 읽기만 하고 미리 보기 파일 하나만 쓴다).
"""

from __future__ import annotations

import logging
from collections import Counter, defaultdict
from datetime import datetime
from typing import Callable

from collector import lock
from collector.config import Settings
from collector.index import Index
from collector.report import iso
from collector.store import Store, open_store

from . import rules as R
from .config import REFINE_DIR, RULES_VERSION, TEAMDOC_SOURCES
from .dedupe import norm_text, pr_duplicates, teamdoc_duplicates
from .model import RefineFailure, Waiting, duplicate_hit, finish, hash_texts
from .report import Capped
from .runner import RefineIndex
from .sources import make

log = logging.getLogger("refine.preview")


def run_preview(settings: Settings, source: str, limit: int | None = None, store: Store | None = None,
                clock: Callable[[], datetime] = lock.now_utc) -> dict:
    store = store or open_store(settings.store_url)
    started = clock()
    refiner = make(source)
    raw = Index(store, source).load()
    idx = RefineIndex(store, source).load()
    keys = [k for k, e in sorted(raw.entries.items())
            if not e.get("gone") and not (idx.get(k) or {}).get("frozen") and not refiner.waiting(e)]
    if limit is not None:
        keys = keys[:max(0, int(limit))]
    out: dict[str, dict] = {}
    texts: dict[str, str] = {}
    failed = Capped()
    for key in keys:
        e = raw.get(key)
        line = idx.get(key) or {}
        try:
            record = store.read_json(e["path"])
            if record is None:
                raise RefineFailure("raw_missing")
            fin = finish(refiner, key, refiner.build(record, e), e.get("version"), RULES_VERSION[source], None)
        except Waiting:
            continue
        except Exception as exc:
            failed.add({"external_key": key, "reason": exc.reason if isinstance(exc, RefineFailure) else f"error_{type(exc).__name__}"})
            continue
        d = fin.doc
        out[key] = {"matched": fin.matched, "content_hash": d["content_hash"], "title": d["title"], **fin.index,
                    "body_hash": d["body_hash"], "repo": d.get("repo")}
        if fin.index.get("dup_candidate") and source in TEAMDOC_SOURCES:
            texts[key] = norm_text(hash_texts(d["parts"]))
    dups = _preview_duplicates(store, source, out, texts)

    result = {"newly_excluded": Capped(), "unexcluded": Capped(), "rule_changed": Capped(), "hash_changed": Capped(),
              "title_changed": Capped(), "new_docs": Capped()}
    by_rule = Counter()
    for key, got in out.items():
        matched = list(got["matched"])
        if key in dups:
            matched.append(duplicate_hit(dups[key]))
        ex = R.excluded_from(matched)
        rule = (ex or {}).get("rule")
        if rule:
            by_rule[rule] += 1
        line = idx.get(key) or {}
        if not line.get("content_hash"):
            result["new_docs"].add(key)
            continue
        before = line.get("excluded_rule")
        if before is None and rule:
            result["newly_excluded"].add({"external_key": key, "rule": rule, "detail": (ex or {}).get("detail"),
                                          "evidence": (ex or {}).get("evidence")})
        elif before and not rule:
            result["unexcluded"].add({"external_key": key, "was": before})
        elif before != rule:
            result["rule_changed"].add({"external_key": key, "before": before, "after": rule})
        if line.get("content_hash") != got["content_hash"]:
            result["hash_changed"].add(key)
        if line.get("title") is not None and line.get("title") != got["title"]:
            result["title_changed"].add({"external_key": key, "before": line.get("title"), "after": got["title"]})
    preview = {
        "source": source, "started_at": iso(started), "finished_at": iso(clock()), "rules_version": RULES_VERSION[source],
        "indexed_rules_versions": dict(Counter(str((idx.get(k) or {}).get("rules_version")) for k in keys)),
        "docs": len(keys), "refined": len(out), "failed": failed.to_json(), "excluded_by_rule": dict(by_rule),
        **{k: v.to_json() for k, v in result.items()},
        "counts": {k: v.total for k, v in result.items()},
    }
    path = f"{REFINE_DIR}/previews/{source}-{started.strftime('%Y-%m-%dT%H-%M-%SZ')}.json"
    store.write_json(path, preview)
    preview["path"] = path
    return preview


def _preview_duplicates(store: Store, source: str, out: dict, texts: dict) -> dict:
    if source not in TEAMDOC_SOURCES and source != "github-pr":
        return {}
    repos: dict[str, list[dict]] = defaultdict(list)
    for key, got in out.items():
        if got.get("dup_candidate"):
            repos[got.get("repo")].append({"key": key, "source": source, "body_hash": got["body_hash"],
                                           "minhash": got.get("minhash"), "readable": got.get("readable"),
                                           "dup_key": got.get("dup_key"), "merged_at": got.get("merged_at")})
    if source == "github-pr":
        res = {}
        for recs in repos.values():
            res.update(pr_duplicates(recs))
        return res
    others = {s: RefineIndex(store, s).load() for s in TEAMDOC_SOURCES if s != source}
    for s, idx in others.items():
        for line in idx.entries.values():
            if (line.get("repo") in repos and line.get("content_hash") and not line.get("gone")
                    and line.get("dup_candidate")):
                repos[line["repo"]].append({"key": line["external_key"], "source": s, "body_hash": line.get("body_hash"),
                                            "minhash": line.get("minhash"), "readable": line.get("readable")})

    def load(rec: dict) -> str | None:
        if rec["key"] in texts:
            return texts[rec["key"]]
        line = others.get(rec["source"]).get(rec["key"]) if rec["source"] in others else None
        try:
            doc = store.read_json(line["path"]) if line else None
        except ValueError:
            return None
        return norm_text(hash_texts(doc.get("parts") or [])) if doc else None

    res = {}
    for recs in repos.values():
        res.update({k: v for k, v in teamdoc_duplicates(recs, load).items() if k in out})
    return res
