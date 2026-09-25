package com.shoutoutz.api.feed.application;

import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.feed.domain.FeedErrorCode;
import com.shoutoutz.api.feed.domain.FeedReactionCounts;
import com.shoutoutz.api.feed.domain.FeedReactionRepository;
import com.shoutoutz.api.feed.domain.FeedReactionType;
import com.shoutoutz.api.feed.domain.FeedRepository;
import com.shoutoutz.api.feed.presentation.dto.response.FeedReactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedReactionService {

    private final FeedRepository feedRepository;
    private final FeedReactionRepository feedReactionRepository;

    @Transactional
    public FeedReactionResponse add(long feedId, long userId, String type) {
        FeedReactionType reactionType = FeedReactionType.from(type);
        validateActiveFeed(feedId);

        feedReactionRepository.add(feedId, userId, reactionType);
        return response(feedId, reactionType, true);
    }

    @Transactional
    public FeedReactionResponse remove(long feedId, long userId, String type) {
        FeedReactionType reactionType = FeedReactionType.from(type);
        validateActiveFeed(feedId);

        feedReactionRepository.remove(feedId, userId, reactionType);
        return response(feedId, reactionType, false);
    }

    private void validateActiveFeed(long feedId) {
        feedRepository.findActiveById(feedId)
                .orElseThrow(() -> new EntityNotFoundException(FeedErrorCode.FEED_NOT_FOUND));
    }

    private FeedReactionResponse response(long feedId, FeedReactionType type, boolean active) {
        FeedReactionCounts counts = feedReactionRepository.countByFeedId(feedId);
        return new FeedReactionResponse(
                feedId,
                type,
                active,
                counts.likeCount(),
                counts.bookmarkCount()
        );
    }
}
