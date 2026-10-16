CREATE INDEX CONCURRENTLY feed_reactions_user_id_reaction_type_feed_id_idx
    ON feed_reactions (user_id, reaction_type, feed_id);
