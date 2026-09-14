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
        List<TechTag> techTags,
        List<Member> members,
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
     * 이관 프로젝트(등록자 null)를 비로그인 사용자(null)가 요청하면 null끼리 같다고 판단되지 않도록, 등록자가 있을 때만 비교한다.
     */
    public boolean isVisibleTo(Long viewerId) {
        return approvalStatus == ApprovalStatus.APPROVED
                || (registeredBy != null && registeredBy.equals(viewerId));
    }

    public ProjectDetail withTechTagsAndMembers(List<TechTag> techTags, List<Member> members) {
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

    public record TechTag(long id, String displayName) {
    }

    /**
     * 팀원 조회 모델
     * 가입한 사용자는 avatarImageId, 가입하지 않은 이관 팀원은 githubAvatarUrl과 githubProfileUrl을 가진다.
     */
    public record Member(
            Long userId,
            String handle,
            String displayName,
            Integer cohort,
            String track,
            Long avatarImageId,
            String githubAvatarUrl,
            String githubProfileUrl
    ) {

        private static final String WITHDRAWN_DISPLAY_NAME = "탈퇴한 사용자";

        public static Member user(
                long userId,
                String handle,
                String displayName,
                Integer cohort,
                String track,
                Long avatarImageId
        ) {
            return new Member(userId, handle, displayName, cohort, track, avatarImageId, null, null);
        }

        /**
         * 탈퇴한 사용자는 사용자 프로필 조회와 같이 식별자만 남기고, 개인정보를 숨긴다.
         */
        public static Member withdrawn(long userId, String handle) {
            return new Member(userId, handle, WITHDRAWN_DISPLAY_NAME, null, null, null, null, null);
        }

        /**
         * 가입하지 않은 이관 팀원은 GitHub 정보로 보여주고, 기수는 프로젝트 기수로 채운다.
         */
        public static Member archived(
                String displayName,
                int cohort,
                String githubAvatarUrl,
                String githubProfileUrl
        ) {
            return new Member(null, null, displayName, cohort, null, null, githubAvatarUrl, githubProfileUrl);
        }
    }
}
