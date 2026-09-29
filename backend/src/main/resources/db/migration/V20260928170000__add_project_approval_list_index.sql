-- 관리자 프로젝트 심사 목록의 상태 필터와 등록 시각 커서 조회를 위한 부분 인덱스
CREATE INDEX idx_projects_approval_list
    ON projects (approval_status, created_at DESC, id DESC)
    WHERE deleted_at IS NULL;
