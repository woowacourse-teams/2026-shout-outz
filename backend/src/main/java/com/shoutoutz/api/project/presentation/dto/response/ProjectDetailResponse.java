package com.shoutoutz.api.project.presentation.dto.response;

import com.shoutoutz.api.project.domain.ApprovalStatus;
import com.shoutoutz.api.project.domain.ProjectDetail;
import com.shoutoutz.api.project.domain.ServiceStatus;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 프로젝트 상세 조회 응답 객체
 */
public record ProjectDetailResponse(
        long id,
        String slug,
        String title,
        String teamName,
        String tagline,
        int cohort,
        String imageUrl,
        String descriptionMd,
        String githubRepositoryUrl,
        String deploymentUrl,
        ServiceStatus serviceStatus,
        ApprovalStatus approvalStatus,
        String rejectReason,
        Long registeredBy,
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

    public static ProjectDetailResponse from(
            ProjectDetail detail,
            Map<Long, URI> mediaUrls,
            String descriptionMd
    ) {
        return new ProjectDetailResponse(
                detail.id(),
                detail.slug(),
                detail.title(),
                detail.teamName(),
                detail.tagline(),
                detail.cohort(),
                toUrl(mediaUrls, detail.thumbnailMediaId()),
                descriptionMd,
                detail.githubRepositoryUrl(),
                detail.deploymentUrl(),
                detail.serviceStatus(),
                detail.approvalStatus(),
                detail.rejectReason(),
                detail.registeredBy(),
                detail.viewCount(),
                detail.starCount(),
                detail.likeCount(),
                detail.bookmarkCount(),
                detail.likedByMe(),
                detail.bookmarkedByMe(),
                detail.commentCount(),
                detail.techTags().stream().map(ProjectTechTagResponse::from).toList(),
                detail.members().stream()
                        .map(member -> ProjectMemberProfileResponse.from(member, mediaUrls))
                        .toList(),
                detail.createdAt(),
                detail.updatedAt()
        );
    }

    @Deprecated
    public ProjectDetailResponse(
            long id,
            String slug,
            String title,
            String teamName,
            String tagline,
            int cohort,
            Long thumbnailMediaId,
            String descriptionMd,
            String githubRepositoryUrl,
            String deploymentUrl,
            ServiceStatus serviceStatus,
            ApprovalStatus approvalStatus,
            String rejectReason,
            Long registeredBy,
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
        this(
                id,
                slug,
                title,
                teamName,
                tagline,
                cohort,
                (String) null,
                descriptionMd,
                githubRepositoryUrl,
                deploymentUrl,
                serviceStatus,
                approvalStatus,
                rejectReason,
                registeredBy,
                viewCount,
                starCount,
                likeCount,
                bookmarkCount,
                likedByMe,
                bookmarkedByMe,
                commentCount,
                techTags,
                members,
                createdAt,
                updatedAt
        );
    }

    private static String toUrl(Map<Long, URI> mediaUrls, Long mediaId) {
        if (mediaId == null || mediaUrls == null) {
            return null;
        }
        URI url = mediaUrls.get(mediaId);
        return url == null ? null : url.toString();
    }

}
