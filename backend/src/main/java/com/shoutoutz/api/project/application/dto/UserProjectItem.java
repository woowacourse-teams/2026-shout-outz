package com.shoutoutz.api.project.application.dto;

import com.shoutoutz.api.project.domain.ProjectCursor;
import com.shoutoutz.api.project.domain.ProjectMemberProfile;
import com.shoutoutz.api.project.domain.ProjectTechTag;
import com.shoutoutz.api.project.domain.ServiceStatus;
import java.time.Instant;
import java.util.List;

/**
 * 사용자 페이지의 프로젝트 카드 조회 결과.
 */
public record UserProjectItem(
        long id,
        String slug,
        String title,
        String teamName,
        String tagline,
        int cohort,
        ServiceStatus serviceStatus,
        Long thumbnailMediaId,
        Long registeredBy,
        Integer starCount,
        long likeCount,
        long commentCount,
        long bookmarkCount,
        boolean likedByMe,
        boolean bookmarkedByMe,
        List<ProjectTechTag> techTags,
        List<ProjectMemberProfile> members,
        Instant createdAt
) {

    public UserProjectItem(
            long id,
            String slug,
            String title,
            String teamName,
            String tagline,
            int cohort,
            ServiceStatus serviceStatus,
            Long thumbnailMediaId,
            Long registeredBy,
            Integer starCount,
            long likeCount,
            long commentCount,
            List<ProjectTechTag> techTags,
            List<ProjectMemberProfile> members,
            Instant createdAt
    ) {
        this(
                id,
                slug,
                title,
                teamName,
                tagline,
                cohort,
                serviceStatus,
                thumbnailMediaId,
                registeredBy,
                starCount,
                likeCount,
                commentCount,
                0L,
                false,
                false,
                techTags,
                members,
                createdAt
        );
    }

    public boolean isArchived() {
        return registeredBy == null;
    }

    public ProjectCursor toCursor() {
        return ProjectCursor.latest(createdAt, id);
    }

    public UserProjectItem withTechTagsAndMembers(
            List<ProjectTechTag> techTags,
            List<ProjectMemberProfile> members
    ) {
        return new UserProjectItem(
                id,
                slug,
                title,
                teamName,
                tagline,
                cohort,
                serviceStatus,
                thumbnailMediaId,
                registeredBy,
                starCount,
                likeCount,
                commentCount,
                bookmarkCount,
                likedByMe,
                bookmarkedByMe,
                List.copyOf(techTags),
                List.copyOf(members),
                createdAt
        );
    }
}
