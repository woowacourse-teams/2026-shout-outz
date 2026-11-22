"""디스커션 github-discussion(09 문서 9장 R10, 11장 R12, 13장 R15, 14장 R13).

- 조각과 순서(R10-1): 글(post) → 댓글(comment)을 쓴 순서대로, 각 댓글 바로 뒤에 그 댓글의 답글(reply)을 쓴 순서대로
- 붙는 칸은 댓글의 is_answer 하나(R10-2). 본문(R10-3): 글 본문, ## 댓글, ### 아이디(채택된 답변), #### 아이디 의 답글
- 공통 정리 뒤 글자가 없는 조각은 넣지 않는다(R10-4). 탈퇴한 계정은 author 를 비우고 본문에 "(알 수 없는 사용자)"(R10-5)
- 봇(작성자 종류 Bot)이 쓴 댓글과 답글은 넣지 않는다(R12-1, R12-2). 작성자 종류가 없는 원본은 정제 실패(작성자 종류 없음)
- 카테고리로 거르지 않는다(R15-7). 제외 규칙은 글과 사람 댓글, 답글을 합쳐 본다(R15, R13-2)
"""

from __future__ import annotations

import re

from ..model import Draft, RefineFailure
from ..text import clean_markdown, clean_title, to_utc
from ..titles import strip_title
from . import Refiner

UNKNOWN = "(알 수 없는 사용자)"


def check_author(a, where: str) -> None:
    if a is not None and "__typename" not in a:
        raise RefineFailure("no_author_type", where)


def is_bot(a) -> bool:
    return a is not None and a.get("__typename") == "Bot"


def login(a) -> str | None:
    return (a or {}).get("login") or None


def chars(t: str | None) -> int:
    return len(re.sub(r"\s", "", t or ""))


class DiscussionRefiner(Refiner):
    name = "github-discussion"
    rules = ("old_repo", "template_as_is", "empty_form", "link_list", "daily_note", "duplicate", "no_title", "short_body")

    def build(self, raw: dict, entry: dict) -> Draft:
        d = raw.get("discussion")
        repo = (raw.get("found_in") or {}).get("repo")
        if not isinstance(d, dict) or not repo:
            raise RefineFailure("bad_raw", "discussion, found_in.repo 중 없는 것이 있음")
        check_author(d.get("author"), "글")
        for c in d.get("comments") or []:
            check_author(c.get("author"), "댓글")
            for r in c.get("replies") or []:
                check_author(r.get("author"), "답글")

        url = d.get("url") or f"https://github.com/{repo}/discussions/{d.get('number')}"
        original = clean_title(d.get("title"))
        tried = [{"step": "discussion.title", "result": "used" if original else "missing"}]
        if original:
            title, removed, reason = strip_title(original)
            title = title or original
        else:  # 제목이 없어 주소를 대신 쓴 문서는 머리말을 떼지 않는다
            original = title = url
            removed, reason = [], None
            tried.append({"step": "url", "result": "used"})
        published, _ = to_utc(d.get("createdAt"))
        author = clean_title(login(d.get("author")), limit=255) or None

        parts, bot_parts, bot_chars = [], 0, 0
        post = clean_markdown(d.get("body"))
        if post:
            parts.append({"kind": "post", "author": login(d.get("author")), "at": d.get("createdAt"), "text": post})
        n_comments = n_replies = 0
        for c in sorted(d.get("comments") or [], key=lambda x: x.get("createdAt") or ""):
            if is_bot(c.get("author")):
                bot_parts, bot_chars = bot_parts + 1, bot_chars + chars(c.get("body"))
            else:
                t = clean_markdown(c.get("body"))
                if t:
                    parts.append({"kind": "comment", "author": login(c.get("author")), "at": c.get("createdAt"),
                                  "is_answer": bool(c.get("isAnswer")), "text": t})
                    n_comments += 1
            # 댓글이 빠져도(봇, 빈 댓글) 사람 답글은 남긴다. 본문에서는 앞 댓글 뒤에 이어진다
            for r in sorted(c.get("replies") or [], key=lambda x: x.get("createdAt") or ""):
                if is_bot(r.get("author")):
                    bot_parts, bot_chars = bot_parts + 1, bot_chars + chars(r.get("body"))
                    continue
                t = clean_markdown(r.get("body"))
                if t:
                    parts.append({"kind": "reply", "author": login(r.get("author")), "at": r.get("createdAt"), "text": t})
                    n_replies += 1
        category = (d.get("category") or {}).get("name")
        return Draft(
            title=title, original_title=original, url=url, published_at=published, author=author, repo=repo,
            meta={"number": d.get("number"), "category": category, "comment_count": n_comments, "reply_count": n_replies,
                  "has_answer": bool(d.get("answer"))},
            parts=parts,
            trace={"title": {"tried": tried, "removed": removed, "no_title": reason},
                   "published_at": {"tried": [{"step": "discussion.createdAt", "result": "used" if published else "missing"}],
                                    "raw": d.get("createdAt"), "assumed_tz": None},
                   "author": {"tried": [{"step": "discussion.author.login", "result": "used" if author else "missing"}]}},
            no_title=reason,
            facts={"label": f"{d.get('title') or ''} [{category or ''}]", "first_text": post},
            stats={"bot_parts_removed": bot_parts, "bot_chars_removed": bot_chars},
        )

    def render(self, parts: list[dict], draft: Draft) -> str:
        out = []
        rest = [p for p in parts if p["kind"] != "post"]
        post = next((p for p in parts if p["kind"] == "post"), None)
        if post:
            out.append(post["text"])
        if rest:
            out.append("## 댓글")
            for p in rest:
                who = p.get("author") or UNKNOWN
                if p["kind"] == "comment":
                    out.append(f"### {who}{'(채택된 답변)' if p.get('is_answer') else ''}\n{p['text']}")
                else:
                    out.append(f"#### {who} 의 답글\n{p['text']}")
        return "\n\n".join(out)
