package com.shoutoutz.api.project.domain;

import java.time.Instant;
import java.util.List;

/**
 * 프로젝트 상세 조회에 필요한 조회 모델
 */
public record ProjectDetail(
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
        List<ProjectTechTag> techTags,
        List<ProjectMemberProfile> members,
        Instant createdAt,
        Instant updatedAt
) {

    /**
     * 등록자가 없는 경우, 이전 기수에서 이관된 프로젝트다.
     */
    public boolean isArchived() {
        return registeredBy == null;
    }

    /**
     * 승인된 프로젝트는 누구나, 승인되지 않은 프로젝트는 등록자만 볼 수 있다.
     */
    public boolean isVisibleTo(Long viewerId) {
        return approvalStatus == ApprovalStatus.APPROVED || isEditableBy(viewerId);
    }

    /**
     * 등록자 본인만 수정하거나 삭제할 수 있다.
     * 이관 프로젝트(등록자 null)를 비로그인 사용자(null)가 요청하면 null끼리 같다고 판단되지 않도록, 등록자가 있을 때만 비교한다.
     */
    public boolean isEditableBy(Long viewerId) {
        return registeredBy != null && registeredBy.equals(viewerId);
    }

    public ProjectDetail withTechTagsAndMembers(List<ProjectTechTag> techTags, List<ProjectMemberProfile> members) {
        return new ProjectDetail(
                id,
                slug,
                title,
                teamName,
                tagline,
                cohort,
                thumbnailMediaId,
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
                List.copyOf(techTags),
                List.copyOf(members),
                createdAt,
                updatedAt
        );
    }
}
