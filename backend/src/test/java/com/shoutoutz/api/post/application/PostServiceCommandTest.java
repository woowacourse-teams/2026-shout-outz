package com.shoutoutz.api.post.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.category.domain.Category;
import com.shoutoutz.api.category.domain.CategoryRepository;
import com.shoutoutz.api.category.domain.CategoryType;
import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.media.domain.MediaPurpose;
import com.shoutoutz.api.media.domain.MediaStatus;
import com.shoutoutz.api.post.application.dto.PostItem;
import com.shoutoutz.api.post.application.dto.PostMediaReference;
import com.shoutoutz.api.post.domain.Post;
import com.shoutoutz.api.post.domain.PostRepository;
import com.shoutoutz.api.post.presentation.dto.request.PostSaveRequest;
import com.shoutoutz.api.post.presentation.dto.request.PostUpdateRequest;
import com.shoutoutz.api.post.presentation.dto.response.PostResponse;
import com.shoutoutz.api.user.domain.account.User;
import com.shoutoutz.api.user.domain.account.UserRepository;
import com.shoutoutz.api.user.domain.account.UserRole;
import com.shoutoutz.api.user.domain.account.UserStatus;
import com.shoutoutz.api.user.domain.profile.UserProfile;
import com.shoutoutz.api.user.domain.profile.UserProfileRepository;
import com.shoutoutz.api.user.domain.profile.UserType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostServiceCommandTest {

    private static final Instant NOW = Instant.parse("2026-09-11T00:00:00Z");

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

    private PostService postService;

    @BeforeEach
    void setUp() {
        postService = new PostService(
                postRepository,
                categoryRepository,
                postQueryRepository,
                userRepository,
                userProfileRepository,
                new PostCursorCodec(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @ParameterizedTest
    @EnumSource(
            value = UserType.class,
            names = {"WOOWACOURSE_CREW", "WOOWACOURSE_COACH"}
    )
    void 크루나_코치는_카테고리와_READY_미디어를_연결해_포스트를_작성한다(
            UserType userType
    ) {
        PostSaveRequest request = request();
        Post saved = Post.reconstitute(10L, 1L, request.content(), NOW, NOW, null);
        PostItem item = item();
        givenWriter(userType);
        when(categoryRepository.findAllActiveByIds(request.categoryIds()))
                .thenReturn(categories());
        when(postQueryRepository.findAllMediaByIds(request.mediaIds()))
                .thenReturn(List.of(readyPostMedia(20L)));
        when(postRepository.save(any(Post.class))).thenReturn(saved);
        when(postQueryRepository.findById(10L)).thenReturn(Optional.of(item));

        PostResponse result = postService.savePost(1L, request);

        assertThat(result.postId()).isEqualTo(item.postId());
        verify(postRepository).saveCategories(10L, List.of(1L, 2L, 3L));
        verify(postRepository).saveMedia(10L, List.of(20L));
    }

    @Test
    void 일반_사용자는_포스트를_작성할_수_없다() {
        givenWriter(UserType.GENERAL);

        assertThatThrownBy(() -> postService.savePost(1L, request()))
                .isInstanceOf(ForbiddenException.class);

        verify(postRepository, never()).save(any());
    }

    @Test
    void 비활성_크루는_포스트를_작성할_수_없다() {
        givenWriter(UserStatus.BANNED, UserType.WOOWACOURSE_CREW);

        assertThatThrownBy(() -> postService.savePost(1L, request()))
                .isInstanceOf(ForbiddenException.class);

        verify(postRepository, never()).save(any());
    }

    @Test
    void 비활성_카테고리가_포함되면_작성하지_않는다() {
        givenWriter(UserType.WOOWACOURSE_CREW);
        when(categoryRepository.findAllActiveByIds(List.of(1L, 2L, 3L)))
                .thenReturn(List.of(generalCategory(1L)));

        assertThatThrownBy(() -> postService.savePost(1L, request()))
                .isInstanceOf(BadRequestException.class);

        verify(postRepository, never()).save(any());
    }

    @Test
    void 본인_소유의_READY_본문_이미지가_아니면_작성하지_않는다() {
        givenWriter(UserType.WOOWACOURSE_CREW);
        when(categoryRepository.findAllActiveByIds(List.of(1L, 2L, 3L)))
                .thenReturn(categories());
        when(postQueryRepository.findAllMediaByIds(List.of(20L)))
                .thenReturn(List.of(new PostMediaReference(
                        20L,
                        2L,
                        MediaPurpose.POST_CONTENT,
                        MediaStatus.READY
                )));

        assertThatThrownBy(() -> postService.savePost(1L, request()))
                .isInstanceOf(BadRequestException.class);

        verify(postRepository, never()).save(any());
    }

    @Test
    void 작성자는_포스트_본문과_카테고리와_미디어를_수정한다() {
        Post post = Post.reconstitute(10L, 1L, "기존 본문", NOW, NOW, null);
        PostUpdateRequest request = new PostUpdateRequest(
                "수정 본문",
                List.of(3L),
                List.of()
        );
        when(postRepository.findActiveById(10L)).thenReturn(Optional.of(post));
        when(categoryRepository.findAllActiveByIds(List.of(3L)))
                .thenReturn(List.of(generalCategory(3L)));
        when(postQueryRepository.findAllMediaByIds(List.of())).thenReturn(List.of());
        when(postRepository.update(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(postQueryRepository.findById(10L)).thenReturn(Optional.of(item()));

        postService.updatePost(10L, 1L, request);

        verify(postRepository).saveCategories(10L, List.of(3L));
        verify(postRepository).saveMedia(10L, List.of());
    }

    @Test
    void 작성자가_아니면_포스트를_수정할_수_없다() {
        Post post = Post.reconstitute(10L, 2L, "본문", NOW, NOW, null);
        when(postRepository.findActiveById(10L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.updatePost(
                10L,
                1L,
                new PostUpdateRequest("수정", List.of(3L), List.of())
        )).isInstanceOf(ForbiddenException.class);

        verify(postRepository, never()).update(any());
    }

    @Test
    void 작성자는_포스트를_soft_delete한다() {
        Post post = Post.reconstitute(10L, 1L, "본문", NOW, NOW, null);
        when(postRepository.findActiveById(10L)).thenReturn(Optional.of(post));
        when(postRepository.update(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        postService.deletePost(10L, 1L);

        verify(postRepository).update(any(Post.class));
    }

    @Test
    void 작성자가_아니면_포스트를_삭제할_수_없다() {
        Post post = Post.reconstitute(10L, 2L, "본문", NOW, NOW, null);
        when(postRepository.findActiveById(10L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.deletePost(10L, 1L))
                .isInstanceOf(ForbiddenException.class);

        verify(postRepository, never()).update(any());
    }

    @Test
    void 일반_카테고리가_없으면_포스트를_작성하지_않는다() {
        givenWriter(UserType.WOOWACOURSE_CREW);
        when(categoryRepository.findAllActiveByIds(List.of(2L, 3L)))
                .thenReturn(List.of(eventCategory(2L), eventCategory(3L)));
        PostSaveRequest request = new PostSaveRequest("본문", List.of(2L, 3L), List.of());

        assertThatThrownBy(() -> postService.savePost(1L, request))
                .isInstanceOf(BadRequestException.class);

        verify(postRepository, never()).save(any());
    }

    @Test
    void 일반_카테고리가_둘_이상이면_포스트를_작성하지_않는다() {
        givenWriter(UserType.WOOWACOURSE_CREW);
        when(categoryRepository.findAllActiveByIds(List.of(1L, 4L)))
                .thenReturn(List.of(generalCategory(1L), generalCategory(4L)));
        PostSaveRequest request = new PostSaveRequest("본문", List.of(1L, 4L), List.of());

        assertThatThrownBy(() -> postService.savePost(1L, request))
                .isInstanceOf(BadRequestException.class);

        verify(postRepository, never()).save(any());
    }

    private PostSaveRequest request() {
        return new PostSaveRequest("본문", List.of(1L, 2L, 3L), List.of(20L));
    }

    private List<Category> categories() {
        return List.of(
                generalCategory(1L),
                eventCategory(2L),
                eventCategory(3L)
        );
    }

    private Category generalCategory(long categoryId) {
        return category(categoryId, CategoryType.GENERAL);
    }

    private Category eventCategory(long categoryId) {
        return category(categoryId, CategoryType.EVENT);
    }

    private Category category(long categoryId, CategoryType type) {
        return Category.reconstitute(
                categoryId,
                "category-" + categoryId,
                "카테고리 " + categoryId,
                type,
                0,
                true
        );
    }

    private void givenWriter(UserType userType) {
        givenWriter(UserStatus.ACTIVE, userType);
    }

    private void givenWriter(UserStatus userStatus, UserType userType) {
        User user = User.builder()
                .id(1L)
                .handle("zzaekkii")
                .status(userStatus)
                .role(UserRole.USER)
                .build();
        UserProfile profile = createProfile(userType);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
    }

    private UserProfile createProfile(UserType userType) {
        if (userType == UserType.WOOWACOURSE_CREW) {
            return UserProfile.builder()
                    .userId(1L)
                    .displayName("재키")
                    .userType(userType)
                    .track("BACKEND")
                    .cohort((short) 8)
                    .build();
        }
        if (userType == UserType.WOOWACOURSE_COACH) {
            return UserProfile.builder()
                    .userId(1L)
                    .displayName("코치")
                    .userType(userType)
                    .build();
        }
        return UserProfile.initialize(1L, "일반 사용자");
    }

    private PostMediaReference readyPostMedia(long mediaId) {
        return new PostMediaReference(
                mediaId,
                1L,
                MediaPurpose.POST_CONTENT,
                MediaStatus.READY
        );
    }

    private PostItem item() {
        return new PostItem(
                10L,
                "본문",
                new PostItem.Author(
                        "zzaekkii",
                        "재키",
                        UserType.WOOWACOURSE_CREW,
                        "BACKEND",
                        (short) 8,
                        null
                ),
                List.of(new PostItem.Category(
                        1L,
                        "backend",
                        "백엔드",
                        CategoryType.GENERAL
                )),
                List.of(new PostItem.Media(20L, 0)),
                0L,
                NOW,
                NOW
        );
    }
}
