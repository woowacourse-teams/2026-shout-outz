package com.shoutoutz.api.project.presentation.dto.response;

import com.shoutoutz.api.common.response.SliceMetaResponse;
import com.shoutoutz.api.project.application.ProjectCursorCodec;
import com.shoutoutz.api.project.application.dto.UserProjectItem;
import com.shoutoutz.api.project.application.dto.UserProjectResult;
import com.shoutoutz.api.project.domain.ProjectCursor;
import com.shoutoutz.api.project.domain.ServiceStatus;
import java.util.List;

/**
 * 사용자 프로젝트 목록과 커서 정보.
 */
public record UserProjectFindResponse(
        List<Item> projects,
        SliceMetaResponse meta
) {

    public UserProjectFindResponse {
        projects = List.copyOf(projects);
    }

    public static UserProjectFindResponse from(UserProjectResult result) {
        return new UserProjectFindResponse(
                result.projects().stream()
                        .map(Item::from)
                        .toList(),
                new SliceMetaResponse(encodeNextCursor(result.nextCursor()), result.hasNext())
        );
    }

    private static String encodeNextCursor(ProjectCursor cursor) {
        if (cursor == null) {
            return null;
        }
        return ProjectCursorCodec.encode(cursor);
    }

    public record Item(
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
            List<ProjectMemberProfileResponse> members
    ) {

        private static Item from(UserProjectItem item) {
            return new Item(
                    item.id(),
                    item.slug(),
                    item.title(),
                    item.teamName(),
                    item.tagline(),
                    item.cohort(),
                    item.serviceStatus(),
                    item.thumbnailMediaId(),
                    item.likeCount(),
                    item.commentCount(),
                    item.techTags().stream().map(ProjectTechTagResponse::from).toList(),
                    item.members().stream().map(ProjectMemberProfileResponse::from).toList()
            );
        }
    }
}
