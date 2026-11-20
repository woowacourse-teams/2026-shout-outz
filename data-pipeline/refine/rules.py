"""제외 규칙(09 문서 2-5 의 표, 확정 규칙 18개).

규칙마다 바뀌지 않는 이름을 쓴다(보고서의 규칙별 수가 이 이름으로 세어진다). 한 문서가 여러 규칙에 걸리면 표 순서의 첫 번째가
대표 규칙(rule)이고, 걸린 규칙은 모두 matched 에 남는다. 규칙 함수는 걸리면 (detail, evidence), 아니면 None 을 돌려준다.
같은 문서 규칙(duplicate)은 문서 하나만 보고 정할 수 없어 레포 단위로 따로 정한다(dedupe.py).

표 순서의 이유(원칙 1): 어떤 레포인가 → 어떤 파일인가(경로) → 어떤 문서인가(내용 모양) → 같은 문서인가 → 제목 → 길이.
"""

from __future__ import annotations

import re

from .config import SHORT_BODY_CHARS
from .text import alnum_chars, heading_lines, readable_chars, strip_structure

ORDER = [
    "old_repo",        # 1  R17-4
    "tool_dir",        # 2  R14-1
    "ai_instruction",  # 3  R14-2
    "test_data",       # 4  R14-3
    "wiki_frame",      # 5  R14-4
    "changelog",       # 6  R14-5
    "template_as_is",  # 7  R15-1, R16-9
    "empty_form",      # 8  R15-2
    "link_list",       # 9  R15-3
    "daily_note",      # 10 R15-10
    "bot_pr",          # 11 R16-1
    "release_pr",      # 12 R16-2
    "revert_pr",       # 13 R16-4
    "empty_pr",        # 14 R16-7
    "deps_pr",         # 15 R16-8
    "duplicate",       # 16 R17-1, R17-2
    "no_title",        # 17 2-3
    "short_body",      # 18 R13
]
RANK = {name: i for i, name in enumerate(ORDER)}
# 같은 문서 비교는 1~15번 규칙을 모두 지난 문서끼리만 한다(R17-6)
BEFORE_DUPLICATE = set(ORDER[:RANK["duplicate"]])


def excluded_from(matched: list[dict]) -> dict | None:
    """걸린 규칙 목록(표 순서로 정렬됨)으로 excluded 칸을 만든다. 걸린 것이 없으면 None."""
    if not matched:
        return None
    m = sorted(matched, key=lambda r: RANK[r["rule"]])
    first = m[0]
    return {"rule": first["rule"], "detail": first.get("detail"), "evidence": first.get("evidence"),
            "matched": [r["rule"] for r in m]}


def hit(rule: str, got) -> dict | None:
    if got is None:
        return None
    detail, evidence = got
    return {"rule": rule, "detail": detail, "evidence": evidence}


# 1. 레포 ------------------------------------------------------------------------------------------------------

def old_repo(repo: str | None):
    """레포 이름이 -old 로 끝나는 레포의 모든 문서와 PR(R17-4, 사용자 결정: 옛 레포의 콘텐츠는 다루지 않는다)."""
    name = (repo or "").split("/", 1)[-1]
    return (None, {"repo": name}) if name.lower().endswith("-old") else None


# 2~6. 경로(R14) ---------------------------------------------------------------------------------------------------

AI_FILES = {"agents.md", "claude.md", "claude.local.md", "gemini.md", "skill.md", "copilot-instructions.md"}
TEST_DIRS = {"fixture", "fixtures", "__fixtures__", "testdata"}
WIKI_FRAME = re.compile(r"^_(sidebar|footer|header)\.md$", re.I)


def tool_dir(path: str):
    """숨김 폴더(이름이 점으로 시작, .github 포함) 아래 문서. detail 은 경로에서 처음 나온 숨김 폴더(R14-1)."""
    for seg in path.split("/")[:-1]:
        if seg.startswith("."):
            return seg, {"path": path, "part": seg}
    return None


def ai_instruction(path: str):
    """AI 도구가 읽으려고 정한 파일 이름(어느 폴더든, R14-2)."""
    name = path.split("/")[-1]
    return (name, {"path": path}) if name.lower() in AI_FILES else None


def test_data(path: str):
    """시험 자료 폴더 아래 문서(R14-3). test, tests, src/test/resources 는 빼지 않는다."""
    for seg in path.split("/")[:-1]:
        if seg.lower() in TEST_DIRS:
            return seg, {"path": path, "part": seg}
    return None


def wiki_frame(path: str):
    """위키 틀 파일(R14-4)."""
    name = path.split("/")[-1]
    return (name, {"path": path}) if WIKI_FRAME.match(name) else None


def changelog(path: str):
    """이름이 CHANGELOG.md 인 문서(대소문자 무관, 어느 폴더든, R14-5)."""
    name = path.split("/")[-1]
    return (name, {"path": path}) if name.lower() == "changelog.md" else None


# 7~10. 내용(R15) --------------------------------------------------------------------------------------------------

# 생성기 문구와 GitHub 기본 문구(R15-1). 문구가 문서(디스커션은 글 본문) 아무 데나 있으면 팀이 덧붙인 글이 있어도 뺀다.
# 목록을 고치면 규칙 버전을 올린다(R19-3). 처음 목록을 고친 예: vue_cli 더함(2026-11-09)
GENERATORS = [
    ("spring_help", re.compile(r"^#\s*Getting Started\s*\n+###\s*Reference Documentation", re.M | re.I)),
    ("create_react_app", re.compile(r"bootstrapped with \[?Create React App|Getting Started with Create React App", re.M | re.I)),
    ("vite", re.compile(r"^#\s*React \+ (TypeScript \+ )?Vite|This template provides a minimal setup to get React working in Vite", re.M | re.I)),
    ("vue_cli", re.compile(r"Compiles and hot-reloads for development", re.M | re.I)),
    ("next", re.compile(r"This is a \[?Next\.js\]? project bootstrapped with", re.M | re.I)),
    ("kotlin_multiplatform", re.compile(r"This is a (basic )?Kotlin Multiplatform", re.M | re.I)),
    ("flutter", re.compile(r"^A new Flutter project", re.M | re.I)),
    ("wiki_home", re.compile(r"Welcome to the [\w.-]+ wiki!", re.M | re.I)),
    ("discussion_welcome", re.compile(r"using Discussions as a place to connect with other members of our community", re.M | re.I)),
]


def template_as_is(first_text: str):
    for name, pat in GENERATORS:
        m = pat.search(first_text or "")
        if m:
            return name, {"generator": name, "matched_text": m.group(0)[:200]}
    return None


def empty_form(texts: list[str]):
    """빈 양식: 제목이 2개 이상이고 구조를 걷어 내면 읽는 글자(글자와 숫자)가 10자 이하(R15-2)."""
    t = "\n\n".join(texts)
    heads = len(re.findall(r"^\s{0,3}#{1,6}\s", t, re.M))
    left = alnum_chars(strip_structure(t))
    if heads >= 2 and left <= 10:
        return None, {"headings": heads, "readable_after_strip": left}
    return None


LINK_LINE = re.compile(r"^\s*(?:[-*+]|\d+[.)])?\s*(?:\[[^\]]*\]\([^)]*\)|<https?://[^>]+>|https?://\S+|\[\[[^\]]+\]\])\s*[,.:]?\s*$")


def link_list(texts: list[str]):
    """링크 목록뿐인 문서: 내용 줄이 3줄 이상이고 그 90% 이상이 링크 하나뿐인 줄(링크 줄 3개 이상)(R15-3)."""
    lines = [ln for ln in strip_structure("\n\n".join(texts)).split("\n") if ln.strip()]
    k = sum(1 for ln in lines if LINK_LINE.match(ln))
    if len(lines) >= 3 and k >= 3 and k / len(lines) >= 0.9:
        return None, {"content_lines": len(lines), "link_lines": k}
    return None


DAILY_NAME = re.compile(r"데일리|스크럼|scrum|스탠드업|stand-?up|daily", re.I)
DAILY_HEAD = re.compile(r"데일리|daily|스크럼|scrum|스탠드업|stand-?up|오늘\s*의?\s*(할|한)\s*일|내일\s*의?\s*할\s*일|어제\s*의?\s*(한\s*일|회고)"
                        r"|진행\s*(상황|이슈)\s*공유|오늘\s*진행할|today|yesterday|blocker", re.I)


def daily_note(label: str, texts: list[str]):
    """데일리 기록: 이름(코드 마크다운은 경로, 위키는 쪽 이름, 디스커션은 제목과 카테고리)에 데일리 낱말이 있고
    본문의 제목 줄에도 데일리 기록 낱말이 있다(R15-10). 이름만 보면 ADR 과 최종 회고가 섞여 제목 줄 조건을 더했다."""
    n = DAILY_NAME.search(label or "")
    if not n:
        return None
    for h in heading_lines("\n".join(texts)):
        if DAILY_HEAD.search(h):
            return None, {"name_word": n.group(0), "heading": h.strip()[:200]}
    return None


# 11~15. PR(R16) ----------------------------------------------------------------------------------------------------

INT = r"(main|master|prod|production|release|stage|staging|dev|develop|development)"
PART = r"(be|fe|an|ios|android|backend|frontend|client|server|web|app|admin|aos)"
# 작업 브랜치가 통합 브랜치(파트 이름이 붙은 것, release/ release- 로 시작, 앞에 버전, 파트 이름만인 것 포함)(R16-2)
INTEGRATION_HEAD = re.compile(rf"^(v\d+[/-])?({INT}|{PART}[-/_]{INT}|{INT}[-/_]{PART}|release[/-].+)$"
                              r"|^(be|fe|an|ios|android|backend|frontend|client|server|aos)$", re.I)
REVERT_TITLE = re.compile(r'^\s*Revert\s+"')
GITHUB_DEFAULT_TITLE = re.compile(r"^(Update|Create|Delete|Rename)\s+\S+\.\w+$|^Add files via upload$")
DEPS_FILE = re.compile(r"(^|/)(package\.json|package-lock\.json|yarn\.lock|pnpm-lock\.yaml|build\.gradle(\.kts)?|libs\.versions\.toml"
                       r"|gradle\.properties|gradle-wrapper\.properties|pom\.xml|Podfile(\.lock)?|Package\.resolved|pubspec\.(yaml|lock)"
                       r"|requirements(-\w+)?\.txt|poetry\.lock|pyproject\.toml|go\.(mod|sum)|Gemfile(\.lock)?|Cargo\.(toml|lock))$", re.I)


def github_default_title(original_title: str, human_chars: int, author_is_bot: bool):
    """제목이 GitHub 웹 편집기 기본값이고 사람 글이 없는 PR(R16-9, template_as_is 의 github_default_title)."""
    if not author_is_bot and human_chars == 0 and GITHUB_DEFAULT_TITLE.match((original_title or "").strip()):
        return "github_default_title", {"original_title": original_title}
    return None


def bot_pr(author: str | None, author_is_bot: bool, human_parts: int):
    """봇이 연 PR 인데 사람이 쓴 글(댓글, 리뷰, 코드 리뷰 대화)이 하나도 없다(R16-1)."""
    if author_is_bot and human_parts == 0:
        return None, {"author": author, "author_type": "Bot", "human_parts": 0}
    return None


def release_pr(base_ref: str | None, head_ref: str | None):
    if INTEGRATION_HEAD.match(head_ref or ""):
        return None, {"base_ref": base_ref, "head_ref": head_ref}
    return None


def revert_pr(original_title: str):
    return (None, {"original_title": original_title}) if REVERT_TITLE.match(original_title or "") else None


def empty_pr(changed_files: int, added: int, deleted: int):
    """바뀐 코드 줄이 0(R16-7). no_files: 바뀐 파일이 없음, no_lines: 이름 바꾸기와 이미지처럼 줄 없는 파일만 바뀜."""
    if added + deleted == 0:
        return ("no_files" if changed_files == 0 else "no_lines"), {"changed_files": changed_files, "added_lines": added,
                                                                      "deleted_lines": deleted}
    return None


def deps_pr(paths: list[str], changed_files: int):
    """바뀐 파일이 모두 의존성 파일(R16-8). 원본의 파일 목록이 다 있을 때만 본다."""
    if paths and changed_files <= len(paths) and all(DEPS_FILE.search(p) for p in paths):
        return None, {"files": paths[:50]}
    return None


# 17, 18. 제목, 길이 ------------------------------------------------------------------------------------------------

def no_title(reason: str | None, original_title: str, left: str):
    return (reason, {"original_title": original_title, "left": left}) if reason else None


def short_body(texts: list[str]):
    """정제 후 본문의 읽는 글자가 200자 미만(R13). 해시는 세지 않는다(R13-5). 근거를 이력에도 남긴다(R13-6)."""
    n, hex_chars = readable_chars("\n".join(texts))
    if n < SHORT_BODY_CHARS:
        return None, {"readable_chars": n, "threshold": SHORT_BODY_CHARS, "hash_chars_not_counted": hex_chars}
    return None
