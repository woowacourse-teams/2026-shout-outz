package com.shoutoutz.api.news.application.query;

import java.util.List;

/**
 * 소식 목록 조회 Repository 결과.
 */
public record NewsPage(List<NewsSummary> items, boolean hasNext) {

    public NewsPage {
        items = List.copyOf(items);
    }
}
