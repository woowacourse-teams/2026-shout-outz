package com.shoutoutz.api.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SearchKeywordMigrationIntegrationTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 10);

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("같은 검색어와 방문자와 날짜의 기록은 한 행만 둔다")
    void rejectsDuplicateVisitorDay() {
        insertDay("redis", "visitor-a", TODAY);

        assertThatThrownBy(() -> insertDay("redis", "visitor-a", TODAY))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("방문자나 날짜가 다르면 같은 검색어도 따로 기록한다")
    void keepsDifferentVisitorsAndDaysSeparately() {
        insertDay("redis", "visitor-a", TODAY);
        insertDay("redis", "visitor-b", TODAY);
        insertDay("redis", "visitor-a", TODAY.minusDays(1));

        Integer rows = jdbc.queryForObject(
                "SELECT count(*) FROM search_keyword_days WHERE keyword = 'redis'", Integer.class);

        assertThat(rows).isEqualTo(3);
    }

    @Test
    @DisplayName("날짜별 집계는 검색어와 날짜마다 한 행만 둔다")
    void rejectsDuplicateDailyCount() {
        insertDailyCount("redis", TODAY, 3);

        assertThatThrownBy(() -> insertDailyCount("redis", TODAY, 1))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("날짜별 집계의 방문자 수는 1 이상이어야 한다")
    void rejectsNonPositiveVisitorCount() {
        assertThatThrownBy(() -> insertDailyCount("jpa", TODAY, 0))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void insertDay(String keyword, String visitorKeyHash, LocalDate searchedOn) {
        jdbc.update("""
                INSERT INTO search_keyword_days (keyword, visitor_key_hash, searched_on, last_searched_at)
                VALUES (?, ?, ?, now())
                """, keyword, visitorKeyHash, searchedOn);
    }

    private void insertDailyCount(String keyword, LocalDate searchedOn, int visitorCount) {
        jdbc.update("""
                INSERT INTO search_keyword_daily_counts (keyword, searched_on, visitor_count)
                VALUES (?, ?, ?)
                """, keyword, searchedOn, visitorCount);
    }
}
