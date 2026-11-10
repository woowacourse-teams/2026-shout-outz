"""민감 정보 가리기(09 문서 19장 R21, 확정 2026-11-10).

문서는 빼지 않고 값만 가린다. 모든 원천의 제목과 조각 글에, 공통 정리 다음이고 내용 해시를 만들기 전에 가린다(R21-3).
정제 문서에는 종류별 개수만 남기고 값은 남기지 않는다(R21-4). 원본은 그대로 둔다(R21-5).
모양 목록을 고치면 규칙 버전을 올린다(R19-3).
"""

from __future__ import annotations

import re
from collections import Counter

EMAIL_LABEL, PHONE_LABEL, RRN_LABEL, SECRET_LABEL, IP_LABEL = "[이메일]", "[전화번호]", "[주민번호]", "[비밀 값]", "[IP]"

# 비밀 값 ---------------------------------------------------------------------------------------------------------
# 개인 키는 블록 전체를 가린다(머리 줄만 가리면 키 본문이 남는다)
PRIVATE_KEY = re.compile(r"-----BEGIN [A-Z ]*PRIVATE KEY-----.*?(?:-----END [A-Z ]*PRIVATE KEY-----|\Z)", re.S)
TOKENS = [
    re.compile(r"\b(?:ghp|gho|ghu|ghs|ghr)_[A-Za-z0-9]{36}(?![A-Za-z0-9])"),       # GitHub 토큰
    re.compile(r"\bgithub_pat_[A-Za-z0-9_]{50,}"),                                 # GitHub 세분화 토큰
    re.compile(r"\b(?:AKIA|ASIA)[0-9A-Z]{16}(?![0-9A-Z])"),                         # AWS 접근 키
    re.compile(r"\bAIza[0-9A-Za-z_-]{35}(?![0-9A-Za-z_-])"),                        # Google API 키
    re.compile(r"\bxox[abprs]-[A-Za-z0-9-]{10,}"),                                  # Slack 토큰
    re.compile(r"https://hooks\.slack\.com/services/[A-Za-z0-9/]{20,}"),            # Slack 웹훅
    re.compile(r"https://(?:discord|discordapp)\.com/api/webhooks/\d+/[A-Za-z0-9_-]{20,}"),  # Discord 웹훅
    re.compile(r"\bsk-proj-[A-Za-z0-9_-]{32,}|\bsk-[A-Za-z0-9]{32,}(?![A-Za-z0-9])"),  # OpenAI 키
    re.compile(r"\beyJ[A-Za-z0-9_-]{10,}\.eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}"),  # JWT
]
# 아이디와 비밀번호가 든 주소: scheme://아이디:비밀번호@호스트 → scheme://[비밀 값]@호스트
CREDENTIAL_URL = re.compile(r"\b([a-z][a-z0-9+.-]{0,30}://)([^/\s:@<>\"']+:[^/\s@<>\"']+)@(?=[^\s/<>\"']+)", re.I)
# 비밀번호 자리가 자리 표시(password, pw, ${…}, <…>, ****)면 두다
CREDENTIAL_PLACEHOLDER = re.compile(r"^(?:\$\{.*|<.*|\{\{.*|password|passwd|pass|pw|pwd|secret|\*+|x+)$", re.I)
# 비밀번호나 비밀 키를 적은 줄의 값. 자리 표시(${…}, <…>, your_password 등)와 코드 모양은 두고, 숫자가 섞인 값만 가린다
SECRET_ASSIGN = re.compile(r"(?i)\b(password|passwd|pwd|secret[_-]?key|client[_-]?secret|api[_-]?key|access[_-]?key|secret)"
                           r"(\s*[:=]\s*['\"]?)([^\s'\"`,;)]{6,})")
PLACEHOLDER = re.compile(r"^\$\{|^\{\{|^<|^\[|^%|^\*+$|^x+$|your|example|sample|password|secret|change|dummy|test|xxx|\.\.\.|^\$|^\(|env|ENV"
                         r"|getenv|process\.|\)$", re.I)

# 개인 연락처 ------------------------------------------------------------------------------------------------------
# 이메일: 도메인 끝이 흔한 것만(systemd 이름 …@prod.service, 이미지 이름 …@2x.webp 를 잘못 잡지 않게)
TLD = r"(?:com|net|org|io|dev|app|kr|co\.kr|ac\.kr|or\.kr|go\.kr|me|art|ai|xyz|site|blog|page|tech|cloud|info|biz|us|jp|cn|uk|de|edu|gov)"
EMAIL = re.compile(r"(?<![\w.+%-])[A-Za-z0-9._+-]+@(?:[A-Za-z0-9-]+\.)+" + TLD + r"(?![\w-])", re.I)
RRN = re.compile(r"(?<!\d)\d{2}(?:0[1-9]|1[0-2])(?:0[1-9]|[12]\d|3[01])-[1-4]\d{6}(?!\d)")
PHONES = [
    re.compile(r"(?<![\d-])01[016789][-. ]?\d{3,4}[-. ]?\d{4}(?![\d-])"),           # 휴대폰(예시 번호도 가림, R21-1)
    re.compile(r"(?<![\d-])0(?:2|[3-6][1-5])[-.) ]\d{3,4}[-. ]\d{4}(?![\d-])"),      # 지역번호
]
# 공인 IPv4 만 가린다. 사설, 루프백, 0.0.0.0, 169.254., 255. 은 둔다. 포트는 남는다(R21-2)
IPV4 = re.compile(r"(?<![\d.])((?:25[0-5]|2[0-4]\d|1?\d?\d)(?:\.(?:25[0-5]|2[0-4]\d|1?\d?\d)){3})(?![\d.])")
PRIVATE_IP = re.compile(r"^(10\.|127\.|0\.|192\.168\.|172\.(1[6-9]|2\d|3[01])\.|169\.254\.|255\.)")


def mask(text: str, counts: Counter | None = None) -> tuple[str, Counter]:
    """가린 글과 종류별 개수(이메일, 전화번호, 주민번호, 비밀 값, IP)."""
    counts = Counter() if counts is None else counts
    if not text:
        return text, counts

    def sub(pat: re.Pattern, label: str, kind: str, s: str) -> str:
        def rep(m: re.Match) -> str:
            counts[kind] += 1
            return label
        return pat.sub(rep, s)

    t = sub(PRIVATE_KEY, SECRET_LABEL, "비밀 값", text)
    for pat in TOKENS:
        t = sub(pat, SECRET_LABEL, "비밀 값", t)

    def cred(m: re.Match) -> str:
        if CREDENTIAL_PLACEHOLDER.match(m.group(2).split(":", 1)[1]):
            return m.group(0)
        counts["비밀 값"] += 1
        return f"{m.group(1)}{SECRET_LABEL}@"
    t = CREDENTIAL_URL.sub(cred, t)

    def assign(m: re.Match) -> str:
        val = m.group(3)
        if PLACEHOLDER.search(val) or not re.search(r"\d", val) or re.search(r"[(){}]", val):
            return m.group(0)
        counts["비밀 값"] += 1
        return f"{m.group(1)}{m.group(2)}{SECRET_LABEL}"
    t = SECRET_ASSIGN.sub(assign, t)

    t = sub(EMAIL, EMAIL_LABEL, "이메일", t)
    t = sub(RRN, RRN_LABEL, "주민번호", t)
    for pat in PHONES:
        t = sub(pat, PHONE_LABEL, "전화번호", t)

    def ip(m: re.Match) -> str:
        if PRIVATE_IP.match(m.group(1)):
            return m.group(0)
        counts["IP"] += 1
        return IP_LABEL
    t = IPV4.sub(ip, t)
    return t, counts
