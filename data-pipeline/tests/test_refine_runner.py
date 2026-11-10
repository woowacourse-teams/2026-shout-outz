"""정제 공통 흐름(09 문서 R4, R8, R17~R20): 고르기, 다시 정제, 실패, 사라짐과 돌아옴, 옮겨짐, 사본, 기다림, 고정, 잠금, 미리 보기."""

import json
from datetime import datetime, timedelta, timezone

from collector import lock
from collector.config import Settings
from collector.keys import raw_path
from collector.store import LocalStore

from refine import config as C
from refine.preview import run_preview
from refine.runner import RefineIndex, doc_path, run_refine

T0 = datetime(2026, 11, 11, 3, 0, tzinfo=timezone.utc)
LONG = "같은 레포 안에서 문서를 옮기거나 이름을 바꾸어도 이어진다. 그래서 다시 판정하지 않는다. " * 8
_DAY = [0]


def clock():
    """실행마다 하루씩 뒤에서 시작하고 부를 때마다 1초씩 간다."""
    _DAY[0] += 1
    box = [T0 + timedelta(days=_DAY[0])]

    def tick():
        box[0] += timedelta(seconds=1)
        return box[0]
    return tick


class Raw:
    """수집기 모양 원본 저장소(원본 파일과 색인)를 시험에서 만든다."""

    def __init__(self, store, source):
        self.store, self.source = store, source
        self.entries = {}

    def put(self, key, body, **fields):
        prev = self.entries.get(key)
        version = (prev or {}).get("version", 0) + 1
        path = raw_path(self.source, key)
        self.store.write_json(path, {"source": self.source, "external_key": key, "version": version,
                                     "fetched_at": "2026-11-10T00:00:00Z", **body})
        self.entries[key] = {"external_key": key, "path": path, "version": version, "fetched_at": "2026-11-10T00:00:00Z",
                             "gone": None, **fields}
        self.save()

    def gone(self, key, reason, **extra):
        self.entries[key]["gone"] = {"at": "2026-11-12T00:00:00Z", "reason": reason, **extra}
        self.save()

    def back(self, key):
        self.entries[key]["gone"] = None
        self.save()

    def save(self):
        lines = [json.dumps(self.entries[k], ensure_ascii=False) for k in sorted(self.entries)]
        self.store.write_bytes(f"index/{self.source}.jsonl", ("\n".join(lines) + "\n").encode("utf-8"))


def codemd(repo, path, content):
    return {"found_in": {"repo": f"woowacourse-teams/{repo}", "default_branch": "main", "commit": "c"},
            "file": {"path": path, "sha": "s", "size": 1, "last_commit": {"at": "2026-11-01T00:00:00Z", "author_name": "레오",
                                                                           "author_login": "leo"}},
            "content": content}


def key_md(repo, path):
    return f"woowacourse-teams/{repo}:{path}"


def go(tmp_path, sources=None, **kw):
    store = LocalStore(str(tmp_path))
    rep = run_refine(Settings(store_url=str(tmp_path)), sources, store=store, clock=clock(), **kw)
    return rep, store


def doc(store, source, key):
    return store.read_json(doc_path(source, key))


def line(store, source, key):
    return RefineIndex(store, source).load().get(key)


def changes(store, rep):
    b = store.read_bytes(f"refine/changes/{rep.run_id}.jsonl")
    return [json.loads(x) for x in (b or b"").decode().splitlines() if x]


def test_first_run_then_unchanged_then_ignored_then_changed(tmp_path):
    store = LocalStore(str(tmp_path))
    raw = Raw(store, "github-teamdoc")
    k = key_md("2026-zeta", "docs/a.md")
    raw.put(k, codemd("2026-zeta", "docs/a.md", "# 배포 전략\n" + LONG), sha="1")
    rep, _ = go(tmp_path, ["github-teamdoc"])
    s = rep.sources["github-teamdoc"]
    assert rep.status == "success" and s.counts["new"] == 1
    d = doc(store, "github-teamdoc", k)
    assert d["title"] == "배포 전략" and d["excluded"] is None and d["raw_version"] == 1 and len(d["history"]) == 1
    assert changes(store, rep)[0]["type"] == "new"
    assert store.read_json(f"refine/reports/{rep.run_id}.json")["sources"]["github-teamdoc"]["totals"]["passed"] == 1

    rep, _ = go(tmp_path, ["github-teamdoc"])
    assert rep.sources["github-teamdoc"].counts["unchanged"] == 1

    # 원본은 새 버전인데 해시에 넣는 글이 같다(작성자만 바뀜) → 무시한 변경
    body = codemd("2026-zeta", "docs/a.md", "# 배포 전략\n" + LONG)
    body["file"]["last_commit"]["author_login"] = "mia"
    raw.put(k, body, sha="2")
    rep, _ = go(tmp_path, ["github-teamdoc"])
    assert rep.sources["github-teamdoc"].counts["ignored"] == 1
    ign = [c for c in changes(store, rep) if c["type"] == "ignored"][0]
    assert ign["changed_fields"] == ["author"] and len(doc(store, "github-teamdoc", k)["history"]) == 1

    raw.put(k, codemd("2026-zeta", "docs/a.md", "# 배포 전략\n" + LONG + "더한 문단"), sha="3")
    rep, _ = go(tmp_path, ["github-teamdoc"])
    s = rep.sources["github-teamdoc"]
    assert s.counts["changed"] == 1 and s.changed_by["raw"] == 1
    ch = [c for c in changes(store, rep) if c["type"] == "changed"][0]
    assert ch["cause"] == "raw" and ch["raw_version"] == [2, 3] and ch["after"]["texts"][0].endswith("더한 문단")
    assert len(doc(store, "github-teamdoc", k)["history"]) == 2


def test_rules_version_bump_re_refines(tmp_path, monkeypatch):
    store = LocalStore(str(tmp_path))
    raw = Raw(store, "github-teamdoc")
    k = key_md("2026-zeta", "docs/a.md")
    raw.put(k, codemd("2026-zeta", "docs/a.md", "# 배포 전략\n" + LONG), sha="1")
    go(tmp_path, ["github-teamdoc"])
    monkeypatch.setitem(C.RULES_VERSION, "github-teamdoc", 2)
    monkeypatch.setattr("refine.runner.RULES_VERSION", C.RULES_VERSION)
    monkeypatch.setattr("refine.preview.RULES_VERSION", C.RULES_VERSION)
    rep, _ = go(tmp_path, ["github-teamdoc"])
    assert rep.sources["github-teamdoc"].counts["unchanged"] == 1  # 다시 정제했지만 결과가 같다
    assert line(store, "github-teamdoc", k)["rules_version"] == 2


def test_failure_is_retried_and_counts_consecutive(tmp_path):
    store = LocalStore(str(tmp_path))
    raw = Raw(store, "github-discussion")
    k = "woowacourse-teams/2026-zeta/discussions/1"
    d = {"number": 1, "title": "세션", "body": LONG, "url": "u", "createdAt": "2026-10-01T00:00:00Z", "category": {"name": "General"},
         "author": {"login": "a"}, "answer": None, "comments": []}  # 작성자 종류가 없는 옛 원본
    raw.put(k, {"found_in": {"repo": "woowacourse-teams/2026-zeta"}, "discussion": d})
    rep, _ = go(tmp_path, ["github-discussion"])
    assert rep.status == "partial_failure" and rep.sources["github-discussion"].counts["failed"] == 1
    ln = line(store, "github-discussion", k)
    assert ln["failed"]["reason"] == "no_author_type" and ln["content_hash"] is None and doc(store, "github-discussion", k) is None
    rep, _ = go(tmp_path, ["github-discussion"])
    assert line(store, "github-discussion", k)["failed"]["count"] == 2
    d["author"]["__typename"] = "User"
    raw.put(k, {"found_in": {"repo": "woowacourse-teams/2026-zeta"}, "discussion": d})
    rep, _ = go(tmp_path, ["github-discussion"])
    assert rep.status == "success" and rep.sources["github-discussion"].counts["new"] == 1
    assert line(store, "github-discussion", k)["failed"] is None


def test_gone_returned_and_gone_not_refined(tmp_path):
    store = LocalStore(str(tmp_path))
    raw = Raw(store, "github-teamdoc")
    k = key_md("2026-zeta", "docs/a.md")
    raw.put(k, codemd("2026-zeta", "docs/a.md", "# 배포 전략\n" + LONG), sha="1")
    go(tmp_path, ["github-teamdoc"])
    raw.gone(k, "repo_not_found")
    rep, _ = go(tmp_path, ["github-teamdoc"])
    s = rep.sources["github-teamdoc"]
    assert s.gone_keys["repo_not_found"] == [k] and line(store, "github-teamdoc", k)["gone"]["reason"] == "repo_not_found"
    d = doc(store, "github-teamdoc", k)
    assert d["gone"]["reason"] == "repo_not_found" and d["history"][-1]["gone"]["reason"] == "repo_not_found"
    assert [c["type"] for c in changes(store, rep)] == ["gone"]
    assert s.totals["gone"] == 1 and s.totals["docs"] == 0
    rep, _ = go(tmp_path, ["github-teamdoc"])
    assert rep.sources["github-teamdoc"].counts == {"new": 0, "changed": 0, "ignored": 0, "unchanged": 0, "failed": 0}
    raw.back(k)
    rep, _ = go(tmp_path, ["github-teamdoc"])
    assert rep.sources["github-teamdoc"].returned_keys == [k] and doc(store, "github-teamdoc", k)["gone"] is None
    assert [c["type"] for c in changes(store, rep)] == ["returned"]


def test_moved_by_repo_rename_and_by_file_move(tmp_path):
    store = LocalStore(str(tmp_path))
    raw = Raw(store, "github-teamdoc")
    old = key_md("2026-zeta", "docs/a.md")
    raw.put(old, codemd("2026-zeta", "docs/a.md", "# 배포 전략\n" + LONG), sha="1")
    moved_old = key_md("2026-zeta", "docs/old/b.md")
    raw.put(moved_old, codemd("2026-zeta", "docs/old/b.md", "# 회고\n" + LONG + "회고 글"), sha="2")
    go(tmp_path, ["github-teamdoc"])
    # 레포 이름이 바뀜(같은 경로의 새 키), 같은 레포 안에서 파일만 옮김(본문이 같은 새 키)
    new = key_md("2026-Zeta2", "docs/a.md")
    raw.gone(old, "repo_renamed", renamed_to="2026-Zeta2")
    raw.put(new, codemd("2026-Zeta2", "docs/a.md", "# 배포 전략\n" + LONG), sha="1")
    moved_new = key_md("2026-zeta", "docs/new/b.md")
    raw.gone(moved_old, "not_in_tree")
    raw.put(moved_new, codemd("2026-zeta", "docs/new/b.md", "# 회고\n" + LONG + "회고 글"), sha="2")
    rep, _ = go(tmp_path, ["github-teamdoc"])
    s = rep.sources["github-teamdoc"]
    assert {(m["from"], m["to"]) for m in s.moved} == {(old, new), (moved_old, moved_new)}
    assert doc(store, "github-teamdoc", new)["meta"]["moved_from"] == old
    assert doc(store, "github-teamdoc", moved_new)["meta"]["moved_from"] == moved_old
    assert sorted(c["type"] for c in changes(store, rep)) == ["moved", "moved", "new", "new"]
    assert not s.gone_keys


def test_duplicates_within_repo_and_kept_copy_returns(tmp_path):
    store = LocalStore(str(tmp_path))
    md, wk = Raw(store, "github-teamdoc"), Raw(store, "github-wiki")
    a = key_md("2026-zeta", "README.md")
    md.put(a, codemd("2026-zeta", "README.md", "# 서비스 소개\n" + LONG + " 더 긴 글"), sha="1")
    w = "woowacourse-teams/2026-zeta:wiki/Home.md"
    wk.put(w, {"found_in": {"repo": "woowacourse-teams/2026-zeta", "wiki_commit": "c"},
               "file": {"path": "Home.md", "sha": "s", "size": 1, "last_commit": {"at": "2026-11-01T00:00:00Z", "author_name": "레오"}},
               "content": "# 서비스 소개\n" + LONG}, sha="1")
    other = key_md("2026-other", "README.md")
    md.put(other, codemd("2026-other", "README.md", "# 서비스 소개\n" + LONG), sha="1")  # 다른 레포는 견주지 않는다
    rep, _ = go(tmp_path)
    dw = doc(store, "github-wiki", w)
    assert dw["excluded"]["rule"] == "duplicate" and dw["excluded"]["detail"] == "near"
    assert dw["excluded"]["evidence"]["duplicate_of"] == a and dw["excluded"]["evidence"]["kept_by"] == "longer"
    assert doc(store, "github-teamdoc", a)["excluded"] is None and doc(store, "github-teamdoc", other)["excluded"] is None
    assert rep.sources["github-wiki"].excluded_by_rule["duplicate"] == 1
    # 남긴 문서가 사라지면 사본이 대신 남는다(R18-7)
    md.gone(a, "not_in_tree")
    rep, _ = go(tmp_path)
    assert doc(store, "github-wiki", w)["excluded"] is None and line(store, "github-wiki", w).get("dup_of") is None
    assert rep.sources["github-wiki"].unexcluded == [{"external_key": w, "was": "duplicate"}]
    assert "unexcluded" in [c["type"] for c in changes(store, rep)]


def test_pr_duplicate_keeps_first_merged(tmp_path):
    store = LocalStore(str(tmp_path))
    raw = Raw(store, "github-pr")
    user = {"login": "leo", "__typename": "User"}

    def pr(n, merged, head):
        return {"found_in": {"repo": "woowacourse-teams/2022-nae-pyeon"}, "pull_request": {
            "number": n, "title": f"[BE] hotfix: 토큰 오발급 수정 (#{n})", "body": "## 작업\n토큰 재발급 조건을 고쳤다", "url": f"u{n}",
            "author": user, "mergedAt": merged, "baseRefName": "develop", "headRefName": head, "additions": 3, "deletions": 1,
            "changedFiles": 1, "labels": [], "comments": [], "reviews": [], "reviewThreads": [],
            "files": [{"path": "a.java", "additions": 3, "deletions": 1}]}}
    raw.put("woowacourse-teams/2022-nae-pyeon#619", pr(619, "2022-10-02T00:00:00Z", "hotfix/a"))
    raw.put("woowacourse-teams/2022-nae-pyeon#618", pr(618, "2022-10-01T00:00:00Z", "hotfix/b"))
    go(tmp_path, ["github-pr"])
    later = doc(store, "github-pr", "woowacourse-teams/2022-nae-pyeon#619")
    assert later["excluded"]["rule"] == "duplicate" and later["excluded"]["evidence"]["kept_by"] == "merged_first"
    assert doc(store, "github-pr", "woowacourse-teams/2022-nae-pyeon#618")["excluded"] is None


def test_tekotalk_waits_for_subtitle(tmp_path):
    store = LocalStore(str(tmp_path))
    raw = Raw(store, "youtube-tekotalk")
    video = {"id": "abc", "snippet": {"title": "[10분 테코톡] 나봄의 CORS", "description": "", "publishedAt": "2026-11-06T09:00:00Z"},
             "contentDetails": {"duration": "PT10M"}}
    raw.put("yt:abc", {"video": video, "subtitle": None}, has_subtitle=False)
    rep, _ = go(tmp_path, ["youtube-tekotalk"])
    s = rep.sources["youtube-tekotalk"]
    assert s.waiting == 1 and s.waiting_keys == ["yt:abc"] and s.counts["new"] == 0 and s.waiting_max_days is not None
    assert line(store, "youtube-tekotalk", "yt:abc") is None and rep.status == "success"
    raw.put("yt:abc", {"video": video, "subtitle": {"lang": "ko", "kind": "manual", "format": "vtt",
                                                   "vtt": "WEBVTT\n\n00:00:00.000 --> 00:00:01.000\n안녕하세요\n"}}, has_subtitle=True)
    rep, _ = go(tmp_path, ["youtube-tekotalk"])
    assert rep.sources["youtube-tekotalk"].counts["new"] == 1 and rep.sources["youtube-tekotalk"].waiting == 0


def test_cohort_1_7_blog_is_frozen(tmp_path):
    store = LocalStore(str(tmp_path))
    raw = Raw(store, "blog-post")
    k = "post:1"
    body = {"url": "https://old.tistory.com/1", "blog_root": "https://old.tistory.com", "platform": "generic", "found_in": [],
            "body_format": "text", "body": LONG, "archive": {"title": "옛 글"}, "origin": "archive-2026-10-22"}
    raw.put(k, body)
    go(tmp_path, ["blog-post"])
    assert line(store, "blog-post", k)["frozen"] is True
    raw.put(k, {**body, "body": LONG + "바뀜"})
    raw.gone(k, "http_404")
    rep, _ = go(tmp_path, ["blog-post"])
    assert rep.sources["blog-post"].counts["changed"] == 0 and doc(store, "blog-post", k)["gone"] is None
    rep, _ = go(tmp_path, ["blog-post"], only=[k])
    assert rep.sources["blog-post"].skipped == [{"external_key": k, "why": "고정된 1~7기 블로그"}]


def test_limit_only_after_collect_and_lock(tmp_path):
    store = LocalStore(str(tmp_path))
    raw = Raw(store, "github-teamdoc")
    keys = [key_md("2026-zeta", f"docs/{i}.md") for i in range(3)]
    for i, k in enumerate(keys):
        raw.put(k, codemd("2026-zeta", f"docs/{i}.md", f"# 문서 {i}\n" + LONG + str(i)), sha=str(i))
    rep, _ = go(tmp_path, ["github-teamdoc"], limit=2)
    assert rep.sources["github-teamdoc"].counts["new"] == 2 and rep.sources["github-teamdoc"].remaining == 1
    rep, _ = go(tmp_path, ["github-teamdoc"], only=[keys[0]])
    assert rep.sources["github-teamdoc"].counts["unchanged"] == 1 and rep.scope["only"] == 1
    # 수집 실행 보고서가 있으면 같은 run_id 로 묶는다
    collect_id = "2026-11-30T03-00-00Z-ec2-aaaa"
    store.write_json(f"reports/{collect_id}.json", {"run_id": collect_id, "started_at": "2026-11-30T03:00:00Z", "scope": {}})
    rep = run_refine(Settings(store_url=str(tmp_path)), ["github-teamdoc"], store=store, after_collect=True,
                     clock=lambda: datetime(2026, 11, 30, 4, 0, tzinfo=timezone.utc))
    assert rep.run_id == collect_id and rep.scope["joined_collect_run"] == collect_id
    rep2 = run_refine(Settings(store_url=str(tmp_path)), ["github-teamdoc"], store=store, after_collect=True,
                      clock=lambda: datetime(2026, 11, 30, 5, 0, tzinfo=timezone.utc))
    assert rep2.run_id != collect_id  # 이미 그 run_id 의 정제 보고서가 있으면 새 run_id
    got, _ = lock.acquire(store, "other", "home", T0 + timedelta(days=400))
    assert got
    rep, _ = go(tmp_path, ["github-teamdoc"])
    assert rep.status == "skipped" and rep.scope["lock_held_by"]["run_id"] == "other"


def test_after_collect_passes_over_runs_skipped_by_lock(tmp_path):
    """잠금 때문에 건너뛴 수집 실행에는 붙지 않고, 잠금 때문에 건너뛴 정제는 수집 실행의 run_id 를 차지하지 않는다."""
    store = LocalStore(str(tmp_path))
    raw = Raw(store, "github-teamdoc")
    k = key_md("2026-zeta", "docs/a.md")
    raw.put(k, codemd("2026-zeta", "docs/a.md", "# 문서\n" + LONG), sha="a")

    def at(h, m=0):
        return datetime(2026, 12, 1, h, m, tzinfo=timezone.utc)

    def collect_report(run_id, started, status, **scope):
        store.write_json(f"reports/{run_id}.json", {"run_id": run_id, "started_at": started.strftime("%Y-%m-%dT%H:%M:%SZ"),
                                                    "status": status, "scope": scope})

    def refine(now):
        return run_refine(Settings(store_url=str(tmp_path)), ["github-teamdoc"], store=store, after_collect=True,
                          clock=lambda: now)

    # 매일 실행이 수집하는 동안 누른 "지금 실행" 의 수집이 건너뛰어 더 늦은 보고서를 남겨도, 매일 실행의 정제는 자기 수집 실행에 붙는다
    daily = "2026-12-01T03-00-00Z-ec2-aaaa"
    collect_report(daily, at(3), "success")
    collect_report("2026-12-01T03-05-00Z-ec2-bbbb", at(3, 5), "skipped", lock_held_by={"run_id": daily})
    assert refine(at(4)).run_id == daily
    # "지금 실행" 의 수집이 도는 동안 다른 정제가 잠금 때문에 건너뛰면, 건너뜀 보고서는 새 run_id 로 남는다
    manual = "2026-12-01T05-00-00Z-ec2-cccc"
    collect_report(manual, at(5), "running")
    assert lock.acquire(store, manual, "ec2", at(5))[0]
    rep = refine(at(5, 10))
    assert rep.status == "skipped" and rep.run_id != manual and rep.scope["joined_collect_run"] is None
    assert store.read_bytes(f"refine/reports/{manual}.json") is None
    # 수집이 끝나 잠금이 풀린 뒤의 정제는 그 수집 실행에 붙는다
    lock.release(store, manual)
    collect_report(manual, at(5), "success")
    rep = refine(at(5, 30))
    assert rep.run_id == manual and rep.status == "success"


def test_preview_does_not_write_docs(tmp_path, monkeypatch):
    store = LocalStore(str(tmp_path))
    raw = Raw(store, "github-teamdoc")
    k = key_md("2026-zeta", "docs/daily-1.md")
    raw.put(k, codemd("2026-zeta", "docs/daily-1.md", "# 우리 팀 기록\n## 오늘 할 일\n" + LONG), sha="1")
    go(tmp_path, ["github-teamdoc"])
    before = store.read_bytes(doc_path("github-teamdoc", k))
    assert doc(store, "github-teamdoc", k)["excluded"]["rule"] == "daily_note"
    monkeypatch.setattr("refine.rules.DAILY_NAME", __import__("re").compile("없는낱말"))
    got = run_preview(Settings(store_url=str(tmp_path)), "github-teamdoc", store=store, clock=clock())
    assert got["counts"]["unexcluded"] == 1 and got["unexcluded"] == [{"external_key": k, "was": "daily_note"}]
    assert store.read_bytes(doc_path("github-teamdoc", k)) == before
    assert store.read_json(got["path"])["counts"]["unexcluded"] == 1
