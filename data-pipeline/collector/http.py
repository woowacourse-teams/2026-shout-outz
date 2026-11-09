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


class ResponseTooSlow(requests.Timeout):
    """응답 하나를 받는 데 max_seconds 를 넘음(2026-11-09).

    requests 의 timeout 은 연결과 읽기 한 번마다만 걸려서, 서버가 아주 느리게 조금씩 보내면 요청 하나가 끝나지 않는다
    (정제 세션이 코드 마크다운 raw 를 받다 겪은 일). 그러면 매일 실행 전체가 멈추므로 요청 하나에 전체 시간 상한을 둔다.
    Timeout 의 한 갈래라 다른 시간 초과처럼 다시 보내 보고, 그래도 넘으면 그 건만 timeout 실패가 된다.
    """


MAX_SECONDS = 120.0  # 응답 하나(연결부터 본문 끝까지)의 전체 시간 상한. 가장 큰 응답(735KB RSS, GraphQL 묶음)도 몇 초면 받는다


class Http:
    def __init__(self, host_delay: float = 0.0, timeout: float = 30.0, retries: int = 3, sleep=time.sleep, session=None,
                 max_bytes: int | None = None, max_seconds: float | None = MAX_SECONDS, clock=time.monotonic) -> None:
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
        self.max_seconds = max_seconds  # 있으면 본문을 나눠 읽다가 요청부터 이만큼 지나면 멈춘다
        self.clock = clock

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

    def _read_capped(self, r: requests.Response, started: float) -> requests.Response:
        """본문을 나눠 읽으며 크기 상한(max_bytes)과 전체 시간 상한(max_seconds)을 지킨다."""
        limit = self.max_bytes
        try:
            declared = int(r.headers.get("Content-Length") or 0)
        except ValueError:
            declared = 0
        if limit and declared > limit:
            r.close()
            raise ResponseTooLarge(f"응답이 {limit} 바이트를 넘음(Content-Length {declared}): {r.url}")
        buf = bytearray()
        for chunk in r.iter_content(64 * 1024):
            buf += chunk
            if limit and len(buf) > limit:
                r.close()
                raise ResponseTooLarge(f"응답이 {limit} 바이트를 넘음: {r.url}")
            if self.max_seconds and self.clock() - started > self.max_seconds:
                r.close()
                raise ResponseTooSlow(f"응답을 {self.max_seconds:.0f}초 안에 다 받지 못함: {r.url}")
        r._content = bytes(buf)  # 이후 r.content, r.text, r.json() 은 읽어 둔 본문을 쓴다
        r._content_consumed = True
        return r

    def request(self, method: str, url: str, **kw) -> requests.Response:
        kw.setdefault("timeout", self.timeout)
        read_here = bool(self.max_bytes or self.max_seconds)
        if read_here:
            kw["stream"] = True
        attempt = 0
        while True:
            self._polite(url)
            self.calls += 1
            started = self.clock()
            try:
                r = self.s.request(method, url, **kw)
                if read_here and r.status_code not in (429, 500, 502, 503, 504):
                    r = self._read_capped(r, started)
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
            if read_here and not getattr(r, "_content_consumed", True):
                r = self._read_capped(r, started)  # 다시 보낼 만한 상태 코드인데 횟수를 다 쓴 응답
            return r

    def _backoff(self, seconds: float) -> None:
        self.retry_sec += seconds
        self.sleep(seconds)

    def get(self, url: str, **kw) -> requests.Response:
        return self.request("GET", url, **kw)

    def post(self, url: str, **kw) -> requests.Response:
        return self.request("POST", url, **kw)
