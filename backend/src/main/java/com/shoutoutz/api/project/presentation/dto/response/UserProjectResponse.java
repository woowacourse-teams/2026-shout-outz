package com.shoutoutz.api.project.presentation.dto.response;

import com.shoutoutz.api.project.application.dto.UserProjectItem;
import com.shoutoutz.api.project.domain.ApprovalStatus;
import com.shoutoutz.api.project.domain.ServiceStatus;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * 사용자 페이지의 프로젝트 카드 응답.
 */
public record UserProjectResponse(
        String slug,
        String title,
        String teamName,
        String tagline,
        int cohort,
        ServiceStatus serviceStatus,
        ApprovalStatus approvalStatus,
        String rejectReason,
        Long thumbnailImageId,
        String thumbnailUrl,
        Integer starCount,
        long likeCount,
        long commentCount,
        long bookmarkCount,
        boolean likedByMe,
        boolean bookmarkedByMe,
        List<ProjectTechTagResponse> techTags,
        List<ProjectMemberProfileResponse> members
) {

    public static List<UserProjectResponse> from(
            List<UserProjectItem> projects,
            Map<Long, URI> mediaUrls
    ) {
        return from(projects, mediaUrls, Map.of());
    }

    public static List<UserProjectResponse> from(
            List<UserProjectItem> projects,
            Map<Long, URI> mediaUrls,
            Map<Long, String> userAvatarUrls
    ) {
        return projects.stream()
                .map(project -> from(project, mediaUrls, userAvatarUrls))
                .toList();
    }

    private static UserProjectResponse from(
            UserProjectItem project,
            Map<Long, URI> mediaUrls,
            Map<Long, String> userAvatarUrls
    ) {
        return new UserProjectResponse(
                project.slug(),
                project.title(),
                project.teamName(),
                project.tagline(),
                project.cohort(),
                project.serviceStatus(),
                project.approvalStatus(),
                project.rejectReason(),
                project.thumbnailMediaId(),
                toUrl(mediaUrls, project.thumbnailMediaId()),
                project.starCount(),
                project.likeCount(),
                project.commentCount(),
                project.bookmarkCount(),
                project.likedByMe(),
                project.bookmarkedByMe(),
                project.techTags().stream().map(ProjectTechTagResponse::from).toList(),
                project.members().stream()
                        .map(member -> ProjectMemberProfileResponse.from(
                                member,
                                mediaUrls,
                                userAvatarUrls
                        ))
                        .toList()
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
