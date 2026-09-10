package com.shoutoutz.api.project.domain;

import com.shoutoutz.api.cohort.domain.Cohort;
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
            Long thumbnailMediaId
    ) {
        ProjectValidator.validateProject(cohort, tagline, descriptionMd);
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
                .slug(Slug.from(githubRepositoryUrl.repositoryName()))
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

    private static ServiceStatus initialServiceStatus(DeploymentUrl deploymentUrl) {
        if (deploymentUrl == null) {
            return ServiceStatus.CLOSED;
        }
        return ServiceStatus.OPERATING;
    }
}
