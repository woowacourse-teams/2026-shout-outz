package com.shoutoutz.api.comment.infrastructure.jpa;

import com.shoutoutz.api.comment.infrastructure.ProjectCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectCommentJpaRepository extends JpaRepository<ProjectCommentEntity, Long> {
}
