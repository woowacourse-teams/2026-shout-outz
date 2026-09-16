CREATE TABLE user_verification_requests (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    user_type    VARCHAR(30) NOT NULL,
    nickname     VARCHAR(50) NOT NULL,
    cohort       INTEGER,
    track        VARCHAR(10),
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    requested_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    decided_at   TIMESTAMPTZ,
    CONSTRAINT user_verification_requests_user_type_check
        CHECK (user_type IN ('WOOWACOURSE_CREW', 'WOOWACOURSE_COACH')),
    CONSTRAINT user_verification_requests_status_check
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT user_verification_requests_nickname_check
        CHECK (char_length(btrim(nickname)) > 0),
    CONSTRAINT user_verification_requests_cohort_check
        CHECK (cohort IS NULL OR cohort > 0),
    CONSTRAINT user_verification_requests_course_info_check
        CHECK (
            (
                user_type = 'WOOWACOURSE_CREW'
                AND cohort IS NOT NULL
                AND track IS NOT NULL
                AND track IN ('BACKEND', 'FRONTEND', 'ANDROID')
            )
            OR (
                user_type = 'WOOWACOURSE_COACH'
                AND cohort IS NULL
                AND track IS NULL
            )
        )
);

CREATE TABLE user_verification_request_histories (
    id          BIGSERIAL PRIMARY KEY,
    request_id  BIGINT      NOT NULL REFERENCES user_verification_requests(id) ON DELETE CASCADE,
    changed_by  BIGINT      REFERENCES users(id),
    from_status VARCHAR(20),
    to_status   VARCHAR(20) NOT NULL,
    reason      VARCHAR(100),
    changed_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT user_verification_request_histories_from_status_check
        CHECK (from_status IS NULL OR from_status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT user_verification_request_histories_to_status_check
        CHECK (to_status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT user_verification_request_histories_reason_length_check
        CHECK (reason IS NULL OR char_length(reason) <= 100)
);

CREATE UNIQUE INDEX uq_user_verification_requests_pending_user
    ON user_verification_requests (user_id)
    WHERE status = 'PENDING';

CREATE INDEX idx_user_verification_requests_user_id_requested_at
    ON user_verification_requests (user_id, requested_at DESC, id DESC);

CREATE INDEX idx_user_verification_requests_status_requested_at
    ON user_verification_requests (status, requested_at DESC, id DESC);

CREATE INDEX idx_user_verification_request_histories_request_id
    ON user_verification_request_histories (request_id, changed_at DESC, id DESC);
