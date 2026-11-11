"""아는 키 색인 index/{source}.jsonl(08 문서 C4, 4-1).

키마다 한 줄. 공통 칸 external_key, path, version, fetched_at, gone 과 원천별 칸(바뀜 표시 등).
원본을 먼저 쓰고 색인은 그 뒤에 고친다. 실행 중에도 INDEX_CHECKPOINT 건마다 중간 저장한다.
"""

from __future__ import annotations

import json

from .config import INDEX_CHECKPOINT
from .store import Store


class Index:
    def __init__(self, store: Store, source: str, checkpoint: int = INDEX_CHECKPOINT) -> None:
        self.store = store
        self.path = f"index/{source}.jsonl"
        self.entries: dict[str, dict] = {}
        self.checkpoint = checkpoint
        self._pending = 0

    def load(self) -> "Index":
        b = self.store.read_bytes(self.path)
        self.entries = {}
        if b:
            for line in b.decode("utf-8").splitlines():
                if line.strip():
                    e = json.loads(line)
                    self.entries[e["external_key"]] = e
        return self

    def get(self, key: str) -> dict | None:
        return self.entries.get(key)

    def alive(self) -> list[dict]:
        return [e for e in self.entries.values() if not e.get("gone")]

    def put(self, entry: dict) -> None:
        self.entries[entry["external_key"]] = entry
        self._touch()

    def mark_gone(self, key: str, at: str, reason: str, **extra) -> None:
        """사라짐 기록. extra 는 이유에 딸린 값(예: 레포 이름이 바뀌면 renamed_to, 2026-11-10 R18-8)."""
        self.entries[key]["gone"] = {"at": at, "reason": reason, **extra}
        self._touch()

    def note_gone_check(self, key: str, at: str) -> None:
        """사라진 문서를 다시 열어 봤지만 아직 없을 때, 다시 확인한 시각만 남긴다(블로그, R18-9)."""
        gone = self.entries[key].get("gone")
        if gone:
            gone["checked_at"] = at
            self._touch()

    def clear_gone(self, key: str) -> None:
        if self.entries[key].get("gone"):
            self.entries[key]["gone"] = None
            self._touch()

    def _touch(self) -> None:
        self._pending += 1
        if self._pending >= self.checkpoint:
            self.save()

    def save(self) -> None:
        lines = [json.dumps(self.entries[k], ensure_ascii=False) for k in sorted(self.entries)]
        self.store.write_bytes(self.path, ("\n".join(lines) + ("\n" if lines else "")).encode("utf-8"), "application/x-ndjson")
        self._pending = 0
