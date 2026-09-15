CREATE INDEX idx_project_members_user_id_project_id
    ON project_members (user_id, project_id);

CREATE INDEX idx_archived_project_members_matched_user_id_project_id
    ON woowa_archived_project_members (matched_user_id, project_id)
    WHERE matched_user_id IS NOT NULL;
