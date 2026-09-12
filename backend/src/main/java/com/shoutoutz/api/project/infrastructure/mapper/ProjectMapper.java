package com.shoutoutz.api.project.infrastructure.mapper;

import com.shoutoutz.api.cohort.domain.Cohort;
import com.shoutoutz.api.project.domain.DeploymentUrl;
import com.shoutoutz.api.project.domain.GithubRepositoryUrl;
import com.shoutoutz.api.project.domain.Project;
import com.shoutoutz.api.project.domain.Slug;
import com.shoutoutz.api.project.domain.TeamName;
import com.shoutoutz.api.project.domain.Title;
import com.shoutoutz.api.project.infrastructure.ProjectEntity;
import com.shoutoutz.api.project.infrastructure.ProjectMemberEntity;
import com.shoutoutz.api.project.infrastructure.ProjectMemberId;
import com.shoutoutz.api.project.infrastructure.ProjectTagEntity;
import com.shoutoutz.api.project.infrastructure.ProjectTagId;
import java.util.List;
import java.util.stream.IntStream;

public final class ProjectMapper {

    private ProjectMapper() {
    }

    public static ProjectEntity toEntity(Project project) {
        return ProjectEntity.builder()
                .id(project.getId())
                .cohort((short) project.getCohort().getValue())
                .registeredBy(project.getRegisteredBy())
                .teamName(project.getTeamName().value())
                .slug(project.getSlug().value())
                .title(project.getTitle().value())
                .tagline(project.getTagline())
                .serviceStatus(project.getServiceStatus())
                .approvalStatus(project.getApprovalStatus())
                .descriptionMd(project.getDescriptionMd())
                .githubRepositoryUrl(project.getGithubRepositoryUrl().value())
                .deploymentUrl(project.getDeploymentUrl() == null ? null : project.getDeploymentUrl().value())
                .thumbnailMediaId(project.getThumbnailMediaId())
                .build();
    }

    public static Project toDomain(ProjectEntity entity) {
        return Project.builder()
                .id(entity.getId())
                .cohort(Cohort.from(entity.getCohort()))
                .registeredBy(entity.getRegisteredBy())
                .teamName(new TeamName(entity.getTeamName()))
                .slug(new Slug(entity.getSlug()))
                .title(new Title(entity.getTitle()))
                .tagline(entity.getTagline())
                .serviceStatus(entity.getServiceStatus())
                .approvalStatus(entity.getApprovalStatus())
                .descriptionMd(entity.getDescriptionMd())
                .githubRepositoryUrl(new GithubRepositoryUrl(entity.getGithubRepositoryUrl()))
                .deploymentUrl(DeploymentUrl.fromNullable(entity.getDeploymentUrl()))
                .thumbnailMediaId(entity.getThumbnailMediaId())
                .build();
    }

    /**
     * 목록 순서가 그대로 display_order 가 된다.
     */
    public static List<ProjectTagEntity> toProjectTagEntities(Long projectId, List<Long> techTagIds) {
        return IntStream.range(0, techTagIds.size())
                .mapToObj(order -> ProjectTagEntity.builder()
                        .id(new ProjectTagId(projectId, techTagIds.get(order)))
                        .displayOrder((short) order)
                        .build())
                .toList();
    }

    /**
     * 목록 순서가 그대로 display_order 가 된다.
     */
    public static List<ProjectMemberEntity> toProjectMemberEntities(Long projectId, List<Long> memberIds) {
        return IntStream.range(0, memberIds.size())
                .mapToObj(order -> ProjectMemberEntity.builder()
                        .id(new ProjectMemberId(projectId, memberIds.get(order)))
                        .displayOrder((short) order)
                        .build())
                .toList();
    }
}
