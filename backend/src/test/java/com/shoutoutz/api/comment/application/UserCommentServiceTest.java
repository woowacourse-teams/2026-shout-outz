package com.shoutoutz.api.comment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.comment.application.dto.UserCommentCursor;
import com.shoutoutz.api.comment.application.dto.UserCommentItem;
import com.shoutoutz.api.comment.application.dto.UserCommentResult;
import com.shoutoutz.api.comment.application.dto.UserCommentType;
import com.shoutoutz.api.comment.presentation.dto.request.UserCommentFindRequest;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserCommentServiceTest {

    @Mock
    private UserCommentQueryRepository userCommentQueryRepository;

    private UserCommentCursorCodec cursorCodec;
    private UserCommentService userCommentService;

    @BeforeEach
    void setUp() {
        cursorCodec = new UserCommentCursorCodec();
        userCommentService = new UserCommentService(userCommentQueryRepository, cursorCodec);
    }

    @Test
    void 한_건을_더_조회해_다음_페이지_커서를_만든다() {
        UserCommentCursor cursor = new UserCommentCursor(
                Instant.parse("2026-09-16T03:00:00Z"),
                UserCommentType.FEED,
                4L
        );
        List<UserCommentItem> queried = List.of(
                comment(3L, UserCommentType.PROJECT, "2026-09-16T02:00:00Z"),
                comment(2L, UserCommentType.FEED, "2026-09-16T01:00:00Z"),
                comment(1L, UserCommentType.FEED, "2026-09-16T00:00:00Z")
        );
        when(userCommentQueryRepository.findAllByAuthorId(1L, cursor, 3))
                .thenReturn(queried);

        UserCommentResult result = userCommentService.findAll(
                1L,
                new UserCommentFindRequest(cursorCodec.encode(cursor), 2)
        );

        assertThat(result.comments()).containsExactly(queried.get(0), queried.get(1));
        assertThat(result.hasNext()).isTrue();
        assertThat(cursorCodec.decode(result.nextCursor())).isEqualTo(new UserCommentCursor(
                queried.get(1).createdAt(),
                queried.get(1).type(),
                queried.get(1).commentId()
        ));
        verify(userCommentQueryRepository).findAllByAuthorId(1L, cursor, 3);
    }

    @Test
    void 다음_페이지가_없으면_커서를_반환하지_않는다() {
        when(userCommentQueryRepository.findAllByAuthorId(1L, null, 21))
                .thenReturn(List.of(comment(1L, UserCommentType.FEED, "2026-09-16T00:00:00Z")));

        UserCommentResult result = userCommentService.findAll(
                1L,
                new UserCommentFindRequest(null, null)
        );

        assertThat(result.comments()).hasSize(1);
        assertThat(result.nextCursor()).isNull();
        assertThat(result.hasNext()).isFalse();
    }

    private UserCommentItem comment(long id, UserCommentType type, String createdAt) {
        Instant instant = Instant.parse(createdAt);
        return new UserCommentItem(id, type, 100L + id, "댓글 " + id, instant, instant);
    }
}
