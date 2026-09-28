package com.shoutoutz.api.project.presentation.dto.response;

import com.shoutoutz.api.project.domain.ApprovalStatus;
import com.shoutoutz.api.project.domain.ServiceStatus;
import java.time.Instant;
import java.util.List;

/**
 * 승인 상태와 관계없이 관리자가 확인할 수 있는 프로젝트 상세 응답이다.
 */
public record AdminProjectDetailResponse(
        long id,
        Long registeredBy,
        String slug,
        String title,
        String teamName,
        String tagline,
        int cohort,
        Long thumbnailImageId,
        String imageUrl,
        String descriptionMd,
        List<ProjectDetailResponse.DescriptionMedia> descriptionMedia,
        String githubRepositoryUrl,
        String deploymentUrl,
        ServiceStatus serviceStatus,
        ApprovalStatus approvalStatus,
        String rejectReason,
        int viewCount,
        Integer starCount,
        long likeCount,
        long bookmarkCount,
        boolean likedByMe,
        boolean bookmarkedByMe,
        long commentCount,
        List<ProjectTechTagResponse> techTags,
        List<ProjectMemberProfileResponse> members,
        Instant createdAt,
        Instant updatedAt
) {

    public static AdminProjectDetailResponse from(
            ProjectDetailResponse detail,
            Long registeredBy
    ) {
        return new AdminProjectDetailResponse(
                detail.id(),
                registeredBy,
                detail.slug(),
                detail.title(),
                detail.teamName(),
                detail.tagline(),
                detail.cohort(),
                detail.thumbnailImageId(),
                detail.imageUrl(),
                detail.descriptionMd(),
                detail.descriptionMedia(),
                detail.githubRepositoryUrl(),
                detail.deploymentUrl(),
                detail.serviceStatus(),
                detail.approvalStatus(),
                detail.rejectReason(),
                detail.viewCount(),
                detail.starCount(),
                detail.likeCount(),
                detail.bookmarkCount(),
                detail.likedByMe(),
                detail.bookmarkedByMe(),
                detail.commentCount(),
                detail.techTags(),
                detail.members(),
                detail.createdAt(),
                detail.updatedAt()
        );
    }
}
