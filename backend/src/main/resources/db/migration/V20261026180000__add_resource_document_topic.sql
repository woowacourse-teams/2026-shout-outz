-- 문서의 기술 주제: 프로젝트 상세 "기술 이야기" 탭과 같은 주제를 다룬 다른 팀 자료 잇기에 쓴다.
-- 값은 고정 주제 69개의 id 하나다(데이터 파이프라인 config/topics.json, 예: cache, n_plus_one). 목록이 바뀔 수 있어 DB 에서 값 제한은 두지 않는다.
ALTER TABLE resource_documents
    ADD COLUMN topic VARCHAR(40);

CREATE INDEX resource_documents_topic_idx
    ON resource_documents (topic)
    WHERE deleted_at IS NULL;
