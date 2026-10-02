package com.shoutoutz.api.comment.application.dto;

import com.shoutoutz.api.comment.domain.FeedComment;
import java.util.List;

/**
 * 전체 피드 댓글 조회 결과
 */
public record FeedCommentPage(
        List<FeedComment> items,
        boolean hasNext,
        long totalCount
) {

    public FeedCommentPage {
        items = List.copyOf(items);
    }
}
