"""데이터 파이프라인 공통: 경로, 시각, 해시, 원자적 파일 쓰기, GitHub 토큰과 클라이언트."""
from __future__ import annotations

import hashlib
import json
import os
import tempfile
import time
from datetime import UTC, datetime
from pathlib import Path

import httpx

ROOT = Path(__file__).resolve().parent.parent  # data-pipeline/
# 원본 저장소 경로와 토큰은 환경 변수가 먼저다(EC2 에서 경로가 다르고 토큰을 파일로 두지 않음, 매일 파이프라인 1-5).
DATA = Path(os.environ.get("RAW_DATA_REPO") or Path.home() / "Desktop" / "woowacourse-raw-data")
STATE = ROOT / "state"  # 수집 캐시, 잠금, 레포 목록 사본(레포에 넣지 않음)
CONFIG = ROOT / "config"
TOKEN_FILE = Path.home() / ".config" / "woowacourse-collect-token"
OWNER = "woowacourse-teams"
API = "https://api.github.com"
RAW = "https://raw.githubusercontent.com"


def now() -> str:
    return datetime.now(UTC).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def stamp(iso: str) -> str:
    return iso.replace("-", "").replace(":", "")


def sha256(obj) -> str:
    return "sha256:" + hashlib.sha256(json.dumps(obj, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode()).hexdigest()


def atomic_write(path: Path, data: bytes) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    fd, tmp = tempfile.mkstemp(dir=path.parent, prefix=".tmp-")
    with os.fdopen(fd, "wb") as f:
        f.write(data)
    os.replace(tmp, path)


class GH:
    def __init__(self, token: str):
        self.c = httpx.Client(timeout=60, follow_redirects=True,
                              headers={"Authorization": f"Bearer {token}", "Accept": "application/vnd.github+json",
                                       "User-Agent": "shout-outz-raw-collector/0.1"})
        self.api_calls = 0
        self.raw_calls = 0

    def get(self, url: str, raw: bool = False) -> httpx.Response:
        for attempt in range(6):
            try:
                r = self.c.get(url)
            except (httpx.TransportError,) as e:
                if attempt == 5:
                    raise
                time.sleep(min(2 ** attempt, 30))
                continue
            if raw:
                self.raw_calls += 1
            else:
                self.api_calls += 1
            if r.status_code in (403, 429) and not raw:
                wait = int(r.headers.get("retry-after") or 0) or max(60, int(r.headers.get("x-ratelimit-reset", time.time() + 60)) - int(time.time()) + 5)
                time.sleep(min(wait, 3700))
                continue
            if r.status_code >= 500 and attempt < 5:
                time.sleep(min(2 ** attempt, 30))
                continue
            return r
        return r


def token() -> str:
    return os.environ.get("GITHUB_TOKEN") or TOKEN_FILE.read_text().strip()
