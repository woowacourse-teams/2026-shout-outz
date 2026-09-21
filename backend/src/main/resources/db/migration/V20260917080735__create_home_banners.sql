CREATE TABLE home_banners (
    id                  bigint generated always as identity primary key,
    media_id            bigint       not null references media_metadata(id),
    destination_type    varchar(20)  not null,
    target_type         varchar(20),
    target_id           bigint,
    link_type           varchar(20),
    link_url            varchar(2048),
    display_order       integer      not null default 0,
    active              boolean      not null default true,
    created_by          bigint       not null references users(id),
    created_at          timestamptz  not null default now(),
    updated_at          timestamptz  not null default now(),

    constraint home_banners_display_order_check check (display_order >= 0),
    constraint home_banners_destination_type_check check (destination_type in ('TARGET', 'URL')),
    constraint home_banners_target_type_check check (
        target_type is null or target_type in ('NEWS', 'PROJECT', 'FEED')
    ),
    constraint home_banners_link_type_check check (
        link_type is null or link_type in ('INTERNAL_PATH', 'EXTERNAL_URL')
    ),
    constraint home_banners_destination_check check (
        (
            destination_type = 'TARGET'
            and target_type is not null
            and target_id is not null
            and target_id > 0
            and link_type is null
            and link_url is null
        )
        or
        (
            destination_type = 'URL'
            and target_type is null
            and target_id is null
            and link_type is not null
            and link_url is not null
            and char_length(btrim(link_url)) > 0
        )
    )
);

CREATE INDEX home_banners_public_order_idx
    ON home_banners (display_order, id)
    WHERE active = true;

CREATE INDEX home_banners_media_id_idx
    ON home_banners (media_id);
