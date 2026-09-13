package com.shoutoutz.api.news.domain;

public record NewsCta(String label, String url) {

    public NewsCta {
        NewsValidator.validateNewsCta(label.strip(), url.strip());
    }
}
