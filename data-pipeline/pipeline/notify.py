"""알림(계획 1-5). ALERT_WEBHOOK_URL 이 있으면 보내고, 없거나 --dry-run 이면 출력만 한다.

채널(Discord 또는 Slack, 계획 7장 4번)이 정해지지 않아 두 형식을 함께 보낸다(Discord 는 content, Slack 은 text).
"""
from __future__ import annotations

import json
import os

import httpx


def _verify_example(row: dict) -> str:
    """검증 실패 한 건을 사람이 읽을 수 있게 줄인다(글 하나의 댓글, 답글 수 또는 레포의 글 수)."""
    where = f"{row.get('repo')}#{row['number']}" if row.get("number") is not None else str(row.get("repo"))
    if "comments_total" in row:
        return (f"{where} 댓글 {row.get('comments_got')}/{row.get('comments_total')}, "
                f"답글 {row.get('replies_got')}/{row.get('replies_total')}")
    return f"{where} {row.get('reason', '')}".strip()


def alerts_for(report: dict) -> list[str]:
    """통합 보고서에서 알릴 일을 고른다."""
    out = []
    s1 = report.get("S1") or {}
    for name, v in (s1.get("sources") or {}).items():
        if v.get("status") == "FAILED":
            out.append(f"{name} 실패: {v.get('error')}")
        elif v.get("status") == "PARTIAL":
            failed = v.get("failure_count", len(v.get("failures") or []))
            verify = v.get("verify_failed_count", len(v.get("verify_failed") or []))
            parts = []
            if failed:
                parts.append(f"수집 실패 {failed}건")
            if verify:  # 받은 글, 댓글 수가 전체 수와 다름. 어느 글인지 3건까지 보여 준다
                examples = ", ".join(_verify_example(r) for r in (v.get("verify_failed") or [])[:3])
                parts.append(f"검증 실패 {verify}건({examples})")
            if v.get("stopped"):
                parts.append(f"멈춤: {v['stopped']}")
            out.append(f"{name} 일부 실패: " + ", ".join(parts))
    h = report.get("handoff") or {}
    if h and h.get("status") not in ("done", "dry_run"):
        out.append(f"건네기 {h.get('status')}: {h.get('note') or h.get('error') or ''}")
    return out


def send(run_id: str, messages: list[str], dry_run: bool = False) -> dict:
    if not messages:
        return {"sent": False, "reason": "알릴 일 없음"}
    text = f"[샤라웃 매일 파이프라인] run {run_id}\n" + "\n".join(f"- {m}" for m in messages)
    url = os.environ.get("ALERT_WEBHOOK_URL")
    if dry_run or not url:
        print(text)
        return {"sent": False, "reason": "dry_run" if dry_run else "ALERT_WEBHOOK_URL 없음", "messages": messages}
    try:
        r = httpx.post(url, json={"content": text[:1900], "text": text}, timeout=10)
        return {"sent": r.status_code < 300, "status": r.status_code, "messages": messages}
    except httpx.HTTPError as e:
        return {"sent": False, "reason": f"{type(e).__name__}: {e}"[:200], "messages": messages}


if __name__ == "__main__":
    print(json.dumps(send("test", ["알림 시험"], dry_run=True), ensure_ascii=False))
