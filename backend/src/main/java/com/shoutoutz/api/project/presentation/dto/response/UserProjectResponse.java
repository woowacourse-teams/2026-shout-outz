package com.shoutoutz.api.project.presentation.dto.response;

import com.shoutoutz.api.cohort.domain.Cohort;
import com.shoutoutz.api.project.application.dto.UserProjectItem;
import com.shoutoutz.api.project.domain.ApprovalStatus;
import com.shoutoutz.api.project.domain.ProjectMemberProfile;
import com.shoutoutz.api.project.domain.ServiceStatus;
import com.shoutoutz.api.user.domain.profile.Track;
import com.shoutoutz.api.user.domain.profile.UserType;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * 사용자 페이지의 프로젝트 카드 응답.
 */
public record UserProjectResponse(
        long id,
        String slug,
        String title,
        String teamName,
        String tagline,
        int cohort,
        ServiceStatus serviceStatus,
        ApprovalStatus approvalStatus,
        Long thumbnailImageId,
        String thumbnailUrl,
        Integer starCount,
        long likeCount,
        long commentCount,
        long bookmarkCount,
        boolean likedByMe,
        boolean bookmarkedByMe,
        List<ProjectTechTagResponse> techTags,
        List<Member> members
) {

    public static List<UserProjectResponse> from(
            List<UserProjectItem> projects,
            Map<Long, URI> mediaUrls
    ) {
        return projects.stream()
                .map(project -> from(project, mediaUrls))
                .toList();
    }

    private static UserProjectResponse from(
            UserProjectItem project,
            Map<Long, URI> mediaUrls
    ) {
        return new UserProjectResponse(
                project.id(),
                project.slug(),
                project.title(),
                project.teamName(),
                project.tagline(),
                project.cohort(),
                project.serviceStatus(),
                project.approvalStatus(),
                project.thumbnailMediaId(),
                toUrl(mediaUrls, project.thumbnailMediaId()),
                project.starCount(),
                project.likeCount(),
                project.commentCount(),
                project.bookmarkCount(),
                project.likedByMe(),
                project.bookmarkedByMe(),
                project.techTags().stream().map(ProjectTechTagResponse::from).toList(),
                project.members().stream().map(member -> Member.from(member, mediaUrls)).toList()
        );
    }

    public record Member(
            Long userId,
            String handle,
            String displayName,
            UserType userType,
            Integer cohort,
            String track,
            Long avatarImageId,
            String avatarUrl,
            String githubAvatarUrl,
            String githubProfileUrl
    ) {

        public Member(
                String handle,
                String displayName,
                Integer cohort,
                String track,
                Long avatarImageId,
                String avatarUrl,
                String githubAvatarUrl,
                String githubProfileUrl
        ) {
            this(
                    null,
                    handle,
                    displayName,
                    UserType.WOOWACOURSE_CREW,
                    cohort,
                    track,
                    avatarImageId,
                    avatarUrl,
                    githubAvatarUrl,
                    githubProfileUrl
            );
        }

        private static Member from(
                ProjectMemberProfile member,
                Map<Long, URI> mediaUrls
        ) {
            return new Member(
                    member.userId(),
                    member.handle(),
                    member.displayName(),
                    member.userType(),
                    cohortValue(member.userType(), member.cohort()),
                    trackValue(member.userType(), member.track()),
                    member.avatarImageId(),
                    toUrl(mediaUrls, member.avatarImageId()),
                    member.githubAvatarUrl(),
                    member.githubProfileUrl()
            );
        }

        private static Integer cohortValue(UserType userType, Cohort cohort) {
            if (userType != UserType.WOOWACOURSE_CREW || cohort == null) {
                return null;
            }
            return cohort.getValue();
        }

        private static String trackValue(UserType userType, Track track) {
            if (userType != UserType.WOOWACOURSE_CREW || track == null) {
                return null;
            }
            return track.getValue();
        }
    }

    private static String toUrl(Map<Long, URI> mediaUrls, Long mediaId) {
        if (mediaId == null || mediaUrls == null) {
            return null;
        }
        URI url = mediaUrls.get(mediaId);
        return url == null ? null : url.toString();
    }
}
