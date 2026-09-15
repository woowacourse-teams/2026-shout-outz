package com.shoutoutz.api.project.presentation.dto.request;

import com.shoutoutz.api.common.util.DataResolveUtil;
import com.shoutoutz.api.project.application.ProjectCursorCodec;
import com.shoutoutz.api.project.domain.ProjectCursor;
import com.shoutoutz.api.project.domain.ProjectSort;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 사용자 프로젝트 목록 조회 파라미터.
 */
public record UserProjectFindRequest(
        @Min(value = 1, message = "size는 1 이상이어야 합니다.")
        @Max(value = 50, message = "size는 50 이하여야 합니다.")
        Integer size,

        String cursor
) {

    private static final int DEFAULT_SIZE = 20;

    public UserProjectFindRequest {
        cursor = DataResolveUtil.sanitizeString(cursor);
    }

    public int resolvedSize() {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        return size;
    }

    public ProjectCursor resolvedCursor() {
        if (cursor == null) {
            return null;
        }
        return ProjectCursorCodec.decode(cursor, ProjectSort.LATEST);
    }
}
