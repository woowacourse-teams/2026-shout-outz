package com.shoutoutz.api.comment.application;

import com.shoutoutz.api.comment.application.dto.InterviewCommentCursor;
import com.shoutoutz.api.comment.application.dto.InterviewCommentPage;
import com.shoutoutz.api.comment.domain.InterviewComment;
import com.shoutoutz.api.comment.domain.InterviewCommentSort;
import java.util.List;

public interface InterviewCommentQueryRepository {

    /**
     * cursor와 sort에 맞게 size개의 댓글을 조회한다.
     * 여기서 댓글은 루트 댓글을 기준으로 조회한다.
     *
     * 예를 들어, 5개의 루트 댓글을 조회한다면, 하위 대댓글도 함께 조회된다. 따라서 최소 댓글 개수는 5개이다.
     */
    InterviewCommentPage findRootCommentsPage(
            long interviewId,
            InterviewCommentCursor cursor,
            InterviewCommentSort sort,
            int size
    );

    /**
     * 대댓글 조회용
     */
    List<InterviewComment> findReplies(long interviewId, List<Long> parentIds);

}
