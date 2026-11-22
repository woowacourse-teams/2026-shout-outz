"""원천별 정제(09 문서 R5, R7, R9~R12, R14~R16)와 제외 규칙."""

import json

import pytest

from refine.model import RefineFailure, Waiting, finish
from refine.sources import make


def refine(source, raw, entry=None, dup_of=None):
    r = make(source)
    draft = r.build(raw, entry or {})
    return finish(r, raw.get("external_key", "k"), draft, 1, 1, dup_of).doc


def rule_of(doc):
    return (doc["excluded"] or {}).get("rule")


# 테코톡(R7, R8) ---------------------------------------------------------------------------------------------------

MANUAL_VTT = ("WEBVTT\nKind: captions\nLanguage: ko\n\n1\n00:00:00.001 --> 00:00:02.001\n안녕하세요 CORS 라는 주제로\n\n"
              "00:00:02.938 --> 00:00:04.398\n발표를 맡은 나봄입니다\n\n00:00:04.398 --> 00:00:05.418\nQ&amp;A 는 List&lt;String&gt; 처럼\n\n"
              "00:00:05.418 --> 00:00:07.000\nQ&amp;A 는 List&lt;String&gt; 처럼\n\nNOTE 메모\n\n00:00:07.000 --> 00:00:08.000\n끝입니다")
AUTO_VTT = ("WEBVTT\nKind: captions\nLanguage: ko\n\n00:00:00.000 --> 00:00:02.640 align:start position:0%\n \n[음악]\n\n"
            "00:00:02.640 --> 00:00:02.650 align:start position:0%\n[음악]\n \n\n"
            "00:00:11.020 --> 00:00:14.220 align:start position:0%\n \n오늘은<00:00:11.410><c> 트랜잭션을</c>\n\n"
            "00:00:14.220 --> 00:00:14.230 align:start position:0%\n오늘은 트랜잭션을\n \n\n"
            "00:00:14.230 --> 00:00:20.040 align:start position:0%\n오늘은 트랜잭션을\n설명하겠습니다\n")
BOILER = ("🙋‍♀️ 우아한테크코스의 크루들이 진행하는 10분 테크토크입니다. 🙋‍♂️\n\n'10분 테코톡'이란  우아한테크코스 과정을 진행하며 크루(수강생)들이 "
          "동료들과 학습한 내용을 공유하고 이야기하는 시간입니다. 서로가 성장하기 위해 지식을 나누고 대화하며 생각해보는 시간으로 자기 주도적인 "
          "성장을 지향하는 우아한테크코스의 문화 중 하나입니다.\n🌕우아한테크코스란 🌕\n")


def tekotalk(vtt, kind="manual", description=BOILER, subtitle=True):
    return {"source": "youtube-tekotalk", "external_key": "yt:abc", "video": {
        "id": "abc", "snippet": {"title": "[10분 테코톡] 🌳 나봄의 CORS", "description": description, "publishedAt": "2021-06-03T02:43:22Z"},
        "contentDetails": {"duration": "PT23M54S"}},
        "subtitle": {"lang": "ko", "kind": kind, "format": "vtt", "vtt": vtt} if subtitle else None}


def test_tekotalk_manual_subtitle():
    doc = refine("youtube-tekotalk", tekotalk(MANUAL_VTT, description=BOILER + "\nChapters:\n0:00 intro"))
    assert doc["title"] == doc["original_title"] == "[10분 테코톡] 🌳 나봄의 CORS"
    assert doc["url"] == "https://www.youtube.com/watch?v=abc" and doc["author"] is None and doc["repo"] is None
    assert doc["meta"] == {"video_id": "abc", "duration_sec": 1434, "subtitle_kind": "manual"}
    assert [p["kind"] for p in doc["parts"]] == ["description", "transcript"]
    assert doc["parts"][0]["text"] == "Chapters:\n0:00 intro"
    # 문장으로 잇고, 같은 줄은 하나만, 엔티티는 글자로(태그로 지워지지 않는다)
    assert doc["parts"][1]["text"] == "안녕하세요 CORS 라는 주제로 발표를 맡은 나봄입니다\nQ&A 는 List<String> 처럼 끝입니다"
    assert doc["body"].startswith("## 영상 설명\nChapters:") and "\n\n## 자막\n" in doc["body"]
    assert doc["excluded"] is None


def test_tekotalk_auto_subtitle_rolling_lines_and_sound():
    doc = refine("youtube-tekotalk", tekotalk(AUTO_VTT, kind="auto"))
    assert [p["kind"] for p in doc["parts"]] == ["transcript"]  # 공통 문구뿐인 설명은 조각이 없다
    assert doc["parts"][0]["text"] == "오늘은 트랜잭션을 설명하겠습니다"
    assert doc["body"] == "## 자막\n오늘은 트랜잭션을 설명하겠습니다"
    assert doc["trace"]["body"]["sound_lines_removed"] == 2 and doc["meta"]["subtitle_kind"] == "auto"


def test_tekotalk_waiting_and_empty_subtitle():
    r = make("youtube-tekotalk")
    assert r.waiting({"has_subtitle": False}) and not r.waiting({"has_subtitle": True})
    with pytest.raises(Waiting):
        r.build(tekotalk("", subtitle=False), {})
    with pytest.raises(RefineFailure) as e:
        r.build(tekotalk("WEBVTT\n\n00:00:00.000 --> 00:00:01.000\n[음악]\n"), {})
    assert e.value.reason == "empty_subtitle"


# 블로그(R5) ------------------------------------------------------------------------------------------------------

LONG = "사파리는 WebP 를 늦게 지원했다. 그래서 이미지 압축 방식을 바꿔야 했다. " * 8


def blog(body, fmt="html", platform="tistory", url="https://team.tistory.com/16", **more):
    return {"source": "blog-post", "external_key": "post:x", "url": url, "blog_root": "https://team.tistory.com", "platform": platform,
            "found_in": [{"blog_source_id": 1, "github_repository_url": "https://github.com/woowacourse-teams/2026-zeta"},
                         {"blog_source_id": 2, "github_repository_url": "https://github.com/woowacourse-teams/2026-alpha"}],
            "body_format": fmt, "body": body, **more}


def test_blog_tistory_body_title_time_repo():
    html = ("<html><head><meta property=\"og:title\" content=\"사파리에서도 WebP로 압축하기&amp;[4편]\">"
            "<meta property=\"article:published_time\" content=\"2026-11-01T20:37:57+09:00\"><title>x :: 블로그</title></head>"
            f"<body><div class=\"contents_style\"><h2>배경</h2><p>{LONG}</p><img src=\"a.png\" alt=\"그림\"></div>"
            "<div>카테고리의 다른 글</div></body></html>")
    doc = refine("blog-post", blog(html))
    assert doc["title"] == "사파리에서도 WebP로 압축하기&[4편]" and doc["published_at"] == "2026-11-01T11:37:57Z"
    assert doc["repo"] == "woowacourse-teams/2026-alpha" and doc["trace"]["repo"]["candidates"][1].endswith("zeta")
    assert doc["trace"]["body"]["method"] == "tistory contents_style"
    assert doc["body"].startswith("## 배경\n") and "[이미지: 그림]" in doc["body"] and "카테고리의 다른 글" not in doc["body"]
    assert doc["excluded"] is None and doc["author"] is None


def test_blog_velog_json_markdown_and_released_at():
    state = {"Post:1": {"__typename": "Post", "url_slug": "글-하나", "body": "# 원본 마크다운\n" + LONG}}
    html = (f"<html><head><meta property=\"og:title\" content=\"글 하나\"></head><body><script>window.__APOLLO_STATE__ = "
            f"{json.dumps(state, ensure_ascii=False)};</script><script>{{\"released_at\":\"2026-10-06T07:46:40.427Z\"}}</script></body></html>")
    doc = refine("blog-post", blog(html, platform="velog", url="https://velog.io/@team/%EA%B8%80-%ED%95%98%EB%82%98"))
    assert doc["body"].startswith("# 원본 마크다운") and doc["published_at"] == "2026-10-06T07:46:40Z"
    assert doc["trace"]["body"]["method"] == "velog JSON"


def test_blog_rilog_markdown_text_and_short():
    md = "---\ntitle: \"재미있는 썰\"\npublishedAt: \"2026-09-01T06:52:32.919945\"\n---\n# 왜 만들었나\n" + LONG
    doc = refine("blog-post", blog(md, fmt="markdown", platform="rilog"))
    assert doc["title"] == "재미있는 썰" and doc["published_at"] == "2026-09-01T06:52:32Z"
    assert doc["trace"]["published_at"]["assumed_tz"] == "UTC" and doc["body"].startswith("# 왜 만들었나")
    short = refine("blog-post", blog("짧은 메모", fmt="text", platform="generic",
                                     archive={"title": "노션 메모", "published_at": "2023-05-01T10:00:00"}, origin="archive-2026-10-22"))
    assert short["title"] == "노션 메모" and short["published_at"] == "2023-05-01T01:00:00Z"
    assert rule_of(short) == "short_body" and short["meta"]["origin"] == "archive-2026-10-22"


def test_blog_generic_html_with_trafilatura():
    html = (f"<html><head><title>범용 글</title><meta property=\"article:published_time\" content=\"2026-08-01\"></head><body>"
            f"<nav>메뉴</nav><article><h1>범용 글</h1><p>{LONG}</p><p>{LONG}</p></article><footer>바닥글</footer></body></html>")
    doc = refine("blog-post", blog(html, platform="generic", url="https://team.github.io/post/1"))
    assert doc["title"] == "범용 글" and "WebP" in doc["body"] and doc["trace"]["body"]["method"] == "trafilatura"
    assert doc["published_at"] == "2026-07-31T15:00:00Z"


def test_blog_shell_page_is_failure_not_short():
    with pytest.raises(RefineFailure) as e:
        make("blog-post").build(blog("<html><head><title>tistory</title></head><body><div>로그인</div></body></html>"), {})
    assert e.value.reason == "no_content"


# 코드 마크다운과 위키(R9, R14, R15, R13) ------------------------------------------------------------------------------

def codemd(path, content, repo="2026-zeta"):
    return {"source": "github-teamdoc", "external_key": f"woowacourse-teams/{repo}:{path}",
            "found_in": {"repo": f"woowacourse-teams/{repo}", "default_branch": "main", "commit": "c"},
            "file": {"path": path, "sha": "s", "size": len(content),
                     "last_commit": {"at": "2026-11-01T00:00:00Z", "author_name": "레오", "author_login": "leo"}},
            "content": content}


def wiki(path, content, repo="2026-zeta"):
    return {"source": "github-wiki", "external_key": f"woowacourse-teams/{repo}:wiki/{path}",
            "found_in": {"repo": f"woowacourse-teams/{repo}", "wiki_commit": "c"},
            "file": {"path": path, "sha": "s", "size": len(content), "last_commit": {"at": "2026-11-01T00:00:00Z", "author_name": "레오"}},
            "content": content}


def test_codemd_title_url_author_and_pass():
    doc = refine("github-teamdoc", codemd("docs/한글 문서.md", "# [BE] feat: 배포 전략 (#12)\n" + LONG))
    assert doc["original_title"] == "[BE] feat: 배포 전략 (#12)" and doc["title"] == "배포 전략"
    assert doc["url"] == "https://github.com/woowacourse-teams/2026-zeta/blob/HEAD/docs/%ED%95%9C%EA%B8%80%20%EB%AC%B8%EC%84%9C.md"
    assert doc["author"] == "leo" and doc["published_at"] == "2026-11-01T00:00:00Z" and doc["meta"] == {"path": "docs/한글 문서.md"}
    assert doc["trace"]["title"]["tried"][1] == {"step": "h1", "result": "used", "line": 1}
    assert doc["excluded"] is None


def test_codemd_path_title_is_not_stripped():
    doc = refine("github-teamdoc", codemd("docs/convention.md", LONG))
    assert doc["title"] == doc["original_title"] == "docs/convention.md" and doc["trace"]["title"]["removed"] == []
    assert doc["trace"]["title"]["tried"][-1] == {"step": "path", "result": "used"}


def test_codemd_path_rules_in_order():
    doc = refine("github-teamdoc", codemd(".github/ISSUE_TEMPLATE/bug.md", "---\ntitle: \"[BUG] 제목\"\n---\n## 설명\n"))
    assert doc["excluded"]["rule"] == "tool_dir" and doc["excluded"]["detail"] == ".github"
    assert doc["excluded"]["evidence"] == {"path": ".github/ISSUE_TEMPLATE/bug.md", "part": ".github"}
    assert "tool_dir" in doc["excluded"]["matched"]
    assert rule_of(refine("github-teamdoc", codemd("backend/AGENTS.md", LONG))) == "ai_instruction"
    assert rule_of(refine("github-teamdoc", codemd("eval/fixtures/README.md", "# 가짜\n" + LONG))) == "test_data"
    assert rule_of(refine("github-teamdoc", codemd("frontend/CHANGELOG.md", "# 변경\n" + LONG))) == "changelog"
    assert rule_of(refine("github-teamdoc", codemd("README.md", "# 앱\n" + LONG, repo="2026-zeta-old"))) == "old_repo"


def test_codemd_content_rules():
    spring = "# Getting Started\n\n### Reference Documentation\n" + LONG
    doc = refine("github-teamdoc", codemd("HELP.md", spring))
    assert doc["excluded"]["rule"] == "template_as_is" and doc["excluded"]["detail"] == "spring_help"
    assert rule_of(refine("github-teamdoc", codemd("docs/form.md", "# 회고\n## 잘한 점\n- \n## 아쉬운 점\n- [ ]\n"))) == "empty_form"
    links = "# 목록\n" + "\n".join(f"- [문서 {i}](https://docs.google.com/{i})" for i in range(5))
    assert rule_of(refine("github-teamdoc", codemd("docs/links.md", links))) == "link_list"
    daily = "# 데일리 스크럼\n## 오늘 할 일\n" + LONG
    assert rule_of(refine("github-teamdoc", codemd("docs/daily/1105.md", daily))) == "daily_note"
    adr = "# 구글 디코드 호출 제한\n## 배경\n" + LONG
    assert refine("github-teamdoc", codemd("docs/adr/limit-daily-calls.md", adr))["excluded"] is None


def test_codemd_short_body_counts_without_hash():
    content = "# 기록\n" + "승인 " + "a" * 64 + "\n짧은 글"
    doc = refine("github-teamdoc", codemd("llm-wiki/manifest.md", content))
    assert doc["excluded"]["rule"] == "short_body"
    assert doc["excluded"]["evidence"]["hash_chars_not_counted"] == 64


def test_wiki_title_links_frame_and_no_title():
    doc = refine("github-wiki", wiki("백엔드-컨벤션.md", "[[팀 규칙]] 을 따른다\n" + LONG))
    assert doc["title"] == "백엔드 컨벤션" and doc["url"] == "https://github.com/woowacourse-teams/2026-zeta/wiki/%EB%B0%B1%EC%97%94%EB%93%9C-%EC%BB%A8%EB%B2%A4%EC%85%98"
    assert doc["body"].startswith("팀 규칙 을 따른다") and doc["author"] == "레오"
    assert rule_of(refine("github-wiki", wiki("_Sidebar.md", LONG))) == "wiki_frame"
    d = refine("github-wiki", wiki("2022-07-21.md", LONG))
    assert d["excluded"]["rule"] == "no_title" and d["excluded"]["detail"] == "date_only"


# 디스커션(R10, R12) ------------------------------------------------------------------------------------------------

def disc(comments, title="세션과 JWT", category="이야기해봐요", body="인증 상태를 무엇으로 할지 정하고 싶어요. " + LONG):
    return {"source": "github-discussion", "external_key": "woowacourse-teams/2026-zeta/discussions/3",
            "found_in": {"repo": "woowacourse-teams/2026-zeta"},
            "discussion": {"number": 3, "title": title, "body": body, "url": "https://github.com/woowacourse-teams/2026-zeta/discussions/3",
                           "createdAt": "2026-10-25T15:55:25Z", "updatedAt": "2026-10-26T00:00:00Z", "category": {"name": category},
                           "author": {"login": "dev-a", "__typename": "User"}, "upvoteCount": 3, "answer": None, "comments": comments}}


def test_discussion_parts_order_bots_and_unknown_author():
    comments = [
        {"author": {"login": "dev-b", "__typename": "User"}, "body": "세션이 단순해요", "createdAt": "2026-10-26T01:10:00Z", "isAnswer": True,
         "replies": [{"author": None, "body": "좋아요", "createdAt": "2026-10-26T02:00:00Z"},
                     {"author": {"login": "bot", "__typename": "Bot"}, "body": "자동 답글", "createdAt": "2026-10-26T03:00:00Z"}]},
        {"author": {"login": "ci", "__typename": "Bot"}, "body": "봇 댓글", "createdAt": "2026-10-26T04:00:00Z", "isAnswer": False, "replies": []},
        {"author": {"login": "dev-c", "__typename": "User"}, "body": "  ", "createdAt": "2026-10-26T05:00:00Z", "isAnswer": False, "replies": []},
    ]
    doc = refine("github-discussion", disc(comments))
    assert [p["kind"] for p in doc["parts"]] == ["post", "comment", "reply"]
    assert doc["meta"] == {"number": 3, "category": "이야기해봐요", "comment_count": 1, "reply_count": 1, "has_answer": False}
    assert "## 댓글\n\n### dev-b(채택된 답변)\n세션이 단순해요\n\n#### (알 수 없는 사용자) 의 답글\n좋아요" in doc["body"]
    assert doc["parts"][2]["author"] is None


def test_discussion_rules_and_missing_author_type():
    d = refine("github-discussion", disc([], title="2023-07-12 회의", category="daily_meeting",
                                          body="## 오늘 할 일\n- 로그인\n## 어제 한 일\n- 설계\n" + LONG))
    assert d["excluded"]["rule"] == "daily_note"
    w = refine("github-discussion", disc([], body="Welcome! We're using Discussions as a place to connect with other members of our community."))
    assert w["excluded"]["detail"] == "discussion_welcome"
    raw = disc([{"author": {"login": "x"}, "body": "옛 원본", "createdAt": "2026-10-26T01:00:00Z", "isAnswer": False, "replies": []}])
    with pytest.raises(RefineFailure) as e:
        make("github-discussion").build(raw, {})
    assert e.value.reason == "no_author_type"


# PR(R11, R12, R16) --------------------------------------------------------------------------------------------------

USER = {"login": "leo", "__typename": "User"}
RABBIT = {"login": "coderabbitai", "__typename": "Bot"}


def pr(**over):
    p = {"number": 900, "title": "[FE] feat: 오늘 칸을 업로드 진입점으로 (#880)", "url": "https://github.com/woowacourse-teams/2026-zeta/pull/900",
         "author": USER, "createdAt": "2026-11-03T05:00:00Z", "mergedAt": "2026-11-03T05:29:00Z", "updatedAt": "2026-11-03T06:00:00Z",
         "baseRefName": "develop", "headRefName": "feat/880", "additions": 120, "deletions": 30, "changedFiles": 3,
         "labels": [{"name": "frontend"}],
         "body": ("## 작업 내용\n주간 기록의 오늘 칸을 바꿨습니다.\n\n## 관련 이슈\n- close #\n\n## 스크린샷\n\n## 체크\n- [ ] 남은 일\n- [x] 테스트\n"
                  "<!-- This is an auto-generated comment: release notes by coderabbit.ai -->\n## Summary by CodeRabbit\n- 요약\n"
                  "<!-- end of auto-generated comment: release notes by coderabbit.ai -->"),
         "comments": [{"author": USER, "body": "스크린샷 공유해 주세요", "createdAt": "2026-11-03T05:40:00Z"},
                      {"author": RABBIT, "body": "봇 요약 댓글", "createdAt": "2026-11-03T05:41:00Z"}],
         "reviews": [{"author": {"login": "rv", "__typename": "User"}, "state": "APPROVED", "body": "좋아요!", "submittedAt": "2026-11-03T05:50:00Z"},
                     {"author": {"login": "rv", "__typename": "User"}, "state": "APPROVED", "body": "", "submittedAt": "2026-11-03T05:51:00Z"}],
         "reviewThreads": [
             {"path": "a/Home.swift", "isResolved": True, "comments": [
                 {"author": {"login": "rv2", "__typename": "User"}, "body": "상수로 빼면?", "diffHunk": "\n".join(f"L{i}" for i in range(1, 15)),
                  "createdAt": "2026-11-03T05:45:00Z"},
                 {"author": USER, "body": "반영했습니다!", "diffHunk": "", "createdAt": "2026-11-03T05:52:00Z"}]},
             {"path": "b/Api.kt", "isResolved": False, "comments": [
                 {"author": RABBIT, "body": "널 처리가 필요합니다", "diffHunk": "@@ x", "createdAt": "2026-11-03T05:30:00Z"},
                 {"author": USER, "body": "여기는 널이 올 수 없어 두었습니다", "diffHunk": "", "createdAt": "2026-11-03T05:35:00Z"}]},
             {"path": "c/Only.kt", "isResolved": False, "comments": [
                 {"author": RABBIT, "body": "봇 혼자 단 대화", "diffHunk": "@@ y", "createdAt": "2026-11-03T05:31:00Z"}]}],
         "files": [{"path": "client/Home.swift", "additions": 100, "deletions": 20}, {"path": "client/yarn.lock", "additions": 20, "deletions": 10},
                   {"path": "client/Api.kt", "additions": 0, "deletions": 0}]}
    p.update(over)
    return {"source": "github-pr", "external_key": "woowacourse-teams/2026-zeta#900", "found_in": {"repo": "woowacourse-teams/2026-zeta"},
            "pull_request": p}


def test_pr_parts_body_and_meta():
    doc = refine("github-pr", pr())
    assert doc["title"] == "오늘 칸을 업로드 진입점으로" and doc["original_title"].startswith("[FE] feat:")
    kinds = [(p["kind"], bool(p.get("bot"))) for p in doc["parts"]]
    assert kinds == [("pr_body", False), ("comment", False), ("review", False), ("thread", True), ("thread_reply", False),
                     ("thread", False), ("thread_reply", False)]
    body = doc["parts"][0]["text"]
    assert "관련 이슈" not in body and "스크린샷" not in body and "Summary by CodeRabbit" not in body
    assert "- [ ] 남은 일" in body  # 내용이 있는 소제목 안의 체크 안 한 항목은 남는다
    home = next(p for p in doc["parts"] if p.get("path") == "a/Home.swift")
    assert home["code"] == "\n".join(f"L{i}" for i in range(5, 15)) and home["resolved"] is True
    assert "### rv, 승인\n좋아요!" in doc["body"]
    assert "coderabbitai(봇): 널 처리가 필요합니다\nleo: 여기는 널이 올 수 없어 두었습니다" in doc["body"]
    assert "봇 혼자 단 대화" not in doc["body"]
    m = doc["meta"]
    assert (m["comment_count"], m["review_count"], m["thread_count"]) == (1, 1, 2)
    assert m["code_lines"] == 120 and m["changed_files"][0] == "client/Home.swift" and m["labels"] == ["frontend"]
    assert doc["excluded"] is None


def test_pr_hash_ignores_bot_context_and_states():
    a = refine("github-pr", pr())
    b_raw = pr()
    b_raw["pull_request"]["reviewThreads"][1]["comments"][0]["body"] = "봇이 지적을 고쳐 씀"
    b_raw["pull_request"]["reviewThreads"][0]["isResolved"] = False
    b_raw["pull_request"]["labels"] = []
    assert refine("github-pr", b_raw)["content_hash"] == a["content_hash"]
    c_raw = pr()
    c_raw["pull_request"]["comments"].append({"author": USER, "body": "머지 뒤 댓글", "createdAt": "2026-11-04T00:00:00Z"})
    assert refine("github-pr", c_raw)["content_hash"] != a["content_hash"]


def test_pr_rules():
    bot = refine("github-pr", pr(author={"login": "dependabot", "__typename": "Bot"}, comments=[], reviews=[], reviewThreads=[]))
    assert bot["excluded"]["rule"] == "bot_pr"
    kept_bot = refine("github-pr", pr(author={"login": "github-actions", "__typename": "Bot"}))
    assert kept_bot["excluded"] is None  # 사람 글이 있는 봇 PR 은 남긴다
    rel = refine("github-pr", pr(baseRefName="backend-prod", headRefName="backend-dev"))
    assert rel["excluded"] == {"rule": "release_pr", "detail": None, "evidence": {"base_ref": "backend-prod", "head_ref": "backend-dev"},
                               "matched": ["release_pr"]}
    for head in ("fe/dev", "v2/develop", "release/1.2", "be"):
        assert rule_of(refine("github-pr", pr(headRefName=head))) == "release_pr", head
    assert rule_of(refine("github-pr", pr(title='Revert "로그인 고침"'))) == "revert_pr"
    empty = refine("github-pr", pr(additions=0, deletions=0, changedFiles=0, files=[]))
    assert empty["excluded"]["detail"] == "no_files"
    deps = refine("github-pr", pr(changedFiles=2, files=[{"path": "back/build.gradle", "additions": 1, "deletions": 1},
                                                          {"path": "front/package-lock.json", "additions": 9, "deletions": 9}]))
    assert deps["excluded"]["rule"] == "deps_pr"
    web = refine("github-pr", pr(title="Update README.md", body="", comments=[], reviews=[], reviewThreads=[]))
    assert web["excluded"]["rule"] == "template_as_is" and web["excluded"]["detail"] == "github_default_title"
    old = refine("github-pr", {**pr(), "found_in": {"repo": "woowacourse-teams/2026-career-form-old"}})
    assert old["excluded"]["rule"] == "old_repo"
    nt = refine("github-pr", pr(title="release: 2.1.2"))
    assert nt["excluded"]["rule"] == "no_title" and nt["excluded"]["detail"] == "date_only"


def test_pr_missing_author_type_is_failure():
    raw = pr(reviews=[{"author": {"login": "rv"}, "state": "APPROVED", "body": "x", "submittedAt": "2026-11-03T05:50:00Z"}])
    with pytest.raises(RefineFailure) as e:
        make("github-pr").build(raw, {})
    assert e.value.reason == "no_author_type"
