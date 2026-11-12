"""원천별 정제. 원천마다 Refiner 를 상속한 클래스 하나(R20-10)."""

from __future__ import annotations

import importlib

from ..model import Draft

_REGISTRY = {
    "youtube-tekotalk": ("tekotalk", "TekotalkRefiner"),
    "blog-post": ("blog", "BlogRefiner"),
    "github-teamdoc": ("teamdoc", "CodeMarkdownRefiner"),
    "github-wiki": ("teamdoc", "WikiRefiner"),
    "github-discussion": ("discussion", "DiscussionRefiner"),
    "github-pr": ("pr", "PrRefiner"),
}


class Refiner:
    name = ""
    rules: tuple[str, ...] = ()   # 이 원천에 쓰는 제외 규칙(2-5 표 순서)

    def build(self, raw: dict, entry: dict) -> Draft:
        """원본 한 건 → Draft. 실패는 RefineFailure, 기다림은 Waiting."""
        raise NotImplementedError

    def render(self, parts: list[dict], draft: Draft) -> str:
        """조각을 정해진 규칙으로 이어 붙인 본문(3-1). 기본은 조각 하나인 원천(블로그, 코드 마크다운, 위키)."""
        return parts[0]["text"] if parts else ""

    def waiting(self, entry: dict) -> bool:
        """원본 색인 한 줄만 보고 아직 정제하지 않고 기다릴 문서인가(테코톡 자막, R8-1)."""
        return False


def make(name: str) -> Refiner:
    module, cls = _REGISTRY[name]
    return getattr(importlib.import_module(f"{__name__}.{module}"), cls)()
