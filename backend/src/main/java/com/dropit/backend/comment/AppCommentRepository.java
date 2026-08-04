package com.dropit.backend.comment;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AppCommentRepository extends JpaRepository<AppCommentEntity, UUID> {

    List<AppCommentEntity> findAllByAppIdOrderByCreatedAtDesc(String appId);
}
