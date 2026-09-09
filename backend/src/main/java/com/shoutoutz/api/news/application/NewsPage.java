package com.shoutoutz.api.news.application;

import com.shoutoutz.api.news.domain.News;
import java.util.List;

/**
 * 소식 목록 조회 결과.
 */
public record NewsPage(List<News> items, boolean hasNext) {

    public NewsPage {
        items = List.copyOf(items);
    }
}
