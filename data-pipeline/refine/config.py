"""정제 설정값(09 문서 R19, R20).

비밀값은 쓰지 않는다. 저장소 위치는 수집기와 같은 PIPELINE_STORE 를 읽는다(R20-2).
"""

from __future__ import annotations

# 정제 순서(R20-6): 작은 원천부터. 첫 정제를 1만 건씩 나눠 돌릴 때 빨리 끝나는 원천의 결과를 먼저 본다
SOURCES = [
    "youtube-tekotalk",
    "blog-post",
    "github-discussion",
    "github-wiki",
    "github-teamdoc",
    "github-pr",
]
# 같은 레포 사본을 함께 견주는 팀 문서 원천과 남길 때의 원천 순서(R17-1: 코드 마크다운, 위키, 디스커션)
TEAMDOC_SOURCES = ("github-teamdoc", "github-wiki", "github-discussion")

# 정제 규칙 버전(R19-3). 원천마다 따로 둔다. 그 원천이 쓰는 규칙(제외 규칙, 본문 정리, 목록, 라이브러리 판)을 고치면
# 그 원천만 올린다. 정제 색인의 rules_version 이 이보다 낮은 문서가 다시 정제 대상이 된다(올리기 전에 preview, R19-4)
RULES_VERSION = {
    "youtube-tekotalk": 1,
    "blog-post": 1,
    "github-discussion": 1,
    "github-wiki": 1,
    "github-teamdoc": 1,
    "github-pr": 1,
}

HASH_VERSION = "v1"        # 내용 해시 규칙 버전(4-5). 해시에 넣는 것을 바꾸면 v2 로 올리고 모두 다시 판정된다
MAX_PER_RUN = 10_000       # 한 번 실행에 정제하는 최대 문서 수(R20-6). EC2 가 긴 작업 중에 굳은 기록이 있다(T1)
INDEX_CHECKPOINT = 200     # 정제 색인 중간 저장 간격(R20-5)
SAVE_EVERY_SEC = 15        # 보고서 중간 저장 간격(R20-8)
SHORT_BODY_CHARS = 200     # 짧은 문서 기준(R13): 읽는 글자가 이보다 적으면 short_body
KEY_LIST_CAP = 500         # 보고서에 남기는 키 목록 하나의 상한. 넘는 것은 수만 센다(첫 정제는 수만 건이라)
CHANGE_TEXT_CAP = 20_000   # 변경 기록에 남기는 글 한 조각의 상한(글자). 넘으면 자르고 표시한다
AFTER_COLLECT_HOURS = 12   # --after-collect: 이보다 오래 전에 시작한 수집 실행과는 같은 실행 묶음으로 보지 않는다

REFINE_DIR = "refine"
