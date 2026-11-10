-- 기술 자료 상세의 원문 링크 미리보기를 피드와 같은 link_preview_cache로 보여준다.
CREATE TABLE resource_link_preview_refs (
    document_id BIGINT PRIMARY KEY REFERENCES resource_documents(id) ON DELETE CASCADE,
    cache_id    BIGINT NOT NULL REFERENCES link_preview_cache(id)
);

CREATE INDEX resource_link_preview_refs_cache_id_idx
    ON resource_link_preview_refs (cache_id);
