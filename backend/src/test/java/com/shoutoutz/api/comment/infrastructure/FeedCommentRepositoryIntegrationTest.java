package com.shoutoutz.api.comment.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.shoutoutz.api.comment.application.FeedCommentQueryRepository;
import com.shoutoutz.api.comment.application.dto.FeedCommentCursor;
import com.shoutoutz.api.comment.application.dto.FeedCommentPage;
import com.shoutoutz.api.comment.domain.FeedComment;
import com.shoutoutz.api.comment.domain.FeedCommentRepository;
import com.shoutoutz.api.comment.domain.FeedCommentSort;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.feed.domain.Feed;
import com.shoutoutz.api.feed.domain.FeedRepository;
import com.shoutoutz.api.user.domain.account.User;
import com.shoutoutz.api.user.domain.account.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
class FeedCommentRepositoryIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-09-14T00:00:00Z");

    @Autowired
    private FeedCommentRepository feedCommentRepository;

    @Autowired
    private FeedCommentQueryRepository feedCommentQueryRepository;

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("피드 댓글을 feed_comments에 저장하고 feedId와 생성 시각을 매핑한다")
    void savesAndFindsFeedComment() {
        User author = userRepository.save(User.initialize("@feed-comment-" + uniqueSuffix()));
        Feed feed = feedRepository.save(Feed.create(author.getId(), "피드 제목", "피드 본문", NOW));

        FeedComment saved = feedCommentRepository.save(
                FeedComment.create(feed.getId(), author.getId(), null, "  댓글 내용  ")
        );

        FeedComment found = feedCommentRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getFeedId()).isEqualTo(feed.getId());
        assertThat(found.getAuthorId()).isEqualTo(author.getId());
        assertThat(found.getParentId()).isNull();
        assertThat(found.getContent()).isEqualTo("댓글 내용");
        assertThat(found.isAnonymous()).isFalse();
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
        assertThat(found.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("같은 피드의 부모 댓글을 참조하는 대댓글을 저장한다")
    void savesReplyWithParentId() {
        User author = userRepository.save(User.initialize("@feed-reply-" + uniqueSuffix()));
        Feed feed = feedRepository.save(Feed.create(author.getId(), "피드 제목", "피드 본문", NOW));
        FeedComment root = feedCommentRepository.save(
                FeedComment.create(feed.getId(), author.getId(), null, "루트 댓글")
        );

        FeedComment reply = feedCommentRepository.save(
                FeedComment.create(feed.getId(), author.getId(), root.getId(), "대댓글")
        );

        FeedComment found = feedCommentRepository.findById(reply.getId()).orElseThrow();
        assertThat(found.getFeedId()).isEqualTo(feed.getId());
        assertThat(found.getParentId()).isEqualTo(root.getId());
    }

    @Test
    @DisplayName("피드 댓글의 익명 여부를 저장하고 조회한다")
    void savesAnonymousFlag() {
        User author = userRepository.save(User.initialize("@feed-anonymous-" + uniqueSuffix()));
        Feed feed = feedRepository.save(Feed.create(author.getId(), "피드 제목", "피드 본문", NOW));

        FeedComment saved = feedCommentRepository.save(
                FeedComment.create(feed.getId(), author.getId(), null, "익명 댓글", true)
        );

        FeedComment found = feedCommentRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.isAnonymous()).isTrue();
    }

    @Test
    @DisplayName("기존 피드 댓글의 내용을 수정하고 생성 시각은 유지한다")
    void updatesFeedComment() {
        User author = userRepository.save(User.initialize("@feed-update-" + uniqueSuffix()));
        Feed feed = feedRepository.save(Feed.create(author.getId(), "피드 제목", "피드 본문", NOW));

        FeedComment saved = feedCommentRepository.save(
                FeedComment.create(feed.getId(), author.getId(), null, "기존 댓글")
        );

        FeedComment updated = feedCommentRepository.save(saved.updateContent("수정된 댓글"));
        FeedComment found = feedCommentRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getContent()).isEqualTo("수정된 댓글");
        // 저장 직후 시각은 메모리 값(리눅스에서는 나노초)이고, 다시 읽은 시각은 DB 가 마이크로초로 반올림한 값이라 1마이크로초까지 다를 수 있다.
        assertThat(found.getCreatedAt()).isCloseTo(saved.getCreatedAt(), within(1, ChronoUnit.MICROS));
        assertThat(found.getUpdatedAt()).isEqualTo(updated.getUpdatedAt());
        assertThat(found.getUpdatedAt()).isNotEqualTo(found.getCreatedAt());
        assertThat(commentCountOf(feed.getId())).isEqualTo(1L);
    }

    @Test
    @DisplayName("이미 삭제된 댓글의 내용을 수정하면 찾을 수 없음 예외가 나고 삭제 상태는 유지된다")
    void doesNotRestoreDeletedCommentWhenUpdatingContent() {
        User author = userRepository.save(User.initialize("@feed-upd-del-" + uniqueSuffix()));
        Feed feed = feedRepository.save(Feed.create(author.getId(), "피드 제목", "피드 본문", NOW));
        FeedComment saved = feedCommentRepository.save(
                FeedComment.create(feed.getId(), author.getId(), null, "기존 댓글")
        );
        feedCommentRepository.save(saved.delete(Instant.now()));

        assertThatThrownBy(() -> feedCommentRepository.save(saved.updateContent("수정된 댓글")))
                .isInstanceOf(EntityNotFoundException.class);

        FeedComment found = feedCommentRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.isDeleted()).isTrue();
        assertThat(found.getContent()).isEqualTo("기존 댓글");
        assertThat(commentCountOf(feed.getId())).isZero();
    }

    @Test
    @DisplayName("피드 댓글을 soft delete하면 삭제·수정 시각을 갱신한다")
    void softDeletesFeedComment() {
        User author = userRepository.save(User.initialize("@feed-delete-" + uniqueSuffix()));
        Feed feed = feedRepository.save(Feed.create(author.getId(), "피드 제목", "피드 본문", NOW));

        FeedComment saved = feedCommentRepository.save(
                FeedComment.create(feed.getId(), author.getId(), null, "삭제할 댓글")
        );

        FeedComment deleted = feedCommentRepository.save(saved.delete(Instant.now()));
        FeedComment found = feedCommentRepository.findById(saved.getId()).orElseThrow();

        assertThat(deleted.isDeleted()).isTrue();
        assertThat(found.isDeleted()).isTrue();
        assertThat(found.getDeletedAt()).isNotNull();
        assertThat(found.getContent()).isEqualTo("삭제할 댓글");
        assertThat(found.getUpdatedAt()).isAfter(saved.getUpdatedAt());
        assertThat(found.getUpdatedAt()).isEqualTo(found.getDeletedAt());
    }

    @Test
    @DisplayName("피드 댓글과 대댓글을 저장하면 피드의 댓글 수를 하나씩 올린다")
    void increasesFeedCommentCountWhenSaved() {
        User author = userRepository.save(User.initialize("@feed-count-" + uniqueSuffix()));
        Feed feed = feedRepository.save(Feed.create(author.getId(), "피드 제목", "피드 본문", NOW));

        FeedComment root = feedCommentRepository.save(
                FeedComment.create(feed.getId(), author.getId(), null, "루트 댓글")
        );
        feedCommentRepository.save(FeedComment.create(feed.getId(), author.getId(), root.getId(), "대댓글"));

        assertThat(commentCountOf(feed.getId())).isEqualTo(2L);
    }

    @Test
    @DisplayName("이미 삭제된 댓글을 다시 삭제하면 피드의 댓글 수를 내리지 않는다")
    void decreasesFeedCommentCountOnlyOnFirstDeletion() {
        User author = userRepository.save(User.initialize("@feed-count-delete-" + uniqueSuffix()));
        Feed feed = feedRepository.save(Feed.create(author.getId(), "피드 제목", "피드 본문", NOW));
        FeedComment deletedTarget = feedCommentRepository.save(
                FeedComment.create(feed.getId(), author.getId(), null, "삭제할 댓글")
        );
        feedCommentRepository.save(FeedComment.create(feed.getId(), author.getId(), null, "남을 댓글"));

        FeedComment firstDeleted = feedCommentRepository.save(deletedTarget.delete(Instant.now()));
        FeedComment secondDeleted = feedCommentRepository.save(deletedTarget.delete(Instant.now()));

        assertThat(firstDeleted.isDeleted()).isTrue();
        assertThat(secondDeleted.isDeleted()).isTrue();
        assertThat(secondDeleted.getDeletedAt()).isEqualTo(firstDeleted.getDeletedAt());
        assertThat(commentCountOf(feed.getId())).isEqualTo(1L);
    }

    private long commentCountOf(long feedId) {
        return jdbcTemplate.queryForObject("SELECT comment_count FROM feeds WHERE id = ?", Long.class, feedId);
    }

    @Test
    @DisplayName("루트 댓글을 정렬 기준과 크기에 따라 조회하고 대댓글을 부모 ID로 조회한다")
    void findsRootPageAndReplies() {
        User author = userRepository.save(User.initialize("@feed-list-" + uniqueSuffix()));
        Feed feed = feedRepository.save(Feed.create(author.getId(), "피드 제목", "피드 본문", NOW));
        FeedComment firstRoot = feedCommentRepository.save(
                FeedComment.create(feed.getId(), author.getId(), null, "첫 번째 루트")
        );
        FeedComment reply = feedCommentRepository.save(
                FeedComment.create(feed.getId(), author.getId(), firstRoot.getId(), "첫 번째 대댓글")
        );
        FeedComment secondRoot = feedCommentRepository.save(
                FeedComment.create(feed.getId(), author.getId(), null, "두 번째 루트")
        );

        FeedCommentPage page = feedCommentQueryRepository.findRootCommentsPage(
                feed.getId(),
                null,
                FeedCommentSort.LATEST,
                1
        );

        assertThat(page.items()).hasSize(1);
        assertThat(page.items().getFirst().getId()).isEqualTo(secondRoot.getId());
        assertThat(page.hasNext()).isTrue();

        FeedCommentPage nextPage = feedCommentQueryRepository.findRootCommentsPage(
                feed.getId(),
                new FeedCommentCursor(
                        page.items().getFirst().getCreatedAt(),
                        page.items().getFirst().getId(),
                        FeedCommentSort.LATEST
                ),
                FeedCommentSort.LATEST,
                1
        );
        assertThat(nextPage.items()).extracting(FeedComment::getId)
                .containsExactly(firstRoot.getId());

        FeedCommentPage oldestPage = feedCommentQueryRepository.findRootCommentsPage(
                feed.getId(),
                null,
                FeedCommentSort.OLDEST,
                1
        );
        assertThat(oldestPage.items().getFirst().getId()).isEqualTo(firstRoot.getId());
        assertThat(feedCommentQueryRepository.findReplies(
                feed.getId(),
                List.of(firstRoot.getId())
        )).extracting(FeedComment::getId).containsExactly(reply.getId());
        assertThat(page.totalCount()).isEqualTo(3L);
        assertThat(nextPage.totalCount()).isEqualTo(3L);
        assertThat(oldestPage.totalCount()).isEqualTo(3L);
    }

    private static String uniqueSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
