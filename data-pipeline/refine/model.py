"""정제 문서 한 건 만들기(09 문서 2장 R1, 3장 R2, 4장 R3).

원천 파일(sources/*.py)은 원본을 읽어 Draft(제목 후보와 선정 기록, 주소, 시각, 작성자, 레포, meta, 공통 정리를 마친 조각,
규칙에 쓸 값)를 만든다. 그 뒤는 모든 원천이 같다(finish):
    민감 정보 가리기(R21) → 내용 해시와 본문 해시(R3, R18-8) → 본문 이어 붙이기(R2) → 제외 규칙(2-5) → 정제 문서
"""

from __future__ import annotations

import hashlib
import json
import re
from collections import Counter
from dataclasses import dataclass, field

from . import rules as R
from .config import HASH_VERSION, TEAMDOC_SOURCES
from .dedupe import norm_text, pr_dup_key, signature
from .mask import mask
from .text import readable_chars

FAILURE_MESSAGES = {
    "no_content": "원본에 글이 없음",          # 블로그 빈 껍데기(2-5, R5-2)
    "no_author_type": "작성자 종류 없음",       # 봇을 가를 수 없는 원본(R12-1)
    "empty_subtitle": "자막에 글이 없음",       # 자막은 있는데 글이 나오지 않음(R7-9)
    "bad_subtitle": "자막 형식을 읽지 못함",
    "raw_missing": "원본 파일이 없음",
    "raw_unreadable": "원본 파일을 읽지 못함",
    "bad_raw": "원본 모양이 틀림",
}


class RefineFailure(Exception):
    """정제 실패. 다시 하면 달라질 수 있는 일이다(2-5 의 실패와 제외 가르기). reason 은 보고서와 색인의 이유."""

    def __init__(self, reason: str, detail: str | None = None) -> None:
        super().__init__(reason)
        self.reason = reason
        self.detail = detail

    @property
    def message(self) -> str:
        base = FAILURE_MESSAGES.get(self.reason, self.reason)
        return f"{base}: {self.detail}" if self.detail else base


class Waiting(Exception):
    """아직 정제하지 않고 기다린다(자막을 받지 않은 테코톡, R8). 제외도 실패도 아니다."""


@dataclass
class Draft:
    title: str                      # 머리말과 꼬리말을 뗀 제목(블로그, 테코톡은 original_title 과 같다)
    original_title: str             # 공통 손질만 한 제목(2-3)
    url: str
    published_at: str | None = None
    author: str | None = None
    repo: str | None = None
    meta: dict = field(default_factory=dict)
    parts: list[dict] = field(default_factory=list)
    trace: dict = field(default_factory=dict)
    no_title: str | None = None     # 떼고 나니 제목이 없음(empty, date_only)
    facts: dict = field(default_factory=dict)  # 제외 규칙에 쓰는 값(path, label, first_text, pr)
    stats: dict = field(default_factory=dict)  # 보고서에 모으는 값(뺀 봇 조각 수와 글자 수 등)


def content_hash(original_title: str, texts: list[str]) -> str:
    """원래 제목과 사람이 쓴 조각 글로 만든 내용 해시(4-5). 칸 이름순, 공백 없는 UTF-8 JSON 의 SHA-256."""
    payload = json.dumps({"title": original_title, "texts": texts}, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
    return f"{HASH_VERSION}:{hashlib.sha256(payload.encode('utf-8')).hexdigest()}"


def body_hash(texts: list[str]) -> str:
    """제목을 빼고 조각 글만으로 만든 해시(공백을 하나로 맞춤). 같은 레포 사본(R17-1)과 옮긴 문서(R18-8)를 찾는다."""
    return hashlib.sha256(norm_text(texts).encode("utf-8")).hexdigest()


def hash_texts(parts: list[dict]) -> list[str]:
    """해시에 넣는 조각 글: 맥락으로 남긴 봇 조각(bot: true)은 뺀다(4-2)."""
    return [p["text"] for p in parts if not p.get("bot")]


@dataclass
class Finished:
    doc: dict          # 정제 문서(history 는 runner 가 붙인다)
    index: dict        # 정제 색인 줄에 더할 칸(사본 비교, 제목 등)
    matched: list      # 걸린 규칙(duplicate 빼고)
    masked: Counter


def finish(refiner, key: str, draft: Draft, raw_version: int, rules_version: int, dup_of: dict | None) -> Finished:
    """Draft 를 정제 문서로 만든다. dup_of 는 지난 정제에서 정한 사본 정보(레포 단위로 다시 정하기 전까지 이어 쓴다)."""
    counts: Counter = Counter()
    original_title, _ = mask(draft.original_title, counts)
    title, _ = mask(draft.title, counts)
    parts = []
    for p in draft.parts:
        q = dict(p)
        q["text"], _ = mask(p["text"], counts)
        if q.get("code"):
            q["code"], _ = mask(q["code"], counts)
        parts.append(q)
    texts = hash_texts(parts)
    body = refiner.render(parts, draft)

    matched = evaluate(refiner, draft, parts, title, original_title)
    candidate = not any(m["rule"] in R.BEFORE_DUPLICATE for m in matched)
    all_matched = list(matched)
    if dup_of and candidate and "duplicate" in refiner.rules:
        all_matched.append(duplicate_hit(dup_of))
    excluded = R.excluded_from(all_matched)

    trace = dict(draft.trace)
    trace["rules"] = sorted(all_matched, key=lambda m: R.RANK[m["rule"]])
    trace["masked"] = dict(counts)
    doc = {
        "source": refiner.name,
        "external_key": key,
        "raw_version": raw_version,
        "rules_version": rules_version,
        "title": title,
        "original_title": original_title,
        "url": draft.url,
        "published_at": draft.published_at,
        "author": draft.author,
        "repo": draft.repo,
        "popularity": None,
        "meta": draft.meta,
        "excluded": excluded,
        "gone": None,
        "content_hash": content_hash(original_title, texts),
        "body_hash": body_hash(texts),
        "parts": parts,
        "body": body,
        "trace": trace,
    }
    index = {"repo": draft.repo, "title": title}
    if refiner.name in TEAMDOC_SOURCES:
        index["dup_candidate"] = candidate
        if candidate:
            index["readable"] = readable_chars("\n".join(texts))[0]
            index["minhash"] = signature(norm_text(texts))
    elif refiner.name == "github-pr":
        index["dup_candidate"] = candidate
        if candidate:
            body_part = next((p["text"] for p in parts if p["kind"] == "pr_body"), "")
            index["dup_key"] = pr_dup_key(original_title, body_part)
            index["merged_at"] = draft.published_at
    return Finished(doc, index, matched, counts)


def duplicate_hit(dup: dict) -> dict:
    return {"rule": "duplicate", "detail": dup.get("detail"),
            "evidence": {"duplicate_of": dup.get("of"), "similarity": dup.get("similarity"), "kept_by": dup.get("kept_by")}}


def evaluate(refiner, draft: Draft, parts: list[dict], title: str, original_title: str) -> list[dict]:
    """원천이 쓰는 규칙(refiner.rules, 표 순서)을 모두 본다. duplicate 는 레포 단위로 따로 정한다."""
    f = draft.facts
    texts = [p["text"] for p in parts if not p.get("bot")]
    pr = f.get("pr") or {}
    out = []
    for name in refiner.rules:
        got = None
        if name == "old_repo":
            got = R.old_repo(draft.repo)
        elif name == "tool_dir":
            got = R.tool_dir(f["path"])
        elif name == "ai_instruction":
            got = R.ai_instruction(f["path"])
        elif name == "test_data":
            got = R.test_data(f["path"])
        elif name == "wiki_frame":
            got = R.wiki_frame(f["path"])
        elif name == "changelog":
            got = R.changelog(f["path"])
        elif name == "template_as_is":
            if refiner.name == "github-pr":
                got = R.github_default_title(original_title, pr["human_chars"], pr["author_is_bot"])
            else:
                got = R.template_as_is(f.get("first_text") or "")
        elif name == "empty_form":
            got = R.empty_form(texts)
        elif name == "link_list":
            got = R.link_list(texts)
        elif name == "daily_note":
            got = R.daily_note(f.get("label") or "", texts)
        elif name == "bot_pr":
            got = R.bot_pr(draft.author, pr["author_is_bot"], pr["human_parts"])
        elif name == "release_pr":
            got = R.release_pr(pr.get("base_ref"), pr.get("head_ref"))
        elif name == "revert_pr":
            got = R.revert_pr(original_title)
        elif name == "empty_pr":
            got = R.empty_pr(pr["changed_file_count"], pr["added_lines"], pr["deleted_lines"])
        elif name == "deps_pr":
            got = R.deps_pr(pr["changed_files"], pr["changed_file_count"])
        elif name == "no_title":
            got = R.no_title(draft.no_title, original_title, title)
        elif name == "short_body":
            got = R.short_body(texts)
        h = R.hit(name, got)
        if h:
            out.append(h)
    return out


def strip_struct_lines(text: str) -> str:
    """PR 양식의 소제목 줄과 체크박스 줄을 뺀 글(R16-12 의 human_chars 세기)."""
    return "\n".join(ln for ln in (text or "").split("\n")
                     if not re.match(r"^\s{0,3}#{1,6}\s", ln) and not re.match(r"^\s*[-*+]\s*\[[ xX]\]", ln))
