"""원천 파일 모음. 원천마다 runner.Source 를 상속한 클래스 하나."""

from __future__ import annotations

import importlib

_REGISTRY = {
    "youtube-tekotalk": ("tekotalk", "TekotalkSource"),
    "blog-post": ("blog", "BlogSource"),
    "github-teamdoc": ("teamdoc", "TeamdocSource"),
    "github-wiki": ("wiki", "WikiSource"),
    "github-discussion": ("discussion", "DiscussionSource"),
    "github-pr": ("pr", "PrSource"),
}


def make(name: str):
    module, cls = _REGISTRY[name]
    return getattr(importlib.import_module(f"{__name__}.{module}"), cls)()
