"""GitHub 요청 도구(06 문서 5-8, 5-9, 07 문서 5-7, 5-8).

- 권한 없는 fine-grained 토큰(GITHUB_TOKEN). REST, GraphQL, raw, git 요청 모두에 붙인다.
- 한 번에 하나씩. 남은 한도가 바닥나면 초기화 시각(X-RateLimit-Reset)까지 기다렸다가 이어 받는다.
- 보조 한도(403/429 + Retry-After)도 기다렸다 다시 보낸다. Retry-After 가 없는 보조 한도는 GitHub 권고대로
  1분부터 두 배씩 늘려 기다리고, SECONDARY_RETRIES 번 넘게 막히면 그대로 실패로 돌려준다.
"""

from __future__ import annotations

import base64
import os
import re
import time

from .config import ORG, USER_AGENT
from .http import Http, retry_after_seconds

API = "https://api.github.com"
SECONDARY_WAIT = 60.0   # Retry-After 없는 보조 한도의 첫 대기(GitHub 문서: 적어도 1분)
SECONDARY_RETRIES = 5   # 한도로 막혀 다시 보내는 횟수 상한(Retry-After 없는 보조 한도면 60, 120, 240, 480, 960초)


class GitHubError(Exception):
    def __init__(self, status: int, message: str) -> None:
        super().__init__(f"{status} {message}")
        self.status = status


class GitHub:
    def __init__(self, token: str | None, http: Http | None = None, sleep=time.sleep, clock=time.time) -> None:
        if not token:
            raise RuntimeError("GITHUB_TOKEN 이 없습니다(08 문서 C12)")
        self.token = token
        self.http = http or Http()
        self.sleep, self.clock = sleep, clock
        self.rest_calls = 0
        self.graphql_points = 0
        self.waited_sec = 0.0

    def _headers(self, extra: dict | None = None) -> dict:
        h = {"Authorization": f"Bearer {self.token}", "Accept": "application/vnd.github+json",
             "X-GitHub-Api-Version": "2022-11-28"}
        h.update(extra or {})
        return h

    def _wait(self, seconds: float) -> None:
        seconds = max(1.0, seconds)
        self.waited_sec += seconds
        self.sleep(seconds)

    def _limited(self, r, secondary_tries: int = 0) -> float | None:
        """한도 때문에 막혔으면 기다릴 초, 아니면 None."""
        if r.status_code not in (403, 429):
            return None
        wait = retry_after_seconds(r.headers.get("Retry-After"))
        if wait is not None:
            return wait
        if r.headers.get("X-RateLimit-Remaining") == "0" and r.headers.get("X-RateLimit-Reset"):
            return float(r.headers["X-RateLimit-Reset"]) - self.clock() + 1
        if r.status_code == 429 or "secondary rate limit" in (getattr(r, "text", "") or "").lower():
            return SECONDARY_WAIT * (2 ** secondary_tries)
        return None  # 권한 없음 같은 보통 403

    def _send(self, method: str, url: str, **kw):
        headers = self._headers(kw.pop("headers", None))
        tries = 0
        while True:
            r = self.http.request(method, url, headers=headers, **kw)
            wait = self._limited(r, tries)
            if wait is None:
                # 다음 요청부터 막히지 않도록, 남은 한도가 0이면 미리 기다린다
                if r.headers.get("X-RateLimit-Remaining") == "0" and r.headers.get("X-RateLimit-Reset"):
                    self._wait(float(r.headers["X-RateLimit-Reset"]) - self.clock() + 1)
                return r
            if tries >= SECONDARY_RETRIES:
                return r  # 계속 막히면 실패로 돌려준다(호출한 쪽이 GitHubError 로 남김)
            tries += 1
            self._wait(wait)

    def rest(self, path_or_url: str, params: dict | None = None, ok404: bool = False):
        url = path_or_url if path_or_url.startswith("http") else API + path_or_url
        self.rest_calls += 1
        r = self._send("GET", url, params=params)
        if r.status_code == 404 and ok404:
            return None
        if r.status_code >= 400:
            raise GitHubError(r.status_code, r.text[:300])
        return r

    def rest_json(self, path: str, params: dict | None = None, ok404: bool = False):
        r = self.rest(path, params, ok404)
        return None if r is None else r.json()

    def paginate(self, path: str, params: dict | None = None):
        url, p = API + path, dict(params or {})
        while url:
            r = self.rest(url, p)
            yield from r.json()
            url, p = _next_link(r.headers.get("Link", "")), None

    def graphql(self, query: str, variables: dict | None = None) -> dict:
        while True:
            r = self._send("POST", API + "/graphql", json={"query": query, "variables": variables or {}})
            if r.status_code >= 400:
                raise GitHubError(r.status_code, r.text[:300])
            body = r.json()
            # GraphQL 은 한도를 넘으면 200 과 함께 RATE_LIMITED 오류를 줄 수 있다. 초기화 시각까지 기다렸다 다시 보낸다
            if any(e.get("type") == "RATE_LIMITED" for e in body.get("errors") or []):
                reset = r.headers.get("X-RateLimit-Reset")
                self._wait(float(reset) - self.clock() + 1 if reset else 60)
                continue
            break
        if body.get("errors"):
            # 일부만 없음(NOT_FOUND)인 경우는 data 와 함께 돌려준다
            if not body.get("data") or any(e.get("type") != "NOT_FOUND" for e in body["errors"]):
                raise GitHubError(200, str(body["errors"])[:300])
        cost = ((body.get("data") or {}).get("rateLimit") or {}).get("cost")
        self.graphql_points += int(cost or 1)
        return body.get("data") or {}

    def raw(self, repo: str, commit: str, path: str) -> bytes:
        from urllib.parse import quote
        url = f"https://raw.githubusercontent.com/{ORG}/{repo}/{commit}/{quote(path)}"
        r = self._send("GET", url)
        if r.status_code >= 400:
            raise GitHubError(r.status_code, f"raw {path}")
        return r.content

    def org_repos(self) -> list[dict]:
        return list(self.paginate(f"/orgs/{ORG}/repos", {"per_page": 100, "type": "all"}))

    def git_env(self) -> dict:
        """git 요청에 토큰, User-Agent, 한글 파일 이름 그대로(06 문서 3-3). 토큰이 명령줄에 드러나지 않게 환경 변수로 넘긴다."""
        basic = base64.b64encode(f"x-access-token:{self.token}".encode()).decode()
        pairs = [("http.extraHeader", f"Authorization: Basic {basic}"), ("http.userAgent", USER_AGENT),
                 ("core.quotePath", "false"), ("credential.helper", "")]
        env = dict(os.environ, GIT_TERMINAL_PROMPT="0", GIT_CONFIG_COUNT=str(len(pairs)))
        for i, (k, v) in enumerate(pairs):
            env[f"GIT_CONFIG_KEY_{i}"], env[f"GIT_CONFIG_VALUE_{i}"] = k, v
        return env


def _next_link(link: str) -> str | None:
    m = re.search(r'<([^>]+)>;\s*rel="next"', link)
    return m.group(1) if m else None
