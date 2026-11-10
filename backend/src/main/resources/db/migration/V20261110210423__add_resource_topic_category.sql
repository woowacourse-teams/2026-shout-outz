-- 프로젝트 상세 기술 이야기 탭은 왼쪽 주제 목록을 분야로 묶어 보여 준다(#612).
-- 분야는 프로토타입 config/topics-v7.json 의 category 를 그대로 옮긴다. 주제 77개의 id, 이름, 직군이 이 표와 같다.
-- 주제를 더할 때는 분야도 함께 정해야 한다(NOT NULL).
ALTER TABLE resource_topic_catalog
    ADD COLUMN category VARCHAR(30);

UPDATE resource_topic_catalog topic
SET category = topic_category.category
FROM (VALUES
    ('spring_core', '백엔드 프레임워크'),
    ('exception_validation', '백엔드 프레임워크'),
    ('auth', '아키텍처, 설계'),
    ('kotlin_server', '백엔드 프레임워크'),
    ('webflux', '백엔드 프레임워크'),
    ('java_jvm', '언어, 기초'),
    ('javascript_lang', '언어, 기초'),
    ('oop_patterns', '언어, 기초'),
    ('cs_basics', '언어, 기초'),
    ('web_fundamentals', '언어, 기초'),
    ('jpa', '데이터'),
    ('querydsl', '데이터'),
    ('db_design', '데이터'),
    ('query_writing', '데이터'),
    ('db_index', '데이터'),
    ('replication', '데이터'),
    ('migration', '데이터'),
    ('nosql_search', '캐시, 메시징, 검색'),
    ('n_plus_one', '데이터'),
    ('pagination', '데이터'),
    ('cache', '캐시, 메시징, 검색'),
    ('messaging', '캐시, 메시징, 검색'),
    ('websocket', '캐시, 메시징, 검색'),
    ('batch', '캐시, 메시징, 검색'),
    ('concurrency', '동시성, 성능'),
    ('load_test', '동시성, 성능'),
    ('performance', '동시성, 성능'),
    ('async', '동시성, 성능'),
    ('transaction', '동시성, 성능'),
    ('pool_tuning', '동시성, 성능'),
    ('idempotency', '동시성, 성능'),
    ('unit_test', '테스트, 품질'),
    ('integration_test', '테스트, 품질'),
    ('api_docs', '테스트, 품질'),
    ('code_quality', '테스트, 품질'),
    ('test_isolation', '테스트, 품질'),
    ('server_infra', '인프라, DevOps'),
    ('docker', '인프라, DevOps'),
    ('cicd', '인프라, DevOps'),
    ('build_tools', '인프라, DevOps'),
    ('nginx', '인프라, DevOps'),
    ('https_security', '인프라, DevOps'),
    ('monitoring', '인프라, DevOps'),
    ('logging', '인프라, DevOps'),
    ('zero_downtime', '인프라, DevOps'),
    ('architecture', '아키텍처, 설계'),
    ('package_structure', '아키텍처, 설계'),
    ('distributed_system', '아키텍처, 설계'),
    ('rest_api', '아키텍처, 설계'),
    ('oauth_login', '아키텍처, 설계'),
    ('payment', '아키텍처, 설계'),
    ('llm', 'LLM, AI'),
    ('react', '프론트엔드'),
    ('component_design', '프론트엔드'),
    ('nextjs', '프론트엔드'),
    ('typescript', '프론트엔드'),
    ('state_mgmt', '프론트엔드'),
    ('server_state', '프론트엔드'),
    ('styling', '프론트엔드'),
    ('bundler', '프론트엔드'),
    ('web_perf', '프론트엔드'),
    ('a11y', '프론트엔드'),
    ('fe_test', '프론트엔드'),
    ('design_system', '프론트엔드'),
    ('pwa', '프론트엔드'),
    ('render_perf', '프론트엔드'),
    ('image_opt', '프론트엔드'),
    ('cdn_cache', '프론트엔드'),
    ('android', '모바일'),
    ('ios', '모바일'),
    ('git_flow', '협업, 운영'),
    ('git', '협업, 운영'),
    ('planning', '협업, 운영'),
    ('incident', '협업, 운영'),
    ('user_feedback', '협업, 운영'),
    ('file_storage', '협업, 운영'),
    ('notification', '협업, 운영')
) AS topic_category (id, category)
WHERE topic.id = topic_category.id;

ALTER TABLE resource_topic_catalog
    ALTER COLUMN category SET NOT NULL;

-- 기술 이야기 탭은 한 프로젝트의 자료를 주제별로 세고 읽는다.
CREATE INDEX resource_documents_project_topic_idx
    ON resource_documents (project_id, topic)
    WHERE deleted_at IS NULL;
