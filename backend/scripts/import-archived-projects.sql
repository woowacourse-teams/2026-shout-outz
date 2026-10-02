-- 이전 기수 아카이브 데이터를 운영 DB에 반영하는 psql 실행 스크립트
--
-- 실행 전 같은 디렉터리에 아래 파일이 있어야 한다.
--   archived-projects.sql          generate-archived-projects.py 산출물
--   match-archived-members.sql     기존 OAuth 회원 연결용 백필
--
-- 이 파일은 순수 SQL이 아니라 psql의 \ir 명령을 사용하는 실행 파일이다.

\set ON_ERROR_STOP on

DO $$
BEGIN
    IF to_regclass('public.projects') IS NULL THEN
        RAISE EXCEPTION 'projects 테이블이 없습니다. 운영 DB 마이그레이션을 먼저 적용하세요.';
    END IF;

    IF to_regclass('public.woowa_archived_project_members') IS NULL THEN
        RAISE EXCEPTION 'woowa_archived_project_members 테이블이 없습니다. 운영 DB 마이그레이션을 먼저 적용하세요.';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'projects'
          AND column_name = 'thumbnail_media_id'
    ) THEN
        RAISE EXCEPTION '현재 운영 DB가 미디어 스키마가 아닙니다. thumbnail_media_id 컬럼이 필요합니다.';
    END IF;
END
$$;

\echo '아카이브 프로젝트와 멤버 Import 시작'
\ir archived-projects.sql

\echo '기존 GitHub OAuth 회원 매칭 시작'
\ir match-archived-members.sql

\echo '아카이브 Import 완료'
