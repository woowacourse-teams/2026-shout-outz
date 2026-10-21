ALTER TABLE feeds
    ADD COLUMN like_count BIGINT NOT NULL DEFAULT 0 CHECK (like_count >= 0),
    ADD COLUMN comment_count BIGINT NOT NULL DEFAULT 0 CHECK (comment_count >= 0);

UPDATE feeds f
SET like_count = r.like_count
FROM (
    SELECT feed_id, COUNT(*) AS like_count
    FROM feed_reactions
    WHERE reaction_type = 'LIKE'
    GROUP BY feed_id
) r
WHERE r.feed_id = f.id;

UPDATE feeds f
SET comment_count = c.comment_count
FROM (
    SELECT feed_id, COUNT(*) AS comment_count
    FROM feed_comments
    WHERE deleted_at IS NULL
    GROUP BY feed_id
) c
WHERE c.feed_id = f.id;
