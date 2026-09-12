package com.shoutoutz.api.news.application.command;

import java.time.Instant;

/**
 * 이벤트 생성 유스케이스 입력.
 */
public record CreateEventCommand(
        String title,
        String summary,
        String body,
        String authorName,
        Instant eventStartAt,
        Instant eventEndAt,
        Cta cta
) {

    public record Cta(String label, String url) {
    }
}
