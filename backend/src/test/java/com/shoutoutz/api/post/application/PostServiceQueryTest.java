package com.shoutoutz.api.post.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.category.domain.CategoryRepository;
import com.shoutoutz.api.common.exception.custom.NotFoundException;
import com.shoutoutz.api.post.application.dto.PostCursor;
import com.shoutoutz.api.post.application.dto.PostFindAllResult;
import com.shoutoutz.api.post.application.dto.PostItem;
import com.shoutoutz.api.post.application.dto.PostSort;
import com.shoutoutz.api.post.domain.PostRepository;
import com.shoutoutz.api.post.presentation.dto.request.PostFindAllRequest;
import com.shoutoutz.api.user.domain.account.UserRepository;
import com.shoutoutz.api.user.domain.profile.UserProfileRepository;
import com.shoutoutz.api.user.domain.profile.UserType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostServiceQueryTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private PostQueryRepository postQueryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    private PostCursorCodec cursorCodec;
    private PostService postService;

    @BeforeEach
    void setUp() {
        cursorCodec = new PostCursorCodec();
        postService = new PostService(
                postRepository,
                categoryRepository,
                postQueryRepository,
                userRepository,
                userProfileRepository,
                cursorCodec,
                java.time.Clock.systemUTC()
        );
    }

    @Test
    void 활성_포스트_상세를_조회한다() {
        PostItem post = post(1L, "2026-09-11T00:00:00Z");
        when(postQueryRepository.findById(1L)).thenReturn(Optional.of(post));

        assertThat(postService.findPost(1L).postId()).isEqualTo(post.postId());
    }

    @Test
    void 삭제되었거나_없는_포스트는_조회할_수_없다() {
        when(postQueryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.findPost(1L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void size보다_한_건_더_조회해_다음_슬라이스_커서를_만든다() {
        PostCursor cursor = new PostCursor(
                PostSort.LATEST,
                0L,
                Instant.parse("2026-09-12T00:00:00Z"),
                4L
        );
        PostFindAllRequest request = new PostFindAllRequest(
                PostSort.LATEST,
                1L,
                cursorCodec.encode(cursor),
                2
        );
        List<PostItem> queried = List.of(
                post(3L, "2026-09-11T00:00:00Z"),
                post(2L, "2026-09-10T00:00:00Z"),
                post(1L, "2026-09-09T00:00:00Z")
        );
        when(postQueryRepository.findAll(PostSort.LATEST, 1L, cursor, 3))
                .thenReturn(queried);

        PostFindAllResult result = postService.findAllPost(request);

        assertThat(result.items()).containsExactly(queried.get(0), queried.get(1));
        assertThat(result.hasNext()).isTrue();
        assertThat(cursorCodec.decode(result.nextCursor(), PostSort.LATEST))
                .isEqualTo(new PostCursor(
                        PostSort.LATEST,
                        0L,
                        queried.get(1).createdAt(),
                        2L
                ));
        verify(postQueryRepository).findAll(PostSort.LATEST, 1L, cursor, 3);
    }

    @Test
    void 다음_슬라이스가_없으면_커서를_반환하지_않는다() {
        PostFindAllRequest request = new PostFindAllRequest(null, null, null, 2);
        when(postQueryRepository.findAll(PostSort.LATEST, null, null, 3))
                .thenReturn(List.of(post(1L, "2026-09-11T00:00:00Z")));

        PostFindAllResult result = postService.findAllPost(request);

        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isNull();
    }

    @Test
    void 인기순을_요청하면_전체_좋아요_수_기준으로_조회한다() {
        PostFindAllRequest request = new PostFindAllRequest(
                PostSort.POPULAR,
                null,
                null,
                2
        );
        List<PostItem> queried = List.of(
                post(2L, "2026-09-10T00:00:00Z", 5L),
                post(1L, "2026-09-09T00:00:00Z", 3L)
        );
        when(postQueryRepository.findAll(PostSort.POPULAR, null, null, 3))
                .thenReturn(queried);

        PostFindAllResult result = postService.findAllPost(request);

        assertThat(result.items()).containsExactlyElementsOf(queried);
        verify(postQueryRepository).findAll(PostSort.POPULAR, null, null, 3);
    }

    private PostItem post(long id, String createdAt) {
        return post(id, createdAt, 0L);
    }

    private PostItem post(long id, String createdAt, long likeCount) {
        Instant instant = Instant.parse(createdAt);
        return new PostItem(
                id,
                "본문 " + id,
                new PostItem.Author(
                        "zzaekkii",
                        "재키",
                        UserType.WOOWACOURSE_CREW,
                        "BACKEND",
                        (short) 8,
                        null
                ),
                List.of(),
                List.of(),
                likeCount,
                instant,
                instant
        );
    }
}
