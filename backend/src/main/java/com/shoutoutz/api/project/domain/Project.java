package com.shoutoutz.api.project.domain;

import com.shoutoutz.api.cohort.domain.Cohort;
import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

@Getter
public class Project {

    private final Long id;
    private final Cohort cohort;
    private final Long registeredBy;
    private final TeamName teamName;
    private final Slug slug;
    private final Title title;
    private final String tagline;
    private final ServiceStatus serviceStatus;
    private final ApprovalStatus approvalStatus;
    private final String descriptionMd;
    private final GithubRepositoryUrl githubRepositoryUrl;
    private final DeploymentUrl deploymentUrl;
    private final Long thumbnailMediaId;
    private final Instant deletedAt;

    @Builder
    private Project(
            Long id,
            Cohort cohort,
            Long registeredBy,
            TeamName teamName,
            Slug slug,
            Title title,
            String tagline,
            ServiceStatus serviceStatus,
            ApprovalStatus approvalStatus,
            String descriptionMd,
            GithubRepositoryUrl githubRepositoryUrl,
            DeploymentUrl deploymentUrl,
            Long thumbnailMediaId,
            Instant deletedAt
    ) {
        ProjectValidator.validateProject(cohort, tagline, descriptionMd, deploymentUrl, serviceStatus);
        this.id = id;
        this.cohort = cohort;
        this.registeredBy = registeredBy;
        this.teamName = teamName;
        this.slug = slug;
        this.title = title;
        this.tagline = tagline;
        this.serviceStatus = serviceStatus;
        this.approvalStatus = approvalStatus;
        this.descriptionMd = descriptionMd;
        this.githubRepositoryUrl = githubRepositoryUrl;
        this.deploymentUrl = deploymentUrl;
        this.thumbnailMediaId = thumbnailMediaId;
        this.deletedAt = deletedAt;
    }

    public static Project register(
            Cohort cohort,
            Long registeredBy,
            TeamName teamName,
            Title title,
            String tagline,
            String descriptionMd,
            GithubRepositoryUrl githubRepositoryUrl,
            DeploymentUrl deploymentUrl,
            Long thumbnailMediaId
    ) {
        ProjectValidator.validateRegistration(registeredBy);
        return Project.builder()
                .cohort(cohort)
                .registeredBy(registeredBy)
                .teamName(teamName)
                .slug(Slug.from(githubRepositoryUrl.getRepositoryName()))
                .title(title)
                .tagline(tagline)
                .serviceStatus(initialServiceStatus(deploymentUrl))
                .approvalStatus(ApprovalStatus.PENDING)
                .descriptionMd(descriptionMd)
                .githubRepositoryUrl(githubRepositoryUrl)
                .deploymentUrl(deploymentUrl)
                .thumbnailMediaId(thumbnailMediaId)
                .build();
    }

    /**
     * 등록자가 없는 이관 프로젝트는 누구의 것도 아니므로, 언제나 거짓이다.
     */
    public boolean isRegisteredBy(Long userId) {
        return registeredBy != null && registeredBy.equals(userId);
    }

    /**
     * 작성자가 프로젝트 내용을 수정한다.
     * id와 등록자는 바뀌지 않으며, 승인 상태는 수정 결과에 따라 전환된다.
     * slug는 등록 시점 값으로 고정해, 리포지토리 URL을 바꿔도 따라가지 않는다.
     * 프로젝트 주소가 바뀌면 이미 공유된 링크가 깨지기 때문이다.
     */
    public Project update(
            Cohort cohort,
            TeamName teamName,
            Title title,
            String tagline,
            String descriptionMd,
            GithubRepositoryUrl githubRepositoryUrl,
            DeploymentUrl deploymentUrl,
            ServiceStatus serviceStatus,
            Long thumbnailMediaId
    ) {
        return Project.builder()
                .id(id)
                .cohort(cohort)
                .registeredBy(registeredBy)
                .teamName(teamName)
                .slug(slug)
                .title(title)
                .tagline(tagline)
                .serviceStatus(serviceStatus)
                .approvalStatus(approvalStatus.afterEdit())
                .descriptionMd(descriptionMd)
                .githubRepositoryUrl(githubRepositoryUrl)
                .deploymentUrl(deploymentUrl)
                .thumbnailMediaId(thumbnailMediaId)
                .build();
    }

    private static ServiceStatus initialServiceStatus(DeploymentUrl deploymentUrl) {
        if (deploymentUrl == null) {
            return ServiceStatus.CLOSED;
        }
        return ServiceStatus.OPERATING;
    }
}
