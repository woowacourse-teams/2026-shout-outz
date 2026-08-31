alter table media_metadata
    drop constraint media_metadata_purpose_check;

alter table media_metadata
    add constraint media_metadata_purpose_check check (
        purpose in (
            'USER_AVATAR',
            'PROJECT_THUMBNAIL',
            'PROJECT_DESCRIPTION',
            'POST_CONTENT'
        )
    );
