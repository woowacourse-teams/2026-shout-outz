package com.shoutoutz.api.feed.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.category.domain.CategoryType;
import com.shoutoutz.api.feed.application.FeedQueryRepository;
import com.shoutoutz.api.feed.application.dto.FeedCursor;
import com.shoutoutz.api.feed.application.dto.FeedItem;
import com.shoutoutz.api.feed.application.dto.FeedMediaReference;
import com.shoutoutz.api.feed.application.dto.FeedSort;
import com.shoutoutz.api.feed.domain.Feed;
import com.shoutoutz.api.feed.domain.FeedRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
class FeedRepositoryIntegrationTest {

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private FeedQueryRepository feedQueryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 피드_검증에_필요한_미디어_데이터를_조회한다() {
        long authorId = insertUser("WOOWACOURSE_CREW", "FRONTEND", (short) 8);
        long otherId = insertUser("WOOWACOURSE_CREW", "BACKEND", (short) 8);
        long readyMediaId = insertMedia(authorId, "FEED_CONTENT", "READY");
        long pendingMediaId = insertMedia(authorId, "FEED_CONTENT", "PENDING_UPLOAD");
        long otherMediaId = insertMedia(otherId, "FEED_CONTENT", "READY");
        long avatarMediaId = insertMedia(authorId, "USER_AVATAR", "READY");

        List<FeedMediaReference> media = feedQueryRepository.findAllMediaByIds(List.of(
                readyMediaId,
                pendingMediaId,
                otherMediaId,
                avatarMediaId
        ));
        assertThat(media).extracting(FeedMediaReference::mediaId)
                .containsExactlyInAnyOrder(
                        readyMediaId,
                        pendingMediaId,
                        otherMediaId,
                        avatarMediaId
                );
    }

    @Test
    void 최신순_슬라이스를_카테고리와_커서로_조회하고_삭제된_피드는_제외한다() {
        long authorId = insertUser("WOOWACOURSE_CREW", "BACKEND", (short) 8);
        long categoryId = insertCategory(true);
        long mediaId = insertMedia(authorId, "FEED_CONTENT", "READY");
        Instant base = Instant.parse("2026-09-11T00:00:00Z");

        Feed oldest = saveFeed(authorId, "첫 번째", base.minus(2, ChronoUnit.HOURS), categoryId, mediaId);
        Feed middle = saveFeed(authorId, "두 번째", base.minus(1, ChronoUnit.HOURS), categoryId);
        Feed latest = saveFeed(authorId, "세 번째", base, categoryId);
        Feed deleted = saveFeed(authorId, "삭제", base.plus(1, ChronoUnit.HOURS), categoryId);
        feedRepository.update(deleted.delete(base.plus(2, ChronoUnit.HOURS)));

        List<FeedItem> firstSlice = feedQueryRepository.findAll(
                FeedSort.LATEST,
                null,
                null,
                2
        );
        List<FeedItem> secondSlice = feedQueryRepository.findAll(
                FeedSort.LATEST,
                null,
                new FeedCursor(
                        FeedSort.LATEST,
                        0L,
                        firstSlice.get(1).createdAt(),
                        firstSlice.get(1).feedId()
                ),
                2
        );
        assertThat(firstSlice).extracting(FeedItem::feedId)
                .containsExactly(latest.getId(), middle.getId());
        assertThat(secondSlice).extracting(FeedItem::feedId).containsExactly(oldest.getId());
        assertThat(feedQueryRepository.findAll(FeedSort.LATEST, categoryId, null, 10))
                .extracting(FeedItem::feedId)
                .containsExactly(latest.getId(), middle.getId(), oldest.getId());
        assertThat(feedQueryRepository.findById(deleted.getId())).isEmpty();

        FeedItem detail = feedQueryRepository.findById(oldest.getId()).orElseThrow();
        assertThat(detail.title()).isEqualTo(oldest.getTitle());
        assertThat(detail.categories()).extracting(FeedItem.Category::categoryId)
                .containsExactly(categoryId);
        assertThat(detail.categories()).extracting(FeedItem.Category::type)
                .containsExactly(CategoryType.GENERAL);
        assertThat(detail.media()).extracting(FeedItem.Media::mediaId).containsExactly(mediaId);
    }

    @Test
    void 전체_좋아요_수로_인기순_슬라이스를_조회한다() {
        long authorId = insertUser("WOOWACOURSE_CREW", "BACKEND", (short) 8);
        long firstUserId = insertUser("GENERAL", null, null);
        long secondUserId = insertUser("GENERAL", null, null);
        long categoryId = insertCategory(true);
        Instant base = Instant.parse("2026-09-11T00:00:00Z");
        Feed noLike = saveFeed(authorId, "좋아요 없음", base, categoryId);
        Feed olderPopular = saveFeed(authorId, "먼저 작성한 인기 글", base.plus(1, ChronoUnit.HOURS), categoryId);
        Feed latestPopular = saveFeed(authorId, "나중에 작성한 인기 글", base.plus(2, ChronoUnit.HOURS), categoryId);
        insertLike(olderPopular.getId(), firstUserId);
        insertLike(olderPopular.getId(), secondUserId);
        insertLike(latestPopular.getId(), firstUserId);
        insertLike(latestPopular.getId(), secondUserId);

        List<FeedItem> firstSlice = feedQueryRepository.findAll(
                FeedSort.POPULAR,
                null,
                null,
                1
        );
        FeedItem firstItem = firstSlice.getFirst();
        List<FeedItem> secondSlice = feedQueryRepository.findAll(
                FeedSort.POPULAR,
                null,
                new FeedCursor(
                        FeedSort.POPULAR,
                        firstItem.likeCount(),
                        firstItem.createdAt(),
                        firstItem.feedId()
                ),
                10
        );

        assertThat(firstSlice).extracting(FeedItem::feedId)
                .containsExactly(latestPopular.getId());
        assertThat(firstItem.likeCount()).isEqualTo(2L);
        assertThat(secondSlice).extracting(FeedItem::feedId)
                .containsExactly(olderPopular.getId(), noLike.getId());
    }

    @Test
    void 사용자가_작성한_피드만_최신순_커서로_조회한다() {
        long authorId = insertUser("WOOWACOURSE_CREW", "BACKEND", (short) 8);
        long otherId = insertUser("WOOWACOURSE_CREW", "FRONTEND", (short) 8);
        long categoryId = insertCategory(true);
        Instant base = Instant.parse("2026-09-11T00:00:00Z");
        Feed oldest = saveFeed(authorId, "첫 번째", base.minus(2, ChronoUnit.HOURS), categoryId);
        Feed middle = saveFeed(authorId, "두 번째", base.minus(1, ChronoUnit.HOURS), categoryId);
        Feed latest = saveFeed(authorId, "세 번째", base, categoryId);
        saveFeed(otherId, "다른 사용자", base.plus(1, ChronoUnit.HOURS), categoryId);
        Feed deleted = saveFeed(authorId, "삭제", base.plus(2, ChronoUnit.HOURS), categoryId);
        feedRepository.update(deleted.delete(base.plus(3, ChronoUnit.HOURS)));
        insertLike(latest.getId(), otherId);
        insertComment(latest.getId(), otherId, false);
        insertComment(latest.getId(), otherId, true);

        List<FeedItem> firstPage = feedQueryRepository.findAllByAuthorId(authorId, null, 2);
        FeedItem lastItem = firstPage.getLast();
        List<FeedItem> secondPage = feedQueryRepository.findAllByAuthorId(
                authorId,
                new FeedCursor(FeedSort.LATEST, 0L, lastItem.createdAt(), lastItem.feedId()),
                2
        );

        assertThat(firstPage).extracting(FeedItem::feedId)
                .containsExactly(latest.getId(), middle.getId());
        assertThat(firstPage.getFirst().likeCount()).isEqualTo(1L);
        assertThat(firstPage.getFirst().commentCount()).isEqualTo(1L);
        assertThat(secondPage).extracting(FeedItem::feedId).containsExactly(oldest.getId());
    }

    private Feed saveFeed(long authorId, String content, Instant createdAt, long categoryId, long... mediaIds) {
        Feed feed = feedRepository.save(Feed.create(authorId, "제목 " + content, content, createdAt));
        feedRepository.saveCategories(feed.getId(), List.of(categoryId));
        feedRepository.saveMedia(feed.getId(), java.util.Arrays.stream(mediaIds).boxed().toList());
        return feed;
    }

    private long insertUser(String userType, String track, Short cohort) {
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Long userId = jdbcTemplate.queryForObject(
                "INSERT INTO users (handle) VALUES (?) RETURNING id",
                Long.class,
                "feed_" + token
        );
        jdbcTemplate.update(
                "INSERT INTO user_profiles (user_id, display_name, user_type, track, cohort) VALUES (?, ?, ?, ?, ?)",
                userId,
                "피드 테스트",
                userType,
                track,
                cohort
        );
        return userId;
    }

    private long insertCategory(boolean active) {
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return jdbcTemplate.queryForObject(
                "INSERT INTO categories (slug, display_name, is_active) VALUES (?, ?, ?) RETURNING id",
                Long.class,
                "feed-" + token,
                "카테고리 " + token,
                active
        );
    }

    private long insertMedia(long userId, String purpose, String status) {
        String token = UUID.randomUUID().toString().replace("-", "");
        return jdbcTemplate.queryForObject(
                """
                        INSERT INTO media_metadata (
                            uploaded_by, purpose, s3_key, mime_type, size_bytes, status, expires_at
                        ) VALUES (?, ?, ?, 'image/png', 100, ?, now() + interval '1 day')
                        RETURNING id
                        """,
                Long.class,
                userId,
                purpose,
                "media/feed-test/" + token,
                status
        );
    }

    private void insertLike(long feedId, long userId) {
        jdbcTemplate.update(
                """
                        INSERT INTO feed_reactions (feed_id, user_id, reaction_type, created_at)
                        VALUES (?, ?, 'LIKE', '2020-01-01T00:00:00Z')
                        """,
                feedId,
                userId
        );
    }

    private void insertComment(long feedId, long userId, boolean deleted) {
        jdbcTemplate.update(
                """
                        INSERT INTO feed_comments (feed_id, author_id, content, deleted_at)
                        VALUES (?, ?, '댓글', ?)
                        """,
                feedId,
                userId,
                deleted ? java.sql.Timestamp.from(Instant.parse("2026-09-11T01:00:00Z")) : null
        );
    }
}
