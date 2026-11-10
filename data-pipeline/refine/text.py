"""제목과 본문의 공통 정리(09 문서 2-3, 3-2), 읽는 글자 세기(3-4, R13-5), 작성 시각 맞추기(2-3-2).

공통 정리(3-2)는 원천별 정리(블로그 HTML → 마크다운, 테코톡 자막 등) 다음에 모든 원천의 조각마다 똑같이 한다.
- 줄바꿈을 \\n 하나로, 유니코드 NFC, 보이지 않는 글자 지우기, 특수 공백은 보통 공백으로
- HTML 주석 지우기, HTML 태그는 태그만 지우고 글은 남김(<br> 은 줄바꿈), 이미지는 대체 글자만([이미지: …])
- 링크는 그대로, 코드 블록(``` 와 ~~~ 울타리) 안은 줄바꿈과 글자 모양(NFC) 말고 손대지 않는다. 한 줄 코드(`…`)도 태그를 지우지 않는다
- 줄 끝 공백 지우기, 빈 줄이 여러 개면 하나로, 글 앞뒤 빈 줄 지우기
- 마크다운 파일(코드 마크다운, 위키, 블로그 markdown)은 맨 앞 앞머리(--- 로 감싼 부분)를 뺀다
"""

from __future__ import annotations

import html
import re
import unicodedata
from datetime import datetime, timedelta, timezone
from email.utils import parsedate_to_datetime

KST = timezone(timedelta(hours=9))

# 폭 없는 공백, 폭 없는 비결합자, 단어 결합자, BOM, 소프트 하이픈. 폭 없는 이음(U+200D)과 이모지 꼴 선택(U+FE0F)은
# 이모지 글자의 일부라 지우지 않는다(지우면 🙋‍♀️ 같은 이모지가 깨진다)
INVISIBLE = {ord(c): None for c in "​‌⁠﻿­"}
SPECIAL_SPACE = re.compile(r"[   -   　]")


def normalize_chars(s: str) -> str:
    """NFC, 보이지 않는 글자 지우기, 특수 공백(&nbsp; 에서 온 공백, 전각 공백)을 보통 공백으로."""
    return SPECIAL_SPACE.sub(" ", unicodedata.normalize("NFC", s).translate(INVISIBLE))


def clean_title(value: str | None, *, entities: bool = False, limit: int = 500) -> str:
    """제목 공통 손질(2-3): 줄바꿈, 탭, 이어진 공백을 공백 하나로, HTML 에서 뽑은 제목은 엔티티를 글자로, NFC,
    보이지 않는 글자 지우기, 앞뒤 공백 지우기, 500자(운영 DB title 길이)."""
    if not value:
        return ""
    s = html.unescape(value) if entities else value
    s = re.sub(r"\s+", " ", normalize_chars(s)).strip()
    return s[:limit]


# 코드 블록 ---------------------------------------------------------------------------------------------------

FENCE_OPEN = re.compile(r"^ {0,3}(`{3,}|~{3,})")


def _fence_close(fence: str) -> re.Pattern:
    return re.compile(r"^ {0,3}" + re.escape(fence[0]) + "{%d,}[ \t]*$" % len(fence))


def split_fences(text: str) -> list[tuple[bool, str]]:
    """글을 [(코드 블록인가, 글)] 로 나눈다. 울타리 줄도 코드 쪽에 넣는다. 닫히지 않은 울타리는 끝까지 코드다(CommonMark).
    나눈 조각을 "\\n" 으로 다시 이으면 처음 글이 된다."""
    out: list[tuple[bool, str]] = []
    buf: list[str] = []
    close = None
    for line in text.split("\n"):
        if close is None:
            m = FENCE_OPEN.match(line)
            # 백틱 울타리의 정보 문자열에는 백틱이 들어갈 수 없다(```js` 같은 줄은 울타리가 아니라 한 줄 코드)
            if m and not (m.group(1)[0] == "`" and "`" in line[m.end():]):
                if buf:
                    out.append((False, "\n".join(buf)))
                buf, close = [line], _fence_close(m.group(1))
                continue
            buf.append(line)
        else:
            buf.append(line)
            if close.match(line):
                out.append((True, "\n".join(buf)))
                buf, close = [], None
    if buf or not out:
        out.append((close is not None, "\n".join(buf)))
    return out


INLINE_CODE = re.compile(r"(?<!`)(`+)(?!`)(.+?)(?<!`)\1(?!`)")


def _split_inline_code(text: str) -> list[tuple[bool, str]]:
    out, pos = [], 0
    for m in INLINE_CODE.finditer(text):
        if m.start() > pos:
            out.append((False, text[pos:m.start()]))
        out.append((True, m.group(0)))
        pos = m.end()
    if pos < len(text) or not out:
        out.append((False, text[pos:]))
    return out


# 공통 정리 -----------------------------------------------------------------------------------------------------

COMMENT = re.compile(r"<!--.*?-->", re.S)
IMG_TAG = re.compile(r"<img\b[^>]*>", re.I)
ALT_ATTR = re.compile(r"""\balt\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s>"']+))""", re.I)
# ![대체 글자](주소 "제목") 꼴. 주소 안의 괄호 한 겹까지 받는다
MD_IMAGE = re.compile(r"!\[([^\]\n]*)\]\((?:[^()\n]|\([^()\n]*\))*\)")
BR = re.compile(r"<br\s*/?>", re.I)
# 태그 이름은 영문자로 시작한다. <https://…> 같은 자동 링크와 "< 비교 >" 는 걸리지 않는다
TAG = re.compile(r"</?[A-Za-z][A-Za-z0-9-]*(?:\s[^<>]*)?/?>")
FRONT_MATTER = re.compile(r"\A---[ \t]*\n.*?\n---[ \t]*(?:\n|\Z)", re.S)
WIKI_LINK = re.compile(r"\[\[([^\]|\n]+?)(?:\|[^\]\n]+)?\]\]")


def image_text(alt: str | None) -> str:
    """이미지는 대체 글자만 남긴다. 대체 글자가 없으면 지운다(3-2)."""
    alt = re.sub(r"\s+", " ", html.unescape(alt or "")).strip()
    return f"[이미지: {alt}]" if alt else ""


def _img_tag(m: re.Match) -> str:
    a = ALT_ATTR.search(m.group(0))
    return image_text(next((g for g in a.groups() if g is not None), "") if a else "")


def _clean_prose(text: str, wiki_links: bool) -> str:
    """코드 블록 밖 글 한 조각. 한 줄 코드(`…`) 안은 태그와 이미지를 건드리지 않는다."""
    text = COMMENT.sub("", text)
    out = []
    for code, piece in _split_inline_code(text):
        if code:
            out.append(unicodedata.normalize("NFC", piece).translate(INVISIBLE))
            continue
        piece = IMG_TAG.sub(_img_tag, piece)
        piece = MD_IMAGE.sub(lambda m: image_text(m.group(1)), piece)
        piece = BR.sub("\n", piece)
        piece = TAG.sub("", piece)
        if wiki_links:  # R9-2: 위키 원천만 [[페이지]] → 페이지, [[글자|페이지]] → 글자
            piece = WIKI_LINK.sub(lambda m: m.group(1).strip(), piece)
        out.append(normalize_chars(piece))
    return "".join(out)


def _tidy_lines(text: str) -> str:
    """코드 블록 밖에서 줄 끝 공백 지우기, 빈 줄 여러 개를 하나로. 글 앞뒤 빈 줄 지우기."""
    out: list[str] = []
    close = None
    blank = False
    for line in text.split("\n"):
        if close is not None:
            out.append(line)
            if close.match(line):
                close = None
            continue
        m = FENCE_OPEN.match(line)
        if m and not (m.group(1)[0] == "`" and "`" in line[m.end():]):
            close = _fence_close(m.group(1))
            out.append(line.rstrip())
            blank = False
            continue
        line = line.rstrip()
        if not line.strip():
            if not blank:
                out.append("")
            blank = True
            continue
        blank = False
        out.append(line)
    while out and not out[0].strip():
        out.pop(0)
    while out and not out[-1].strip():
        out.pop()
    return "\n".join(out)


def clean_markdown(text: str | None, *, front_matter: bool = False, wiki_links: bool = False) -> str:
    """조각 하나의 공통 정리(3-2). front_matter 는 마크다운 파일만 켠다(PR 본문의 --- 가로줄을 앞머리로 잘못 지우지 않게)."""
    if not text:
        return ""
    text = text.replace("\r\n", "\n").replace("\r", "\n").lstrip("﻿")
    if front_matter:
        text = FRONT_MATTER.sub("", text, count=1)
    segs = []
    for code, seg in split_fences(text):
        segs.append(unicodedata.normalize("NFC", seg).translate(INVISIBLE) if code else _clean_prose(seg, wiki_links))
    return _tidy_lines("\n".join(segs))


def clean_plain(text: str | None) -> str:
    """마크다운이 아닌 글(테코톡 자막과 영상 설명)의 공통 정리. 줄바꿈, 글자 모양(NFC, 보이지 않는 글자, 특수 공백),
    줄 끝 공백, 빈 줄만 맞춘다. HTML 태그 지우기는 마크다운 안의 태그에 대한 규칙이라 하지 않는다
    (자막에서 엔티티를 푼 "List<String>" 같은 말이 태그로 잘못 지워지지 않게)."""
    if not text:
        return ""
    t = normalize_chars(text.replace("\r\n", "\n").replace("\r", "\n"))
    out: list[str] = []
    for line in t.split("\n"):
        line = line.rstrip()
        if not line.strip():
            if out and out[-1] == "":
                continue
            line = ""
        out.append(line)
    while out and not out[0]:
        out.pop(0)
    while out and not out[-1]:
        out.pop()
    return "\n".join(out)


def front_matter_title(text: str | None) -> str | None:
    """마크다운 앞머리의 title 값. 앞머리가 없거나 title 이 없으면 None."""
    if not text:
        return None
    m = FRONT_MATTER.match(text.replace("\r\n", "\n").replace("\r", "\n").lstrip("﻿"))
    if not m:
        return None
    for line in m.group(0).split("\n")[1:]:
        t = re.match(r"^title\s*:\s*(.*?)\s*$", line)
        if t:
            v = t.group(1)
            if len(v) >= 2 and v[0] == v[-1] and v[0] in "\"'":
                v = v[1:-1]
            return v or None
    return None


def front_matter_value(text: str | None, name: str) -> str | None:
    """앞머리의 이름 하나의 값(따옴표를 벗긴 글자)."""
    if not text:
        return None
    m = FRONT_MATTER.match(text.replace("\r\n", "\n").replace("\r", "\n").lstrip("﻿"))
    if not m:
        return None
    for line in m.group(0).split("\n")[1:]:
        t = re.match(r"^" + re.escape(name) + r"\s*:\s*(.*?)\s*$", line)
        if t:
            v = t.group(1)
            if len(v) >= 2 and v[0] == v[-1] and v[0] in "\"'":
                v = v[1:-1]
            return v or None
    return None


# 읽는 글자(3-4, R13-5) -----------------------------------------------------------------------------------------

HEX_RUN = re.compile(r"(?<![0-9A-Za-z])[0-9a-fA-F]{32,}(?![0-9A-Za-z])")


def readable_chars(text: str) -> tuple[int, int]:
    """(읽는 글자 수, 세지 않은 해시 글자 수).

    공백, 줄바꿈, 마크다운 기호(# * > | ` _ ~ = [ ] ( ) \\ -), 링크와 이미지의 주소, 이미지 표시([이미지: …])와
    해시(0~9, a~f 로만 된 32자 이상 덩어리, 커밋 번호나 검증 값)를 빼고 센다. 코드 블록 안 글자는 센다."""
    hex_chars = sum(len(m.group(0)) for m in HEX_RUN.finditer(text))
    t = HEX_RUN.sub("", text)
    t = re.sub(r"!\[[^\]]*\]\([^)]*\)", "", t)
    t = re.sub(r"\[이미지:[^\]]*\]", "", t)
    t = re.sub(r"\[([^\]]*)\]\([^)]*\)", r"\1", t)
    t = re.sub(r"https?://\S+", "", t)
    t = re.sub(r"[#*>|`_~=\[\]()\\-]", "", t)
    return len(re.sub(r"\s", "", t)), hex_chars


def alnum_chars(text: str | None) -> int:
    """글자와 숫자만 센다(빈 양식, 링크 목록 규칙의 읽는 글자, R15-2)."""
    return len(re.sub(r"[\W_]", "", text or ""))


def strip_structure(text: str) -> str:
    """구조 줄(제목, 빈 목록과 체크박스, 표 틀, 가로줄, 빈 인용, 빈 코드 울타리)과 주석, 앞머리를 걷어 낸 내용 줄(R15-2, R15-3)."""
    t = COMMENT.sub("", text)
    t = re.sub(r"\A---\n.*?\n---\n", "", t, flags=re.S)
    out = []
    for ln in t.split("\n"):
        x = ln.strip()
        if (not x or re.match(r"^#{1,6}\s", x) or re.match(r"^([-*+]|\d+[.)])\s*(\[[ xX]?\])?\s*:?\s*$", x)
                or re.match(r"^\|?[\s:|-]*\|[\s:|-]*$", x) or re.match(r"^([-*_]\s*){3,}$", x)
                or re.match(r"^(>\s*)+$", x) or re.match(r"^(```|~~~)\w*$", x)):
            continue
        out.append(x)
    return "\n".join(out)


def heading_lines(text: str) -> list[str]:
    """마크다운 제목 줄의 글자들(# 를 뗀 것)."""
    return re.findall(r"^\s{0,3}#{1,6}\s*(.+)$", text, re.M)


# 작성 시각(2-3-2) -----------------------------------------------------------------------------------------------

def to_utc(value: str | None, naive_tz: str = "UTC") -> tuple[str | None, str | None]:
    """원천의 시각 글자를 UTC `YYYY-MM-DDTHH:MM:SSZ`(초 아래 버림)로 바꾼다. (값, 가정한 시간대) 를 돌려준다.

    시간대 표시가 없으면 naive_tz(UTC 또는 Asia/Seoul)로 보고, 날짜만 있으면 그날 한국 시각 0시로 본다. 읽지 못하면 (None, None)."""
    if not value or not str(value).strip():
        return None, None
    v = str(value).strip()
    assumed = None
    if re.fullmatch(r"\d{4}-\d{2}-\d{2}", v):
        dt = datetime.fromisoformat(v).replace(tzinfo=KST)
        assumed = "Asia/Seoul"
    else:
        dt = _parse_iso(v)
        if dt is None:
            try:
                dt = parsedate_to_datetime(v)  # RFC 2822(피드 등)
            except (TypeError, ValueError, IndexError):
                return None, None
        if dt.tzinfo is None:
            dt = dt.replace(tzinfo=KST if naive_tz == "Asia/Seoul" else timezone.utc)
            assumed = naive_tz
    return dt.astimezone(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"), assumed


def _parse_iso(v: str) -> datetime | None:
    s = v.replace("Z", "+00:00").replace("z", "+00:00")
    # 초 아래가 6자리를 넘으면 fromisoformat 이 못 읽는다(예: .9199451)
    s = re.sub(r"(\.\d{6})\d+", r"\1", s)
    try:
        return datetime.fromisoformat(s)
    except ValueError:
        return None
