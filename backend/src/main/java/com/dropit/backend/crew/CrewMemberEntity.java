package com.dropit.backend.crew;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "crew_members")
public class CrewMemberEntity {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "verified_at", nullable = false)
    private Instant verifiedAt;

    protected CrewMemberEntity() {
    }

    public CrewMemberEntity(UUID userId) {
        this.userId = userId;
        this.verifiedAt = Instant.now();
    }

    public UUID getUserId() {
        return userId;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }
}
