package com.shoutoutz.api.resourcesearch.application;

import com.shoutoutz.api.resourcesearch.domain.KeywordSearchCount;
import java.time.LocalDate;
import java.util.List;

public interface PopularKeywordQueryRepository {

    List<KeywordSearchCount> findCounts(LocalDate searchedOnFrom, int minSearchCount);
}
