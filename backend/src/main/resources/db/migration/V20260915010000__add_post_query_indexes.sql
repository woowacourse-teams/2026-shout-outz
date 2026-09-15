create index posts_active_created_at_id_idx
    on posts (created_at desc, id desc)
    where deleted_at is null;

create index post_categories_category_id_post_id_idx
    on post_categories (category_id, post_id);
