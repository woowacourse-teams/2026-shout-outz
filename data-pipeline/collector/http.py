"""HTTP 요청 도구. 고정 User-Agent, 한 번에 하나씩, 서버가 알려 주는 기다릴 시간(Retry-After) 지키기.

블로그는 같은 호스트에 1초에 1번 이하로 보낸다(03 문서 5-6). host_delay 로 정한다.
"""

from __future__ import annotations

import time
from datetime import datetime, timezone
from email.utils import parsedate_to_datetime
from urllib.parse import urlsplit

import requests

from .config import USER_AGENT


def retry_after_seconds(value: str | None, now: datetime | None = None) -> float | None:
    """Retry-After 값을 기다릴 초로. 초(숫자)와 날짜(HTTP-date) 둘 다 된다(RFC 9110 10.2.3). 읽지 못하면 None."""
    if not value:
        return None
    value = value.strip()
    if value.isdigit():
        return float(value)
    try:
        when = parsedate_to_datetime(value)
    except (TypeError, ValueError):
        return None
    if when.tzinfo is None:
        when = when.replace(tzinfo=timezone.utc)
    return max(0.0, (when - (now or datetime.now(timezone.utc))).total_seconds())


class ResponseTooLarge(requests.RequestException):
    """응답 본문이 max_bytes 를 넘음. 1GB EC2 에서 큰 응답 하나가 수집 전체를 멈추지 않게 한다."""


class Http:
    def __init__(self, host_delay: float = 0.0, timeout: float = 30.0, retries: int = 3, sleep=time.sleep, session=None,
                 max_bytes: int | None = None) -> None:
        self.s = session or requests.Session()
        self.s.headers["User-Agent"] = USER_AGENT
        self.host_delay = host_delay
        self.timeout = timeout
        self.retries = retries
        self.sleep = sleep
        self._last: dict[str, float] = {}
        self.calls = 0
        self.polite_sec = 0.0  # 같은 호스트 간격을 지키느라 기다린 초(보고서 waits)
        self.retry_sec = 0.0   # 오류 뒤 다시 보내려고 기다린 초(보고서 waits)
        self.max_bytes = max_bytes  # 있으면 본문을 나눠 읽다가 넘는 순간 멈춘다

    def _polite(self, url: str) -> None:
        if not self.host_delay:
            return
        host = urlsplit(url).hostname or ""
        last = self._last.get(host)
        if last is not None:
            wait = self.host_delay - (time.monotonic() - last)
            if wait > 0:
                self.polite_sec += wait
                self.sleep(wait)
        self._last[host] = time.monotonic()

    def _read_capped(self, r: requests.Response) -> requests.Response:
        limit = self.max_bytes
        try:
            declared = int(r.headers.get("Content-Length") or 0)
        except ValueError:
            declared = 0
        if declared > limit:
            r.close()
            raise ResponseTooLarge(f"응답이 {limit} 바이트를 넘음(Content-Length {declared}): {r.url}")
        buf = bytearray()
        for chunk in r.iter_content(64 * 1024):
            buf += chunk
            if len(buf) > limit:
                r.close()
                raise ResponseTooLarge(f"응답이 {limit} 바이트를 넘음: {r.url}")
        r._content = bytes(buf)  # 이후 r.content, r.text, r.json() 은 읽어 둔 본문을 쓴다
        r._content_consumed = True
        return r

    def request(self, method: str, url: str, **kw) -> requests.Response:
        kw.setdefault("timeout", self.timeout)
        if self.max_bytes:
            kw["stream"] = True
        attempt = 0
        while True:
            self._polite(url)
            self.calls += 1
            try:
                r = self.s.request(method, url, **kw)
            except (requests.ConnectionError, requests.Timeout):
                if attempt >= self.retries:
                    raise
                attempt += 1
                self._backoff(2 ** attempt)
                continue
            if r.status_code in (429, 500, 502, 503, 504) and attempt < self.retries:
                attempt += 1
                wait = retry_after_seconds(r.headers.get("Retry-After"))
                r.close()
                self._backoff(wait if wait is not None else 2 ** attempt)
                continue
            return self._read_capped(r) if self.max_bytes else r

    def _backoff(self, seconds: float) -> None:
        self.retry_sec += seconds
        self.sleep(seconds)

    def get(self, url: str, **kw) -> requests.Response:
        return self.request("GET", url, **kw)

    def post(self, url: str, **kw) -> requests.Response:
        return self.request("POST", url, **kw)
