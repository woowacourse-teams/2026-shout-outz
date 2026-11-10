package com.shoutoutz.api.resourcesearch.domain;

import java.time.Instant;

public record KeywordSearchCount(String keyword, long searchCount, Instant lastSearchedAt) {
}
