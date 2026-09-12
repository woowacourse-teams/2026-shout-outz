package com.shoutoutz.api.news.application;

import java.util.List;

/**
 * 소식 목록 조회 결과.
 */
public record NewsPage(List<NewsSummary> items, boolean hasNext) {

    public NewsPage {
        items = List.copyOf(items);
    }
}
