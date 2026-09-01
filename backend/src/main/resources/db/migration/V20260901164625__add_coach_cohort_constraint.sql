ALTER TABLE user_profiles
    ADD CONSTRAINT chk_user_profiles_coach_cohort_null
        CHECK (
            user_type <> 'WOOWACOURSE_COACH'
                OR cohort IS NULL
        );
