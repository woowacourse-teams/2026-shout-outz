"""HTTP 요청 도구. 고정 User-Agent, 한 번에 하나씩, 서버가 알려 주는 기다릴 시간(Retry-After) 지키기.

블로그는 같은 호스트에 1초에 1번 이하로 보낸다(03 문서 5-6). host_delay 로 정한다.
"""

from __future__ import annotations

import time
from urllib.parse import urlsplit

import requests

from .config import USER_AGENT


class Http:
    def __init__(self, host_delay: float = 0.0, timeout: float = 30.0, retries: int = 3, sleep=time.sleep, session=None) -> None:
        self.s = session or requests.Session()
        self.s.headers["User-Agent"] = USER_AGENT
        self.host_delay = host_delay
        self.timeout = timeout
        self.retries = retries
        self.sleep = sleep
        self._last: dict[str, float] = {}
        self.calls = 0

    def _polite(self, url: str) -> None:
        if not self.host_delay:
            return
        host = urlsplit(url).hostname or ""
        last = self._last.get(host)
        if last is not None:
            wait = self.host_delay - (time.monotonic() - last)
            if wait > 0:
                self.sleep(wait)
        self._last[host] = time.monotonic()

    def request(self, method: str, url: str, **kw) -> requests.Response:
        kw.setdefault("timeout", self.timeout)
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
                self.sleep(2 ** attempt)
                continue
            if r.status_code in (429, 500, 502, 503, 504) and attempt < self.retries:
                attempt += 1
                retry_after = r.headers.get("Retry-After")
                self.sleep(float(retry_after) if retry_after and retry_after.isdigit() else 2 ** attempt)
                continue
            return r

    def get(self, url: str, **kw) -> requests.Response:
        return self.request("GET", url, **kw)

    def post(self, url: str, **kw) -> requests.Response:
        return self.request("POST", url, **kw)
