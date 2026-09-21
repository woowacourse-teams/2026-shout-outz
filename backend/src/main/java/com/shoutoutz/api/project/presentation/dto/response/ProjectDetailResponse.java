package com.shoutoutz.api.project.presentation.dto.response;

import com.shoutoutz.api.project.domain.ApprovalStatus;
import com.shoutoutz.api.project.domain.DescriptionMediaReferences;
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
        Long thumbnailImageId,
        String imageUrl,
        String descriptionMd,
        List<DescriptionMedia> descriptionMedia,
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
        boolean editable,
        long commentCount,
        List<ProjectTechTagResponse> techTags,
        List<ProjectMemberProfileResponse> members,
        Instant createdAt,
        Instant updatedAt
) {

    public static ProjectDetailResponse from(
            ProjectDetail detail,
            Map<Long, URI> mediaUrls,
            String descriptionMd,
            Long viewerId
    ) {
        return new ProjectDetailResponse(
                detail.id(),
                detail.slug(),
                detail.title(),
                detail.teamName(),
                detail.tagline(),
                detail.cohort(),
                detail.thumbnailMediaId(),
                toUrl(mediaUrls, detail.thumbnailMediaId()),
                descriptionMd,
                DescriptionMedia.from(detail.descriptionMd(), mediaUrls),
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
                detail.isEditableBy(viewerId),
                detail.commentCount(),
                detail.techTags().stream().map(ProjectTechTagResponse::from).toList(),
                detail.members().stream()
                        .map(member -> ProjectMemberProfileResponse.from(member, mediaUrls))
                        .toList(),
                detail.createdAt(),
                detail.updatedAt()
        );
    }

    private static String toUrl(Map<Long, URI> mediaUrls, Long mediaId) {
        if (mediaId == null || mediaUrls == null) {
            return null;
        }
        URI url = mediaUrls.get(mediaId);
        return url == null ? null : url.toString();
    }

    public record DescriptionMedia(long mediaId, String url) {

        private static List<DescriptionMedia> from(String descriptionMd, Map<Long, URI> mediaUrls) {
            return DescriptionMediaReferences.extractMediaIds(descriptionMd).stream()
                    .map(mediaId -> new DescriptionMedia(mediaId, toUrl(mediaUrls, mediaId)))
                    .toList();
        }
    }

}
