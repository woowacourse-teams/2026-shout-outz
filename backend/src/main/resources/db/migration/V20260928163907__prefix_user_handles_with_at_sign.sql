ALTER TABLE users
    DROP CONSTRAINT users_handle_check;

ALTER TABLE users
    ALTER COLUMN handle TYPE VARCHAR(31);

UPDATE users
SET handle = '@' || handle;
