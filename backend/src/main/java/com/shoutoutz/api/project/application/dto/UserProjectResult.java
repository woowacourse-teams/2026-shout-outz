package com.shoutoutz.api.project.application.dto;

import com.shoutoutz.api.project.domain.ProjectCursor;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * 사용자가 참여한 프로젝트 목록과 다음 조회 여부.
 */
public record UserProjectResult(
        List<UserProjectItem> projects,
        boolean hasNext,
        long totalCount,
        Map<Long, URI> mediaUrls,
        Map<Long, String> userAvatarUrls
) {

    public UserProjectResult(
            List<UserProjectItem> projects,
            boolean hasNext,
            long totalCount,
            Map<Long, URI> mediaUrls
    ) {
        this(projects, hasNext, totalCount, mediaUrls, Map.of());
    }

    public UserProjectResult {
        projects = List.copyOf(projects);
        mediaUrls = mediaUrls == null ? Map.of() : Map.copyOf(mediaUrls);
        userAvatarUrls = userAvatarUrls == null ? Map.of() : Map.copyOf(userAvatarUrls);
    }

    public ProjectCursor nextCursor() {
        if (!hasNext || projects.isEmpty()) {
            return null;
        }
        return projects.getLast().toCursor();
    }
}
