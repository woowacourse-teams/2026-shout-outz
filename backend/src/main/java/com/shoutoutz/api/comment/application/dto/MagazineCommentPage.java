package com.shoutoutz.api.comment.application.dto;

import com.shoutoutz.api.comment.domain.MagazineComment;
import java.util.List;

/**
 * 전체 매거진 댓글 조회 결과
 */
public record MagazineCommentPage(
        List<MagazineComment> items,
        boolean hasNext,
        long totalCount
) {

    public MagazineCommentPage {
        items = List.copyOf(items);
    }
}
