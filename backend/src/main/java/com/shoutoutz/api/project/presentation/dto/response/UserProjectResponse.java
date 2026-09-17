package com.shoutoutz.api.project.presentation.dto.response;

import com.shoutoutz.api.cohort.domain.Cohort;
import com.shoutoutz.api.project.application.dto.UserProjectItem;
import com.shoutoutz.api.project.domain.ProjectMemberProfile;
import com.shoutoutz.api.project.domain.ServiceStatus;
import com.shoutoutz.api.user.domain.profile.Track;
import java.util.List;

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
        Long thumbnailMediaId,
        long likeCount,
        long commentCount,
        List<ProjectTechTagResponse> techTags,
        List<Member> members
) {

    public static List<UserProjectResponse> from(List<UserProjectItem> projects) {
        return projects.stream()
                .map(UserProjectResponse::from)
                .toList();
    }

    private static UserProjectResponse from(UserProjectItem project) {
        return new UserProjectResponse(
                project.id(),
                project.slug(),
                project.title(),
                project.teamName(),
                project.tagline(),
                project.cohort(),
                project.serviceStatus(),
                project.thumbnailMediaId(),
                project.likeCount(),
                project.commentCount(),
                project.techTags().stream().map(ProjectTechTagResponse::from).toList(),
                project.members().stream().map(Member::from).toList()
        );
    }

    public record Member(
            String handle,
            String displayName,
            Integer cohort,
            String track,
            Long avatarImageId,
            String githubAvatarUrl,
            String githubProfileUrl
    ) {

        private static Member from(ProjectMemberProfile member) {
            return new Member(
                    member.handle(),
                    member.displayName(),
                    cohortValue(member.cohort()),
                    trackValue(member.track()),
                    member.avatarImageId(),
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
}
