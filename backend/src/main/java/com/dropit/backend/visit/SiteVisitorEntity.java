package com.dropit.backend.visit;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "site_visitors")
public class SiteVisitorEntity {

    @EmbeddedId
    private VisitorRelationId id;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected SiteVisitorEntity() {
    }
}
