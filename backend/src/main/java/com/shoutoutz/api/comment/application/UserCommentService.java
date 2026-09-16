package com.shoutoutz.api.comment.application;

import com.shoutoutz.api.comment.application.dto.UserCommentCursor;
import com.shoutoutz.api.comment.application.dto.UserCommentItem;
import com.shoutoutz.api.comment.application.dto.UserCommentResult;
import com.shoutoutz.api.comment.presentation.dto.request.UserCommentFindRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 사용자가 작성한 댓글 목록 조회 흐름.
 */
@Service
@RequiredArgsConstructor
public class UserCommentService {

    private final UserCommentQueryRepository userCommentQueryRepository;
    private final UserCommentCursorCodec cursorCodec;

    public UserCommentResult findAll(long userId, UserCommentFindRequest request) {
        UserCommentCursor cursor = cursorCodec.decode(request.cursor());
        int size = request.resolvedSize();
        List<UserCommentItem> comments = userCommentQueryRepository.findAllByAuthorId(
                userId,
                cursor,
                size + 1
        );
        return createResult(comments, size);
    }

    private UserCommentResult createResult(List<UserCommentItem> comments, int size) {
        if (comments.size() <= size) {
            return new UserCommentResult(comments, null, false);
        }

        List<UserCommentItem> items = List.copyOf(comments.subList(0, size));
        UserCommentItem lastItem = items.getLast();
        String nextCursor = cursorCodec.encode(new UserCommentCursor(
                lastItem.createdAt(),
                lastItem.type(),
                lastItem.commentId()
        ));
        return new UserCommentResult(items, nextCursor, true);
    }
}
