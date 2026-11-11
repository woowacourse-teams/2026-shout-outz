package com.shoutoutz.api.resourcesearch.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.resourcesearch.domain.SearchKeyword;
import com.shoutoutz.api.resourcesearch.domain.SearchKeywordRepository;
import com.shoutoutz.api.visitor.domain.VisitorKey;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
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
class SearchKeywordRepositoryIntegrationTest {

    private static final SearchKeyword REDIS = SearchKeyword.from("Redis");
    private static final VisitorKey VISITOR_A = new VisitorKey("a".repeat(64));
    private static final VisitorKey VISITOR_B = new VisitorKey("b".repeat(64));
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 10);
    private static final Instant MORNING = Instant.parse("2026-11-10T00:00:00Z");
    private static final Instant NOON = Instant.parse("2026-11-10T03:00:00Z");

    @Autowired
    private SearchKeywordRepository searchKeywordRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("정규화한 검색어로 기록한다")
    void recordsNormalizedKeyword() {
        searchKeywordRepository.record(SearchKeyword.from("  Spring   Boot "), VISITOR_A, TODAY, NOON);

        assertThat(jdbc.queryForList("SELECT keyword FROM search_keyword_days", String.class))
                .containsExactly("spring boot");
    }

    @Test
    @DisplayName("같은 방문자가 같은 날 다시 검색하면 한 행을 유지하고 마지막 검색 시각만 갱신한다")
    void keepsOneRowPerVisitorDay() {
        searchKeywordRepository.record(REDIS, VISITOR_A, TODAY, MORNING);
        searchKeywordRepository.record(REDIS, VISITOR_A, TODAY, NOON);

        assertThat(rowCount()).isEqualTo(1);
        assertThat(lastSearchedAt()).isEqualTo(NOON);
    }

    @Test
    @DisplayName("늦게 도착한 이전 검색은 마지막 검색 시각을 되돌리지 않는다")
    void doesNotMoveLastSearchedAtBackwards() {
        searchKeywordRepository.record(REDIS, VISITOR_A, TODAY, NOON);
        searchKeywordRepository.record(REDIS, VISITOR_A, TODAY, MORNING);

        assertThat(lastSearchedAt()).isEqualTo(NOON);
    }

    @Test
    @DisplayName("다른 방문자나 다른 날의 검색은 따로 기록한다")
    void recordsDifferentVisitorsAndDaysSeparately() {
        searchKeywordRepository.record(REDIS, VISITOR_A, TODAY, NOON);
        searchKeywordRepository.record(REDIS, VISITOR_B, TODAY, NOON);
        searchKeywordRepository.record(REDIS, VISITOR_A, TODAY.minusDays(1), NOON.minusSeconds(86_400));

        assertThat(rowCount()).isEqualTo(3);
    }

    private Integer rowCount() {
        return jdbc.queryForObject("SELECT count(*) FROM search_keyword_days", Integer.class);
    }

    private Instant lastSearchedAt() {
        return jdbc.queryForObject("SELECT last_searched_at FROM search_keyword_days", Timestamp.class).toInstant();
    }
}
