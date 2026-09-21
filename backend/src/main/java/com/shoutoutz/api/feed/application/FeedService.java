package com.shoutoutz.api.feed.application;

import com.shoutoutz.api.category.domain.Category;
import com.shoutoutz.api.category.domain.CategoryRepository;
import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.common.exception.custom.NotFoundException;
import com.shoutoutz.api.feed.application.dto.FeedCursor;
import com.shoutoutz.api.feed.application.dto.FeedFindAllResult;
import com.shoutoutz.api.feed.application.dto.FeedItem;
import com.shoutoutz.api.feed.application.dto.FeedMediaReference;
import com.shoutoutz.api.feed.application.dto.FeedSort;
import com.shoutoutz.api.feed.domain.Feed;
import com.shoutoutz.api.feed.domain.FeedErrorCode;
import com.shoutoutz.api.feed.domain.FeedRepository;
import com.shoutoutz.api.media.application.MediaUrlResolver;
import com.shoutoutz.api.feed.presentation.dto.request.FeedFindAllRequest;
import com.shoutoutz.api.feed.presentation.dto.request.FeedSaveRequest;
import com.shoutoutz.api.feed.presentation.dto.request.FeedUpdateRequest;
import com.shoutoutz.api.feed.presentation.dto.request.UserFeedFindRequest;
import com.shoutoutz.api.feed.presentation.dto.response.FeedCommandResponse;
import com.shoutoutz.api.feed.presentation.dto.response.FeedResponse;
import com.shoutoutz.api.media.domain.MediaPurpose;
import com.shoutoutz.api.media.domain.MediaStatus;
import com.shoutoutz.api.user.domain.account.User;
import com.shoutoutz.api.user.domain.account.UserErrorCode;
import com.shoutoutz.api.user.domain.account.UserRepository;
import com.shoutoutz.api.user.domain.account.UserStatus;
import com.shoutoutz.api.user.domain.profile.UserProfile;
import com.shoutoutz.api.user.domain.profile.UserProfileRepository;
import com.shoutoutz.api.user.domain.profile.UserType;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 피드 생성, 조회, 수정, 삭제 흐름 조정
 */
@Service
@RequiredArgsConstructor
public class FeedService {

    private static final Set<UserType> WRITER_TYPES = Set.of(
            UserType.WOOWACOURSE_CREW,
            UserType.WOOWACOURSE_COACH
    );

    private final FeedRepository feedRepository;
    private final CategoryRepository categoryRepository;
    private final FeedQueryRepository feedQueryRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final FeedCursorCodec feedCursorCodec;
    private final MediaUrlResolver mediaUrlResolver;
    private final Clock clock;

    @Transactional
    public FeedCommandResponse saveFeed(long userId, FeedSaveRequest request) {
        validateWriter(userId);
        validateCategories(request.categoryIds());
        validateMedia(request.mediaIds(), userId);

        Instant now = clock.instant();
        Feed savedFeed = feedRepository.save(Feed.create(userId, request.content(), now));
        feedRepository.saveCategories(savedFeed.getId(), request.categoryIds());
        feedRepository.saveMedia(savedFeed.getId(), request.mediaIds());

        return toCommandResponse(findFeedItem(savedFeed.getId()));
    }

    @Transactional(readOnly = true)
    public FeedResponse findFeed(long feedId) {
        return toQueryResponse(findFeedItem(feedId));
    }

    @Transactional(readOnly = true)
    public FeedFindAllResult findAllFeed(FeedFindAllRequest request) {
        FeedSort sort = request.resolvedSort();
        FeedCursor cursor = feedCursorCodec.decode(request.cursor(), sort);
        int size = request.resolvedSize();
        List<FeedItem> feedsWithExtraItem = feedQueryRepository.findAll(
                sort,
                request.categoryId(),
                cursor,
                size + 1
        );
        return createSlice(feedsWithExtraItem, size, sort);
    }

    /**
     * 사용자가 작성한 피드를 최신순으로 조회한다.
     * 탈퇴한 사용자의 피드는 공개하지 않는다.
     */
    @Transactional(readOnly = true)
    public FeedFindAllResult findAllByUser(String handle, UserFeedFindRequest request) {
        User user = userRepository.findByHandle(handle)
                .orElseThrow(() -> new EntityNotFoundException(UserErrorCode.USER_NOT_FOUND));
        if (user.isDeleted()) {
            return new FeedFindAllResult(List.of(), null, false);
        }

        FeedSort sort = FeedSort.LATEST;
        FeedCursor cursor = feedCursorCodec.decode(request.cursor(), sort);
        int size = request.resolvedSize();
        List<FeedItem> feedsWithExtraItem = feedQueryRepository.findAllByAuthorId(
                user.getId(),
                cursor,
                size + 1
        );
        return createSlice(feedsWithExtraItem, size, sort);
    }

    @Transactional
    public FeedCommandResponse updateFeed(long feedId, long userId, FeedUpdateRequest request) {
        Feed feed = findOwnedFeed(feedId, userId);
        validateCategories(request.categoryIds());
        validateMedia(request.mediaIds(), userId);

        Feed updatedFeed = feedRepository.update(feed.updateContent(request.content(), clock.instant()));
        feedRepository.saveCategories(feedId, request.categoryIds());
        feedRepository.saveMedia(feedId, request.mediaIds());
        return toCommandResponse(findFeedItem(updatedFeed.getId()));
    }

    @Transactional
    public void deleteFeed(long feedId, long userId) {
        Feed feed = findOwnedFeed(feedId, userId);
        feedRepository.update(feed.delete(clock.instant()));
    }

    /**
     * 요청 크기보다 한 건 더 조회한 결과로 다음 Slice 존재 여부와 커서 계산
     */
    private FeedFindAllResult createSlice(
            List<FeedItem> feedsWithExtraItem,
            int size,
            FeedSort sort
    ) {
        if (feedsWithExtraItem.size() <= size) {
            List<FeedItem> items = List.copyOf(feedsWithExtraItem);
            return new FeedFindAllResult(items, null, false, resolveMediaUrls(items));
        }

        List<FeedItem> items = List.copyOf(feedsWithExtraItem.subList(0, size));
        FeedItem lastItem = items.getLast();
        String nextCursor = feedCursorCodec.encode(
                new FeedCursor(
                        sort,
                        lastItem.likeCount(),
                        lastItem.createdAt(),
                        lastItem.feedId()
                )
        );
        return new FeedFindAllResult(items, nextCursor, true, resolveMediaUrls(items));
    }

    private FeedResponse toQueryResponse(FeedItem item) {
        return FeedResponse.from(item, resolveMediaUrls(List.of(item)));
    }

    private FeedCommandResponse toCommandResponse(FeedItem item) {
        return FeedCommandResponse.from(item, resolveMediaUrls(List.of(item)));
    }

    private Map<Long, java.net.URI> resolveMediaUrls(List<FeedItem> items) {
        Set<Long> mediaIds = items.stream()
                .flatMap(item -> java.util.stream.Stream.concat(
                        java.util.stream.Stream.of(item.author().avatarImageId()),
                        item.media().stream().map(FeedItem.Media::mediaId)
                ))
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        Map<Long, java.net.URI> urls = mediaUrlResolver.resolveAll(mediaIds);
        return urls == null ? Map.of() : urls;
    }

    /**
     * 사용자 존재 여부와 상태를 구분하지 않고 작성 불가를 동일한 응답으로 처리
     */
    private void validateWriter(long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ForbiddenException(
                        FeedErrorCode.FEED_WRITER_TYPE_FORBIDDEN
                ));
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ForbiddenException(
                        FeedErrorCode.FEED_WRITER_TYPE_FORBIDDEN
                ));

        if (!isWriter(user, profile)) {
            throw new ForbiddenException(FeedErrorCode.FEED_WRITER_TYPE_FORBIDDEN);
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
            throw new BadRequestException(FeedErrorCode.FEED_CATEGORY_INVALID);
        }
        long generalCategoryCount = categories.stream()
                .filter(Category::isGeneral)
                .count();
        if (generalCategoryCount != 1) {
            throw new BadRequestException(FeedErrorCode.FEED_CATEGORY_SELECTION_INVALID);
        }
    }

    /**
     * 미디어 존재 여부를 노출하지 않도록 소유자, 용도, 처리 상태를 한 번에 검증
     */
    private void validateMedia(List<Long> mediaIds, long userId) {
        List<FeedMediaReference> mediaReferences = feedQueryRepository.findAllMediaByIds(mediaIds);
        if (mediaReferences.size() != mediaIds.size()) {
            throw new BadRequestException(FeedErrorCode.FEED_MEDIA_INVALID);
        }

        for (FeedMediaReference reference : mediaReferences) {
            validateMediaReference(reference, userId);
        }
    }

    private void validateMediaReference(FeedMediaReference media, long userId) {
        if (media.uploadedBy() != userId
                || media.purpose() != MediaPurpose.FEED_CONTENT
                || media.status() != MediaStatus.READY) {
            throw new BadRequestException(FeedErrorCode.FEED_MEDIA_INVALID);
        }
    }

    private FeedItem findFeedItem(long feedId) {
        return feedQueryRepository.findById(feedId)
                .orElseThrow(() -> new NotFoundException(FeedErrorCode.FEED_NOT_FOUND));
    }

    private Feed findOwnedFeed(long feedId, long userId) {
        Feed feed = feedRepository.findActiveById(feedId)
                .orElseThrow(() -> new NotFoundException(FeedErrorCode.FEED_NOT_FOUND));
        if (!feed.isWrittenBy(userId)) {
            throw new ForbiddenException(FeedErrorCode.FEED_AUTHOR_FORBIDDEN);
        }
        return feed;
    }
}
