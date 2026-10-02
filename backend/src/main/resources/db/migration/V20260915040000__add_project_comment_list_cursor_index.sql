CREATE INDEX project_comments_project_parent_created_at_id_idx
    ON project_comments (project_id, parent_id, created_at, id);
