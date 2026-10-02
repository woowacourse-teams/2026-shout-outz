package com.shoutoutz.api.comment.domain;

import java.util.Optional;

public interface ProjectCommentRepository {

    ProjectComment save(ProjectComment comment);

    Optional<ProjectComment> findById(long commentId);
}
