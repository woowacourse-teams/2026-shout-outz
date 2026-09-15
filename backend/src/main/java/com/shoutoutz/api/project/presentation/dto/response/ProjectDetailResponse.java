package com.shoutoutz.api.project.presentation.dto.response;

import com.shoutoutz.api.project.domain.ApprovalStatus;
import com.shoutoutz.api.project.domain.ProjectDetail;
import com.shoutoutz.api.project.domain.ServiceStatus;
import java.time.Instant;
import java.util.List;

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

    public static ProjectDetailResponse from(ProjectDetail detail) {
        return new ProjectDetailResponse(
                detail.id(),
                detail.slug(),
                detail.title(),
                detail.teamName(),
                detail.tagline(),
                detail.cohort(),
                detail.thumbnailMediaId(),
                detail.descriptionMd(),
                detail.githubRepositoryUrl(),
                detail.deploymentUrl(),
                detail.serviceStatus(),
                detail.approvalStatus(),
                detail.rejectReason(),
                detail.registeredBy(),
                detail.viewCount(),
                detail.starCount(),
                detail.likeCount(),
                detail.bookmarkCount(),
                detail.likedByMe(),
                detail.bookmarkedByMe(),
                detail.commentCount(),
                detail.techTags().stream().map(TechTag::from).toList(),
                detail.members().stream().map(Member::from).toList(),
                detail.createdAt(),
                detail.updatedAt()
        );
    }

    public record TechTag(long id, String displayName) {

        private static TechTag from(ProjectDetail.TechTag techTag) {
            return new TechTag(techTag.id(), techTag.displayName());
        }
    }

    /**
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

        private static Member from(ProjectDetail.Member member) {
            return new Member(
                    member.userId(),
                    member.handle(),
                    member.displayName(),
                    member.cohort(),
                    member.track(),
                    member.avatarImageId(),
                    member.githubAvatarUrl(),
                    member.githubProfileUrl()
            );
        }
    }
}
