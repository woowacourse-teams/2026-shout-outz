package com.shoutoutz.api.post.application;

import com.shoutoutz.api.category.domain.Category;
import com.shoutoutz.api.category.domain.CategoryRepository;
import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.common.exception.custom.NotFoundException;
import com.shoutoutz.api.media.domain.MediaPurpose;
import com.shoutoutz.api.media.domain.MediaStatus;
import com.shoutoutz.api.post.application.dto.PostCursor;
import com.shoutoutz.api.post.application.dto.PostFindAllResult;
import com.shoutoutz.api.post.application.dto.PostItem;
import com.shoutoutz.api.post.application.dto.PostMediaReference;
import com.shoutoutz.api.post.application.dto.PostSort;
import com.shoutoutz.api.post.domain.Post;
import com.shoutoutz.api.post.domain.PostErrorCode;
import com.shoutoutz.api.post.domain.PostRepository;
import com.shoutoutz.api.post.presentation.dto.request.PostFindAllRequest;
import com.shoutoutz.api.post.presentation.dto.request.PostSaveRequest;
import com.shoutoutz.api.post.presentation.dto.request.PostUpdateRequest;
import com.shoutoutz.api.post.presentation.dto.response.PostResponse;
import com.shoutoutz.api.user.domain.account.User;
import com.shoutoutz.api.user.domain.account.UserRepository;
import com.shoutoutz.api.user.domain.account.UserStatus;
import com.shoutoutz.api.user.domain.profile.UserProfile;
import com.shoutoutz.api.user.domain.profile.UserProfileRepository;
import com.shoutoutz.api.user.domain.profile.UserType;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 포스트 생성, 조회, 수정, 삭제 흐름 조정
 */
@Service
@RequiredArgsConstructor
public class PostService {

    private static final Set<UserType> WRITER_TYPES = Set.of(
            UserType.WOOWACOURSE_CREW,
            UserType.WOOWACOURSE_COACH
    );

    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;
    private final PostQueryRepository postQueryRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PostCursorCodec postCursorCodec;
    private final Clock clock;

    @Transactional
    public PostResponse savePost(long userId, PostSaveRequest request) {
        validateWriter(userId);
        validateCategories(request.categoryIds());
        validateMedia(request.mediaIds(), userId);

        Instant now = clock.instant();
        Post savedPost = postRepository.save(Post.create(userId, request.content(), now));
        postRepository.saveCategories(savedPost.getId(), request.categoryIds());
        postRepository.saveMedia(savedPost.getId(), request.mediaIds());

        return PostResponse.from(findPostItem(savedPost.getId()));
    }

    @Transactional(readOnly = true)
    public PostResponse findPost(long postId) {
        return PostResponse.from(findPostItem(postId));
    }

    @Transactional(readOnly = true)
    public PostFindAllResult findAllPost(PostFindAllRequest request) {
        PostSort sort = request.resolvedSort();
        PostCursor cursor = postCursorCodec.decode(request.cursor(), sort);
        int size = request.resolvedSize();
        List<PostItem> postsWithExtraItem = postQueryRepository.findAll(
                sort,
                request.categoryId(),
                cursor,
                size + 1
        );
        return createSlice(postsWithExtraItem, size, sort);
    }

    @Transactional
    public PostResponse updatePost(long postId, long userId, PostUpdateRequest request) {
        Post post = findOwnedPost(postId, userId);
        validateCategories(request.categoryIds());
        validateMedia(request.mediaIds(), userId);

        Post updatedPost = postRepository.update(post.updateContent(request.content(), clock.instant()));
        postRepository.saveCategories(postId, request.categoryIds());
        postRepository.saveMedia(postId, request.mediaIds());
        return PostResponse.from(findPostItem(updatedPost.getId()));
    }

    @Transactional
    public void deletePost(long postId, long userId) {
        Post post = findOwnedPost(postId, userId);
        postRepository.update(post.delete(clock.instant()));
    }

    /**
     * 요청 크기보다 한 건 더 조회한 결과로 다음 Slice 존재 여부와 커서 계산
     */
    private PostFindAllResult createSlice(
            List<PostItem> postsWithExtraItem,
            int size,
            PostSort sort
    ) {
        if (postsWithExtraItem.size() <= size) {
            return new PostFindAllResult(List.copyOf(postsWithExtraItem), null, false);
        }

        List<PostItem> items = List.copyOf(postsWithExtraItem.subList(0, size));
        PostItem lastItem = items.getLast();
        String nextCursor = postCursorCodec.encode(
                new PostCursor(
                        sort,
                        lastItem.likeCount(),
                        lastItem.createdAt(),
                        lastItem.postId()
                )
        );
        return new PostFindAllResult(items, nextCursor, true);
    }

    /**
     * 사용자 존재 여부와 상태를 구분하지 않고 작성 불가를 동일한 응답으로 처리
     */
    private void validateWriter(long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ForbiddenException(
                        PostErrorCode.POST_WRITER_TYPE_FORBIDDEN
                ));
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ForbiddenException(
                        PostErrorCode.POST_WRITER_TYPE_FORBIDDEN
                ));

        if (!isWriter(user, profile)) {
            throw new ForbiddenException(PostErrorCode.POST_WRITER_TYPE_FORBIDDEN);
        }
    }

    private boolean isWriter(User user, UserProfile profile) {
        return user.getStatus() == UserStatus.ACTIVE
                && WRITER_TYPES.contains(profile.getUserType());
    }

    /**
     * 일반 카테고리 하나와 제한 없는 이벤트 카테고리 선택 정책 검증
     */
    private void validateCategories(List<Long> categoryIds) {
        List<Category> categories = categoryRepository.findAllActiveByIds(categoryIds);
        if (categories.size() != categoryIds.size()) {
            throw new BadRequestException(PostErrorCode.POST_CATEGORY_INVALID);
        }
        long generalCategoryCount = categories.stream()
                .filter(Category::isGeneral)
                .count();
        if (generalCategoryCount != 1) {
            throw new BadRequestException(PostErrorCode.POST_CATEGORY_SELECTION_INVALID);
        }
    }

    /**
     * 미디어 존재 여부를 노출하지 않도록 소유자, 용도, 처리 상태를 한 번에 검증
     */
    private void validateMedia(List<Long> mediaIds, long userId) {
        List<PostMediaReference> mediaReferences = postQueryRepository.findAllMediaByIds(mediaIds);
        if (mediaReferences.size() != mediaIds.size()) {
            throw new BadRequestException(PostErrorCode.POST_MEDIA_INVALID);
        }

        for (PostMediaReference reference : mediaReferences) {
            validateMediaReference(reference, userId);
        }
    }

    private void validateMediaReference(PostMediaReference media, long userId) {
        if (media.uploadedBy() != userId
                || media.purpose() != MediaPurpose.POST_CONTENT
                || media.status() != MediaStatus.READY) {
            throw new BadRequestException(PostErrorCode.POST_MEDIA_INVALID);
        }
    }

    private PostItem findPostItem(long postId) {
        return postQueryRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException(PostErrorCode.POST_NOT_FOUND));
    }

    private Post findOwnedPost(long postId, long userId) {
        Post post = postRepository.findActiveById(postId)
                .orElseThrow(() -> new NotFoundException(PostErrorCode.POST_NOT_FOUND));
        if (!post.isWrittenBy(userId)) {
            throw new ForbiddenException(PostErrorCode.POST_AUTHOR_FORBIDDEN);
        }
        return post;
    }
}
