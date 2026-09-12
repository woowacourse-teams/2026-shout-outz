package com.shoutoutz.api.news.application;

import com.shoutoutz.api.news.domain.NewsType;
import java.time.Instant;

/**
 * 소식 목록 조회에 필요한 요약 정보.
 *
 * <p>목록 조회에서는 본문, 작성자, CTA가 필요하지 않으므로 전체 소식 도메인 대신
 * 필요한 값만 전달한다.</p>
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
