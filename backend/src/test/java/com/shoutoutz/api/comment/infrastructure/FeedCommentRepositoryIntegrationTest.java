package com.shoutoutz.api.comment.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.comment.application.FeedCommentQueryRepository;
import com.shoutoutz.api.comment.application.dto.FeedCommentCursor;
import com.shoutoutz.api.comment.application.dto.FeedCommentPage;
import com.shoutoutz.api.comment.domain.FeedComment;
import com.shoutoutz.api.comment.domain.FeedCommentRepository;
import com.shoutoutz.api.comment.domain.FeedCommentSort;
import com.shoutoutz.api.feed.domain.Feed;
import com.shoutoutz.api.feed.domain.FeedRepository;
import com.shoutoutz.api.user.domain.account.User;
import com.shoutoutz.api.user.domain.account.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

    @Test
    @DisplayName("피드 댓글을 feed_comments에 저장하고 feedId와 생성 시각을 매핑한다")
    void savesAndFindsFeedComment() {
        User author = userRepository.save(User.initialize("feed-comment-" + uniqueSuffix()));
        Feed feed = feedRepository.save(Feed.create(author.getId(), "피드 본문", NOW));

        FeedComment saved = feedCommentRepository.save(
                FeedComment.create(feed.getId(), author.getId(), null, "  댓글 내용  ")
        );

        FeedComment found = feedCommentRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getFeedId()).isEqualTo(feed.getId());
        assertThat(found.getAuthorId()).isEqualTo(author.getId());
        assertThat(found.getParentId()).isNull();
        assertThat(found.getContent()).isEqualTo("댓글 내용");
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
        assertThat(found.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("같은 피드의 부모 댓글을 참조하는 대댓글을 저장한다")
    void savesReplyWithParentId() {
        User author = userRepository.save(User.initialize("feed-reply-" + uniqueSuffix()));
        Feed feed = feedRepository.save(Feed.create(author.getId(), "피드 본문", NOW));
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
    @DisplayName("기존 피드 댓글의 내용을 수정하고 생성 시각은 유지한다")
    void updatesFeedComment() {
        User author = userRepository.save(User.initialize("feed-update-" + uniqueSuffix()));
        Feed feed = feedRepository.save(Feed.create(author.getId(), "피드 본문", NOW));

        FeedComment saved = feedCommentRepository.save(
                FeedComment.create(feed.getId(), author.getId(), null, "기존 댓글")
        );

        FeedComment updated = feedCommentRepository.save(saved.updateContent("수정된 댓글"));
        FeedComment found = feedCommentRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getContent()).isEqualTo("수정된 댓글");
        assertThat(found.getCreatedAt()).isEqualTo(saved.getCreatedAt());
        assertThat(found.getUpdatedAt()).isEqualTo(updated.getUpdatedAt());
        assertThat(found.getUpdatedAt()).isNotEqualTo(found.getCreatedAt());
    }

    @Test
    @DisplayName("루트 댓글을 정렬 기준과 크기에 따라 조회하고 대댓글을 부모 ID로 조회한다")
    void findsRootPageAndReplies() {
        User author = userRepository.save(User.initialize("feed-list-" + uniqueSuffix()));
        Feed feed = feedRepository.save(Feed.create(author.getId(), "피드 본문", NOW));
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

        assertThat(page.comments()).hasSize(1);
        assertThat(page.comments().getFirst().getId()).isEqualTo(secondRoot.getId());
        assertThat(page.hasNext()).isTrue();

        FeedCommentPage nextPage = feedCommentQueryRepository.findRootCommentsPage(
                feed.getId(),
                new FeedCommentCursor(
                        page.comments().getFirst().getCreatedAt(),
                        page.comments().getFirst().getId(),
                        FeedCommentSort.LATEST
                ),
                FeedCommentSort.LATEST,
                1
        );
        assertThat(nextPage.comments()).extracting(FeedComment::getId)
                .containsExactly(firstRoot.getId());

        FeedCommentPage oldestPage = feedCommentQueryRepository.findRootCommentsPage(
                feed.getId(),
                null,
                FeedCommentSort.OLDEST,
                1
        );
        assertThat(oldestPage.comments().getFirst().getId()).isEqualTo(firstRoot.getId());
        assertThat(feedCommentQueryRepository.findReplies(
                feed.getId(),
                List.of(firstRoot.getId())
        )).extracting(FeedComment::getId).containsExactly(reply.getId());
    }

    private static String uniqueSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
