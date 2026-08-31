create table media_metadata (
    id bigint generated always as identity primary key,
    purpose varchar(32) not null,
    s3_key varchar(1024) not null,
    original_filename varchar(255),
    mime_type varchar(100) not null,
    size_bytes bigint not null,
    status varchar(20) not null default 'PENDING_UPLOAD',
    expires_at timestamptz not null,
    failure_reason text,
    uploaded_at timestamptz,
    created_at timestamptz not null default timezone('utc', now()),
    updated_at timestamptz not null default timezone('utc', now()),

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
            'POST_CONTENT',
            'PROJECT_MEDIA'
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
