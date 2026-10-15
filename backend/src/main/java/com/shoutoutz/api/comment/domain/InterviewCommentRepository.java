package com.shoutoutz.api.comment.domain;

import java.util.Optional;

public interface InterviewCommentRepository {

    InterviewComment save(InterviewComment comment);

    Optional<InterviewComment> findById(long commentId);
}
