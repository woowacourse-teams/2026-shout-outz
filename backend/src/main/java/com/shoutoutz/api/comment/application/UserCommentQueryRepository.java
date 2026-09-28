package com.shoutoutz.api.comment.application;

import com.shoutoutz.api.comment.application.dto.UserCommentCursor;
import com.shoutoutz.api.comment.application.dto.UserCommentPage;

/**
 * 사용자가 작성한 댓글 통합 조회 포트.
 */
public interface UserCommentQueryRepository {

    UserCommentPage findAllByAuthorId(long authorId, UserCommentCursor cursor, int size);

    UserCommentPage findAllByAuthorId(long authorId, Long viewerId, UserCommentCursor cursor, int size);
}
