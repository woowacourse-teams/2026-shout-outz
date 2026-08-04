package com.dropit.backend.reaction;

import java.time.Instant;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_bookmarks")
public class AppBookmarkEntity {

    @EmbeddedId
    private AppRelationId id;

    private Instant createdAt;

    protected AppBookmarkEntity() {
    }
}
