"""테코톡 youtube-tekotalk(09 문서 21장 R7, 22장 R8).

- 자막 VTT 를 글로(R7-1): 빈 줄(글자가 하나도 없는 줄)에서만 자막을 나눈다. 머리, 자막 번호, NOTE, STYLE, REGION, 시간 줄과
  위치 설정, 줄 안 시각 태그, 글자 꾸밈 태그를 지우고 HTML 엔티티를 글자로 바꾼다. 시간 표시는 남기지 않는다.
  자동 자막은 글 앞에 공백 한 칸인 줄이 있어, 공백 줄에서도 나누면 시간 줄과 글이 갈려 글이 빠진다(측정 중 1,477곳).
- 바로 앞 줄과 같은 줄은 하나만(R7-2, 자동 자막의 굴러가는 표시), 소리 표시만 있는 줄([음악], [박수], [웃음])은 지운다(R7-3).
- 자막 줄을 문장으로 잇는다(R7-4): 줄 끝이 문장 끝이면 줄을 바꾸고 아니면 공백으로 잇는다. 문단은 나누지 않는다.
- 영상 설명은 채널 공통 문구를 줄 단위로 지우고 남는 글만 description 조각으로(R7-5). 남는 글이 없으면 조각을 두지 않는다.
- 제목은 그대로, 발표자와 주제를 따로 뽑지 않는다(R7-6). 작성자는 받지 않는다(2-3-3). 거르지 않는다(R13-1).
- 자막을 아직 받지 않은 영상은 정제하지 않고 기다린다(R8-1).
"""

from __future__ import annotations

import html
import re
import unicodedata

from ..model import Draft, RefineFailure, Waiting
from ..text import clean_plain, clean_title, normalize_chars, to_utc
from . import Refiner

TIME = re.compile(r"^(\d{2}:)?\d{2}:\d{2}\.\d{3}\s+-->\s+")
INLINE_TS = re.compile(r"<\d{2}:\d{2}(:\d{2})?\.\d{3}>")
TAG = re.compile(r"</?[a-zA-Z]+(\.[^>\s]*)?(\s[^>]*)?>")
SOUND = {"[음악]", "[박수]", "[웃음]"}  # YouTube 자동 자막의 소리 표시
# 문장 끝: 마침표, 물음표, 느낌표, 말줄임표, 물결, 또는 '다', '요', '죠', '까', '네', '군'. 뒤에 닫는 따옴표나 괄호가 붙어도 된다
SENTENCE_END = re.compile(r"([.?!…~]|[다요죠까네군])[\"'”’)\]]*$")

# 채널 공통 문구(R7-5). 줄의 글자와 숫자만 남겨 견준다(이모지, 기호, 띄어쓰기가 다른 모양이 섞여 있다).
# 목록을 고치면 테코톡 규칙 버전을 올린다(R19-3). 측정(2026-11-10): 설명 글자의 91% 가 이 문구였다
BOILERPLATE = [
    "우아한테크코스의 크루들이 진행하는 10분 테크토크입니다",
    "우아한테크코스의 크루들이 진행하는 10분 테코톡입니다",
    "10분 테코톡이란",
    "10분 테크토크란",
    "우아한테크코스란",
    "우아한테크코스 과정을 진행하며 크루(수강생)들이 동료들과 학습한 내용을 공유하고 이야기하는 시간입니다. 서로가 성장하기 위해 "
    "지식을 나누고 대화하며 생각해보는 시간으로 자기 주도적인 성장을 지향하는 우아한테크코스의 문화 중 하나입니다.",
    "'10분 테코톡'이란 우아한테크코스 과정을 진행하며 크루(수강생)들이 동료들과 학습한 내용을 공유하고 이야기하는 시간입니다. "
    "서로가 성장하기 위해 지식을 나누고 대화하며 생각해보는 시간으로 자기 주도적인 성장을 지향하는 우아한테크코스의 문화 중 하나입니다.",
    "우아한테크코스는 일반 사용자용 서비스를 개발하는 회사가 필요로 하는 역량을 가진 프로그래머를 양성하기 위한 교육입니다. "
    "우리의 목표는 자기 주도적으로 학습하고 성장하고 싶은 개발자를 위한 교육을 만드는 것입니다.",
    "*우아한테크코스 3기는 “온라인”으로 강의 및 발표를 진행하고 있습니다. 온라인 발표 화면인 점 참고 부탁드립니다 : )",
    "*3기부터, 프론트엔드 과정이 추가되었습니다.",
    "*3기부터, 프론트엔드 과정이 추가되었습니다. (짝짝짝!!)",
]


def _letters(s: str) -> str:
    return "".join(ch for ch in unicodedata.normalize("NFC", s) if ch.isalnum())


BOILERPLATE_KEYS = {_letters(b) for b in BOILERPLATE}


def vtt_lines(vtt: str) -> tuple[list[str], dict]:
    """VTT → 자막 줄(R7-1~R7-3). 지운 것의 수를 함께 돌려준다."""
    vtt = vtt.replace("\r\n", "\n").replace("\r", "\n")
    out: list[str] = []
    stat = {"cues": 0, "sound_lines": 0, "repeated_lines": 0}
    for i, block in enumerate(re.split(r"\n\n+", vtt)):  # 글자가 하나도 없는 줄에서만 나눈다
        lines = block.split("\n")
        if i == 0 and lines[0].startswith("WEBVTT"):
            continue
        head = lines[0].strip()
        if head.startswith(("NOTE", "STYLE", "REGION")):
            continue
        if TIME.match(head):
            k = 0
        elif len(lines) > 1 and TIME.match(lines[1].strip()):
            k = 1  # 자막 번호 줄
        else:
            continue
        stat["cues"] += 1
        for ln in lines[k + 1:]:
            ln = INLINE_TS.sub("", ln)
            ln = TAG.sub("", ln)
            ln = html.unescape(ln)
            ln = re.sub(r"[ \t]+", " ", normalize_chars(ln)).strip()
            if not ln:
                continue
            if ln in SOUND:
                stat["sound_lines"] += 1
                continue
            if out and out[-1] == ln:
                stat["repeated_lines"] += 1
                continue
            out.append(ln)
    return out, stat


def join_sentences(lines: list[str]) -> list[str]:
    sents, cur = [], []
    for ln in lines:
        cur.append(ln)
        if SENTENCE_END.search(ln):
            sents.append(" ".join(cur))
            cur = []
    if cur:
        sents.append(" ".join(cur))
    return sents


def strip_boilerplate(description: str) -> tuple[str, int]:
    """채널 공통 문구 줄을 지운 영상 설명과 지운 줄 수."""
    keep, removed = [], 0
    for ln in (description or "").replace("\r\n", "\n").split("\n"):
        if ln.strip() and _letters(ln) in BOILERPLATE_KEYS:
            removed += 1
            continue
        keep.append(ln)
    return "\n".join(keep), removed


def duration_sec(value: str | None) -> int | None:
    m = re.fullmatch(r"P(?:(\d+)D)?T?(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?", value or "")
    if not m or not value:
        return None
    d, h, mi, s = (int(x) if x else 0 for x in m.groups())
    return d * 86400 + h * 3600 + mi * 60 + s


class TekotalkRefiner(Refiner):
    name = "youtube-tekotalk"
    rules = ()  # 테코톡은 거르지 않는다(R13-1, R7-8)

    def waiting(self, entry: dict) -> bool:
        return not entry.get("has_subtitle")

    def build(self, raw: dict, entry: dict) -> Draft:
        video = raw.get("video") or {}
        sn = video.get("snippet") or {}
        vid = video.get("id") or raw["external_key"][len("yt:"):]
        url = f"https://www.youtube.com/watch?v={vid}"
        sub = raw.get("subtitle")
        if not sub:
            raise Waiting()
        if sub.get("format", "vtt") != "vtt" or not isinstance(sub.get("vtt"), str):
            raise RefineFailure("bad_subtitle", str(sub.get("format")))

        title = clean_title(sn.get("title"))
        tried = [{"step": "video.snippet.title", "result": "used" if title else "missing"}]
        if not title:
            title = url
            tried.append({"step": "url", "result": "used"})
        published, _ = to_utc(sn.get("publishedAt"))

        lines, stat = vtt_lines(sub["vtt"])
        sentences = join_sentences(lines)
        transcript = clean_plain("\n".join(sentences))
        if not transcript:
            raise RefineFailure("empty_subtitle")
        desc, removed = strip_boilerplate(sn.get("description") or "")
        desc = clean_plain(desc)
        parts = []
        if desc:
            parts.append({"kind": "description", "author": None, "at": None, "text": desc})
        parts.append({"kind": "transcript", "author": None, "at": None, "text": transcript})
        return Draft(
            title=title, original_title=title, url=url, published_at=published, author=None, repo=None,
            meta={"video_id": vid, "duration_sec": duration_sec((video.get("contentDetails") or {}).get("duration")),
                  "subtitle_kind": sub.get("kind")},
            parts=parts,
            trace={"title": {"tried": tried, "removed": [], "no_title": None},
                   "published_at": {"tried": [{"step": "video.snippet.publishedAt", "result": "used" if published else "missing"}],
                                    "raw": sn.get("publishedAt"), "assumed_tz": None},
                   "body": {"subtitle_kind": sub.get("kind"), "subtitle_lang": sub.get("lang"), "cues": stat["cues"],
                            "lines": len(lines), "sentences": len(sentences), "sound_lines_removed": stat["sound_lines"],
                            "repeated_lines_removed": stat["repeated_lines"], "description_lines_removed": removed}},
        )

    def render(self, parts: list[dict], draft: Draft) -> str:
        out = []
        for p in parts:
            out.append(("## 영상 설명\n" if p["kind"] == "description" else "## 자막\n") + p["text"])
        return "\n\n".join(out)
