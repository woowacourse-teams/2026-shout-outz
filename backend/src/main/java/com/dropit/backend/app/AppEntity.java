package com.dropit.backend.app;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "apps")
public class AppEntity {

    @Id
    private String id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(nullable = false, length = 40)
    private String name;

    @Column(nullable = false, length = 80)
    private String tagline;

    @Column(nullable = false, length = 5000)
    private String description;

    @Column(nullable = false, length = 20)
    private String category;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "text[]")
    private List<String> categories = new ArrayList<>();

    @Column(name = "thumbnail_variant", nullable = false, length = 20)
    private String thumbnailVariant;

    @Column(name = "thumbnail_url", columnDefinition = "text")
    private String thumbnailUrl;

    @Column(name = "app_url", nullable = false, columnDefinition = "text")
    private String appUrl;

    @Column(name = "github_url", columnDefinition = "text")
    private String githubUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> maker;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "tech_tags", nullable = false, columnDefinition = "text[]")
    private List<String> techTags = new ArrayList<>();

    @Column(nullable = false)
    private int plays;

    @Column(nullable = false)
    private int likes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(nullable = false, length = 20)
    private String source;

    protected AppEntity() {
    }

    public AppEntity(
        String id,
        UUID ownerId,
        String name,
        String tagline,
        String description,
        List<String> categories,
        String thumbnailVariant,
        String thumbnailUrl,
        String appUrl,
        String githubUrl,
        Map<String, Object> maker,
        List<String> techTags
    ) {
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
        this.tagline = tagline;
        this.description = description;
        this.categories = new ArrayList<>(categories);
        this.category = categories.getFirst();
        this.thumbnailVariant = thumbnailVariant;
        this.thumbnailUrl = thumbnailUrl;
        this.appUrl = appUrl;
        this.githubUrl = githubUrl;
        this.maker = maker;
        this.techTags = new ArrayList<>(techTags);
        this.plays = 0;
        this.likes = 0;
        this.createdAt = Instant.now();
        this.source = "submitted";
    }

    public void update(
        String name,
        String tagline,
        String description,
        List<String> categories,
        String thumbnailVariant,
        String thumbnailUrl,
        String appUrl,
        String githubUrl,
        Map<String, Object> maker,
        List<String> techTags
    ) {
        this.name = name;
        this.tagline = tagline;
        this.description = description;
        this.categories = new ArrayList<>(categories);
        this.category = categories.getFirst();
        this.thumbnailVariant = thumbnailVariant;
        this.thumbnailUrl = thumbnailUrl;
        this.appUrl = appUrl;
        this.githubUrl = githubUrl;
        this.maker = maker;
        this.techTags = new ArrayList<>(techTags);
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
    }

    public void restore() {
        this.deletedAt = null;
    }

    public String getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getName() {
        return name;
    }

    public String getTagline() {
        return tagline;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getCategories() {
        return categories;
    }

    public String getThumbnailVariant() {
        return thumbnailVariant;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public String getAppUrl() {
        return appUrl;
    }

    public String getGithubUrl() {
        return githubUrl;
    }

    public Map<String, Object> getMaker() {
        return maker;
    }

    public List<String> getTechTags() {
        return techTags;
    }

    public int getPlays() {
        return plays;
    }

    public int getLikes() {
        return likes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public String getSource() {
        return source;
    }
}
