package com.shoutoutz.api.project.application.dto;

import com.shoutoutz.api.project.domain.ApprovalStatus;
import com.shoutoutz.api.project.domain.ProjectMemberProfile;
import com.shoutoutz.api.project.domain.ProjectTechTag;
import com.shoutoutz.api.project.domain.ServiceStatus;
import java.time.Instant;
import java.util.List;

public record AdminProjectItem(
        long projectId,
        String slug,
        String title,
        String teamName,
        String tagline,
        int cohort,
        ServiceStatus serviceStatus,
        ApprovalStatus approvalStatus,
        String rejectReason,
        Long thumbnailImageId,
        Long registeredBy,
        Integer starCount,
        long likeCount,
        long commentCount,
        long bookmarkCount,
        List<ProjectTechTag> techTags,
        List<ProjectMemberProfile> members,
        Instant createdAt,
        Instant updatedAt
) {

    public AdminProjectItem {
        techTags = List.copyOf(techTags);
        members = List.copyOf(members);
    }

    public AdminProjectCursor toCursor() {
        return new AdminProjectCursor(createdAt, projectId);
    }

    public AdminProjectItem withTechTagsAndMembers(
            List<ProjectTechTag> techTags,
            List<ProjectMemberProfile> members
    ) {
        return new AdminProjectItem(
                projectId,
                slug,
                title,
                teamName,
                tagline,
                cohort,
                serviceStatus,
                approvalStatus,
                rejectReason,
                thumbnailImageId,
                registeredBy,
                starCount,
                likeCount,
                commentCount,
                bookmarkCount,
                techTags,
                members,
                createdAt,
                updatedAt
        );
    }
}
