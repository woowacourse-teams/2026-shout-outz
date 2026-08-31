-- 포스트 본문에서 여러 미디어를 연결하기 위한 매핑 테이블이다.

create table post_media (
    post_id            bigint      not null references posts(id) on delete cascade,
    media_metadata_id  bigint      not null references media_metadata(id) on delete cascade,
    display_order      smallint    not null default 0,
    created_at          timestamptz not null default now(),
    updated_at          timestamptz not null default now(),

    constraint post_media_pk primary key (post_id, media_metadata_id),
    constraint post_media_display_order_check check (display_order >= 0)
);

create index post_media_media_metadata_id_idx
    on post_media (media_metadata_id);

create index post_media_post_display_order_idx
    on post_media (post_id, display_order, media_metadata_id);
