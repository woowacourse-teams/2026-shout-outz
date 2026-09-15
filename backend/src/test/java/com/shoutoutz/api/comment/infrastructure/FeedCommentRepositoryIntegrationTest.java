package com.shoutoutz.api.comment.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.comment.domain.FeedComment;
import com.shoutoutz.api.comment.domain.FeedCommentRepository;
import com.shoutoutz.api.post.domain.Post;
import com.shoutoutz.api.post.domain.PostRepository;
import com.shoutoutz.api.user.domain.account.User;
import com.shoutoutz.api.user.domain.account.UserRepository;
import java.time.Instant;
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
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("피드 댓글을 post_comments에 저장하고 feedId와 생성 시각을 매핑한다")
    void savesAndFindsFeedComment() {
        User author = userRepository.save(User.initialize("feed-comment-" + uniqueSuffix()));
        Post feed = postRepository.save(Post.create(author.getId(), "피드 본문", NOW));

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
        Post feed = postRepository.save(Post.create(author.getId(), "피드 본문", NOW));
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

    private static String uniqueSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
