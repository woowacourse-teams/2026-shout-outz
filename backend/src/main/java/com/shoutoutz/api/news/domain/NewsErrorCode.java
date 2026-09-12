package com.shoutoutz.api.news.domain;

import com.shoutoutz.api.common.exception.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NewsErrorCode implements ErrorCode {
    /**
     * 소식 (News) 에러 코드
     */
    NEWS_INVALID_ID_SIZE("소식 ID는 0보다 커야 합니다."),

    NEWS_TYPE_NULL("소식 Type은 null일 수 없습니다."),

    NEWS_TITLE_NULL_OR_BLANK("소식 제목(Title)은 null이거나 빈값일 수 없습니다."),
    NEWS_INVALID_TITLE_LENGTH("소식 제목(Title)은 100자 이하여야 합니다."),

    NEWS_SUMMARY_NULL_OR_BLANK("소식 요약(Summary)은 null이거나 빈값일 수 없습니다."),
    NEWS_INVALID_SUMMARY_LENGTH("소식 요약(Summary)은 200자 이하여야 합니다."),

    NEWS_BODY_NULL_OR_BLANK("소식 본문(Body)은 null이거나 빈값일 수 없습니다."),
    NEWS_INVALID_BODY_LENGTH("소식 본문(Body)은 100000자 이하여야 합니다."),

    NEWS_AUTHOR_ID_NULL("소식 작성자 ID는 null일 수 없습니다."),
    NEWS_INVALID_AUTHOR_ID_SIZE("소식 작성자 ID는 0보다 커야 합니다."),

    NEWS_AUTHOR_NAME_NULL_OR_BLANK("소식 작성자 이름은 null이거나 빈값일 수 없습니다."),
    NEWS_INVALID_AUTHOR_NAME_LENGTH("소식 작성자 이름은 50자 이하여야 합니다."),

    NEWS_PUBLISHED_AT_NULL("소식 게시 시각은 null일 수 없습니다."),

    NEWS_PIN_ORDER_NOT_NULL("고정되지 않은 소식의 고정 순서는 null이어야 합니다."),
    NEWS_INVALID_PIN_ORDER("고정된 소식의 고정 순서는 1 이상이어야 합니다."),

    NEWS_EVENT_START_AT_NULL("이벤트 시작 시각은 null일 수 없습니다."),
    NEWS_EVENT_END_AT_NULL("이벤트 종료 시각은 null일 수 없습니다."),
    NEWS_EVENT_PERIOD_INVALID("이벤트 시작 시각은 종료 시각보다 늦을 수 없습니다."),
    NEWS_EVENT_START_AT_NOT_ALLOWED("공지의 이벤트 시작 시각은 null이어야 합니다."),
    NEWS_EVENT_END_AT_NOT_ALLOWED("공지의 이벤트 종료 시각은 null이어야 합니다."),

    /**
     * 소식 콜투액션 (News Cta) 에러 코드
     */
    NEWS_CTA_LABEL_NULL_OR_BLANK("소식 CTA Label은 null이거나 빈값일 수 없습니다."),
    NEWS_CTA_INVALID_LABEL_LENGTH("소식 CTA Label은 100자 이하여야 합니다."),

    NEWS_CTA_URL_NULL_OR_BLANK("소식 CTA URL은 null이거나 빈값일 수 없습니다."),
    NEWS_CTA_INVALID_URL_LENGTH("소식 CTA URL은 2048자 이하여야 합니다.");

    private final String message;
}
