-- 문서의 기술 주제: 프로젝트 상세 "기술 이야기" 탭과 같은 주제를 다룬 다른 팀 자료 잇기에 쓴다.
-- 값은 고정 주제 77개의 id 하나다(데이터 파이프라인 config/topics.json, 예: cache, n_plus_one).
-- 주제 목록 테이블과 외래 키는 다음 마이그레이션(V20261026190000)에서 만든다.
ALTER TABLE resource_documents
    ADD COLUMN topic VARCHAR(40);

CREATE INDEX resource_documents_topic_idx
    ON resource_documents (topic)
    WHERE deleted_at IS NULL;
