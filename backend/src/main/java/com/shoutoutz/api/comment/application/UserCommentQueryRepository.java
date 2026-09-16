package com.shoutoutz.api.comment.application;

import com.shoutoutz.api.comment.application.dto.UserCommentCursor;
import com.shoutoutz.api.comment.application.dto.UserCommentItem;
import java.util.List;

/**
 * 사용자가 작성한 댓글 통합 조회 포트.
 */
public interface UserCommentQueryRepository {

    List<UserCommentItem> findAllByAuthorId(long authorId, UserCommentCursor cursor, int limit);
}
