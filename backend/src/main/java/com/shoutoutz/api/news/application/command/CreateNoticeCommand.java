package com.shoutoutz.api.news.application.command;

/**
 * 공지 생성 유스케이스 입력.
 */
public record CreateNoticeCommand(
        String title,
        String summary,
        String body,
        String authorName,
        Cta cta
) {

    public record Cta(String label, String url) {
    }
}
