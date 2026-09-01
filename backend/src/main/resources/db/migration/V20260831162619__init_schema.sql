
-- ============================================
-- 사용자
-- ============================================

CREATE TABLE users (
    id             BIGSERIAL    PRIMARY KEY,
    handle         VARCHAR(30)  NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    role           VARCHAR(20)  NOT NULL DEFAULT 'USER',
    last_login_at  TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at     TIMESTAMPTZ,
    purged_at      TIMESTAMPTZ,
    CHECK (status IN ('ACTIVE', 'BANNED', 'DELETED')),
    CHECK ((status = 'DELETED') = (deleted_at IS NOT NULL)),
    CHECK (role IN ('USER', 'ADMIN')),
    -- URL 경로(/@handle)로 쓰이므로 영숫자/하이픈/언더스코어만 허용한다.
    CHECK (handle ~ '^[A-Za-z0-9_-]{2,30}$')
);

CREATE TABLE oauth_accounts (
    id                   BIGSERIAL    PRIMARY KEY,
    user_id              BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider             VARCHAR(20)  NOT NULL,
    provider_account_id  VARCHAR(255) NOT NULL,
    provider_avatar_url  TEXT,
    last_synced_at       TIMESTAMPTZ,
    last_login_at        TIMESTAMPTZ,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (provider, provider_account_id)
);

CREATE TABLE user_profiles (
    user_id             BIGINT       PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    display_name        VARCHAR(50)  NOT NULL,
    user_type           VARCHAR(30)  NOT NULL DEFAULT 'GENERAL',
    track               VARCHAR(10),
    cohort              SMALLINT,
    bio                 TEXT,
    avatar_url          TEXT,
    github_profile_url  TEXT,
    blog_url            TEXT,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CHECK (user_type IN ('GENERAL', 'WOOWACOURSE_CREW', 'WOOWACOURSE_COACH')),
    CHECK (
        CASE user_type
            WHEN 'GENERAL'          THEN track IS NULL     AND cohort IS NULL
            WHEN 'WOOWACOURSE_CREW' THEN track IS NOT NULL AND cohort IS NOT NULL
            ELSE true
        END
    )
);

-- ============================================
-- 프로젝트
-- ============================================

CREATE TABLE tech_tags (
    id            BIGSERIAL PRIMARY KEY,
    slug          VARCHAR(50) NOT NULL UNIQUE,
    display_name  VARCHAR(50) NOT NULL UNIQUE,
    is_active     BOOLEAN     NOT NULL DEFAULT true
);

CREATE TABLE projects (
    id                     BIGSERIAL    PRIMARY KEY,
    thumbnail_url          TEXT,
    cohort                 SMALLINT     NOT NULL,
    registered_by          BIGINT       REFERENCES users(id),
    team_name              VARCHAR(50)  NOT NULL,
    slug                   VARCHAR(100) NOT NULL UNIQUE,
    title                  VARCHAR(100) NOT NULL,
    tagline                VARCHAR(200),
    star_count             INTEGER,
    star_synced_at         TIMESTAMPTZ,
    view_count             INTEGER      NOT NULL DEFAULT 0,
    service_status         VARCHAR(20)  NOT NULL,
    approval_status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    description_md         TEXT,
    github_repository_url  TEXT,
    deployment_url         TEXT,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at             TIMESTAMPTZ,
    CHECK (service_status IN ('OPERATING', 'CLOSED')),
    CHECK (approval_status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$')
);

CREATE TABLE project_tags (
    project_id     BIGINT   NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    tech_tag_id    BIGINT   NOT NULL REFERENCES tech_tags(id),
    display_order  SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (project_id, tech_tag_id)
);

CREATE TABLE project_deletions (
    id                   BIGSERIAL    PRIMARY KEY,
    project_id           BIGINT       NOT NULL,
    project_slug         VARCHAR(100) NOT NULL,
    project_title        VARCHAR(100) NOT NULL,
    deleted_by           BIGINT       REFERENCES users(id),
    deletion_type        VARCHAR(30)  NOT NULL,
    deletion_reason      TEXT,
    deleted_at           TIMESTAMPTZ  NOT NULL,
    restore_deadline_at  TIMESTAMPTZ,
    restored_by          BIGINT       REFERENCES users(id),
    restored_at          TIMESTAMPTZ,
    CHECK (deletion_type IN ('SELF_DELETE', 'ADMIN_DELETE', 'SYSTEM_PURGE'))
);

CREATE TABLE project_approval_histories (
    id           BIGSERIAL    PRIMARY KEY,
    project_id   BIGINT       NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    changed_by   BIGINT       REFERENCES users(id),
    from_status  VARCHAR(20),
    to_status    VARCHAR(20)  NOT NULL,
    reason       VARCHAR(500),
    changed_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CHECK (from_status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CHECK (to_status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE TABLE project_members (
    project_id     BIGINT   NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id        BIGINT   NOT NULL REFERENCES users(id),
    display_order  SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (project_id, user_id)
);

CREATE TABLE woowa_archived_project_members (
    id                  BIGSERIAL    PRIMARY KEY,
    project_id          BIGINT       NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    matched_user_id     BIGINT       REFERENCES users(id),
    github_account_id   VARCHAR(20)  NOT NULL,
    github_login        VARCHAR(39)  NOT NULL,
    display_name        VARCHAR(255),
    avatar_url          TEXT,
    github_profile_url  TEXT,
    display_order       SMALLINT     NOT NULL DEFAULT 0
);

-- ============================================
-- 포스트
-- ============================================

CREATE TABLE categories (
    id             BIGSERIAL PRIMARY KEY,
    slug           VARCHAR(50) NOT NULL UNIQUE,
    display_name   VARCHAR(50) NOT NULL UNIQUE,
    display_order  SMALLINT    NOT NULL DEFAULT 0,
    is_active      BOOLEAN     NOT NULL DEFAULT true
);

CREATE TABLE posts (
    id          BIGSERIAL   PRIMARY KEY,
    author_id   BIGINT      NOT NULL REFERENCES users(id),
    content     TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ
);

CREATE TABLE post_categories (
    post_id      BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    category_id  BIGINT NOT NULL REFERENCES categories(id),
    PRIMARY KEY (post_id, category_id)
);

-- ============================================
-- 리액션
-- ============================================

CREATE TABLE project_reactions (
    project_id     BIGINT      NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id        BIGINT      NOT NULL REFERENCES users(id),
    reaction_type  VARCHAR(20) NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (project_id, user_id, reaction_type)
);

CREATE TABLE post_reactions (
    post_id        BIGINT      NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id        BIGINT      NOT NULL REFERENCES users(id),
    reaction_type  VARCHAR(20) NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (post_id, user_id, reaction_type)
);

-- ============================================
-- 댓글
-- ============================================

CREATE TABLE project_comments (
    id          BIGSERIAL   PRIMARY KEY,
    project_id  BIGINT      NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    author_id   BIGINT      NOT NULL REFERENCES users(id),
    parent_id   BIGINT      REFERENCES project_comments(id),
    content     TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ
);

CREATE TABLE post_comments (
    id          BIGSERIAL   PRIMARY KEY,
    post_id     BIGINT      NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    author_id   BIGINT      NOT NULL REFERENCES users(id),
    parent_id   BIGINT      REFERENCES post_comments(id),
    content     TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ
);

-- ============================================
-- 조회수
-- ============================================

CREATE TABLE site_visitor_days (
    visitor_key_hash  TEXT        NOT NULL,
    visited_on        DATE        NOT NULL,
    first_seen_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (visitor_key_hash, visited_on)
);

CREATE TABLE project_view_days (
    project_id        BIGINT      NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    visitor_key_hash  TEXT        NOT NULL,
    viewed_on         DATE        NOT NULL,
    first_seen_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (project_id, visitor_key_hash, viewed_on)
);

-- CASCADE 로 부모가 지워질 때 자식 행을 찾기 위한 인덱스
CREATE INDEX idx_project_comments_project_id ON project_comments (project_id);
CREATE INDEX idx_post_comments_post_id ON post_comments (post_id);
CREATE INDEX idx_project_approval_histories_project_id ON project_approval_histories (project_id);
CREATE INDEX idx_woowa_archived_members_project_id ON woowa_archived_project_members (project_id);

-- 탈퇴 정리 배치용
CREATE INDEX idx_oauth_accounts_user_id ON oauth_accounts (user_id);

-- 대소문자 구분 없는 유일성
CREATE UNIQUE INDEX uq_users_handle_lower ON users (lower(handle));

-- 복구 대상 이력 조회용 (프로젝트당 미복구 삭제 이력은 최대 1건)
CREATE UNIQUE INDEX uq_project_deletions_pending_restore
    ON project_deletions (project_id)
    WHERE restored_at IS NULL AND deletion_type <> 'SYSTEM_PURGE';

-- 탈퇴 정리 배치 대상 조회용 (deleted_at < now() - 30일 AND purged_at IS NULL)
CREATE INDEX idx_users_pending_purge
    ON users (deleted_at)
    WHERE deleted_at IS NOT NULL AND purged_at IS NULL;
