package com.shoutoutz.api.feed.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.category.domain.CategoryType;
import com.shoutoutz.api.cohort.domain.Cohort;
import com.shoutoutz.api.feed.application.FeedQueryRepository;
import com.shoutoutz.api.feed.application.dto.FeedCursor;
import com.shoutoutz.api.feed.application.dto.FeedItem;
import com.shoutoutz.api.feed.application.dto.FeedMediaReference;
import com.shoutoutz.api.feed.application.dto.FeedPage;
import com.shoutoutz.api.feed.application.dto.FeedSort;
import com.shoutoutz.api.feed.domain.Feed;
import com.shoutoutz.api.feed.domain.FeedRepository;
import com.shoutoutz.api.feed.domain.FeedType;
import com.shoutoutz.api.user.domain.profile.UserType;
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
    void 피드_타입으로_질문과_포스트를_분리해_조회한다() {
        long authorId = insertUser("GENERAL", null, null);
        long categoryId = insertCategory(true);
        Instant base = Instant.parse("2026-09-11T00:00:00Z");

        Feed post = saveFeed(authorId, "일반 포스트", base, categoryId);
        jdbcTemplate.update(
                "INSERT INTO category_feed_types (category_id, feed_type) VALUES (?, 'QUESTION')",
                categoryId
        );
        Feed question = feedRepository.save(Feed.create(
                authorId,
                FeedType.QUESTION,
                "질문",
                "질문 본문",
                false,
                base.plus(1, ChronoUnit.HOURS)
        ));
        feedRepository.saveCategories(question.getId(), List.of(categoryId));

        FeedPage questionPage = feedQueryRepository.findAll(
                FeedSort.LATEST,
                null,
                null,
                FeedType.QUESTION,
                null,
                10
        );
        FeedPage postPage = feedQueryRepository.findAll(
                FeedSort.LATEST,
                null,
                null,
                FeedType.POST,
                null,
                10
        );

        assertThat(questionPage.items()).extracting(FeedItem::feedId)
                .containsExactly(question.getId());
        assertThat(questionPage.items()).extracting(FeedItem::feedType)
                .containsOnly(FeedType.QUESTION);
        assertThat(questionPage.items().getFirst().categories())
                .extracting(FeedItem.Category::feedType)
                .containsOnly(FeedType.QUESTION);
        assertThat(questionPage.totalCount()).isEqualTo(1L);
        assertThat(postPage.items()).extracting(FeedItem::feedId)
                .containsExactly(post.getId());
        assertThat(postPage.items()).extracting(FeedItem::feedType)
                .containsOnly(FeedType.POST);
        assertThat(postPage.items().getFirst().categories())
                .extracting(FeedItem.Category::feedType)
                .containsOnly(FeedType.POST);
        assertThat(postPage.totalCount()).isEqualTo(1L);
    }

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

        FeedPage firstSlice = feedQueryRepository.findAll(
                FeedSort.LATEST,
                null,
                null,
                null,
                2
        );
        FeedPage secondSlice = feedQueryRepository.findAll(
                FeedSort.LATEST,
                null,
                null,
                new FeedCursor(
                        FeedSort.LATEST,
                        0,
                        0L,
                        firstSlice.items().get(1).createdAt(),
                        firstSlice.items().get(1).feedId()
                ),
                2
        );
        assertThat(firstSlice.items()).extracting(FeedItem::feedId)
                .containsExactly(latest.getId(), middle.getId());
        assertThat(firstSlice.totalCount()).isEqualTo(3L);
        assertThat(secondSlice.items()).extracting(FeedItem::feedId).containsExactly(oldest.getId());
        assertThat(secondSlice.totalCount()).isEqualTo(3L);
        assertThat(feedQueryRepository.findAll(FeedSort.LATEST, categoryId, null, null, 10).items())
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
    void 답변_대기순은_활성_댓글이_없는_피드만_최신순으로_조회하고_익명_작성자를_마스킹한다() {
        long authorId = insertUser("GENERAL", null, null);
        long otherUserId = insertUser("GENERAL", null, null);
        long categoryId = insertCategory(true);
        Instant base = Instant.parse("2026-09-11T00:00:00Z");

        Feed unanswered = feedRepository.save(
                Feed.create(authorId, "답변 대기", "본문", true, base)
        );
        feedRepository.saveCategories(unanswered.getId(), List.of(categoryId));

        Feed answered = saveFeed(authorId, "답변 완료", base.plus(1, ChronoUnit.HOURS), categoryId);
        insertComment(answered.getId(), otherUserId, false);

        Feed deletedAnswer = saveFeed(
                authorId,
                "삭제된 답변만 있음",
                base.plus(2, ChronoUnit.HOURS),
                categoryId
        );
        insertComment(deletedAnswer.getId(), otherUserId, true);

        FeedPage waitingPage = feedQueryRepository.findAll(
                FeedSort.WAITING,
                null,
                null,
                null,
                10
        );

        assertThat(waitingPage.items()).extracting(FeedItem::feedId)
                .containsExactly(deletedAnswer.getId(), unanswered.getId());
        assertThat(waitingPage.totalCount()).isEqualTo(2L);
        FeedItem masked = waitingPage.items().stream()
                .filter(item -> item.feedId() == unanswered.getId())
                .findFirst()
                .orElseThrow();
        assertThat(masked.isAnonymous()).isTrue();
        assertThat(masked.author().userId()).isNull();
        assertThat(masked.author().handle()).isNull();
        assertThat(masked.author().displayName()).isNull();

        FeedItem ownerView = feedQueryRepository.findById(unanswered.getId(), authorId).orElseThrow();
        assertThat(ownerView.author().userId()).isEqualTo(authorId);
    }

    @Test
    void 익명_피드의_작성자_유형과_기수는_조회_모델에_보존하고_신원은_숨긴다() {
        long crewId = insertUser("WOOWACOURSE_CREW", "BACKEND", (short) 7);
        long coachId = insertUser("WOOWACOURSE_COACH", null, null);
        long generalId = insertUser("GENERAL", null, null);
        long viewerId = insertUser("GENERAL", null, null);
        long categoryId = insertCategory(true);
        Instant base = Instant.parse("2026-09-11T00:00:00Z");
        Feed crewFeed = saveAnonymousFeed(crewId, "크루 익명", base, categoryId);
        Feed coachFeed = saveAnonymousFeed(coachId, "코치 익명", base.plus(1, ChronoUnit.HOURS), categoryId);
        Feed generalFeed = saveAnonymousFeed(generalId, "일반 익명", base.plus(2, ChronoUnit.HOURS), categoryId);

        FeedItem.Author crew = feedQueryRepository.findById(crewFeed.getId(), viewerId).orElseThrow().author();
        FeedItem.Author coach = feedQueryRepository.findById(coachFeed.getId(), viewerId).orElseThrow().author();
        FeedItem.Author general = feedQueryRepository.findById(generalFeed.getId(), null).orElseThrow().author();

        assertThat(crew.userType()).isEqualTo(UserType.WOOWACOURSE_CREW);
        assertThat(crew.cohort()).isEqualTo(Cohort.from((short) 7));
        assertThat(coach.userType()).isEqualTo(UserType.WOOWACOURSE_COACH);
        assertThat(coach.cohort()).isNull();
        assertThat(general.userType()).isEqualTo(UserType.GENERAL);
        assertThat(general.cohort()).isNull();
        assertThat(List.of(crew, coach, general)).allSatisfy(author -> {
            assertThat(author.userId()).isNull();
            assertThat(author.handle()).isNull();
            assertThat(author.displayName()).isNull();
            assertThat(author.track()).isNull();
            assertThat(author.avatarImageId()).isNull();
        });
    }

    @Test
    void 익명_피드의_작성자_유형과_기수는_목록_조회_모델에도_보존한다() {
        long crewId = insertUser("WOOWACOURSE_CREW", "BACKEND", (short) 7);
        long viewerId = insertUser("GENERAL", null, null);
        long categoryId = insertCategory(true);
        Instant base = Instant.parse("2026-09-11T00:00:00Z");
        Feed anonymous = saveAnonymousFeed(crewId, "크루 익명", base, categoryId);

        FeedItem listed = feedQueryRepository.findAll(FeedSort.LATEST, null, null, viewerId, null, 10).items().stream()
                .filter(item -> item.feedId() == anonymous.getId())
                .findFirst()
                .orElseThrow();

        assertThat(listed.author().userType()).isEqualTo(UserType.WOOWACOURSE_CREW);
        assertThat(listed.author().cohort()).isEqualTo(Cohort.from((short) 7));
        assertThat(listed.author().userId()).isNull();
        assertThat(listed.author().track()).isNull();
    }

    @Test
    void 좋아요_수가_많은_글부터_인기순_슬라이스를_조회한다() {
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

        FeedPage firstSlice = feedQueryRepository.findAll(
                FeedSort.POPULAR,
                null,
                null,
                null,
                1
        );
        FeedItem firstItem = firstSlice.items().getFirst();
        FeedPage secondSlice = feedQueryRepository.findAll(
                FeedSort.POPULAR,
                null,
                null,
                new FeedCursor(
                        FeedSort.POPULAR,
                        0,
                        firstItem.popularityScore(),
                        firstItem.createdAt(),
                        firstItem.feedId()
                ),
                10
        );

        assertThat(firstSlice.items()).extracting(FeedItem::feedId)
                .containsExactly(latestPopular.getId());
        assertThat(firstItem.likeCount()).isEqualTo(2L);
        assertThat(secondSlice.items()).extracting(FeedItem::feedId)
                .containsExactly(olderPopular.getId(), noLike.getId());
    }

    @Test
    void 좋아요_수와_삭제되지_않은_댓글_수의_합으로_인기순_슬라이스를_조회한다() {
        long authorId = insertUser("WOOWACOURSE_CREW", "BACKEND", (short) 8);
        long firstUserId = insertUser("GENERAL", null, null);
        long secondUserId = insertUser("GENERAL", null, null);
        long categoryId = insertCategory(true);
        Instant base = Instant.parse("2026-09-11T00:00:00Z");
        Feed commentOnly = saveFeed(authorId, "댓글만 있는 글", base, categoryId);
        Feed likeOnly = saveFeed(authorId, "좋아요만 있는 글", base.plus(1, ChronoUnit.HOURS), categoryId);
        Feed likeAndComment = saveFeed(authorId, "좋아요와 댓글이 있는 글", base.plus(2, ChronoUnit.HOURS), categoryId);
        Feed deletedCommentOnly = saveFeed(authorId, "삭제된 댓글만 있는 글", base.plus(3, ChronoUnit.HOURS), categoryId);
        insertComment(commentOnly.getId(), firstUserId, false);
        insertComment(commentOnly.getId(), secondUserId, false);
        insertComment(commentOnly.getId(), authorId, false);
        insertLike(likeOnly.getId(), firstUserId);
        insertLike(likeOnly.getId(), secondUserId);
        insertLike(likeAndComment.getId(), firstUserId);
        insertComment(likeAndComment.getId(), secondUserId, false);
        insertComment(deletedCommentOnly.getId(), firstUserId, true);
        insertComment(deletedCommentOnly.getId(), secondUserId, true);

        FeedPage firstSlice = feedQueryRepository.findAll(
                FeedSort.POPULAR,
                null,
                null,
                null,
                2
        );
        FeedItem lastItem = firstSlice.items().getLast();
        FeedPage secondSlice = feedQueryRepository.findAll(
                FeedSort.POPULAR,
                null,
                null,
                new FeedCursor(
                        FeedSort.POPULAR,
                        0,
                        lastItem.popularityScore(),
                        lastItem.createdAt(),
                        lastItem.feedId()
                ),
                10
        );

        assertThat(firstSlice.items()).extracting(FeedItem::feedId)
                .containsExactly(commentOnly.getId(), likeAndComment.getId());
        assertThat(firstSlice.items()).extracting(FeedItem::popularityScore)
                .containsExactly(3L, 2L);
        assertThat(secondSlice.items()).extracting(FeedItem::feedId)
                .containsExactly(likeOnly.getId(), deletedCommentOnly.getId());
        assertThat(secondSlice.items().getLast().commentCount()).isZero();
    }

    @Test
    void 피드_조회에_좋아요와_북마크_수와_현재_사용자_반응_여부를_함께_반환한다() {
        long authorId = insertUser("WOOWACOURSE_CREW", "BACKEND", (short) 8);
        long viewerId = insertUser("GENERAL", null, null);
        long otherId = insertUser("GENERAL", null, null);
        long categoryId = insertCategory(true);
        Feed feed = saveFeed(authorId, "반응 피드", Instant.parse("2026-09-11T00:00:00Z"), categoryId);
        insertLike(feed.getId(), viewerId);
        insertLike(feed.getId(), otherId);
        insertBookmark(feed.getId(), viewerId);
        insertComment(feed.getId(), otherId, false);

        FeedItem detail = feedQueryRepository.findById(feed.getId(), viewerId).orElseThrow();
        FeedItem listItem = feedQueryRepository.findAll(
                FeedSort.LATEST,
                null,
                null,
                viewerId,
                null,
                10
        ).items().stream().filter(item -> item.feedId() == feed.getId()).findFirst().orElseThrow();
        FeedItem userFeedItem = feedQueryRepository.findAllByAuthorId(
                authorId,
                viewerId,
                null,
                10
        ).items().stream().filter(item -> item.feedId() == feed.getId()).findFirst().orElseThrow();

        assertThat(detail.likeCount()).isEqualTo(2L);
        assertThat(detail.bookmarkCount()).isEqualTo(1L);
        assertThat(detail.commentCount()).isEqualTo(1L);
        assertThat(detail.likedByMe()).isTrue();
        assertThat(detail.bookmarkedByMe()).isTrue();
        assertThat(listItem.likedByMe()).isTrue();
        assertThat(listItem.bookmarkedByMe()).isTrue();
        assertThat(userFeedItem.likedByMe()).isTrue();
        assertThat(userFeedItem.bookmarkedByMe()).isTrue();

        FeedItem anonymous = feedQueryRepository.findById(feed.getId()).orElseThrow();
        assertThat(anonymous.likedByMe()).isFalse();
        assertThat(anonymous.bookmarkedByMe()).isFalse();
    }

    @Test
    void 제목과_본문을_검색해_정확도순과_최신순으로_조회한다() {
        long authorId = insertUser("WOOWACOURSE_CREW", "BACKEND", (short) 8);
        long categoryId = insertCategory(true);
        long otherCategoryId = insertCategory(true);
        Instant base = Instant.parse("2026-09-11T00:00:00Z");
        Feed exact = saveFeed(authorId, "우테코", "본문", base, categoryId);
        Feed olderPrefix = saveFeed(
                authorId,
                "우테코 이전 이야기",
                "본문",
                base.plus(1, ChronoUnit.HOURS),
                categoryId
        );
        Feed latestPrefix = saveFeed(
                authorId,
                "우테코 최신 이야기",
                "본문",
                base.plus(2, ChronoUnit.HOURS),
                categoryId
        );
        Feed titleContains = saveFeed(
                authorId,
                "함께한 우테코 회고",
                "본문",
                base.plus(3, ChronoUnit.HOURS),
                categoryId
        );
        Feed contentContains = saveFeed(
                authorId,
                "다른 제목",
                "우테코 본문",
                base.plus(4, ChronoUnit.HOURS),
                categoryId
        );
        saveFeed(authorId, "검색 제외", "다른 본문", base.plus(5, ChronoUnit.HOURS), categoryId);
        saveFeed(
                authorId,
                "우테코 다른 카테고리",
                "본문",
                base.plus(6, ChronoUnit.HOURS),
                otherCategoryId
        );
        Feed deleted = saveFeed(
                authorId,
                "우테코 삭제 피드",
                "본문",
                base.plus(7, ChronoUnit.HOURS),
                categoryId
        );
        feedRepository.update(deleted.delete(base.plus(8, ChronoUnit.HOURS)));

        FeedPage firstPage = feedQueryRepository.findAll(
                FeedSort.RELEVANCE,
                categoryId,
                "우테코",
                null,
                2
        );
        FeedItem lastItem = firstPage.items().getLast();
        FeedPage secondPage = feedQueryRepository.findAll(
                FeedSort.RELEVANCE,
                categoryId,
                "우테코",
                new FeedCursor(
                        FeedSort.RELEVANCE,
                        lastItem.relevanceRank(),
                        0L,
                        lastItem.createdAt(),
                        lastItem.feedId()
                ),
                10
        );

        assertThat(firstPage.items()).extracting(FeedItem::feedId)
                .containsExactly(exact.getId(), latestPrefix.getId());
        assertThat(secondPage.items()).extracting(FeedItem::feedId)
                .containsExactly(
                        olderPrefix.getId(),
                        titleContains.getId(),
                        contentContains.getId()
                );
        assertThat(firstPage.items()).extracting(FeedItem::relevanceRank).containsExactly(0, 1);
        assertThat(secondPage.items()).extracting(FeedItem::relevanceRank).containsExactly(1, 2, 3);
        assertThat(firstPage.totalCount()).isEqualTo(5L);
        assertThat(secondPage.totalCount()).isEqualTo(5L);
    }

    @Test
    void LIKE_와일드카드를_일반_문자로_검색한다() {
        long authorId = insertUser("WOOWACOURSE_CREW", "BACKEND", (short) 8);
        long categoryId = insertCategory(true);
        Instant base = Instant.parse("2026-09-11T00:00:00Z");
        Feed percent = saveFeed(authorId, "진행률 100%", "본문", base, categoryId);
        Feed underscore = saveFeed(authorId, "검색_대상", "본문", base, categoryId);
        Feed backslash = saveFeed(authorId, "경로\\검색", "본문", base, categoryId);
        saveFeed(authorId, "진행률 1000", "검색대상", base, categoryId);

        assertThat(feedQueryRepository.findAll(
                FeedSort.RELEVANCE, categoryId, "%", null, 10
        ).items()).extracting(FeedItem::feedId).containsExactly(percent.getId());
        assertThat(feedQueryRepository.findAll(
                FeedSort.RELEVANCE, categoryId, "_", null, 10
        ).items()).extracting(FeedItem::feedId).containsExactly(underscore.getId());
        assertThat(feedQueryRepository.findAll(
                FeedSort.RELEVANCE, categoryId, "\\", null, 10
        ).items()).extracting(FeedItem::feedId).containsExactly(backslash.getId());
    }

    @Test
    void 피드_제목_자동완성_후보를_중복_없이_정확도순으로_조회한다() {
        long authorId = insertUser("WOOWACOURSE_CREW", "BACKEND", (short) 8);
        long categoryId = insertCategory(true);
        Instant base = Instant.parse("2026-09-11T00:00:00Z");
        saveFeed(authorId, "우테코", "본문", base, categoryId);
        saveFeed(authorId, "우테코 회고", "본문", base.plus(1, ChronoUnit.HOURS), categoryId);
        saveFeed(authorId, "우테코 회고", "다른 본문", base.plus(2, ChronoUnit.HOURS), categoryId);
        saveFeed(authorId, "함께한 우테코 이야기", "본문", base.plus(3, ChronoUnit.HOURS), categoryId);
        Feed deleted = saveFeed(
                authorId,
                "우테코 삭제",
                "본문",
                base.plus(4, ChronoUnit.HOURS),
                categoryId
        );
        feedRepository.update(deleted.delete(base.plus(5, ChronoUnit.HOURS)));

        List<String> suggestions = feedQueryRepository.findTitleSuggestions("우테코", 10);

        assertThat(suggestions).containsExactly("우테코", "우테코 회고", "함께한 우테코 이야기");
        assertThat(feedQueryRepository.findTitleSuggestions("우테코", 2))
                .containsExactly("우테코", "우테코 회고");
    }

    @Test
    void 제목_자동완성에서도_LIKE_와일드카드를_일반_문자로_검색한다() {
        long authorId = insertUser("WOOWACOURSE_CREW", "BACKEND", (short) 8);
        long categoryId = insertCategory(true);
        Instant base = Instant.parse("2026-09-11T00:00:00Z");
        saveFeed(authorId, "진행률 100%", "본문", base, categoryId);
        saveFeed(authorId, "진행률 1000", "본문", base, categoryId);

        assertThat(feedQueryRepository.findTitleSuggestions("100%", 10))
                .containsExactly("진행률 100%");
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

        FeedPage firstPage = feedQueryRepository.findAllByAuthorId(authorId, null, 2);
        FeedItem lastItem = firstPage.items().getLast();
        FeedPage secondPage = feedQueryRepository.findAllByAuthorId(
                authorId,
                new FeedCursor(FeedSort.LATEST, 0, 0L, lastItem.createdAt(), lastItem.feedId()),
                2
        );

        assertThat(firstPage.items()).extracting(FeedItem::feedId)
                .containsExactly(latest.getId(), middle.getId());
        assertThat(firstPage.items().getFirst().likeCount()).isEqualTo(1L);
        assertThat(firstPage.items().getFirst().commentCount()).isEqualTo(1L);
        assertThat(secondPage.items()).extracting(FeedItem::feedId).containsExactly(oldest.getId());
        assertThat(firstPage.totalCount()).isEqualTo(3L);
        assertThat(secondPage.totalCount()).isEqualTo(3L);
    }

    @Test
    void 타인과_비로그인_사용자의_프로필_피드에서는_익명_글을_제외하고_개수도_맞춘다() {
        long authorId = insertUser("GENERAL", null, null);
        long viewerId = insertUser("GENERAL", null, null);
        long categoryId = insertCategory(true);
        Instant base = Instant.parse("2026-09-11T00:00:00Z");
        Feed open = saveFeed(authorId, "공개 글", base, categoryId);
        saveAnonymousFeed(authorId, "익명 글", base.plus(1, ChronoUnit.HOURS), categoryId);

        FeedPage otherView = feedQueryRepository.findAllByAuthorId(authorId, viewerId, null, 10);
        FeedPage guestView = feedQueryRepository.findAllByAuthorId(authorId, null, 10);

        assertThat(otherView.items()).extracting(FeedItem::feedId).containsExactly(open.getId());
        assertThat(otherView.totalCount()).isEqualTo(1L);
        assertThat(guestView.items()).extracting(FeedItem::feedId).containsExactly(open.getId());
        assertThat(guestView.totalCount()).isEqualTo(1L);
    }

    @Test
    void 본인_프로필_피드에서는_익명_글을_포함하고_개수도_맞춘다() {
        long authorId = insertUser("GENERAL", null, null);
        long categoryId = insertCategory(true);
        Instant base = Instant.parse("2026-09-11T00:00:00Z");
        Feed open = saveFeed(authorId, "공개 글", base, categoryId);
        Feed anonymous = saveAnonymousFeed(authorId, "익명 글", base.plus(1, ChronoUnit.HOURS), categoryId);

        FeedPage ownerView = feedQueryRepository.findAllByAuthorId(authorId, authorId, null, 10);

        assertThat(ownerView.items()).extracting(FeedItem::feedId)
                .containsExactly(anonymous.getId(), open.getId());
        assertThat(ownerView.totalCount()).isEqualTo(2L);
    }

    @Test
    void 피드_타입_필터와_함께_조회해도_타인에게는_익명_글이_제외된다() {
        long authorId = insertUser("GENERAL", null, null);
        long viewerId = insertUser("GENERAL", null, null);
        long categoryId = insertCategory(true);
        Instant base = Instant.parse("2026-09-11T00:00:00Z");
        Feed openQuestion = feedRepository.save(Feed.create(
                authorId, FeedType.QUESTION, "공개 질문", "본문", false, base));
        feedRepository.saveCategories(openQuestion.getId(), List.of(categoryId));
        Feed anonymousQuestion = feedRepository.save(Feed.create(
                authorId, FeedType.QUESTION, "익명 질문", "본문", true, base.plus(1, ChronoUnit.HOURS)));
        feedRepository.saveCategories(anonymousQuestion.getId(), List.of(categoryId));

        FeedPage page = feedQueryRepository.findAllByAuthorId(
                authorId, FeedType.QUESTION, viewerId, null, 10);

        assertThat(page.items()).extracting(FeedItem::feedId).containsExactly(openQuestion.getId());
        assertThat(page.totalCount()).isEqualTo(1L);
    }

    private Feed saveAnonymousFeed(long authorId, String content, Instant createdAt, long categoryId) {
        Feed feed = feedRepository.save(Feed.create(authorId, "제목 " + content, content, true, createdAt));
        feedRepository.saveCategories(feed.getId(), List.of(categoryId));
        return feed;
    }

    private Feed saveFeed(long authorId, String content, Instant createdAt, long categoryId, long... mediaIds) {
        return saveFeed(authorId, "제목 " + content, content, createdAt, categoryId, mediaIds);
    }

    private Feed saveFeed(
            long authorId,
            String title,
            String content,
            Instant createdAt,
            long categoryId,
            long... mediaIds
    ) {
        Feed feed = feedRepository.save(Feed.create(authorId, title, content, createdAt));
        feedRepository.saveCategories(feed.getId(), List.of(categoryId));
        feedRepository.saveMedia(feed.getId(), java.util.Arrays.stream(mediaIds).boxed().toList());
        return feed;
    }

    private long insertUser(String userType, String track, Short cohort) {
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Long userId = jdbcTemplate.queryForObject(
                "INSERT INTO users (handle) VALUES (?) RETURNING id",
                Long.class,
                "@feed_" + token
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
        long categoryId = jdbcTemplate.queryForObject(
                "INSERT INTO categories (slug, display_name, is_active) VALUES (?, ?, ?) RETURNING id",
                Long.class,
                "feed-" + token,
                "카테고리 " + token,
                active
        );
        jdbcTemplate.update(
                "INSERT INTO category_feed_types (category_id, feed_type) VALUES (?, 'POST')",
                categoryId
        );
        return categoryId;
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

    private void insertBookmark(long feedId, long userId) {
        jdbcTemplate.update(
                """
                        INSERT INTO feed_reactions (feed_id, user_id, reaction_type, created_at)
                        VALUES (?, ?, 'BOOKMARK', '2020-01-01T00:00:00Z')
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
