package com.shoutoutz.api.resourcesearch.domain;

import com.shoutoutz.api.visitor.domain.VisitorKey;
import java.time.Instant;
import java.time.LocalDate;

public interface SearchKeywordRepository {

    void record(SearchKeyword keyword, VisitorKey visitorKey, LocalDate searchedOn, Instant searchedAt);
}
