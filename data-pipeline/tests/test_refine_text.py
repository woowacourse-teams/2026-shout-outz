"""정제 공통 정리, 읽는 글자, 제목 손질, 민감 정보 가리기, 시각(09 문서 2-3, 3-2, 3-4, R21)."""

from refine.mask import mask
from refine.text import clean_markdown, clean_plain, clean_title, readable_chars, split_fences, to_utc
from refine.titles import first_h1, strip_title


# 공통 정리(3-2) ---------------------------------------------------------------------------------------------------

def test_clean_markdown_comments_tags_images_and_blank_lines():
    text = ("<!-- 이슈 번호를 적어 주세요 -->\r\n<div align=\"center\">\r\n\r\n\r\n# 찰칵 \r\n"
            "![ERD](https://x/erd.png) ![](https://img.shields.io/badge)\n<img src=\"a.png\" alt=\"로고\"> 줄<br>바꿈\n\n\n\n끝  ")
    assert clean_markdown(text) == "# 찰칵\n[이미지: ERD]\n[이미지: 로고] 줄\n바꿈\n\n끝"


def test_clean_markdown_keeps_code_blocks_and_inline_code():
    text = "앞 <b>굵게</b>\n```java\nList<String> a;   \n\n\n<!-- 코드 안 주석 -->\n```\n`Map<K, V>` 뒤"
    assert clean_markdown(text) == "앞 굵게\n```java\nList<String> a;   \n\n\n<!-- 코드 안 주석 -->\n```\n`Map<K, V>` 뒤"


def test_clean_markdown_front_matter_only_for_files():
    text = "---\ntitle: 문서\n---\n본문\n\n---\n가로줄 뒤"
    assert clean_markdown(text, front_matter=True) == "본문\n\n---\n가로줄 뒤"
    assert clean_markdown(text).startswith("---\ntitle: 문서")


def test_clean_markdown_wiki_links_only_when_asked():
    assert clean_markdown("[[팀 규칙]] 과 [[보기|Team-Rules]]", wiki_links=True) == "팀 규칙 과 보기"
    assert clean_markdown("[[${value}]]") == "[[${value}]]"


def test_clean_markdown_characters():
    assert clean_markdown("가​나 다　라 ﻿") == "가나 다 라"
    nfd = "한글"
    import unicodedata
    assert clean_markdown(unicodedata.normalize("NFD", nfd)) == nfd


def test_split_fences_unclosed_fence_is_code_to_end():
    segs = split_fences("글\n```\n코드\n끝")
    assert segs == [(False, "글"), (True, "```\n코드\n끝")]


def test_clean_plain_keeps_angle_brackets():
    assert clean_plain("List<String> 은\r\n\r\n\r\n좋다  ") == "List<String> 은\n\n좋다"


# 읽는 글자(3-4, R13-5) --------------------------------------------------------------------------------------------

def test_readable_chars_skips_markup_links_images_and_hashes():
    sha = "a" * 40
    text = f"# 제목\n- [링크 글](https://example.com/very/long/url)\n[이미지: 그림]\n커밋 {sha} 끝"
    n, hex_chars = readable_chars(text)
    assert (n, hex_chars) == (len("제목링크글커밋끝"), 40)


# 제목(2-3) ----------------------------------------------------------------------------------------------------------

def test_clean_title():
    assert clean_title("  a\n\tb  ") == "a b"
    assert clean_title("A &amp; B", entities=True) == "A & B"
    assert clean_title("x" * 600) == "x" * 500


def test_strip_title_prefixes_and_suffixes():
    t, removed, why = strip_title("[BE] refactor: 놀이터 알림 전송 비동기 호출로 변경 (#512)")
    assert t == "놀이터 알림 전송 비동기 호출로 변경" and why is None
    assert [r["rule"] for r in removed] == ["대괄호", "작업 종류:", "괄호 안 이슈 참조"]


def test_strip_title_keeps_emoji_and_topic_words():
    assert strip_title("✅ [BE] 코딩 컨벤션")[0] == "✅ 코딩 컨벤션"
    assert strip_title("JPA: 영속성 컨텍스트 기초")[0] == "JPA: 영속성 컨텍스트 기초"
    assert strip_title("CI/CD 구축")[0] == "CI/CD 구축"
    assert strip_title("Feature/#801 로그인")[0] == "로그인"
    assert strip_title(":sparkles: 칵테일픽")[0] == "칵테일픽"
    assert strip_title("[RELEASE]")[0] == "[RELEASE]"
    assert strip_title("로그인 고침 (by 레오)")[0] == "로그인 고침"


def test_strip_title_no_title():
    assert strip_title("Feature/17")[2] == "empty"
    assert strip_title("??")[2] == "empty"
    assert strip_title("release: 2.1.2")[2] == "date_only"
    assert strip_title("2022 07 12(화)")[2] == "date_only"
    assert strip_title("[3주차 공통회의] 2021.07.12~07.18")[2] == "date_only"


def test_first_h1_skips_code_comments_and_symbol_only_headings():
    text = "<!-- # 주석 -->\n```sh\n# 셸 주석\n```\n# 🚀\n# [![로고](a.png)](x)\n소개\n===\n# 진짜 제목"
    assert first_h1(text) == ("소개", 7)


# 시각(2-3-2) ----------------------------------------------------------------------------------------------------------

def test_to_utc():
    assert to_utc("2026-11-01T20:37:57+09:00") == ("2026-11-01T11:37:57Z", None)
    assert to_utc("2026-09-01T06:52:32.919945") == ("2026-09-01T06:52:32Z", "UTC")
    assert to_utc("2026-10-06T07:46:40.427Z") == ("2026-10-06T07:46:40Z", None)
    assert to_utc("2026-11-01 09:00:00", "Asia/Seoul") == ("2026-11-01T00:00:00Z", "Asia/Seoul")
    assert to_utc("2026-11-01") == ("2026-10-31T15:00:00Z", "Asia/Seoul")
    assert to_utc("어제") == (None, None)


# 민감 정보(R21) ------------------------------------------------------------------------------------------------------

def test_mask_kinds():
    text = ("문의 crew@woowa.co.kr, 010-1234-5678, 02-123-4567, 900101-1234567\n"
            "key AKIA" + "A" * 16 + " url mysql://root:pass1234@db.example.com/x\n"
            "password: abc12345, password=${DB_PASSWORD}\n서버 54.180.1.2:8080 내부 192.168.0.1")
    out, counts = mask(text)
    assert "crew@" not in out and "1234-5678" not in out and "1234567" not in out
    assert "AKIA" not in out and "pass1234" not in out and "abc12345" not in out
    assert "${DB_PASSWORD}" in out and "192.168.0.1" in out and "[IP]:8080" in out
    assert "mysql://[비밀 값]@db.example.com" in out
    assert counts == {"이메일": 1, "전화번호": 2, "주민번호": 1, "비밀 값": 3, "IP": 1}


def test_mask_private_key_block_whole():
    text = "앞\n-----BEGIN RSA PRIVATE KEY-----\nMIIEow\nabc\n-----END RSA PRIVATE KEY-----\n뒤"
    out, counts = mask(text)
    assert out == "앞\n[비밀 값]\n뒤" and counts["비밀 값"] == 1


def test_mask_leaves_service_names_and_image_names():
    out, counts = mask("app@prod.service, logo@2x.webp, @coderabbitai")
    assert out == "app@prod.service, logo@2x.webp, @coderabbitai" and not counts
