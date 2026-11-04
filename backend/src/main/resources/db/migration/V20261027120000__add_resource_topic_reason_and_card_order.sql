-- LLM 결과 중 담을 칸이 없던 두 값을 더한다. 두 테이블은 아직 적재기가 없어 비어 있다.

-- 문서의 기술 주제를 고른 근거(LLM 의 topic_reason, 한 문장). 주제 분류를 검수할 때 본다.
ALTER TABLE resource_documents
    ADD COLUMN topic_reason VARCHAR(200);

-- 이력서 항목 순서: 이력서 묶기 단계는 프로젝트, 직군마다 항목을 이력서에서 강한 순서로 돌려준다(0부터).
-- 프로젝트 상세 "전체 경험" 탭은 이 순서대로 항목을 보여 준다.
ALTER TABLE resource_career_cards
    ADD COLUMN sort_order SMALLINT NOT NULL DEFAULT 0 CHECK (sort_order >= 0);

DROP INDEX resource_career_cards_project_idx;

CREATE INDEX resource_career_cards_project_idx
    ON resource_career_cards (project_id, track, sort_order)
    WHERE deleted_at IS NULL;
