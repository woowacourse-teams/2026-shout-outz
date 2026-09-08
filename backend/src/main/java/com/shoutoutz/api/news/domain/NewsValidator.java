package com.shoutoutz.api.news.domain;

import static com.shoutoutz.api.news.domain.NewsErrorCode.NEWS_AUTHOR_ID_NULL;
import static com.shoutoutz.api.news.domain.NewsErrorCode.NEWS_AUTHOR_NAME_NULL_OR_BLANK;
import static com.shoutoutz.api.news.domain.NewsErrorCode.NEWS_BODY_NULL_OR_BLANK;
import static com.shoutoutz.api.news.domain.NewsErrorCode.NEWS_CTA_INVALID_LABEL_LENGTH;
import static com.shoutoutz.api.news.domain.NewsErrorCode.NEWS_CTA_INVALID_URL_LENGTH;
import static com.shoutoutz.api.news.domain.NewsErrorCode.NEWS_CTA_LABEL_NULL_OR_BLANK;
import static com.shoutoutz.api.news.domain.NewsErrorCode.NEWS_CTA_URL_NULL_OR_BLANK;
import static com.shoutoutz.api.news.domain.NewsErrorCode.NEWS_EVENT_START_AT_NOT_ALLOWED;
import static com.shoutoutz.api.news.domain.NewsErrorCode.NEWS_EVENT_START_AT_NULL;
import static com.shoutoutz.api.news.domain.NewsErrorCode.NEWS_INVALID_AUTHOR_ID_SIZE;
import static com.shoutoutz.api.news.domain.NewsErrorCode.NEWS_INVALID_AUTHOR_NAME_LENGTH;
import static com.shoutoutz.api.news.domain.NewsErrorCode.NEWS_INVALID_BODY_LENGTH;
import static com.shoutoutz.api.news.domain.NewsErrorCode.NEWS_INVALID_ID_SIZE;
import static com.shoutoutz.api.news.domain.NewsErrorCode.NEWS_INVALID_PIN_ORDER;
import static com.shoutoutz.api.news.domain.NewsErrorCode.NEWS_INVALID_SUMMARY_LENGTH;
import static com.shoutoutz.api.news.domain.NewsErrorCode.NEWS_INVALID_TITLE_LENGTH;
import static com.shoutoutz.api.news.domain.NewsErrorCode.NEWS_PIN_ORDER_NOT_NULL;
import static com.shoutoutz.api.news.domain.NewsErrorCode.NEWS_PUBLISHED_AT_NULL;
import static com.shoutoutz.api.news.domain.NewsErrorCode.NEWS_SUMMARY_NULL_OR_BLANK;
import static com.shoutoutz.api.news.domain.NewsErrorCode.NEWS_TITLE_NULL_OR_BLANK;
import static com.shoutoutz.api.news.domain.NewsErrorCode.NEWS_TYPE_NULL;

import com.shoutoutz.api.common.exception.custom.DomainValidationException;
import com.shoutoutz.api.common.validator.DomainValidator;
import java.time.Instant;

/**
 * 소식 도메인 전용 Validator.
 */
final class NewsValidator extends DomainValidator {

    private static final int MIN_ID_SIZE = 0;
    private static final int MIN_AUTHOR_ID_SIZE = 1;
    private static final int MIN_PIN_ORDER = 1;
    private static final int MAX_TITLE_LENGTH = 100;
    private static final int MAX_SUMMARY_LENGTH = 200;
    private static final int MAX_BODY_LENGTH = 100_000;
    private static final int MAX_AUTHOR_NAME_LENGTH = 50;
    private static final int MAX_CTA_LABEL_LENGTH = 100;
    private static final int MAX_CTA_URL_LENGTH = 2_048;

    private NewsValidator() {
    }

    /**
     * 유효성 검증 진입점 접근 제한자 private-package 준수 순수 검증만 담당
     */
    static void validateNews(
            Long id,
            NewsType type,
            String title,
            String summary,
            String body,
            Long authorId,
            String authorName,
            Instant publishedAt,
            NewsEventPeriod eventPeriod,
            boolean pinned,
            Integer pinOrder
    ) {
        validateId(id);
        validateType(type);
        validateTitle(title);
        validateSummary(summary);
        validateBody(body);
        validateAuthorId(authorId);
        validateAuthorName(authorName);
        validatePublishedAt(publishedAt);
        validateEventPeriod(type, eventPeriod);
        validatePin(pinned, pinOrder);
    }

    static void validateNewsCta(String label, String url) {
        validateCtaLabel(label);
        validateCtaUrl(url);
    }

    private static void validateId(Long id) {
        if (id == null) {
            return;
        }
        validateLongMinSize(id, MIN_ID_SIZE, NEWS_INVALID_ID_SIZE);
    }

    private static void validateType(NewsType type) {
        validateNotNull(type, NEWS_TYPE_NULL);
    }

    private static void validateTitle(String title) {
        validateNotNullOrBlank(title, NEWS_TITLE_NULL_OR_BLANK);
        validateMaxLength(title, MAX_TITLE_LENGTH, NEWS_INVALID_TITLE_LENGTH);
    }

    private static void validateSummary(String summary) {
        validateNotNullOrBlank(summary, NEWS_SUMMARY_NULL_OR_BLANK);
        validateMaxLength(summary, MAX_SUMMARY_LENGTH, NEWS_INVALID_SUMMARY_LENGTH);
    }

    private static void validateBody(String body) {
        validateNotNullOrBlank(body, NEWS_BODY_NULL_OR_BLANK);
        validateMaxLength(body, MAX_BODY_LENGTH, NEWS_INVALID_BODY_LENGTH);
    }

    private static void validateAuthorId(Long authorId) {
        validateNotNull(authorId, NEWS_AUTHOR_ID_NULL);
        validateLongMinSize(authorId, MIN_AUTHOR_ID_SIZE, NEWS_INVALID_AUTHOR_ID_SIZE);
    }

    private static void validateAuthorName(String authorName) {
        validateNotNullOrBlank(authorName, NEWS_AUTHOR_NAME_NULL_OR_BLANK);
        validateMaxLength(authorName, MAX_AUTHOR_NAME_LENGTH, NEWS_INVALID_AUTHOR_NAME_LENGTH);
    }

    private static void validatePublishedAt(Instant publishedAt) {
        validateNotNull(publishedAt, NEWS_PUBLISHED_AT_NULL);
        //TODO: PublishedAt 포멧 검증 필요
    }

    private static void validateEventPeriod(NewsType type, NewsEventPeriod eventPeriod) {
        if (type == NewsType.EVENT) {
            validateNotNull(eventPeriod, NEWS_EVENT_START_AT_NULL);
            return;
        }

        if (eventPeriod != null) {
            throw new DomainValidationException(NEWS_EVENT_START_AT_NOT_ALLOWED);
        }
    }

    private static void validatePin(boolean pinned, Integer pinOrder) {
        if (!pinned && pinOrder != null) {
            throw new DomainValidationException(NEWS_PIN_ORDER_NOT_NULL);
        }
        if (pinned) {
            validateNotNull(pinOrder, NEWS_INVALID_PIN_ORDER);
            validateLongMinSize(pinOrder, MIN_PIN_ORDER, NEWS_INVALID_PIN_ORDER);
        }
    }

    /**
     * NewsCta 검증
     * CTA 객체가 있다면, 내부 label과 url은 필수이다.
     */
    private static void validateCtaLabel(String label) {
        validateNotNullOrBlank(label, NEWS_CTA_LABEL_NULL_OR_BLANK);
        validateMaxLength(label, MAX_CTA_LABEL_LENGTH, NEWS_CTA_INVALID_LABEL_LENGTH);
    }

    private static void validateCtaUrl(String url) {
        validateNotNullOrBlank(url, NEWS_CTA_URL_NULL_OR_BLANK);
        validateMaxLength(url, MAX_CTA_URL_LENGTH, NEWS_CTA_INVALID_URL_LENGTH);
        //TODO: URL 형식 검증 추가 필요
    }
}
