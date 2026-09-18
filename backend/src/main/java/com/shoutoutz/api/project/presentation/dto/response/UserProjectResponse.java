package com.shoutoutz.api.project.presentation.dto.response;

import com.shoutoutz.api.cohort.domain.Cohort;
import com.shoutoutz.api.project.application.dto.UserProjectItem;
import com.shoutoutz.api.project.domain.ProjectMemberProfile;
import com.shoutoutz.api.project.domain.ServiceStatus;
import com.shoutoutz.api.user.domain.profile.Track;
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
        String thumbnailUrl,
        Integer starCount,
        long likeCount,
        long commentCount,
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
                toUrl(mediaUrls, project.thumbnailMediaId()),
                project.starCount(),
                project.likeCount(),
                project.commentCount(),
                project.techTags().stream().map(ProjectTechTagResponse::from).toList(),
                project.members().stream().map(member -> Member.from(member, mediaUrls)).toList()
        );
    }

    public record Member(
            String handle,
            String displayName,
            Integer cohort,
            String track,
            String avatarUrl,
            String githubAvatarUrl,
            String githubProfileUrl
    ) {

        private static Member from(
                ProjectMemberProfile member,
                Map<Long, URI> mediaUrls
        ) {
            return new Member(
                    member.handle(),
                    member.displayName(),
                    cohortValue(member.cohort()),
                    trackValue(member.track()),
                    toUrl(mediaUrls, member.avatarImageId()),
                    member.githubAvatarUrl(),
                    member.githubProfileUrl()
            );
        }

        private static Integer cohortValue(Cohort cohort) {
            if (cohort == null) {
                return null;
            }
            return cohort.getValue();
        }

        private static String trackValue(Track track) {
            if (track == null) {
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
