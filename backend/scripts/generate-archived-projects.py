#!/usr/bin/env python3
"""이전 기수 팀 프로젝트 데이터(prev-crew.json)를 SQL INSERT 문으로 변환한다.

Dropit의 scripts/generate-prev-crew.mjs 가 GitHub에서 수집한 json 을 입력으로 받아
projects, woowa_archived_project_members 두 테이블에 넣을 SQL 을 만든다.

사용법:
    python3 generate-archived-projects.py <prev-crew.json> > archived-projects.sql

생성된 SQL 은 로컬 검증 -> dev -> prod 순으로 동일하게 실행한다.
"""

import json
import re
import sys

# ---------------------------------------------------------------------------
# RFC 논의 결과에 따라 바뀔 수 있는 값들.
# 결론이 바뀌면 여기만 수정하면 된다.
# ---------------------------------------------------------------------------
SERVICE_STATUS = "CLOSED"       # 운영 여부는 배포 URL 확인 후 수동으로 OPERATING 수정
APPROVAL_STATUS = "APPROVED"    # 심사를 거치지 않고 들어오는 데이터
STAR_SYNCED_AT = "2026-08-07"   # prev-crew.json 수집 시점
STRIP_YEAR_PREFIX = True        # repo 의 연도 접두사를 떼고 slug 로 사용


def quote(value):
    """PostgreSQL 문자열 리터럴로 변환한다.

    readme 에 작은따옴표가 65개 있어 이스케이프하지 않으면 SQL 이 깨진다.
    작은따옴표를 두 개로 바꾸는 것이 PostgreSQL 의 이스케이프 방식이다.
    줄바꿈은 문자열 리터럴 안에 그대로 들어가도 문제없다.
    """
    if value is None or value == "":
        return "NULL"
    return "'" + str(value).replace("'", "''") + "'"


def parse_cohort(generation):
    """'7기' -> 7"""
    matched = re.match(r"(\d+)기", generation or "")
    if not matched:
        raise ValueError(f"기수를 파싱할 수 없습니다: {generation!r}")
    return int(matched.group(1))


def to_slug(repo):
    """'2025-ah-madda' -> 'ah-madda', '2025-hEARit' -> 'hearit'

    slug 의 CHECK 제약이 '^[a-z0-9]+(-[a-z0-9]+)*$' 로 소문자만 허용한다.
    repo 명에 대문자가 섞인 경우가 3건(hEARit, Todok-Todok, Turip) 있어 소문자로 변환한다.
    연도를 떼고 소문자로 바꿔도 52개가 전부 고유해 UNIQUE 제약과 충돌하지 않는다.
    """
    slug = repo
    if STRIP_YEAR_PREFIX:
        slug = re.sub(r"^\d{4}-", "", slug)
    return slug.lower()


def extract_github_id(avatar_url):
    """'https://avatars.githubusercontent.com/u/108217858?v=4' -> '108217858'"""
    matched = re.search(r"/u/(\d+)", avatar_url or "")
    if not matched:
        raise ValueError(f"GitHub 계정 ID 를 추출할 수 없습니다: {avatar_url!r}")
    return matched.group(1)


def is_bot(contributor):
    """봇 계정을 판별한다.

    github-actions[bot], dependabot[bot], Copilot 은 아바타 URL 이
    사용자(/u/)가 아니라 GitHub App(/in/) 경로다. 총 6건이 걸러진다.
    """
    return "/in/" in (contributor.get("avatarUrl") or "")


# 배너가 아닌 장식용 이미지들. README 상단에 자주 등장해 대표 이미지 선택을 방해한다.
NOT_THUMBNAIL_PATTERNS = (
    "shields.io",   # 빌드 상태, 라이선스 등 뱃지
    "avatars.",     # GitHub 프로필 사진
    "/badges/",     # 앱스토어 다운로드 버튼 (play.google.com, apple.com)
    "/icons/",      # 기술 스택 아이콘
)


def pick_thumbnail(images):
    """대표 이미지를 고른다.

    images 는 README 에 등장하는 모든 이미지를 문서 순서대로 담은 배열이다.
    README 는 보통 최상단에 배너를 두므로 첫 번째가 대표 이미지일 가능성이 높다.
    다만 뱃지나 아이콘이 앞에 오는 경우가 있어 걸러낸다.

    자동으로 완벽히 판별할 수는 없으므로, service_status 를 확인할 때
    52건을 함께 눈으로 검수하는 것을 전제로 한다.
    """
    for url in images or []:
        if any(pattern in url for pattern in NOT_THUMBNAIL_PATTERNS):
            continue
        return url
    return None


def build_project_insert(project):
    columns = (
        "thumbnail_url, cohort, registered_by, team_name, slug, title, tagline, "
        "star_count, star_synced_at, service_status, approval_status, "
        "description_md, github_repository_url, deployment_url, created_at"
    )
    values = ", ".join([
        quote(pick_thumbnail(project.get("images"))),
        str(parse_cohort(project["generation"])),
        "NULL",                                  # registered_by: ARCHIVED 는 null 로 취급
        quote(project["title"]),                 # team_name: 팀명 정보가 없어 title 사용
        quote(to_slug(project["repo"])),
        quote(project["title"]),
        quote(project.get("about")),
        str(project.get("stars", 0)),
        quote(STAR_SYNCED_AT),
        quote(SERVICE_STATUS),
        quote(APPROVAL_STATUS),
        quote(project.get("readme")),
        quote(project.get("githubUrl")),
        "NULL",                                  # deployment_url: json 에 없음
        quote(f"{project['year']}-01-01"),       # created_at: year 만 있어 연초로 근사
    ])
    return f"INSERT INTO projects ({columns}) VALUES ({values});"


def build_member_insert(slug, contributor, display_order):
    columns = (
        "project_id, matched_user_id, github_account_id, github_login, "
        "display_name, avatar_url, github_profile_url, display_order"
    )
    values = ", ".join([
        f"(SELECT id FROM projects WHERE slug = {quote(slug)})",
        "NULL",                                  # matched_user_id: 로그인 시 매칭
        quote(extract_github_id(contributor["avatarUrl"])),
        quote(contributor["login"]),
        quote(contributor.get("name")),
        quote(contributor.get("avatarUrl")),
        quote(contributor.get("htmlUrl")),
        str(display_order),
    ])
    return f"INSERT INTO woowa_archived_project_members ({columns}) VALUES ({values});"


def generate(projects):
    statements = []
    member_count = 0
    bot_count = 0

    # projects 를 먼저 넣어야 멤버가 project_id 를 참조할 수 있다.
    for project in projects:
        statements.append(build_project_insert(project))

    statements.append("")

    for project in projects:
        slug = to_slug(project["repo"])
        display_order = 0
        for contributor in project.get("contributors") or []:
            if is_bot(contributor):
                bot_count += 1
                continue
            statements.append(build_member_insert(slug, contributor, display_order))
            display_order += 1
            member_count += 1

    return statements, member_count, bot_count


def main():
    if len(sys.argv) != 2:
        print(f"사용법: {sys.argv[0]} <prev-crew.json>", file=sys.stderr)
        return 1

    with open(sys.argv[1], encoding="utf-8") as file:
        projects = json.load(file)

    statements, member_count, bot_count = generate(projects)

    print("-- 이전 기수 팀 프로젝트 아카이브 데이터")
    print(f"-- 입력: {sys.argv[1]}")
    print(f"-- 프로젝트 {len(projects)}건, 멤버 {member_count}건 (봇 {bot_count}건 제외)")
    print("-- 생성: backend/scripts/generate-archived-projects.py")
    print()
    # 하나라도 실패하면 전체를 되돌린다. 부분 적재 상태를 만들지 않기 위함.
    print("BEGIN;")
    print()
    print("\n".join(statements))
    print()
    print("COMMIT;")

    print(
        f"프로젝트 {len(projects)}건, 멤버 {member_count}건 생성 (봇 {bot_count}건 제외)",
        file=sys.stderr,
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
