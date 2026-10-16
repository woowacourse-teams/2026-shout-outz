package com.shoutoutz.api.comment.application.dto;

import com.shoutoutz.api.comment.domain.InterviewComment;
import java.util.List;

/**
 * 전체 인터뷰 댓글 조회 결과
 */
public record InterviewCommentPage(
        List<InterviewComment> items,
        boolean hasNext,
        long totalCount
) {

    public InterviewCommentPage {
        items = List.copyOf(items);
    }
}
