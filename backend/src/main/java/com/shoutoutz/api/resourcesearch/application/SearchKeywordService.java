package com.shoutoutz.api.resourcesearch.application;

import com.shoutoutz.api.resourcesearch.domain.PopularKeywords;
import com.shoutoutz.api.resourcesearch.domain.SearchKeyword;
import com.shoutoutz.api.resourcesearch.domain.SearchKeywordRepository;
import com.shoutoutz.api.visitor.domain.VisitorKey;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SearchKeywordService {

    private static final ZoneId SEARCH_DATE_ZONE = ZoneId.of("Asia/Seoul");

    private final SearchKeywordRepository searchKeywordRepository;
    private final Clock clock;

    public void record(SearchKeyword keyword, VisitorKey visitorKey) {
        Instant searchedAt = clock.instant();
        LocalDate searchedOn = LocalDate.ofInstant(searchedAt, SEARCH_DATE_ZONE);
        searchKeywordRepository.record(keyword, visitorKey, searchedOn, searchedAt);
    }

    public void archiveExpired() {
        LocalDate today = LocalDate.ofInstant(clock.instant(), SEARCH_DATE_ZONE);
        searchKeywordRepository.archiveBefore(today.minusDays(PopularKeywords.WINDOW_DAYS - 1));
    }
}
