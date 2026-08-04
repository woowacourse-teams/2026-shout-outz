package com.dropit.backend.reaction;

import java.time.Instant;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_likes")
public class AppLikeEntity {

    @EmbeddedId
    private AppRelationId id;

    private Instant createdAt;

    protected AppLikeEntity() {
    }
}
