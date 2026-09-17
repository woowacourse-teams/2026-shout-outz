package com.shoutoutz.api.project.infrastructure;

import com.shoutoutz.api.common.entity.BaseEntity;
import com.shoutoutz.api.project.domain.Project;
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

    @Column(nullable = false, length = 200)
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

    @Column(name = "github_repository_url", nullable = false, columnDefinition = "TEXT")
    private String githubRepositoryUrl;

    @Column(name = "deployment_url", columnDefinition = "TEXT")
    private String deploymentUrl;

    @Column(name = "thumbnail_media_id")
    private Long thumbnailMediaId;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    /**
     * 작성자가 고칠 수 있는 컬럼만 바꾼다.
     * id, slug, registered_by 는 바뀌지 않는 값이고, view_count 와 star_count, star_synced_at, deleted_at은
     * 수정 요청이 다루지 않는 값이라 그대로 둔다. 엔티티를 새로 만들어 저장하면 이 값들이 함께 덮이므로,
     * 조회한 엔티티의 필드만 바꿔 더티 체킹으로 반영한다.
     */
    public void update(Project project) {
        this.cohort = (short) project.getCohort().getValue();
        this.teamName = project.getTeamName().value();
        this.title = project.getTitle().value();
        this.tagline = project.getTagline();
        this.serviceStatus = project.getServiceStatus();
        this.approvalStatus = project.getApprovalStatus();
        this.descriptionMd = project.getDescriptionMd();
        this.githubRepositoryUrl = project.getGithubRepositoryUrl().value();
        this.deploymentUrl = project.getDeploymentUrl() == null ? null : project.getDeploymentUrl().value();
        this.thumbnailMediaId = project.getThumbnailMediaId();
    }
}
