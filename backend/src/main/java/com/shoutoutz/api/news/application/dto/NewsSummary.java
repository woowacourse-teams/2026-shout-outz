package com.shoutoutz.api.news.application.dto;

import com.shoutoutz.api.news.domain.enums.NewsType;
import java.time.Instant;

/**
 * 소식 목록 조회에 필요한 일부 정보만 전달하기 위한 객체
 *
 * 목록 조회 API에서는 본문, 작성자, CTA가 필요하지 않으므로
 * 전체 소식 도메인 대신 필요한 값만 전달.
 */
public record NewsSummary(
        Long id,
        NewsType type,
        String title,
        String summary,
        Instant publishedAt,
        Instant eventStartAt,
        Instant eventEndAt,
        boolean pinned,
        Integer pinOrder
) {
}
