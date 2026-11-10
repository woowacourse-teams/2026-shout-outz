package com.shoutoutz.api.resourcesearch.application;

import static org.mockito.Mockito.verify;

import com.shoutoutz.api.resourcesearch.domain.SearchKeyword;
import com.shoutoutz.api.resourcesearch.domain.SearchKeywordRepository;
import com.shoutoutz.api.visitor.domain.VisitorKey;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SearchKeywordServiceTest {

    private static final VisitorKey VISITOR = new VisitorKey("a".repeat(64));

    @Mock
    private SearchKeywordRepository searchKeywordRepository;

    @ParameterizedTest
    @CsvSource({
            "2026-11-10T03:00:00Z, 2026-11-10",
            "2026-11-09T15:30:00Z, 2026-11-10",
            "2026-11-09T14:59:59Z, 2026-11-09"
    })
    @DisplayName("검색어를 현재 시각과 한국 날짜로 기록한다")
    void recordsKeywordWithKoreanDate(Instant now, LocalDate expectedDate) {
        SearchKeywordService service = new SearchKeywordService(searchKeywordRepository, Clock.fixed(now, ZoneOffset.UTC));
        SearchKeyword keyword = SearchKeyword.from("Redis");

        service.record(keyword, VISITOR);

        verify(searchKeywordRepository).record(keyword, VISITOR, expectedDate, now);
    }

    @ParameterizedTest
    @CsvSource({
            "2026-11-10T03:00:00Z, 2026-11-04",
            "2026-11-09T15:30:00Z, 2026-11-04",
            "2026-11-09T14:59:59Z, 2026-11-03"
    })
    @DisplayName("인기 검색어 집계 기간(한국 날짜로 오늘 포함 7일)보다 오래된 기록을 옮긴다")
    void archivesRecordsOlderThanWindow(Instant now, LocalDate expectedCutoff) {
        SearchKeywordService service = new SearchKeywordService(searchKeywordRepository, Clock.fixed(now, ZoneOffset.UTC));

        service.archiveExpired();

        verify(searchKeywordRepository).archiveBefore(expectedCutoff);
    }
}
