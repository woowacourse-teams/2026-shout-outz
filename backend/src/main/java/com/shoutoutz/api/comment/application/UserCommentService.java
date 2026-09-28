package com.shoutoutz.api.comment.application;

import com.shoutoutz.api.comment.application.dto.UserCommentCursor;
import com.shoutoutz.api.comment.application.dto.UserCommentItem;
import com.shoutoutz.api.comment.application.dto.UserCommentPage;
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
        UserCommentPage page = userCommentQueryRepository.findAllByAuthorId(
                userId,
                cursor,
                size
        );
        return createResult(page);
    }

    public UserCommentResult findAll(long userId, Long viewerId, UserCommentFindRequest request) {
        UserCommentCursor cursor = cursorCodec.decode(request.cursor());
        int size = request.resolvedSize();
        UserCommentPage page = userCommentQueryRepository.findAllByAuthorId(
                userId,
                viewerId,
                cursor,
                size
        );
        return createResult(page);
    }

    private UserCommentResult createResult(UserCommentPage page) {
        if (!page.hasNext()) {
            return new UserCommentResult(page.items(), null, false, page.totalCount());
        }

        List<UserCommentItem> items = page.items();
        UserCommentItem lastItem = items.getLast();
        String nextCursor = cursorCodec.encode(new UserCommentCursor(
                lastItem.createdAt(),
                lastItem.type(),
                lastItem.commentId()
        ));
        return new UserCommentResult(items, nextCursor, true, page.totalCount());
    }
}
