package com.shoutoutz.api.news.domain;

public record NewsCta(String label, String url) {

    /**
     * canonical constructor
     */
    public NewsCta {
        label = label != null ? label.strip() : null;
        url = url != null ? url.strip() : null;
        NewsValidator.validateNewsCta(label, url);
    }
}
