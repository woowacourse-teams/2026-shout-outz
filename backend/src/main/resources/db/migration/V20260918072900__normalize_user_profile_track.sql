UPDATE user_profiles
SET track = CASE track
    WHEN 'BE' THEN 'BACKEND'
    WHEN 'FE' THEN 'FRONTEND'
    ELSE track
END
WHERE track IN ('BE', 'FE');

ALTER TABLE user_profiles
    ADD CONSTRAINT chk_user_profiles_track
        CHECK (track IS NULL OR track IN ('BACKEND', 'ANDROID', 'FRONTEND'));
