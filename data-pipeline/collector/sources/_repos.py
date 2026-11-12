"""팀 문서 세 원천(github-teamdoc, github-wiki, github-discussion)이 함께 쓰는 레포 다루기(06 문서 2장, 3-1, 5-7).

- 대상 레포: 조직 레포 목록 API 의 모든 레포. --repos 가 있으면 그 레포만. 이름은 API 의 name 그대로.
- 사라짐 확인 묶음은 레포 이름(group_of = repo_of_key).
- 조직 목록에서 빠진 아는 레포는 "목록을 끝까지 받은 묶음" 에 넣고, 레포를 직접 물어 404 면 그 레포 키 전부 사라짐.
"""

from __future__ import annotations

import logging

import requests

from ..config import ORG
from ..github import GitHubError
from ..keys import repo_of_key
from ..runner import Context, FetchError, Source

log = logging.getLogger("collector")


def target_repos(gh, ctx: Context) -> list[dict]:
    """대상 레포 목록(API 응답 그대로). --repos 는 대소문자를 가리지 않고 고르되, 이름은 API 값을 쓴다."""
    repos = gh.org_repos()
    if ctx.repos:
        want = {r.lower() for r in ctx.repos}
        repos = [r for r in repos if r["name"].lower() in want]
    return repos


def known_repos(ctx: Context) -> set[str]:
    """색인에서 사라지지 않은 키가 있는 레포."""
    out = set()
    for e in ctx.index.alive():
        repo = repo_of_key(e["external_key"])
        if repo:
            out.add(repo)
    return out


def missing_repos(ctx: Context, listed: set[str]) -> set[str]:
    """아는 키가 있는데 이번 조직 목록에 없는 레포. --repos 로 좁혔으면 그 안에서만 본다."""
    missing = known_repos(ctx) - listed
    if ctx.repos:
        want = {r.lower() for r in ctx.repos}
        missing = {r for r in missing if r.lower() in want}
    return missing


def to_fetch_error(exc: Exception, url: str | None = None) -> Exception:
    """요청 예외를 보고서 실패 이유로 바꾼다. 모르는 예외는 그대로 돌려준다(runner 가 error_종류 로 남긴다)."""
    if isinstance(exc, FetchError):
        return exc
    if isinstance(exc, GitHubError):
        return FetchError(f"http_{exc.status}", url)
    if isinstance(exc, requests.Timeout):
        return FetchError("timeout", url)
    if isinstance(exc, requests.ConnectionError):
        return FetchError("connection_error", url)
    return exc



def repo_gone_from(data: dict | None, repo: str) -> str | dict | None:
    """`GET /repos/{org}/{repo}` 응답으로 레포가 사라졌는지. 이름이 바뀌었으면 새 이름(같은 조직이면 레포 이름, 다른 곳으로
    옮겼으면 owner/이름)을 renamed_to 로 남긴다(R18-8)."""
    if data is None:
        return "repo_not_found"
    owner = (data.get("owner") or {}).get("login", ORG)
    if data.get("name") != repo or owner != ORG:
        new = data.get("name") if owner == ORG else data.get("full_name") or f"{owner}/{data.get('name')}"
        return {"reason": "repo_renamed", "renamed_to": new}
    return None

class RepoSource(Source):
    """세 원천 공통: GitHub 클라이언트 주입, 레포 단위 사라짐 확인, 보고서 칸."""

    def __init__(self, github=None) -> None:
        self._gh = github          # 시험에서 가짜 클라이언트를 넣는다. 없으면 ctx.github
        self.listed: set[str] = set()       # 이번 조직 목록(좁힌 뒤)에 있던 레포
        self.repos_failed: list[dict] = []  # 목록 받기가 실패한 레포(사라짐 판단을 건너뜀)
        self._repo_gone: dict[str, str | None] = {}

    def gh(self, ctx: Context):
        return self._gh if self._gh is not None else ctx.github

    def group_of(self, entry: dict) -> str | None:
        return repo_of_key(entry["external_key"])

    def repo_failed(self, repo: str, exc: Exception) -> None:
        log.warning("%s %s 목록 받기 실패: %r", self.name, repo, exc)
        self.repos_failed.append({"repo": repo, "reason": f"{type(exc).__name__}: {exc}"[:300]})

    def repo_gone_reason(self, ctx: Context, repo: str) -> str | dict | None:
        """조직 목록에서 빠진 레포를 직접 물어 본다. 레포마다 한 번만 묻는다.

        - 404: repo_not_found
        - 다른 이름으로 옮겨 감(이름 바꾸기, 이전): GitHub 이 새 레포로 넘겨 주므로 응답의 이름이 다르다. 키의 레포 이름으로는
          더 찾을 수 없으므로 repo_renamed(06 문서 7장 "레포 이름을 바꾸면 원래 키 사라짐"). 새 이름도 renamed_to 로 남긴다
          (2026-11-10, R18-8: 정제가 옛 키와 새 키를 이어 DB 행을 지우지 않고 옮기게)
        - 그대로 있음: None(목록에서 잠깐 빠진 것으로 보고 사라짐으로 기록하지 않는다)
        """
        if repo not in self._repo_gone:
            self._repo_gone[repo] = repo_gone_from(self.gh(ctx).rest_json(f"/repos/{ORG}/{repo}", ok404=True), repo)
        return self._repo_gone[repo]

    def report_extra(self, ctx: Context, **more) -> None:
        ctx.report.extra["repos_checked"] = len(self.listed)
        ctx.report.extra["repos_failed"] = self.repos_failed
        ctx.report.extra.update(more)
