"""수집기 상태 state/{source}.json(08 문서 C5, 4-2). 블로그(전체 목록 날짜, 피드 주소), 위키(마지막 위키 커밋)만 쓴다."""

from __future__ import annotations

from .store import Store


class State:
    def __init__(self, store: Store, source: str) -> None:
        self.store = store
        self.path = f"state/{source}.json"
        self.data: dict = {}

    def load(self) -> "State":
        self.data = self.store.read_json(self.path) or {}
        return self

    def save(self) -> None:
        self.store.write_json(self.path, self.data)
