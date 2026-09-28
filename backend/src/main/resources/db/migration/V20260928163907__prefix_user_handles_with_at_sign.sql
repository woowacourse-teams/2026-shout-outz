ALTER TABLE users
    DROP CONSTRAINT users_handle_check;

ALTER TABLE users
    ALTER COLUMN handle TYPE VARCHAR(31);

UPDATE users
SET handle = '@' || handle;

ALTER TABLE users
    ADD CONSTRAINT chk_users_handle_format
        CHECK (handle ~ '^@[A-Za-z0-9_-]{2,30}$');
