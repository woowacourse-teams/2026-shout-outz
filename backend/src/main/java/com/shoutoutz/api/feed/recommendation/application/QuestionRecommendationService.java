package com.shoutoutz.api.feed.recommendation.application;

import com.shoutoutz.api.feed.application.dto.FeedItem;
import com.shoutoutz.api.feed.recommendation.application.dto.QuestionRecommendationItem;
import com.shoutoutz.api.feed.recommendation.application.dto.QuestionRecommendationSlot;
import com.shoutoutz.api.feed.recommendation.application.dto.RelatedQuestionRecommendations;
import com.shoutoutz.api.feed.recommendation.domain.QuestionRecommendationStatus;
import com.shoutoutz.api.feed.recommendation.infrastructure.QuestionRecommendationProperties;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QuestionRecommendationService {

    private static final int LIST_INTERVAL = 5;

    private final QuestionRecommendationRepository repository;
    private final QuestionRecommendationProperties properties;
    private final Clock clock;

    @Transactional
    public QuestionRecommendationStatus requestGeneration(long feedId, String title, String content) {
        if (!properties.sqs().enabled()) {
            return null;
        }
        repository.requestGeneration(feedId, title, content, UUID.randomUUID(), clock.instant());
        return QuestionRecommendationStatus.PENDING;
    }

    @Transactional(readOnly = true)
    public Optional<RelatedQuestionRecommendations> findByFeedId(long feedId) {
        return repository.findByFeedId(feedId);
    }

    @Transactional(readOnly = true)
    public List<QuestionRecommendationSlot> findListSlots(List<FeedItem> questions) {
        if (questions == null || questions.size() < LIST_INTERVAL) {
            return List.of();
        }

        int slotCount = questions.size() / LIST_INTERVAL;
        List<QuestionRecommendationItem> recommendations = repository.findRandomItems(slotCount);
        int resultSize = Math.min(slotCount, recommendations.size());
        List<QuestionRecommendationSlot> slots = new ArrayList<>(resultSize);
        for (int index = 0; index < resultSize; index++) {
            long afterFeedId = questions.get((index + 1) * LIST_INTERVAL - 1).feedId();
            slots.add(new QuestionRecommendationSlot(afterFeedId, recommendations.get(index)));
        }
        return List.copyOf(slots);
    }
}
