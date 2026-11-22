"""코드 마크다운 github-teamdoc 과 위키 github-wiki(09 문서 8장 R9, 2-3, 12장 R14, 13장 R15, 14장 R13).

- 제목(2-3): 코드 마크다운은 앞머리 title → 본문의 첫 1단계 제목(글자나 숫자가 남는 첫 번째, R9-1) → 레포 안 경로.
  위키는 파일 이름에서 .md 를 떼고 - 를 공백으로(폴더는 넣지 않는다). 둘 다 머리말과 꼬리말을 뗀다.
- 주소(2-3-1): 코드 마크다운 https://github.com/{org}/{repo}/blob/HEAD/{경로}, 위키 https://github.com/{org}/{repo}/wiki/{파일 이름}
- 작성 시각과 작성자: 마지막으로 고친 커밋(2-3-2, 2-3-3). 코드 마크다운은 아이디 → 커밋 이름, 위키는 커밋 이름
- 본문: 앞머리를 빼고 공통 정리(3-2). 위키만 [[페이지]] → 페이지, [[글자|페이지]] → 글자(R9-2)
"""

from __future__ import annotations

from urllib.parse import quote

from ..model import Draft, RefineFailure
from ..text import clean_markdown, clean_title, front_matter_title, to_utc
from ..titles import first_h1, strip_title
from . import Refiner

TEAM_RULES = ("old_repo", "tool_dir", "ai_instruction", "test_data", "changelog", "template_as_is", "empty_form",
              "link_list", "daily_note", "duplicate", "no_title", "short_body")


def _common(raw: dict) -> tuple[str, str, str, dict | None]:
    found = raw.get("found_in") or {}
    file = raw.get("file") or {}
    repo, path = found.get("repo"), file.get("path")
    if not repo or not path or not isinstance(raw.get("content"), str):
        raise RefineFailure("bad_raw", "found_in.repo, file.path, content 중 없는 것이 있음")
    return repo, path, raw["content"], file.get("last_commit")


def _time(last: dict | None) -> tuple[str | None, dict]:
    at = (last or {}).get("at")
    v, _ = to_utc(at)
    return v, {"tried": [{"step": "file.last_commit.at", "result": "used" if v else "missing"}], "raw": at, "assumed_tz": None}


def _title(original: str, tried: list[dict], strip: bool = True) -> tuple[str, dict, str | None]:
    """머리말과 꼬리말을 뗀다. 제목이 없어 경로를 대신 쓴 문서는 떼지 않는다(경로는 레포 안에서 유일해서 쓰는 값이다)."""
    if not strip:
        return original, {"tried": tried, "removed": [], "no_title": None}, None
    title, removed, reason = strip_title(original)
    return (title or original), {"tried": tried, "removed": removed, "no_title": reason}, reason


class CodeMarkdownRefiner(Refiner):
    name = "github-teamdoc"
    rules = TEAM_RULES

    def build(self, raw: dict, entry: dict) -> Draft:
        repo, path, content, last = _common(raw)
        tried = []
        original = ""
        from_path = False
        fm = clean_title(front_matter_title(content))
        tried.append({"step": "front_matter.title", "result": "used" if fm else "missing"})
        if fm:
            original = fm
        else:
            h = first_h1(content)
            if h and clean_title(h[0]):
                original = clean_title(h[0])
                tried.append({"step": "h1", "result": "used", "line": h[1]})
            else:
                tried.append({"step": "h1", "result": "missing"})
                original = clean_title(path)
                from_path = True
                tried.append({"step": "path", "result": "used"})
        title, ttrace, reason = _title(original, tried, strip=not from_path)
        published, ptrace = _time(last)
        login, name = (last or {}).get("author_login"), (last or {}).get("author_name")
        author = clean_title(login, limit=255) or clean_title(name, limit=255) or None
        atrace = {"tried": [{"step": "file.last_commit.author_login", "result": "used" if clean_title(login) else "missing"}]}
        if not clean_title(login):
            atrace["tried"].append({"step": "file.last_commit.author_name", "result": "used" if author else "missing"})
        text = clean_markdown(content, front_matter=True)
        return Draft(
            title=title, original_title=original, url=f"https://github.com/{repo}/blob/HEAD/{quote(path)}",
            published_at=published, author=author, repo=repo, meta={"path": path},
            parts=[{"kind": "body", "author": None, "at": None, "text": text}] if text else [],
            trace={"title": ttrace, "published_at": ptrace, "author": atrace},
            no_title=reason, facts={"path": path, "label": path, "first_text": text},
        )


class WikiRefiner(Refiner):
    name = "github-wiki"
    rules = TEAM_RULES[:4] + ("wiki_frame",) + TEAM_RULES[4:]

    def build(self, raw: dict, entry: dict) -> Draft:
        repo, path, content, last = _common(raw)
        page = path.rsplit("/", 1)[-1]
        if page.lower().endswith(".md"):
            page = page[:-3]
        original = clean_title(page.replace("-", " ")) or clean_title(path)
        title, ttrace, reason = _title(original, [{"step": "file_name", "result": "used"}])
        published, ptrace = _time(last)
        author = clean_title((last or {}).get("author_name"), limit=255) or None
        text = clean_markdown(content, front_matter=True, wiki_links=True)
        return Draft(
            title=title, original_title=original, url=f"https://github.com/{repo}/wiki/{quote(page, safe='-')}",
            published_at=published, author=author, repo=repo, meta={"path": path},
            parts=[{"kind": "body", "author": None, "at": None, "text": text}] if text else [],
            trace={"title": ttrace, "published_at": ptrace,
                   "author": {"tried": [{"step": "file.last_commit.author_name", "result": "used" if author else "missing"}]}},
            no_title=reason, facts={"path": path, "label": path, "first_text": text},
        )
