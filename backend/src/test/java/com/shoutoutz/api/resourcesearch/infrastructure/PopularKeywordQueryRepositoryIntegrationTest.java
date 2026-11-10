package com.shoutoutz.api.resourcesearch.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.resourcesearch.application.PopularKeywordQueryRepository;
import com.shoutoutz.api.resourcesearch.domain.KeywordSearchCount;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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
class PopularKeywordQueryRepositoryIntegrationTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 10);
    private static final LocalDate WINDOW_START = TODAY.minusDays(6);
    private static final int MIN_COUNT = 2;

    @Autowired
    private PopularKeywordQueryRepository popularKeywordQueryRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("기술 태그에 있는 검색어를 방문자와 날짜 조합 수로 세고 태그의 원래 표기로 준다")
    void countsWhitelistedTechTagKeyword() {
        documentWithTags(false, "Redis", "JPA");
        recordVisitorDays("redis", 3, TODAY);

        List<KeywordSearchCount> counts = popularKeywordQueryRepository.findCounts(WINDOW_START, MIN_COUNT);

        assertThat(counts).singleElement().satisfies(count -> {
            assertThat(count.keyword()).isEqualTo("Redis");
            assertThat(count.searchCount()).isEqualTo(3);
        });
    }

    @Test
    @DisplayName("기술 주제 이름과 같은 검색어도 후보로 준다")
    void countsTopicNameKeyword() {
        recordVisitorDays("동시성 제어/락", 2, TODAY);

        List<KeywordSearchCount> counts = popularKeywordQueryRepository.findCounts(WINDOW_START, MIN_COUNT);

        assertThat(counts).extracting(KeywordSearchCount::keyword).containsExactly("동시성 제어/락");
    }

    @Test
    @DisplayName("기술 태그나 주제 이름에 없는 검색어는 아무리 많이 검색돼도 주지 않는다")
    void excludesKeywordsOutsideWhitelist() {
        documentWithTags(false, "Redis");
        recordVisitorDays("아무말", 10, TODAY);

        assertThat(popularKeywordQueryRepository.findCounts(WINDOW_START, MIN_COUNT)).isEmpty();
    }

    @Test
    @DisplayName("삭제된 자료의 기술 태그는 허용 목록에 넣지 않는다")
    void ignoresTagsOfDeletedDocuments() {
        documentWithTags(true, "Kafka");
        recordVisitorDays("kafka", 5, TODAY);

        assertThat(popularKeywordQueryRepository.findCounts(WINDOW_START, MIN_COUNT)).isEmpty();
    }

    @Test
    @DisplayName("집계 기간보다 오래된 기록은 세지 않는다")
    void countsOnlyWithinWindow() {
        documentWithTags(false, "Redis");
        recordVisitorDays("redis", 1, WINDOW_START);
        recordVisitorDays("redis", 1, WINDOW_START.minusDays(1));

        assertThat(popularKeywordQueryRepository.findCounts(WINDOW_START, 1))
                .singleElement()
                .extracting(KeywordSearchCount::searchCount)
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("최소 횟수에 못 미치는 검색어는 주지 않는다")
    void excludesBelowMinimumCount() {
        documentWithTags(false, "Redis");
        recordVisitorDays("redis", 1, TODAY);

        assertThat(popularKeywordQueryRepository.findCounts(WINDOW_START, MIN_COUNT)).isEmpty();
    }

    @Test
    @DisplayName("가장 최근 검색 시각을 함께 준다")
    void returnsLatestSearchTime() {
        documentWithTags(false, "Redis");
        Instant latest = Instant.parse("2026-11-10T05:00:00Z");
        insertDay("redis", "visitor-a", TODAY.minusDays(1), Instant.parse("2026-11-09T05:00:00Z"));
        insertDay("redis", "visitor-b", TODAY, latest);

        KeywordSearchCount count = popularKeywordQueryRepository.findCounts(WINDOW_START, MIN_COUNT).getFirst();

        assertThat(count.lastSearchedAt()).isEqualTo(latest);
    }

    private void documentWithTags(boolean deleted, String... tags) {
        jdbc.update("""
                INSERT INTO resource_documents (
                    source, external_key, version, title, url, tech_tags, prompt_version, updated_at, deleted_at
                ) VALUES ('blog-post', ?, 1, '제목', 'https://example.com', ?, 'test-v1', now(), ?)
                """, UUID.randomUUID().toString(), tags, deleted ? Timestamp.from(Instant.now()) : null);
    }

    private void recordVisitorDays(String keyword, int visitors, LocalDate searchedOn) {
        for (int i = 0; i < visitors; i++) {
            insertDay(keyword, UUID.randomUUID().toString(), searchedOn, Instant.parse("2026-11-10T03:00:00Z"));
        }
    }

    private void insertDay(String keyword, String visitor, LocalDate searchedOn, Instant searchedAt) {
        jdbc.update("""
                INSERT INTO search_keyword_days (keyword, visitor_key_hash, searched_on, last_searched_at)
                VALUES (?, ?, ?, ?)
                """, keyword, visitor, searchedOn, Timestamp.from(searchedAt));
    }
}
