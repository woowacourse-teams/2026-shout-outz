"""원본 저장소: 원천별 목록 파일(raw_objects/{source}.jsonl)과 버전별 원본 파일(raw/...json.gz)."""
from __future__ import annotations

import gzip
import json

from pipeline.common import DATA, atomic_write, sha256, stamp


class Store:
    """원천별 목록 파일 하나를 맡는다."""

    def __init__(self, source: str, run_id: str, version: str = "1.0.0"):
        # 수집기 버전은 저장소마다 따로 둔다(예전에는 공용 변수를 import 때 바꿔 다른 원천 기록이 섞였음)
        self.source, self.run_id, self.version = source, run_id, version
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
               "collector": {"name": self.source, "version": self.version, "run_id": self.run_id}, "content_hash": h,
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
