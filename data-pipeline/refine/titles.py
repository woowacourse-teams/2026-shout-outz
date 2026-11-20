"""PR 과 팀 문서 제목의 머리말, 꼬리말 떼기와 제목 없음 판단(09 문서 2-3, 확정 2026-11-06).

머리말: 맨 앞에서 차례로 보고 더 뗄 것이 없을 때까지 되풀이한다. 맨 앞 이모지 문자는 건너뛰고 그 뒤를 보며 제자리에 남긴다.
  대괄호(안 40자 이하, 뒤에 글이 있을 때만), 이슈 번호(#12), 작업 종류 낱말 + ':'(범위 괄호, !, 직군 붙은 꼴, issue12: 포함),
  작업 종류 낱말 + '/' 나 ' - '(ci, ui 빼고), 이모지 코드(:sparkles:)
꼬리말: 끝에서 차례로 보고 되풀이한다. 끝 이모지 문자는 남기고 그 앞을 본다.
  괄호 안 이슈 참조((#297), [ISSUE-491], (close #12) 등), 괄호 없는 이슈 참조(#21, close #12), (by 이름)
떼고 남은 제목에 글자가 없으면 empty, 날짜와 차례 말뿐이면 date_only(제외 규칙 no_title).
측정(2026-11-06): PR 제목 12,923개 중 85.3% 에서 뗄 것이 있었고 0.8% 가 제목 없음.
"""

from __future__ import annotations

import html
import re

from .text import remove_comments, split_fences

# 이모지 문자(그림 문자, 기호, 화살표, 기술 기호, 도형, 잡다한 기호, 피부색, 태그, 폭 없는 이음, 꼴 선택, 키캡)
EMOJI_CHARS = (r"[\U0001F000-\U0001FAFF☀-➿⬀-⯿←-⇿⌀-⏿■-◿"
               r"‼⁉™ℹ〰〽㊗㊙©®‍️⃣"
               r"\U0001F3FB-\U0001F3FF\U000E0020-\U000E007F]")
EMOJI_CODE = r":[a-z0-9_+\-]+:"
LEAD_EMOJI = re.compile(rf"^\s*(?:{EMOJI_CHARS}\s*)+")
TAIL_EMOJI = re.compile(rf"(?:\s*{EMOJI_CHARS})+\s*$")

TYPES = ("feat|feature|features|fix|bugfix|hotfix|refactor|refactoring|chore|docs|doc|test|tests|style|design|perf|build|ci|"
         "deploy|release|infra|config|setting|settings|setup|init|merge|sync|revert|rename|remove|env|ui|bug")
TRACKS = "be|fe|an|ios|android|web|all|common|backend|frontend"
WORD = rf"(?:{TYPES}|{TRACKS}|issue\s*#?\d+)(?:[-_/](?:{TYPES}|{TRACKS}))?"
# '/' 규칙에는 ci, ui 를 쓰지 않는다: "CI/CD 구축" 이 "CD 구축", "UI/UX" 가 "UX" 가 되지 않게
SLASH_TYPES = "|".join(w for w in TYPES.split("|") if w not in ("ci", "ui"))

LEAD_RULES = [
    ("대괄호", re.compile(r"^\s*[\[【]([^\]】]{0,40})[\]】](?=\s*\S)")),  # 제목 전체가 대괄호 하나면 머리말이 아니다
    ("이슈 번호", re.compile(r"^\s*#\d+(?!\d)")),
    ("작업 종류:", re.compile(rf"^\s*{WORD}(?:\s*\([^)]{{0,30}}\))?!?\s*[:：]", re.I)),
    ("작업 종류/", re.compile(rf"^\s*(?:{SLASH_TYPES})\s*(?:/\s*#?\d*|\s+-\s+)", re.I)),
    ("이모지 코드", re.compile(rf"^\s*{EMOJI_CODE}")),
]
SEP_LEAD = re.compile(r"^[\s\-–—:|/>]+")

REF = r"(?:#\d+|[A-Z][A-Z0-9]*-\d+)"
KW = r"(?:(?:close[sd]?|fix(?:e[sd])?|resolve[sd]?|issue|refs?)\s*:?\s*)?"
TAIL_RULES = [
    ("괄호 안 이슈 참조", re.compile(r"\s*[\(\[]\s*" + KW + REF + r"(?:\s*,\s*" + KW + REF + r")*\s*[\)\]]\s*$", re.I)),
    ("괄호 없는 이슈 참조", re.compile(r"\s*" + KW + r"#\d+(?:\s*,\s*#\d+)*\s*$", re.I)),
    ("(by 이름)", re.compile(r"\s*\(\s*by\s+[^()]{1,30}\)\s*$", re.I)),
]
SEP_TAIL = re.compile(r"[\s\-–—:|/,]+$")

NOT_LETTER = re.compile(rf"{EMOJI_CHARS}|{EMOJI_CODE}|[\s\W_]")
DATE_WORDS = re.compile(r"[vV](?=\d)|\d|년|월|일|주차|차|회|번째|주|요일|[화수목금토]")


def strip_title(title: str) -> tuple[str, list[dict], str | None]:
    """(뗀 제목, 뗀 것 [{rule, text}], 제목 없음 이유 empty/date_only 또는 None)."""
    s, removed, head, tail = title, [], "", ""
    for _ in range(12):
        before = s
        m = LEAD_EMOJI.match(s)
        if m:
            head += m.group(0)
            s = s[m.end():]
        for name, pat in LEAD_RULES:
            m = pat.match(s)
            if m:
                removed.append({"rule": name, "text": m.group(0).strip()})
                s = s[m.end():]
                break
        s = SEP_LEAD.sub("", s)
        if s == before:
            break
    for _ in range(12):
        before = s
        m = TAIL_EMOJI.search(s)
        if m:
            tail = m.group(0) + tail
            s = s[:m.start()]
        for name, pat in TAIL_RULES:
            m = pat.search(s)
            if m:
                removed.append({"rule": name, "text": m.group(0).strip()})
                s = s[:m.start()]
                break
        s = SEP_TAIL.sub("", s)
        if s == before:
            break
    core = s.strip()
    final = re.sub(r"\s+", " ", head + core + tail).strip()
    letters = NOT_LETTER.sub("", core)
    if not letters:
        return final, removed, "empty"
    if DATE_WORDS.sub("", letters) == "":
        return final, removed, "date_only"
    return final, removed, None


def first_h1(text: str) -> tuple[str, int] | None:
    """본문의 첫 1단계 제목(# 제목, === 밑줄 제목, <h1>) 중 서식, 링크, 이미지를 걷어 낸 뒤 글자나 숫자가 남는 첫 번째와
    그 줄 번호(1부터). 코드 블록과 HTML 주석 안의 # 줄은 보지 않는다(2-3, R9-1)."""
    t = remove_comments(text, keep_newlines=True)
    t = "\n".join("\n" * seg.count("\n") if code else seg for code, seg in split_fences(t))  # 코드 블록은 줄 수만 남긴다
    lines = t.split("\n")
    cands = []
    for i, ln in enumerate(lines):
        m = re.match(r"^ {0,3}#(?!#)\s*(.*?)\s*#*\s*$", ln)
        if m:
            cands.append((i, m.group(1)))
        if i + 1 < len(lines) and re.match(r"^ {0,3}=+\s*$", lines[i + 1]) and ln.strip():
            cands.append((i, ln.strip()))
        for h in re.findall(r"<h1[^>]*>(.*?)</h1>", ln, flags=re.I | re.S):
            cands.append((i, h))
    for i, c in sorted(cands, key=lambda x: x[0]):
        s = re.sub(r"!\[[^\]]*\]\([^)]*\)", "", c)
        s = re.sub(r"\[([^\]]*)\]\([^)]*\)", r"\1", s)
        s = re.sub(r"<img[^>]*>", "", s, flags=re.I)
        s = re.sub(r"<[^>]+>", "", s)
        s = html.unescape(s)
        s = re.sub(r"[*_`~]", "", s)
        s = re.sub(r"\s+", " ", s).strip()
        if any(ch.isalnum() for ch in s):  # R9-1: 이모지나 기호만 남으면 다음 제목으로
            return s, i + 1
    return None
