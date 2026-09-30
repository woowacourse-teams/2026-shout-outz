package com.shoutoutz.api.feed.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.feed.application.FeedLinkPreviewRepository;
import com.shoutoutz.api.feed.application.FeedLinkPreviewRepository.FetchJob;
import com.shoutoutz.api.feed.application.dto.LinkPreviewMetadata;
import com.shoutoutz.api.feed.domain.Feed;
import com.shoutoutz.api.feed.domain.FeedRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "aws.s3.bucket=test-bucket",
        "aws.s3.region=ap-northeast-2",
        "aws.s3.presigned-url-expiration-seconds=300",
        "spring.flyway.ignore-migration-patterns=*:missing"
})
@Transactional
class FeedLinkPreviewRepositoryIntegrationTest {

    /*
     * claimDue와 deleteUnusedBefore는 테이블 전체를 대상으로 한다.
     * 로컬 DB에 남은 실제 데이터가 후보가 되지 않도록 테스트 데이터와 기준 시각을 모두 먼 과거로 둔다.
     */
    private static final Instant BASE = Instant.parse("2000-01-01T00:00:00Z");

    @Autowired
    private FeedLinkPreviewRepository linkPreviewRepository;

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 처리할_때가_된_작업을_선점하고_시도_횟수와_선점_만료_시각을_갱신한다() {
        long cacheId = insertCache("PENDING", 0, BASE, null, null);
        linkToFeed(cacheId);
        Instant now = BASE.plus(1, ChronoUnit.MINUTES);

        Optional<FetchJob> job = linkPreviewRepository.claimDue(now, now.minus(7, ChronoUnit.DAYS));

        assertThat(job).hasValueSatisfying(claimed -> {
            assertThat(claimed.id()).isEqualTo(cacheId);
            assertThat(claimed.attempts()).isEqualTo(1);
        });
        Map<String, Object> row = findCache(cacheId);
        assertThat(row.get("status")).isEqualTo("PROCESSING");
        assertThat(row.get("attempts")).isEqualTo(1);
        assertThat(toInstant(row.get("lease_until"))).isEqualTo(now.plusSeconds(45));
    }

    @Test
    void 처리할_때가_된_작업이_없으면_선점하지_않는다() {
        long cacheId = insertCache("PENDING", 0, BASE.plus(1, ChronoUnit.DAYS), null, null);
        linkToFeed(cacheId);

        Optional<FetchJob> job = linkPreviewRepository.claimDue(BASE, BASE.minus(7, ChronoUnit.DAYS));

        assertThat(job).isEmpty();
        assertThat(findCache(cacheId).get("status")).isEqualTo("PENDING");
    }

    @Test
    void 선점한_작업을_완료하면_메타데이터와_가져온_시각을_저장한다() {
        long cacheId = insertCache("PROCESSING", 1, null, BASE.plusSeconds(45), null);
        FetchJob job = new FetchJob(cacheId, "https://example.com", 1);
        LinkPreviewMetadata metadata = new LinkPreviewMetadata("제목", "설명", "https://example.com/og.png", "예시");

        linkPreviewRepository.complete(job, metadata, BASE);

        Map<String, Object> row = findCache(cacheId);
        assertThat(row.get("status")).isEqualTo("READY");
        assertThat(row.get("title")).isEqualTo("제목");
        assertThat(row.get("site_name")).isEqualTo("예시");
        assertThat(row.get("attempts")).isEqualTo(0);
        assertThat(toInstant(row.get("fetched_at"))).isEqualTo(BASE);
        assertThat(row.get("next_attempt_at")).isNull();
        assertThat(row.get("lease_until")).isNull();
    }

    @Test
    void 선점한_작업이_실패하면_다음_시도_시각을_저장한다() {
        long cacheId = insertCache("PROCESSING", 1, null, BASE.plusSeconds(45), null);
        FetchJob job = new FetchJob(cacheId, "https://example.com", 1);
        Instant nextAttemptAt = BASE.plus(10, ChronoUnit.MINUTES);

        linkPreviewRepository.fail(job, nextAttemptAt);

        Map<String, Object> row = findCache(cacheId);
        assertThat(row.get("status")).isEqualTo("FAILED");
        assertThat(toInstant(row.get("next_attempt_at"))).isEqualTo(nextAttemptAt);
        assertThat(row.get("lease_until")).isNull();
    }

    @Test
    void 기준_시각_이전이면서_참조되지_않는_캐시만_삭제한다() {
        Instant cutoff = BASE.plus(1, ChronoUnit.DAYS);
        long unusedOld = insertCache("READY", 0, null, null, BASE);
        long referencedOld = insertCache("READY", 0, null, null, BASE);
        linkToFeed(referencedOld);
        long unusedRecent = insertCache("READY", 0, null, null, cutoff.plus(1, ChronoUnit.DAYS));

        int deleted = linkPreviewRepository.deleteUnusedBefore(cutoff);

        assertThat(deleted).isEqualTo(1);
        assertThat(existsCache(unusedOld)).isFalse();
        assertThat(existsCache(referencedOld)).isTrue();
        assertThat(existsCache(unusedRecent)).isTrue();
    }

    private long insertCache(
            String status,
            int attempts,
            Instant nextAttemptAt,
            Instant leaseUntil,
            Instant fetchedAt
    ) {
        String token = UUID.randomUUID().toString().replace("-", "");
        return jdbcTemplate.queryForObject(
                """
                        INSERT INTO link_preview_cache (
                            url_hash, url, status, attempts, next_attempt_at, lease_until, fetched_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?)
                        RETURNING id
                        """,
                Long.class,
                token + token,
                "https://example.com/" + token,
                status,
                attempts,
                toTimestamp(nextAttemptAt),
                toTimestamp(leaseUntil),
                toTimestamp(fetchedAt)
        );
    }

    private void linkToFeed(long cacheId) {
        long authorId = insertUser();
        Feed feed = feedRepository.save(Feed.create(authorId, "링크 미리보기", "본문", BASE));
        jdbcTemplate.update(
                "INSERT INTO feed_link_preview_refs (feed_id, cache_id) VALUES (?, ?)",
                feed.getId(),
                cacheId
        );
    }

    private long insertUser() {
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Long userId = jdbcTemplate.queryForObject(
                "INSERT INTO users (handle) VALUES (?) RETURNING id",
                Long.class,
                "@preview_" + token
        );
        jdbcTemplate.update(
                "INSERT INTO user_profiles (user_id, display_name, user_type, track, cohort) VALUES (?, ?, ?, ?, ?)",
                userId,
                "미리보기 테스트",
                "WOOWACOURSE_CREW",
                "BACKEND",
                (short) 8
        );
        return userId;
    }

    private Map<String, Object> findCache(long cacheId) {
        return jdbcTemplate.queryForMap("SELECT * FROM link_preview_cache WHERE id = ?", cacheId);
    }

    private boolean existsCache(long cacheId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM link_preview_cache WHERE id = ?)",
                Boolean.class,
                cacheId
        ));
    }

    private static Timestamp toTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private static Instant toInstant(Object value) {
        return ((Timestamp) value).toInstant();
    }
}
