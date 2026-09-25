package com.shoutoutz.api.news.application;

import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.news.domain.NewsErrorCode;
import com.shoutoutz.api.news.domain.NewsReactionRepository;
import com.shoutoutz.api.news.domain.NewsReactionType;
import com.shoutoutz.api.news.domain.NewsRepository;
import com.shoutoutz.api.news.presentation.dto.response.NewsReactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NewsReactionService {

    private final NewsRepository newsRepository;
    private final NewsReactionRepository newsReactionRepository;

    @Transactional
    public NewsReactionResponse add(long newsId, long userId, String type) {
        NewsReactionType reactionType = NewsReactionType.from(type);
        validateActiveNews(newsId);

        newsReactionRepository.add(newsId, userId, reactionType);
        return response(newsId, reactionType, true);
    }

    @Transactional
    public NewsReactionResponse remove(long newsId, long userId, String type) {
        NewsReactionType reactionType = NewsReactionType.from(type);
        validateActiveNews(newsId);

        newsReactionRepository.remove(newsId, userId, reactionType);
        return response(newsId, reactionType, false);
    }

    private void validateActiveNews(long newsId) {
        newsRepository.findActiveById(newsId)
                .orElseThrow(() -> new EntityNotFoundException(NewsErrorCode.NEWS_NOT_FOUND));
    }

    private NewsReactionResponse response(long newsId, NewsReactionType type, boolean active) {
        return new NewsReactionResponse(
                newsId,
                type,
                active,
                newsReactionRepository.countByNewsId(newsId)
        );
    }
}
