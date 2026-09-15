package com.shoutoutz.api.comment.domain;

import java.util.Optional;

public interface FeedCommentRepository {

    FeedComment save(FeedComment comment);

    Optional<FeedComment> findById(long commentId);
}
