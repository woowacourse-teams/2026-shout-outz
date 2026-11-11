package com.shoutoutz.api.resourcesearch.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.resourcesearch.domain.SearchKeywordRepository;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class SearchKeywordArchiveIntegrationTest {

    private static final LocalDate CUTOFF = LocalDate.of(2026, 11, 4);

    @Autowired
    private SearchKeywordRepository searchKeywordRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("기준일 전의 방문자 기록을 검색어와 날짜별 방문자 수로 옮기고 지운다")
    void movesExpiredDaysIntoDailyCounts() {
        recordVisitors("redis", 3, CUTOFF.minusDays(1));
        recordVisitors("jpa", 1, CUTOFF.minusDays(2));

        searchKeywordRepository.archiveBefore(CUTOFF);

        assertThat(dailyCounts()).containsExactlyInAnyOrder(
                Map.of("keyword", "redis", "searched_on", Date.valueOf(CUTOFF.minusDays(1)), "visitor_count", 3),
                Map.of("keyword", "jpa", "searched_on", Date.valueOf(CUTOFF.minusDays(2)), "visitor_count", 1));
        assertThat(dayRows()).isZero();
    }

    @Test
    @DisplayName("기준일과 그 뒤의 방문자 기록은 그대로 둔다")
    void keepsDaysFromCutoff() {
        recordVisitors("redis", 2, CUTOFF);
        recordVisitors("redis", 1, CUTOFF.plusDays(3));

        searchKeywordRepository.archiveBefore(CUTOFF);

        assertThat(dayRows()).isEqualTo(3);
        assertThat(dailyCounts()).isEmpty();
    }

    @Test
    @DisplayName("두 번 실행해도 집계가 중복되지 않는다")
    void isIdempotent() {
        recordVisitors("redis", 2, CUTOFF.minusDays(1));

        searchKeywordRepository.archiveBefore(CUTOFF);
        searchKeywordRepository.archiveBefore(CUTOFF);

        assertThat(dailyCounts()).singleElement()
                .satisfies(row -> assertThat(row.get("visitor_count")).isEqualTo(2));
    }

    @Test
    @DisplayName("이미 집계된 날짜에 남은 기록이 있으면 방문자 수에 더한다")
    void addsToExistingDailyCount() {
        jdbc.update("INSERT INTO search_keyword_daily_counts (keyword, searched_on, visitor_count) VALUES ('redis', ?, 4)",
                CUTOFF.minusDays(1));
        recordVisitors("redis", 2, CUTOFF.minusDays(1));

        searchKeywordRepository.archiveBefore(CUTOFF);

        assertThat(dailyCounts()).singleElement()
                .satisfies(row -> assertThat(row.get("visitor_count")).isEqualTo(6));
    }

    private void recordVisitors(String keyword, int visitors, LocalDate searchedOn) {
        for (int i = 0; i < visitors; i++) {
            jdbc.update("""
                    INSERT INTO search_keyword_days (keyword, visitor_key_hash, searched_on, last_searched_at)
                    VALUES (?, ?, ?, ?)
                    """, keyword, UUID.randomUUID().toString(), searchedOn, Timestamp.from(Instant.now()));
        }
    }

    private List<Map<String, Object>> dailyCounts() {
        return jdbc.queryForList("SELECT keyword, searched_on, visitor_count FROM search_keyword_daily_counts");
    }

    private Integer dayRows() {
        return jdbc.queryForObject("SELECT count(*) FROM search_keyword_days", Integer.class);
    }
}
