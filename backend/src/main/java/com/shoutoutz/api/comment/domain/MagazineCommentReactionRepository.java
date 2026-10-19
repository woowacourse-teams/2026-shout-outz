package com.shoutoutz.api.comment.domain;

import java.util.List;
import java.util.Map;

public interface MagazineCommentReactionRepository {

    void add(long commentId, long userId, MagazineCommentReactionType type);

    boolean remove(long commentId, long userId, MagazineCommentReactionType type);

    MagazineCommentReactionCounts countByCommentId(long commentId);

    Map<Long, MagazineCommentReactionCounts> findByCommentIds(List<Long> commentIds, Long viewerId);
}
