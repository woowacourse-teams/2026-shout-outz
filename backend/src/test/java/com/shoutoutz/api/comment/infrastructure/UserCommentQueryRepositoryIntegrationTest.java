package com.shoutoutz.api.comment.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.comment.application.UserCommentQueryRepository;
import com.shoutoutz.api.comment.application.dto.UserCommentCursor;
import com.shoutoutz.api.comment.application.dto.UserCommentItem;
import com.shoutoutz.api.comment.application.dto.UserCommentType;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class UserCommentQueryRepositoryIntegrationTest {

    private static final Instant BASE_TIME = Instant.parse("2026-09-16T00:00:00Z");

    @Autowired
    private UserCommentQueryRepository userCommentQueryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 공개_대상에_작성한_피드와_프로젝트_댓글을_최신순으로_조회한다() {
        long authorId = insertUser();
        long otherId = insertUser();
        long feedId = insertFeed(authorId, false);
        long deletedFeedId = insertFeed(authorId, true);
        long projectId = insertProject(authorId, "APPROVED", false);
        long pendingProjectId = insertProject(authorId, "PENDING", false);
        long deletedProjectId = insertProject(authorId, "APPROVED", true);

        long olderFeedCommentId = insertFeedComment(feedId, authorId, "오래된 피드 댓글", BASE_TIME, false);
        long projectCommentId = insertProjectComment(
                projectId,
                authorId,
                "프로젝트 댓글",
                BASE_TIME.plusSeconds(60),
                false
        );
        long latestFeedCommentId = insertFeedComment(
                feedId,
                authorId,
                "최신 피드 댓글",
                BASE_TIME.plusSeconds(120),
                false
        );
        insertFeedComment(feedId, otherId, "다른 사용자 댓글", BASE_TIME.plusSeconds(300), false);
        insertFeedComment(feedId, authorId, "삭제된 댓글", BASE_TIME.plusSeconds(290), true);
        insertFeedComment(deletedFeedId, authorId, "삭제된 피드 댓글", BASE_TIME.plusSeconds(280), false);
        insertProjectComment(pendingProjectId, authorId, "미승인 프로젝트 댓글", BASE_TIME.plusSeconds(270), false);
        insertProjectComment(deletedProjectId, authorId, "삭제된 프로젝트 댓글", BASE_TIME.plusSeconds(260), false);

        List<UserCommentItem> firstPage = userCommentQueryRepository.findAllByAuthorId(authorId, null, 2);
        UserCommentItem lastItem = firstPage.getLast();
        List<UserCommentItem> secondPage = userCommentQueryRepository.findAllByAuthorId(
                authorId,
                new UserCommentCursor(lastItem.createdAt(), lastItem.type(), lastItem.commentId()),
                2
        );

        assertThat(firstPage).extracting(UserCommentItem::commentId)
                .containsExactly(latestFeedCommentId, projectCommentId);
        assertThat(firstPage).extracting(UserCommentItem::type)
                .containsExactly(UserCommentType.FEED, UserCommentType.PROJECT);
        assertThat(firstPage).extracting(UserCommentItem::targetId)
                .containsExactly(feedId, projectId);
        assertThat(secondPage).extracting(UserCommentItem::commentId)
                .containsExactly(olderFeedCommentId);
    }

    private long insertUser() {
        String handle = "comment_" + token();
        return jdbcTemplate.queryForObject(
                "INSERT INTO users (handle) VALUES (?) RETURNING id",
                Long.class,
                handle
        );
    }

    private long insertFeed(long authorId, boolean deleted) {
        return jdbcTemplate.queryForObject(
                """
                        INSERT INTO feeds (author_id, content, deleted_at)
                        VALUES (?, '피드 본문', ?)
                        RETURNING id
                        """,
                Long.class,
                authorId,
                deletedAt(deleted, BASE_TIME)
        );
    }

    private long insertProject(long authorId, String approvalStatus, boolean deleted) {
        String token = token();
        return jdbcTemplate.queryForObject(
                """
                        INSERT INTO projects (
                            cohort, registered_by, team_name, slug, title, tagline,
                            service_status, approval_status, github_repository_url, deleted_at
                        ) VALUES (
                            8, ?, '테스트 팀', ?, '테스트 프로젝트', '프로젝트 소개',
                            'OPERATING', ?, 'https://github.com/test/project',
                            ?
                        )
                        RETURNING id
                        """,
                Long.class,
                authorId,
                "comment-" + token,
                approvalStatus,
                deletedAt(deleted, BASE_TIME)
        );
    }

    private long insertFeedComment(
            long feedId,
            long authorId,
            String content,
            Instant createdAt,
            boolean deleted
    ) {
        return jdbcTemplate.queryForObject(
                """
                        INSERT INTO feed_comments (
                            feed_id, author_id, content, created_at, updated_at, deleted_at
                        ) VALUES (?, ?, ?, ?, ?, ?)
                        RETURNING id
                        """,
                Long.class,
                feedId,
                authorId,
                content,
                Timestamp.from(createdAt),
                Timestamp.from(createdAt),
                deletedAt(deleted, createdAt.plusSeconds(1))
        );
    }

    private long insertProjectComment(
            long projectId,
            long authorId,
            String content,
            Instant createdAt,
            boolean deleted
    ) {
        return jdbcTemplate.queryForObject(
                """
                        INSERT INTO project_comments (
                            project_id, author_id, content, created_at, updated_at, deleted_at
                        ) VALUES (?, ?, ?, ?, ?, ?)
                        RETURNING id
                        """,
                Long.class,
                projectId,
                authorId,
                content,
                Timestamp.from(createdAt),
                Timestamp.from(createdAt),
                deletedAt(deleted, createdAt.plusSeconds(1))
        );
    }

    private Timestamp deletedAt(boolean deleted, Instant deletedAt) {
        if (!deleted) {
            return null;
        }
        return Timestamp.from(deletedAt);
    }

    private String token() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
