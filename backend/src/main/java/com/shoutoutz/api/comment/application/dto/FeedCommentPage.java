package com.shoutoutz.api.comment.application.dto;

import com.shoutoutz.api.comment.domain.FeedComment;
import java.util.List;

/**
 * 전체 피드 댓글 조회시의 응답 객체
 */
public record FeedCommentPage(
        List<FeedComment> comments,
        boolean hasNext
) {

    public FeedCommentPage {
        comments = List.copyOf(comments);
    }
}
