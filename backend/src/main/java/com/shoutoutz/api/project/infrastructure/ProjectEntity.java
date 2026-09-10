package com.shoutoutz.api.project.infrastructure;

import com.shoutoutz.api.common.entity.BaseEntity;
import com.shoutoutz.api.project.domain.ApprovalStatus;
import com.shoutoutz.api.project.domain.ServiceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "projects")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ProjectEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private short cohort;

    @Column(name = "registered_by")
    private Long registeredBy;

    @Column(name = "team_name", nullable = false, length = 50)
    private String teamName;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 200)
    private String tagline;

    @Column(name = "star_count")
    private Integer starCount;

    @Column(name = "star_synced_at")
    private Instant starSyncedAt;

    @Column(name = "view_count", nullable = false)
    private int viewCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_status", nullable = false, length = 20)
    private ServiceStatus serviceStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    private ApprovalStatus approvalStatus;

    @Column(name = "description_md", columnDefinition = "TEXT")
    private String descriptionMd;

    @Column(name = "github_repository_url", columnDefinition = "TEXT")
    private String githubRepositoryUrl;

    @Column(name = "deployment_url", columnDefinition = "TEXT")
    private String deploymentUrl;

    @Column(name = "thumbnail_media_id")
    private Long thumbnailMediaId;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
