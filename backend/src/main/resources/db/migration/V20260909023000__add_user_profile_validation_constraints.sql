ALTER TABLE user_profiles
    ADD CONSTRAINT chk_user_profiles_bio_length
        CHECK (bio IS NULL OR char_length(btrim(bio)) BETWEEN 1 AND 200);
