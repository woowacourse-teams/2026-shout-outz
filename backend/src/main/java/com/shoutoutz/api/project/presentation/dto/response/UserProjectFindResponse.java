package com.shoutoutz.api.project.presentation.dto.response;

import com.shoutoutz.api.common.response.SliceMetaResponse;
import com.shoutoutz.api.project.application.ProjectCursorCodec;
import com.shoutoutz.api.project.application.dto.UserProjectResult;
import com.shoutoutz.api.project.domain.ProjectCursor;
import java.util.List;

/**
 * 사용자 프로젝트 목록과 커서 정보.
 */
public record UserProjectFindResponse(
        List<ProjectFindAllResponse.Item> projects,
        SliceMetaResponse meta
) {

    public UserProjectFindResponse {
        projects = List.copyOf(projects);
    }

    public static UserProjectFindResponse from(UserProjectResult result) {
        return new UserProjectFindResponse(
                result.projects().stream()
                        .map(ProjectFindAllResponse.Item::from)
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
}
