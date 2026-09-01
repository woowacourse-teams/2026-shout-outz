-- 이미지 업로드, 처리, 조회에 필요한 미디어 메타데이터 테이블

create table media_metadata (
    id              bigint generated always as identity primary key,
    uploaded_by     bigint references users(id),
    purpose         varchar(32) not null,
    s3_key          varchar(1024) not null,
    original_filename varchar(255),
    mime_type       varchar(100) not null,
    size_bytes      bigint not null,
    status          varchar(20) not null default 'PENDING_UPLOAD',
    expires_at      timestamptz not null,
    failure_reason  text,
    uploaded_at     timestamptz,
    created_at      timestamptz not null default timezone('utc', now()),
    updated_at      timestamptz not null default timezone('utc', now()),

    constraint media_metadata_s3_key_unique unique (s3_key),
    constraint media_metadata_s3_key_check check (
        char_length(btrim(s3_key)) > 0 and s3_key like 'media/%'
    ),
    constraint media_metadata_mime_type_check check (char_length(btrim(mime_type)) > 0),
    constraint media_metadata_purpose_check check (
        purpose in (
            'USER_AVATAR',
            'PROJECT_THUMBNAIL',
            'PROJECT_DESCRIPTION',
            'POST_CONTENT'
        )
    ),
    constraint media_metadata_size_bytes_positive check (size_bytes > 0),
    constraint media_metadata_status_check check (
        status in (
            'PENDING_UPLOAD',
            'PROCESSING',
            'READY',
            'FAILED',
            'EXPIRED'
        )
    ),
    constraint media_metadata_failure_reason_check check (
        (status = 'FAILED' and failure_reason is not null and char_length(btrim(failure_reason)) > 0)
        or (status <> 'FAILED' and failure_reason is null)
    )
);

create index media_metadata_pending_upload_expires_at_idx
    on media_metadata (expires_at)
    where status = 'PENDING_UPLOAD';

create index media_metadata_purpose_status_idx
    on media_metadata (purpose, status);

create index media_metadata_uploaded_by_idx
    on media_metadata (uploaded_by);

-- 단일 이미지만 갖는 프로젝트 썸네일과 사용자 프로필 이미지는
-- 별도 매핑 테이블 대신 각 도메인 테이블이 미디어를 직접 참조한다.
-- 초기 스키마의 URL 컬럼은 호환 레이어로 유지하지 않고 미디어 ID 참조로 교체한다.
alter table projects
    drop column thumbnail_url;

alter table projects
    add column thumbnail_media_id bigint;

alter table projects
    add constraint projects_thumbnail_media_fk
        foreign key (thumbnail_media_id)
        references media_metadata(id)
        on delete set null;

create index projects_thumbnail_media_id_idx
    on projects (thumbnail_media_id);

alter table user_profiles
    drop column avatar_url;

alter table user_profiles
    add column avatar_media_id bigint;

alter table user_profiles
    add constraint user_profiles_avatar_media_fk
        foreign key (avatar_media_id)
        references media_metadata(id)
        on delete set null;

create index user_profiles_avatar_media_id_idx
    on user_profiles (avatar_media_id);

-- 포스트 본문에서 여러 미디어를 연결하기 위한 매핑 테이블이다.
create table post_media (
    post_id            bigint      not null references posts(id) on delete cascade,
    media_metadata_id  bigint      not null references media_metadata(id) on delete cascade,
    display_order      smallint    not null default 0,
    created_at         timestamptz not null default now(),
    updated_at         timestamptz not null default now(),

    constraint post_media_pk primary key (post_id, media_metadata_id),
    constraint post_media_display_order_check check (display_order >= 0)
);

create index post_media_media_metadata_id_idx
    on post_media (media_metadata_id);

create index post_media_post_display_order_idx
    on post_media (post_id, display_order, media_metadata_id);
