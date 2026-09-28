package com.shoutoutz.api.comment.application.dto;

import com.shoutoutz.api.comment.domain.ProjectComment;
import java.util.List;

/**
 * 전체 프로젝트 댓글 조회 결과
 */
public record ProjectCommentPage(
        List<ProjectComment> items,
        boolean hasNext,
        long totalCount
) {

    public ProjectCommentPage {
        items = List.copyOf(items);
    }
}
