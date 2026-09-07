package com.shoutoutz.api.news.domain;

import com.shoutoutz.api.news.presentation.dto.request.NoticeCreateRequest;

public record NewsCta(String label, String url) {

    public NewsCta(NoticeCreateRequest.Cta cta) {
        this(cta.label(), cta.url());
    }

    /**
     * canonical constructor
     */
    public NewsCta {
        label = label != null ? label.strip() : null;
        url = url != null ? url.strip() : null;
        NewsValidator.validateNewsCta(label, url);
    }
}
