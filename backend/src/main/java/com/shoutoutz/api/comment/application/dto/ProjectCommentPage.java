package com.shoutoutz.api.comment.application.dto;

import com.shoutoutz.api.comment.domain.ProjectComment;
import java.util.List;

/**
 * 전체 댓글 조회시의 응답 객체
 */
public record ProjectCommentPage(
        List<ProjectComment> comments,
        boolean hasNext
) {

    public ProjectCommentPage {
        comments = List.copyOf(comments);
    }
}
