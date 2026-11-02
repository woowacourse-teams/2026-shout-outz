"""Discussions 원본 최초 적재 — 모든 카테고리의 모든 글, 채택 답변, 모든 댓글과 답글(봇, 이모지 포함)을 받는다(결정 기록 19번).

GraphQL 로 레포마다 글을 묶어 받고(한 쿼리에 글 10개, 글마다 댓글 50 · 답글 20 까지),
넘친 댓글 · 답글은 그 글 · 그 댓글만 따로 끝까지 이어 받는다(totalCount 와 받은 수가 같은지 확인).
PR 수집과 같은 토큰 · GraphQL 한도를 쓰므로, 한도가 바닥나면 다시 찰 때까지 기다린다.
원본 목록은 원천별 파일(raw_objects/github-discussion.jsonl).

실행: uv run python -m pipeline.collectors.discussions [--repos 이름 ...]
"""
from __future__ import annotations

import argparse
import json
import time

import httpx

from datetime import datetime, timedelta

from pipeline.common import DATA, OWNER, STATE, atomic_write, now, stamp, token
from pipeline.store import Store

VERSION = "1.1.0"  # 1.1.0: 최근 수정순, 커서 이후만

GRAPHQL = "https://api.github.com/graphql"
PAGE = 10

AUTHOR = "author { login __typename }"
REPLY = f"id databaseId body createdAt updatedAt isAnswer {AUTHOR}"
COMMENT = (f"id databaseId body createdAt updatedAt isAnswer {AUTHOR} "
           f"replies(first: 20) {{ totalCount pageInfo {{ hasNextPage endCursor }} nodes {{ {REPLY} }} }}")
DISCUSSION = (f"id number title body url createdAt updatedAt closed closedAt locked upvoteCount {AUTHOR} "
              "category { name slug isAnswerable } labels(first: 20) { nodes { name } } "
              f"answer {{ id }} answerChosenAt "
              f"comments(first: 50) {{ totalCount pageInfo {{ hasNextPage endCursor }} nodes {{ {COMMENT} }} }}")
LIST_Q = ("query($o:String!,$n:String!,$after:String){ rateLimit{cost remaining resetAt} repository(owner:$o,name:$n){ "
          "hasDiscussionsEnabled discussions(first:%d, after:$after, orderBy:{field:UPDATED_AT,direction:DESC}){ "
          "totalCount pageInfo{hasNextPage endCursor} nodes{ %s } } } }" % (PAGE, DISCUSSION))
MORE_COMMENTS_Q = ("query($id:ID!,$after:String){ rateLimit{cost remaining resetAt} node(id:$id){ ... on Discussion { "
                   "comments(first:50, after:$after){ pageInfo{hasNextPage endCursor} nodes{ %s } } } } }" % COMMENT)
MORE_REPLIES_Q = ("query($id:ID!,$after:String){ rateLimit{cost remaining resetAt} node(id:$id){ ... on DiscussionComment { "
                  "replies(first:50, after:$after){ pageInfo{hasNextPage endCursor} nodes{ %s } } } } }" % REPLY)


class GQL:
    def __init__(self, token: str):
        self.c = httpx.Client(timeout=90, headers={"Authorization": f"Bearer {token}",
                                                   "User-Agent": "shout-outz-raw-collector/0.1"})
        self.calls = 0
        self.points = 0

    def q(self, query: str, variables: dict) -> dict:
        for attempt in range(8):
            try:
                r = self.c.post(GRAPHQL, json={"query": query, "variables": variables})
            except httpx.TransportError:
                time.sleep(min(2 ** attempt, 60))
                continue
            self.calls += 1
            if r.status_code in (403, 429):
                reset = int(r.headers.get("x-ratelimit-reset", time.time() + 60))
                wait = int(r.headers.get("retry-after") or 0) or max(60, reset - int(time.time()) + 10)
                print(f"  GraphQL 한도 대기 {wait}s", flush=True)
                time.sleep(min(wait, 3700))
                continue
            if r.status_code >= 500:
                time.sleep(min(2 ** attempt, 60))
                continue
            d = r.json()
            errs = d.get("errors") or []
            if any(e.get("type") == "RATE_LIMITED" for e in errs):
                reset = int(r.headers.get("x-ratelimit-reset", time.time() + 60))
                time.sleep(min(max(60, reset - int(time.time()) + 10), 3700))
                continue
            if errs:
                raise RuntimeError(json.dumps(errs, ensure_ascii=False)[:400])
            rl = d["data"].get("rateLimit") or {}
            self.points += int(rl.get("cost") or 0)
            if rl.get("remaining") is not None and int(rl["remaining"]) < 200:
                reset = int(r.headers.get("x-ratelimit-reset", time.time() + 60))
                print(f"  GraphQL 남은 한도 {rl['remaining']} → {max(0, reset - int(time.time()))}s 대기", flush=True)
                time.sleep(min(max(0, reset - int(time.time())) + 10, 3700))
            return d["data"]
        raise RuntimeError("GraphQL 요청 반복 실패")


def complete(gql: GQL, disc: dict) -> dict:
    """넘친 댓글 · 답글을 끝까지 받아 채우고, (받은 수 == totalCount) 검증 결과를 돌려준다."""
    comments = disc["comments"]
    cur = comments["pageInfo"]
    while cur["hasNextPage"]:
        d = gql.q(MORE_COMMENTS_Q, {"id": disc["id"], "after": cur["endCursor"]})
        more = d["node"]["comments"]
        comments["nodes"] += more["nodes"]
        cur = more["pageInfo"]
    for c in comments["nodes"]:
        rep = c["replies"]
        cur = rep["pageInfo"]
        while cur["hasNextPage"]:
            d = gql.q(MORE_REPLIES_Q, {"id": c["id"], "after": cur["endCursor"]})
            more = d["node"]["replies"]
            rep["nodes"] += more["nodes"]
            cur = more["pageInfo"]
        rep["pageInfo"] = {"hasNextPage": False, "endCursor": None}
    comments["pageInfo"] = {"hasNextPage": False, "endCursor": None}
    ok_comments = len(comments["nodes"]) == comments["totalCount"]
    ok_replies = all(len(c["replies"]["nodes"]) == c["replies"]["totalCount"] for c in comments["nodes"])
    return {"comments_total": comments["totalCount"], "comments_got": len(comments["nodes"]),
            "replies_total": sum(c["replies"]["totalCount"] for c in comments["nodes"]),
            "replies_got": sum(len(c["replies"]["nodes"]) for c in comments["nodes"]),
            "pass": ok_comments and ok_replies}


CURSOR = "state/cursors/github-discussion.json"  # 레포별 지금까지 본 가장 늦은 updatedAt
WINDOW = timedelta(days=30)  # 새 댓글이 글의 updatedAt 을 올리는지 확인 전이라 커서보다 30일 앞까지 다시 본다


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--repos", nargs="+")
    ap.add_argument("--full", action="store_true", help="커서와 상관없이 모든 글을 본다")
    args = ap.parse_args()
    repos = args.repos or json.loads((STATE / "org_repos.json").read_text())
    report, _ = collect(repos, "discussion-" + stamp(now()), force=args.full)
    atomic_write(DATA / "runs" / f"run-{report['run_id']}.json",
                 (json.dumps(report, ensure_ascii=False, sort_keys=True, indent=2) + "\n").encode())
    print(json.dumps({"counts": report["counts"], "failures": len(report["failures"]),
                      "verify_failed": len(report["verify_failed"]), "graphql_calls": report["graphql_calls"],
                      "graphql_points": report["graphql_points"]}, ensure_ascii=False))
    return 0


def _ts(s: str) -> datetime:
    return datetime.fromisoformat(s.replace("Z", "+00:00"))


def collect(repos: list[str], run_id: str, force: bool = False) -> tuple[dict, list[Store]]:
    """최근 수정순으로 받다가 (커서 - 30일)보다 오래된 글이 나오면 그 레포는 멈춘다. 커서가 없으면 전부."""
    cursor_path = DATA / CURSOR
    cursors = json.loads(cursor_path.read_text()) if cursor_path.exists() else {}
    gql = GQL(token())
    store = Store("github-discussion", run_id, VERSION)
    report = {"run_id": run_id, "started_at": now(), "repos": [], "failures": [], "verify_failed": [],
              "counts": {"repos_enabled": 0, "repos_with_discussions": 0, "discussions": 0, "comments": 0, "replies": 0,
                         "saved_new": 0, "saved_changed": 0, "already_present": 0}}
    for i, repo in enumerate(repos, 1):
        try:
            after, total, got, stopped = None, None, 0, False
            since = None if force or repo not in cursors else _ts(cursors[repo]) - WINDOW
            while True:
                d = gql.q(LIST_Q, {"o": OWNER, "n": repo, "after": after})
                r = d["repository"]
                if r is None:
                    report["failures"].append({"repo": repo, "reason": "레포 없음"})
                    break
                if not r["hasDiscussionsEnabled"]:
                    break
                conn = r["discussions"]
                if total is None:
                    total = conn["totalCount"]
                    report["counts"]["repos_enabled"] += 1
                    if total:
                        report["counts"]["repos_with_discussions"] += 1
                for disc in conn["nodes"]:
                    if since and _ts(disc["updatedAt"]) < since:
                        stopped = True  # 이 뒤는 모두 더 오래된 글
                        break
                    if repo not in cursors or disc["updatedAt"] > cursors[repo]:
                        cursors[repo] = disc["updatedAt"]
                    check = complete(gql, disc)
                    if not check["pass"]:
                        report["verify_failed"].append({"repo": repo, "number": disc["number"], **check})
                    got += 1
                    report["counts"]["discussions"] += 1
                    report["counts"]["comments"] += check["comments_got"]
                    report["counts"]["replies"] += check["replies_got"]
                    hash_obj = {"title": disc["title"], "body": disc["body"], "category": disc["category"]["name"],
                                "answer": (disc.get("answer") or {}).get("id"),
                                "comments": [(c["id"], c["body"], [(x["id"], x["body"]) for x in c["replies"]["nodes"]])
                                             for c in disc["comments"]["nodes"]]}
                    outcome = store.save(f"{OWNER}/{repo}/discussions/{disc['number']}",
                                         f"{OWNER}/{repo}/{disc['number']}",
                                         {"fetched_at": now(), "request": {"graphql": "discussions-v1", "cursor": after}},
                                         {"repo": f"{OWNER}/{repo}", "discussion": disc, "verification": check},
                                         hash_obj, disc["url"], disc["updatedAt"])
                    key = {"new": "saved_new", "changed": "saved_changed"}.get(outcome, "already_present")
                    report["counts"][key] += 1
                if stopped or not conn["pageInfo"]["hasNextPage"]:
                    break
                after = conn["pageInfo"]["endCursor"]
            if total is not None:
                full_scan = since is None  # 전부 본 경우에만 글 수를 검증할 수 있다
                report["repos"].append({"repo": repo, "total": total, "got": got, "incremental": not full_scan,
                                        "pass": total == got if full_scan else None})
                if full_scan and total != got:
                    report["verify_failed"].append({"repo": repo, "reason": f"글 수 불일치 {got}/{total}"})
        except (RuntimeError, httpx.HTTPError, KeyError, TypeError) as e:
            report["failures"].append({"repo": repo, "reason": f"{type(e).__name__}: {e}"[:400]})
        if i % 10 == 0:
            store.flush()
            atomic_write(cursor_path, (json.dumps(cursors, ensure_ascii=False, sort_keys=True, indent=1) + "\n").encode())
            print(f"[{i}/{len(repos)}] {report['counts']} 실패 {len(report['failures'])} 검증실패 {len(report['verify_failed'])}",
                  flush=True)
    store.flush()
    atomic_write(cursor_path, (json.dumps(cursors, ensure_ascii=False, sort_keys=True, indent=1) + "\n").encode())
    report["graphql_calls"], report["graphql_points"] = gql.calls, gql.points
    report["finished_at"] = now()
    return report, [store]


if __name__ == "__main__":
    raise SystemExit(main())
