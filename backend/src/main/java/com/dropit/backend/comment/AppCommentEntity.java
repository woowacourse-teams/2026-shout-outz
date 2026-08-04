package com.dropit.backend.comment;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_comments")
public class AppCommentEntity {

    @Id
    private UUID id;

    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(nullable = false, length = 60)
    private String title;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AppCommentEntity() {
    }

    public AppCommentEntity(String appId, UUID userId, UUID parentId, String title, String content) {
        this.id = UUID.randomUUID();
        this.appId = appId;
        this.userId = userId;
        this.parentId = parentId;
        this.title = title;
        this.content = content;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getParentId() {
        return parentId;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
