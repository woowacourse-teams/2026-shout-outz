package com.shoutoutz.api.comment.infrastructure.jpa;

import com.shoutoutz.api.comment.infrastructure.FeedCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedCommentJpaRepository extends JpaRepository<FeedCommentEntity, Long> {
}
