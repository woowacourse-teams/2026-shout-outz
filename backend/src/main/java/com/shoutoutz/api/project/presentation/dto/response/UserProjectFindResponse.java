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
        ProjectCursor cursor = result.nextCursor();
        String nextCursor = cursor == null
                ? null
                : ProjectCursorCodec.encode(cursor);
        return new UserProjectFindResponse(
                result.projects().stream()
                        .map(ProjectFindAllResponse.Item::from)
                        .toList(),
                new SliceMetaResponse(nextCursor, result.hasNext())
        );
    }
}
