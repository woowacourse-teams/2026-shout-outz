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
        List<ProjectTechTag> techTags,
        List<ProjectMemberProfile> members,
        Instant createdAt
) {

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
                List.copyOf(techTags),
                List.copyOf(members),
                createdAt
        );
    }
}
