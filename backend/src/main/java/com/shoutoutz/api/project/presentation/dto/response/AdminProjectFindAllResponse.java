package com.shoutoutz.api.project.presentation.dto.response;

import com.shoutoutz.api.common.response.SliceMetaResponse;
import com.shoutoutz.api.project.application.dto.AdminProjectItem;
import com.shoutoutz.api.project.domain.ApprovalStatus;
import com.shoutoutz.api.project.domain.ServiceStatus;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record AdminProjectFindAllResponse(
        List<Item> items,
        SliceMetaResponse meta
) {

    public AdminProjectFindAllResponse {
        items = List.copyOf(items);
    }

    public static AdminProjectFindAllResponse from(
            List<AdminProjectItem> items,
            SliceMetaResponse meta,
            Map<Long, URI> mediaUrls
    ) {
        return from(items, meta, mediaUrls, Map.of());
    }

    public static AdminProjectFindAllResponse from(
            List<AdminProjectItem> items,
            SliceMetaResponse meta,
            Map<Long, URI> mediaUrls,
            Map<Long, String> userAvatarUrls
    ) {
        return new AdminProjectFindAllResponse(
                items.stream()
                        .map(item -> Item.from(item, mediaUrls, userAvatarUrls))
                        .toList(),
                meta
        );
    }

    public record Item(
            long id,
            String slug,
            String title,
            String teamName,
            String tagline,
            int cohort,
            Long thumbnailImageId,
            String thumbnailUrl,
            Integer starCount,
            long likeCount,
            long commentCount,
            long bookmarkCount,
            boolean likedByMe,
            boolean bookmarkedByMe,
            ServiceStatus serviceStatus,
            ApprovalStatus approvalStatus,
            String rejectReason,
            Long registeredBy,
            List<ProjectTechTagResponse> techTags,
            List<ProjectMemberProfileResponse> members,
            Instant createdAt,
            Instant updatedAt
    ) {

        private static Item from(
                AdminProjectItem item,
                Map<Long, URI> mediaUrls,
                Map<Long, String> userAvatarUrls
        ) {
            return new Item(
                    item.projectId(),
                    item.slug(),
                    item.title(),
                    item.teamName(),
                    item.tagline(),
                    item.cohort(),
                    item.thumbnailImageId(),
                    toUrl(mediaUrls, item.thumbnailImageId()),
                    item.starCount(),
                    item.likeCount(),
                    item.commentCount(),
                    item.bookmarkCount(),
                    false,
                    false,
                    item.serviceStatus(),
                    item.approvalStatus(),
                    item.rejectReason(),
                    item.registeredBy(),
                    item.techTags().stream().map(ProjectTechTagResponse::from).toList(),
                    item.members().stream()
                            .map(member -> ProjectMemberProfileResponse.from(
                                    member,
                                    mediaUrls,
                                    userAvatarUrls
                            ))
                            .toList(),
                    item.createdAt(),
                    item.updatedAt()
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
}
