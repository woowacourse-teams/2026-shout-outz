"""PR github-pr(09 문서 10장 R11, 11장 R12, 15장 R16, 16장 R17).

조각과 순서(R11-1): PR 본문(pr_body) → 일반 댓글(comment, 쓴 순서) → 리뷰(review, 낸 순서) → 코드 리뷰 대화(첫 코멘트 thread 의
시각 순, 대화 안의 thread_reply 는 쓴 순서). 붙는 칸(R11-2): 리뷰 state, 첫 코멘트의 path, resolved, code(마지막 10줄, R11-3).
- PR 본문: 봇이 끼워 넣은 요약(auto-generated comment 시작 표시부터 끝 표시까지)을 지우고(R12-4), 내용이 없는 소제목을
  소제목째 지운다(아래가 비었거나, 빈 목록, 체크 안 한 체크박스, close # 만 있는 소제목, R11-5)
- 바뀐 파일 목록은 본문에 넣지 않고 meta.changed_files 에 둔다(R11-4). 글 없이 승인만 한 리뷰는 빈 조각이라 빠진다(R11-6)
- 봇(작성자 종류 Bot)이 쓴 댓글, 리뷰, 코드 리뷰 코멘트는 넣지 않는다(R12-2). 봇이 시작한 대화에 사람이 답했으면 대화를 남기고
  봇의 첫 코멘트를 맥락으로 둔다(bot: true, 본문에는 "(봇)", 해시에는 사람 글만, R12-3)
- 봇이 연 PR 의 본문은 사람이 쓴 PR 설명으로 본다(R16-11). PR 은 길이로 거르지 않는다(R16-3)
- meta 에 사람 글 글자 수(human_chars)와 바뀐 코드 줄(code_lines)을 둔다(R16-12)
"""

from __future__ import annotations

import re

from ..model import Draft, RefineFailure, strip_struct_lines
from ..text import COMMENT, clean_markdown, clean_title, readable_chars, to_utc
from ..titles import strip_title
from . import Refiner
from .discussion import UNKNOWN, chars, check_author, is_bot, login

# 봇 요약 덩어리(CodeRabbit 의 release notes 요약 등). 시작 표시부터 이름이 같은 끝 표시까지 통째로 지운다(R12-4)
BOT_START = re.compile(r"<!--\s*This is an auto-generated comment:?\s*(.*?)\s*-->", re.S | re.I)
# 빈 목록, 체크 안 한 체크박스, 번호 없는 이슈 참조(close #, - close #)만 있는 줄
EMPTY_LINE = re.compile(r"^\s*([-*]\s*\[ \].*|[-*]\s*|([-*]\s*)?(close[sd]?|fix(e[sd])?|resolve[sd]?)?\s*#\s*)$", re.I)
LOCK_FILE = re.compile(r"(^|/)(package-lock\.json|yarn\.lock|pnpm-lock\.yaml|Podfile\.lock|Gemfile\.lock|poetry\.lock|Cargo\.lock|go\.sum"
                       r"|[^/]*\.lock|[^/]*\.min\.(js|css)|[^/]*\.snap)$", re.I)
STATE = {"APPROVED": "승인", "CHANGES_REQUESTED": "변경 요청", "COMMENTED": "의견", "DISMISSED": "무시됨"}
CODE_LINES = 10


def remove_bot_blocks(text: str) -> tuple[str, int]:
    """봇 요약 덩어리를 지운 글과 지운 글자 수(공백 뺌). 끝 표시가 없는 시작 표시는 그대로 둔다(뒤의 사람 글을 지우지 않게)."""
    removed, pos = 0, 0
    while True:
        m = BOT_START.search(text, pos)
        if not m:
            return text, removed
        end = re.compile(r"<!--\s*end of auto-generated comment:?\s*" + re.escape(m.group(1)) + r"\s*-->", re.I).search(text, m.end())
        if not end:
            pos = m.end()
            continue
        removed += chars(text[m.start():end.end()])
        text = text[:m.start()] + text[end.end():]
        pos = m.start()


def drop_empty_sections(body: str) -> str:
    """내용이 없는 소제목을 소제목째 지운다(R11-5). 체크 안 한 체크박스 줄은 내용이 있는 소제목 안에서는 남는다."""
    lines = body.split("\n")
    heads = [i for i, ln in enumerate(lines) if re.match(r"^#{1,6}\s", ln)]
    drop: set[int] = set()
    for k, i in enumerate(heads):
        nxt = heads[k + 1] if k + 1 < len(heads) else len(lines)
        if all(not s.strip() or EMPTY_LINE.match(s) for s in lines[i + 1:nxt]):
            drop.update(range(i, nxt))
    return "\n".join(ln for j, ln in enumerate(lines) if j not in drop)


def last_lines(code: str | None, n: int = CODE_LINES) -> str:
    lines = (code or "").replace("\r\n", "\n").split("\n")
    return "\n".join(lines[-n:]).strip("\n")


def fence_for(code: str) -> str:
    run = max((len(m.group(0)) for m in re.finditer(r"`+", code)), default=0)
    return "`" * max(3, run + 1)


class PrRefiner(Refiner):
    name = "github-pr"
    rules = ("old_repo", "template_as_is", "bot_pr", "release_pr", "revert_pr", "empty_pr", "deps_pr", "duplicate", "no_title")

    def build(self, raw: dict, entry: dict) -> Draft:
        p = raw.get("pull_request")
        repo = (raw.get("found_in") or {}).get("repo")
        if not isinstance(p, dict) or not repo:
            raise RefineFailure("bad_raw", "pull_request, found_in.repo 중 없는 것이 있음")
        check_author(p.get("author"), "PR")
        for c in p.get("comments") or []:
            check_author(c.get("author"), "댓글")
        for r in p.get("reviews") or []:
            check_author(r.get("author"), "리뷰")
        for t in p.get("reviewThreads") or []:
            for c in t.get("comments") or []:
                check_author(c.get("author"), "코드 리뷰 코멘트")

        url = p.get("url") or f"https://github.com/{repo}/pull/{p.get('number')}"
        original = clean_title(p.get("title"))
        tried = [{"step": "pull_request.title", "result": "used" if original else "missing"}]
        if original:
            title, removed, reason = strip_title(original)
            title = title or original
        else:  # 제목이 없어 주소를 대신 쓴 문서는 머리말을 떼지 않는다
            original = title = url
            removed, reason = [], None
            tried.append({"step": "url", "result": "used"})
        published, _ = to_utc(p.get("mergedAt"))
        author = clean_title(login(p.get("author")), limit=255) or None
        author_is_bot = is_bot(p.get("author"))

        bot_parts = bot_chars = 0
        body_md, removed_chars = remove_bot_blocks(p.get("body") or "")
        bot_chars += removed_chars
        body_md = drop_empty_sections(COMMENT.sub("", body_md.replace("\r\n", "\n").replace("\r", "\n")))
        pr_body = clean_markdown(body_md)

        parts: list[dict] = []
        if pr_body:
            parts.append({"kind": "pr_body", "author": login(p.get("author")), "at": p.get("createdAt"), "text": pr_body})
        n_comments = n_reviews = 0
        for c in sorted(p.get("comments") or [], key=lambda x: x.get("createdAt") or ""):
            if is_bot(c.get("author")):
                bot_parts, bot_chars = bot_parts + 1, bot_chars + chars(c.get("body"))
                continue
            t = clean_markdown(c.get("body"))
            if t:
                parts.append({"kind": "comment", "author": login(c.get("author")), "at": c.get("createdAt"), "text": t})
                n_comments += 1
        for r in sorted(p.get("reviews") or [], key=lambda x: x.get("submittedAt") or ""):
            if is_bot(r.get("author")):
                bot_parts, bot_chars = bot_parts + 1, bot_chars + chars(r.get("body"))
                continue
            t = clean_markdown(r.get("body"))
            if t:
                parts.append({"kind": "review", "author": login(r.get("author")), "at": r.get("submittedAt"),
                              "state": r.get("state"), "text": t})
                n_reviews += 1

        threads = []
        for th in p.get("reviewThreads") or []:
            cs = sorted(th.get("comments") or [], key=lambda x: x.get("createdAt") or "")
            if not cs:
                continue
            code = last_lines(cs[0].get("diffHunk"))
            humans = [(c, clean_markdown(c.get("body"))) for c in cs if not is_bot(c.get("author"))]
            humans = [(c, t) for c, t in humans if t]
            bots = [c for c in cs if is_bot(c.get("author"))]
            if is_bot(cs[0].get("author")):
                if not humans:  # 사람이 답하지 않은 봇 대화는 통째로 빠진다(R12-3 의 B안)
                    bot_parts, bot_chars = bot_parts + len(bots), bot_chars + sum(chars(c.get("body")) for c in bots)
                    continue
                first = cs[0]
                head = {"kind": "thread", "author": login(first.get("author")), "at": first.get("createdAt"),
                        "path": th.get("path"), "resolved": bool(th.get("isResolved")), "code": code,
                        "text": clean_markdown(first.get("body")), "bot": True}
                replies = humans
                dropped = bots[1:]
            else:
                if not humans:
                    bot_parts, bot_chars = bot_parts + len(bots), bot_chars + sum(chars(c.get("body")) for c in bots)
                    continue
                c0, t0 = humans[0]
                head = {"kind": "thread", "author": login(c0.get("author")), "at": c0.get("createdAt"),
                        "path": th.get("path"), "resolved": bool(th.get("isResolved")), "code": code, "text": t0}
                replies = humans[1:]
                dropped = bots
            bot_parts, bot_chars = bot_parts + len(dropped), bot_chars + sum(chars(c.get("body")) for c in dropped)
            group = [head] + [{"kind": "thread_reply", "author": login(c.get("author")), "at": c.get("createdAt"), "text": t}
                              for c, t in replies]
            threads.append((cs[0].get("createdAt") or "", group))
        for _, group in sorted(threads, key=lambda x: x[0]):
            parts.extend(group)

        files = p.get("files") or []
        paths = [f.get("path") for f in files if f.get("path")]
        added, deleted = int(p.get("additions") or 0), int(p.get("deletions") or 0)
        lock_lines = sum(int(f.get("additions") or 0) + int(f.get("deletions") or 0) for f in files if LOCK_FILE.search(f.get("path") or ""))
        human_texts = [strip_struct_lines(pr_body)] + [x["text"] for x in parts if x["kind"] != "pr_body" and not x.get("bot")]
        human_chars = readable_chars("\n".join(human_texts))[0]
        human_parts = sum(1 for x in parts if x["kind"] != "pr_body" and not x.get("bot"))
        meta = {
            "number": p.get("number"), "base_ref": p.get("baseRefName"), "head_ref": p.get("headRefName"),
            "labels": [lb.get("name") for lb in p.get("labels") or [] if lb.get("name")],
            "added_lines": added, "deleted_lines": deleted, "changed_file_count": int(p.get("changedFiles") or 0),
            "comment_count": n_comments, "review_count": n_reviews, "thread_count": len(threads),
            "changed_files": paths, "human_chars": human_chars, "code_lines": max(0, added + deleted - lock_lines),
        }
        return Draft(
            title=title, original_title=original, url=url, published_at=published, author=author, repo=repo, meta=meta,
            parts=parts,
            trace={"title": {"tried": tried, "removed": removed, "no_title": reason},
                   "published_at": {"tried": [{"step": "pull_request.mergedAt", "result": "used" if published else "missing"}],
                                    "raw": p.get("mergedAt"), "assumed_tz": None},
                   "author": {"tried": [{"step": "pull_request.author.login", "result": "used" if author else "missing"}]}},
            no_title=reason,
            facts={"pr": {"author_is_bot": author_is_bot, "human_parts": human_parts, "human_chars": human_chars,
                          "base_ref": meta["base_ref"], "head_ref": meta["head_ref"], "added_lines": added,
                          "deleted_lines": deleted, "changed_file_count": meta["changed_file_count"], "changed_files": paths}},
            stats={"bot_parts_removed": bot_parts, "bot_chars_removed": bot_chars},
        )

    def render(self, parts: list[dict], draft: Draft) -> str:
        out: list[str] = []
        body = next((p for p in parts if p["kind"] == "pr_body"), None)
        if body:
            out.append(body["text"])
        comments = [p for p in parts if p["kind"] == "comment"]
        reviews = [p for p in parts if p["kind"] == "review"]
        if comments:
            out.append("## 댓글\n" + "\n\n".join(f"### {p.get('author') or UNKNOWN}\n{p['text']}" for p in comments))
        if reviews:
            out.append("## 리뷰\n" + "\n\n".join(
                f"### {p.get('author') or UNKNOWN}, {STATE.get(p.get('state'), p.get('state') or '')}\n{p['text']}" for p in reviews))
        blocks: list[str] = []
        for p in parts:
            who = (p.get("author") or UNKNOWN) + ("(봇)" if p.get("bot") else "")
            if p["kind"] == "thread":
                head = f"### {p.get('path') or ''}{'(해결됨)' if p.get('resolved') else ''}"
                code = p.get("code") or ""
                fence = fence_for(code)
                blocks.append(head + (f"\n{fence}\n{code}\n{fence}" if code else "") + f"\n{who}: {p['text']}")
            elif p["kind"] == "thread_reply" and blocks:
                blocks[-1] += f"\n{who}: {p['text']}"
        if blocks:
            out.append("## 코드 리뷰 대화\n" + "\n\n".join(blocks))
        return "\n\n".join(out)
