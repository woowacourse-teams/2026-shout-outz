package com.dropit.backend.maker;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "makers")
public class MakerEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(nullable = false, length = 20)
    private String initials;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(nullable = false, length = 40)
    private String role;

    @Column(nullable = false, length = 100)
    private String bio;

    @Column(nullable = false, length = 20)
    private String tone;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MakerEntity() {
    }

    public MakerEntity(UUID id, String name, String initials, String avatarUrl, String role, String bio, String tone) {
        this.id = id;
        this.name = name;
        this.initials = initials;
        this.avatarUrl = avatarUrl;
        this.role = role;
        this.bio = bio;
        this.tone = tone;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void update(String name, String initials, String avatarUrl, String role, String bio, String tone) {
        this.name = name;
        this.initials = initials;
        this.avatarUrl = avatarUrl;
        this.role = role;
        this.bio = bio;
        this.tone = tone;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getInitials() {
        return initials;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getRole() {
        return role;
    }

    public String getBio() {
        return bio;
    }

    public String getTone() {
        return tone;
    }
}
