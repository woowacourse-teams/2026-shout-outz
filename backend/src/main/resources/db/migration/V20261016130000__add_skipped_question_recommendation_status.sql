ALTER TABLE question_recommendation_jobs
    DROP CONSTRAINT question_recommendation_jobs_status_check;

ALTER TABLE question_recommendation_jobs
    ADD CONSTRAINT question_recommendation_jobs_status_check
        CHECK (status IN ('PENDING', 'PROCESSING', 'READY', 'FAILED', 'SKIPPED'));
