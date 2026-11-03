"""PR 원본 수집(github-pr, 계획 3장, 2026-11-03 재수집 기준).

- 바뀐 PR 찾기: 레포마다 `pullRequests(orderBy: UPDATED_AT DESC)` 로 받다가, 커서(지금까지 본 가장 늦은 updatedAt)에서
  1시간 여유를 둔 시각보다 오래된 PR 이 나오면 멈춘다. 커서가 없거나 --full 이면 전부 본다(빈 상태와 같은 코드).
- 재수집 판단: 사람이 쓴 글과 상태로 만든 `text_hash`(pr-v2, pipeline/pr_hash.py)가 지난 최신 버전과 같으면
  last_seen_at 만 갱신한다. 다르면(새 PR, 글 작성과 수정, 열림과 닫힘과 머지) 새 버전을 저장한다.
- 파일 목록은 해시 밖이라 새 버전을 쓸 때만 REST 로 받는다. patch 는 저장하지 않는다(경로, 상태, 줄 수만).
- 일반 대화 댓글은 받지 않는다(결정 5). 리뷰와 스레드 댓글은 봇, 빈 승인, 이모지만 있는 것을 뺀다(최초 수집과 같은 규칙).
- 리뷰, 댓글 수정은 PR updatedAt 을 올리지 않을 수 있어 매월 1일 전체 확인(--full)으로 보완한다.

실행: uv run python -m pipeline.collectors.pr [--repos 이름 ...] [--full]
"""
from __future__ import annotations

import argparse
import copy
import json
import unicodedata
from datetime import datetime, timedelta

from pipeline.common import API, DATA, GH, GQL, OWNER, STATE, atomic_write, now, stamp, token
from pipeline.pr_hash import SCHEME, text_hash
from pipeline.store import Store

VERSION = "2.1.0"  # 2.1.0: 첫 조회 크기를 줄이고 넘친 것만 따로 받음(GraphQL 점수 줄이기), 매월 확인 기간 지정
CURSOR = "state/cursors/github-pr.json"
WINDOW = timedelta(hours=1)
FIRST = 20
MAX_FILES = 3000

BATCH_QUERY = """
query PullRequestUpdated($owner: String!, $name: String!, $first: Int!, $after: String) {
  rateLimit { cost remaining resetAt }
  repository(owner: $owner, name: $name) {
    pullRequests(first: $first, after: $after,
      orderBy: {field: UPDATED_AT, direction: DESC},
      states: [OPEN, CLOSED, MERGED]) {
      totalCount
      pageInfo { hasNextPage endCursor }
      nodes {
        number title body url state isDraft merged mergedAt createdAt updatedAt closedAt
        author { login __typename }
        labels(first: 10) {
          totalCount pageInfo { hasNextPage endCursor }
          nodes { name }
        }
        baseRefName headRefName headRefOid additions deletions changedFiles
        reviews(first: 10) {
          totalCount pageInfo { hasNextPage endCursor }
          nodes { id databaseId author { login __typename } state body submittedAt }
        }
        reviewThreads(first: 10) {
          totalCount pageInfo { hasNextPage endCursor }
          nodes {
            id isResolved path line
            comments(first: 5) {
              totalCount pageInfo { hasNextPage endCursor }
              nodes { id databaseId author { login __typename } body createdAt }
            }
          }
        }
      }
    }
  }
}
"""

CONNECTION_QUERIES = {
    "labels": """
      query PullRequestLabelsPage($owner: String!, $name: String!, $number: Int!, $after: String) {
        rateLimit { cost remaining resetAt }
        repository(owner: $owner, name: $name) {
          pullRequest(number: $number) {
            labels(first: 100, after: $after) {
              totalCount pageInfo { hasNextPage endCursor } nodes { name }
            }
          }
        }
      }
    """,
    "reviews": """
      query PullRequestReviewsPage($owner: String!, $name: String!, $number: Int!, $after: String) {
        rateLimit { cost remaining resetAt }
        repository(owner: $owner, name: $name) {
          pullRequest(number: $number) {
            reviews(first: 100, after: $after) {
              totalCount pageInfo { hasNextPage endCursor }
              nodes { id databaseId author { login __typename } state body submittedAt }
            }
          }
        }
      }
    """,
    "reviewThreads": """
      query PullRequestReviewThreadsPage($owner: String!, $name: String!, $number: Int!, $after: String) {
        rateLimit { cost remaining resetAt }
        repository(owner: $owner, name: $name) {
          pullRequest(number: $number) {
            reviewThreads(first: 100, after: $after) {
              totalCount pageInfo { hasNextPage endCursor }
              nodes {
                id isResolved path line
                comments(first: 20) {
                  totalCount pageInfo { hasNextPage endCursor }
                  nodes { id databaseId author { login __typename } body createdAt }
                }
              }
            }
          }
        }
      }
    """,
}

COMMENTS_QUERY = """
query ReviewThreadCommentsPage($threadId: ID!, $after: String) {
  rateLimit { cost remaining resetAt }
  node(id: $threadId) {
    ... on PullRequestReviewThread {
      comments(first: 100, after: $after) {
        totalCount pageInfo { hasNextPage endCursor }
        nodes { id databaseId author { login __typename } body createdAt }
      }
    }
  }
}
"""

BOT_PATTERNS = (
    "[bot]", "coderabbit", "gemini-code-assist", "copilot",
    "github-actions", "sonarcloud",
)

def is_bot(author: dict[str, Any] | None) -> bool:
    if not author:
        return False
    if author.get("__typename") == "Bot":
        return True
    login = str(author.get("login") or "").lower()
    return any(pattern in login for pattern in BOT_PATTERNS)


def is_emoji_only(text: str | None) -> bool:
    value = (text or "").strip()
    if not value:
        return False
    compact = "".join(ch for ch in value if not ch.isspace())
    if compact.lower() in {"+1", "-1"}:
        return True
    if not compact:
        return False
    for index, char in enumerate(compact):
        category = unicodedata.category(char)
        if category.startswith("S") or category in {"Mn", "Me", "Cf"}:
            continue
        if char.isdigit() and index + 1 < len(compact) and compact[index + 1] == "\u20e3":
            continue
        return False
    return True




def _ts(s: str) -> datetime:
    return datetime.fromisoformat(s.replace("Z", "+00:00"))


def _complete(gql: GQL, repo: str, number: int, name: str, connection: dict) -> dict:
    """넘친 labels, reviews, reviewThreads 를 끝까지 받는다(받은 수 == totalCount 확인)."""
    nodes, total = list(connection.get("nodes") or []), int(connection.get("totalCount") or 0)
    info = connection.get("pageInfo") or {}
    while info.get("hasNextPage"):
        d = gql.q(CONNECTION_QUERIES[name], {"owner": OWNER, "name": repo, "number": number, "after": info["endCursor"]})
        nxt = ((d.get("repository") or {}).get("pullRequest") or {}).get(name) or {}
        nodes += nxt.get("nodes") or []
        info = nxt.get("pageInfo") or {}
    if len(nodes) != total:
        raise RuntimeError(f"{name} 수 불일치 {repo}#{number}: {len(nodes)} != {total}")
    return {"totalCount": total, "pageInfo": {"hasNextPage": False, "endCursor": None}, "nodes": nodes}


def _complete_comments(gql: GQL, repo: str, number: int, thread: dict) -> None:
    c = thread.get("comments") or {}
    nodes, total, info = list(c.get("nodes") or []), int(c.get("totalCount") or 0), c.get("pageInfo") or {}
    while info.get("hasNextPage"):
        d = gql.q(COMMENTS_QUERY, {"threadId": thread["id"], "after": info["endCursor"]})
        nxt = (d.get("node") or {}).get("comments") or {}
        nodes += nxt.get("nodes") or []
        info = nxt.get("pageInfo") or {}
    if len(nodes) != total:
        raise RuntimeError(f"스레드 댓글 수 불일치 {repo}#{number}: {len(nodes)} != {total}")
    thread["comments"] = {"totalCount": total, "pageInfo": {"hasNextPage": False, "endCursor": None}, "nodes": nodes}


def _filter(pr: dict) -> tuple[list, list, dict]:
    """사람이 쓴 리뷰와 스레드 댓글만 남긴다(최초 수집 schema.make_envelope 와 같은 규칙)."""
    excluded = {"bot_reviews": 0, "empty_approvals": 0, "emoji_only_reviews": 0,
                "bot_thread_comments": 0, "emoji_only_thread_comments": 0}
    reviews = []
    for r in (pr.get("reviews") or {}).get("nodes", []):
        if is_bot(r.get("author")):
            excluded["bot_reviews"] += 1
        elif r.get("state") == "APPROVED" and not (r.get("body") or "").strip():
            excluded["empty_approvals"] += 1
        elif is_emoji_only(r.get("body")):
            excluded["emoji_only_reviews"] += 1
        else:
            reviews.append(r)
    threads = []
    for raw in (pr.get("reviewThreads") or {}).get("nodes", []):
        t = {k: v for k, v in raw.items() if k != "comments"}
        conn = raw.get("comments") or {}
        kept = []
        for c in conn.get("nodes", []):
            if is_bot(c.get("author")):
                excluded["bot_thread_comments"] += 1
            elif is_emoji_only(c.get("body")):
                excluded["emoji_only_thread_comments"] += 1
            else:
                kept.append(c)
        t["comments"] = {"totalCount": conn.get("totalCount", len(conn.get("nodes", []))),
                         "pageInfo": conn.get("pageInfo", {"hasNextPage": False, "endCursor": None}), "nodes": kept}
        threads.append(t)
    return reviews, threads, excluded


def _files(gh: GH, repo: str, number: int) -> tuple[list, dict]:
    """파일 목록(경로, 상태, 줄 수). patch 는 버린다."""
    out, url, pages = [], f"{API}/repos/{OWNER}/{repo}/pulls/{number}/files?per_page=100", 0
    for page in range(1, 31):
        r = gh.get(f"{url}&page={page}")
        r.raise_for_status()
        items = r.json()
        pages += 1
        out += [{"filename": f.get("filename"), "status": f.get("status"), "additions": f.get("additions", 0),
                 "deletions": f.get("deletions", 0), "changes": f.get("changes", 0),
                 "previous_filename": f.get("previous_filename")} for f in items]
        if len(out) >= MAX_FILES or len(items) < 100:
            break
    return out[:MAX_FILES], {"url": url, "pages": pages}


def collect(repos: list[str], run_id: str, force: bool = False, window_days: int | None = None) -> tuple[dict, list[Store]]:
    """force 면 커서를 무시하고 다시 본다(매월 1일, 리뷰 수정은 PR updatedAt 을 올리지 않기 때문).
    window_days 가 있으면 force 일 때 그 기간 안에 수정된 PR 까지만 본다(오래된 PR 의 리뷰 수정은 드물다고 보고)."""
    cursor_path = DATA / CURSOR
    cursors = json.loads(cursor_path.read_text()) if cursor_path.exists() else {}
    gql, gh = GQL(token()), GH(token())
    store = Store("github-pr", run_id, VERSION)
    report = {"run_id": run_id, "started_at": now(), "repos": [], "failures": [],
              "counts": {"seen": 0, "unchanged": 0, "saved_new": 0, "saved_changed": 0, "restored": 0}}
    for i, repo in enumerate(repos, 1):
        if force:
            since = (datetime.now(_ts(now()).tzinfo) - timedelta(days=window_days)) if window_days else None
        else:
            since = None if repo not in cursors else _ts(cursors[repo]) - WINDOW
        newest = cursors.get(repo)
        info = {"repo": repo, "seen": 0, "saved": 0, "full": since is None}
        try:
            after = None
            while True:
                d = gql.q(BATCH_QUERY, {"owner": OWNER, "name": repo, "first": FIRST, "after": after})
                r = d.get("repository")
                if r is None:
                    report["failures"].append({"repo": repo, "reason": "레포 없음"})
                    break
                conn, stop = r["pullRequests"], False
                for node in conn["nodes"]:
                    if since and _ts(node["updatedAt"]) < since:
                        stop = True  # 이 뒤는 모두 더 오래 전에 바뀐 PR
                        break
                    if newest is None or node["updatedAt"] > newest:
                        newest = node["updatedAt"]
                    number = int(node["number"])
                    pr = copy.deepcopy(node)
                    for name in ("labels", "reviews", "reviewThreads"):
                        pr[name] = _complete(gql, repo, number, name, pr.get(name) or {})
                    for t in pr["reviewThreads"]["nodes"]:
                        _complete_comments(gql, repo, number, t)
                    reviews, threads, excluded = _filter(pr)
                    pull = {k: v for k, v in pr.items() if k not in ("reviews", "reviewThreads")}
                    th = text_hash(pull, reviews, threads)
                    key = f"{OWNER}/{repo}#{number}"
                    info["seen"] += 1
                    report["counts"]["seen"] += 1
                    if store.unchanged(key, th):
                        store.touch(key, now())
                        report["counts"]["unchanged"] += 1
                        continue
                    files, file_req = _files(gh, repo, number)
                    payload = {"pull": pull, "files": files, "files_total": len(files), "reviews": reviews,
                               "review_threads": threads, "excluded": excluded}
                    full_material = {"text_hash": th, "labels": sorted(l["name"] for l in pull["labels"]["nodes"]),
                                     "headRefOid": pull.get("headRefOid"),
                                     "files": sorted((f["filename"] or "", f["status"], f["additions"], f["deletions"])
                                                     for f in files)}
                    outcome = store.save(key, f"{OWNER}/{repo}/{number}",
                                         {"fetched_at": now(), "request": {"graphql": "pr-updated-v2", "cursor": after,
                                                                           "rest_files": file_req}},
                                         payload, full_material, pull.get("url"), pull.get("updatedAt"),
                                         text_hash=th, hash_scheme=SCHEME)
                    report["counts"]["saved_new" if outcome == "new" else "saved_changed" if outcome == "changed"
                                     else "restored" if outcome == "restored" else "unchanged"] += 1
                    info["saved"] += outcome in ("new", "changed", "restored")
                if stop or not conn["pageInfo"]["hasNextPage"]:
                    break
                after = conn["pageInfo"]["endCursor"]
            if newest:
                cursors[repo] = newest  # 레포를 끝까지 본 뒤에만 커서를 올린다
        except Exception as e:  # noqa: BLE001 — 레포 하나가 실패해도 나머지는 돈다(PARTIAL)
            report["failures"].append({"repo": repo, "reason": f"{type(e).__name__}: {e}"[:300]})
        report["repos"].append(info)
        if i % 5 == 0:
            store.flush()
            atomic_write(cursor_path, (json.dumps(cursors, ensure_ascii=False, sort_keys=True, indent=1) + "\n").encode())
            print(f"[{i}/{len(repos)}] {report['counts']} 실패 {len(report['failures'])}", flush=True)
    store.flush()
    atomic_write(cursor_path, (json.dumps(cursors, ensure_ascii=False, sort_keys=True, indent=1) + "\n").encode())
    report.update({"finished_at": now(), "graphql_calls": gql.calls, "graphql_points": gql.points,
                   "api_calls": gh.api_calls})
    return report, [store]


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--repos", nargs="+")
    ap.add_argument("--full", action="store_true", help="커서와 상관없이 모든 PR 을 본다(매월 1일)")
    args = ap.parse_args()
    repos = args.repos or json.loads((STATE / "org_repos.json").read_text())
    report, _ = collect(repos, "pr-" + stamp(now()), force=args.full)
    atomic_write(DATA / "runs" / f"run-{report['run_id']}.json",
                 (json.dumps(report, ensure_ascii=False, sort_keys=True, indent=2) + "\n").encode())
    print(json.dumps({"counts": report["counts"], "failures": len(report["failures"]),
                      "graphql_calls": report["graphql_calls"], "api_calls": report["api_calls"]}, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
