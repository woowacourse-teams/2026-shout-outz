package com.shoutoutz.api.interview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.comment.application.InterviewCommentService;
import com.shoutoutz.api.comment.application.InterviewCommentReactionService;
import com.shoutoutz.api.comment.application.UserCommentService;
import com.shoutoutz.api.comment.application.dto.UserCommentType;
import com.shoutoutz.api.comment.presentation.dto.request.InterviewCommentCreateRequest;
import com.shoutoutz.api.comment.presentation.dto.request.InterviewCommentUpdateRequest;
import com.shoutoutz.api.comment.presentation.dto.request.InterviewCommentFindRequest;
import com.shoutoutz.api.comment.presentation.dto.request.UserCommentFindRequest;
import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.feed.application.FeedLinkPreviewRepository;
import com.shoutoutz.api.feed.application.dto.LinkPreviewMetadata;
import com.shoutoutz.api.notification.application.NotificationService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InterviewCommentIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired jakarta.persistence.EntityManager entityManager;
    @Autowired InterviewCommentService comments;
    @Autowired InterviewCommentReactionService reactions;
    @Autowired FeedLinkPreviewRepository previews;
    @Autowired NotificationService notifications;
    @Autowired UserCommentService userComments;
    long interviewId;
    long adminId;
    long authorId;
    long otherId;

    @BeforeEach
    void setUp() {
        adminId = user("ADMIN");
        authorId = user("USER");
        otherId = user("USER");
        interviewId = jdbc.queryForObject("""
                INSERT INTO interviews(title, summary, interviewee_id, author_id, status, published_at)
                VALUES ('인터뷰', '요약', ?, ?, 'PUBLISHED', now()) RETURNING id
                """, Long.class, authorId, adminId);
    }

    @Test
    void 댓글과_대댓글을_집계하고_중복_삭제는_차감하지_않는다() {
        long root = create(authorId, null, false);
        long reply = create(otherId, root, false);
        assertThat(count()).isEqualTo(2);
        comments.update(interviewId, root, authorId, new InterviewCommentUpdateRequest("수정"));
        assertThat(count()).isEqualTo(2);
        comments.delete(interviewId, root, authorId);
        assertThat(count()).isEqualTo(1);
        assertThatThrownBy(() -> comments.delete(interviewId, root, authorId))
                .isInstanceOf(EntityNotFoundException.class);
        assertThat(count()).isEqualTo(1);
        var page = comments.findAll(interviewId, new InterviewCommentFindRequest(null, 5, "LATEST"), null);
        assertThat(page.comments()).hasSize(2);
        assertThat(page.comments().getFirst().deleted()).isTrue();
        assertThat(page.comments().getFirst().content()).isNull();
        assertThat(page.comments().getLast().id()).isEqualTo(reply);
        assertThat(page.meta().totalCount()).isEqualTo(1);
    }

    @Test
    void 대댓글의_대댓글과_다른_인터뷰의_부모를_거부한다() {
        long root = create(authorId, null, false);
        long reply = create(otherId, root, false);
        assertThatThrownBy(() -> create(otherId, reply, false)).isInstanceOf(BadRequestException.class);
        long second = jdbc.queryForObject("""
                INSERT INTO interviews(title, interviewee_id, author_id, status, published_at)
                VALUES ('다른 인터뷰', ?, ?, 'PUBLISHED', now()) RETURNING id
                """, Long.class, authorId, adminId);
        assertThatThrownBy(() -> comments.create(second, authorId,
                new InterviewCommentCreateRequest("잘못된 답글", root, false)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void 본인만_수정과_삭제할_수_있다() {
        long root = create(authorId, null, false);
        assertThatThrownBy(() -> comments.update(interviewId, root, otherId,
                new InterviewCommentUpdateRequest("수정"))).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> comments.delete(interviewId, root, otherId))
                .isInstanceOf(ForbiddenException.class);
        assertThat(count()).isEqualTo(1);
    }

    @Test
    void 익명_작성자는_본인에게만_보이고_알림에서도_숨긴다() {
        long root = create(authorId, null, true);
        var publicComment = comments.findAll(interviewId,
                new InterviewCommentFindRequest(null, 5, "LATEST"), otherId).comments().getFirst();
        assertThat(publicComment.author().userId()).isNull();
        assertThat(publicComment.author().handle()).isNull();
        assertThat(publicComment.author().displayName()).isNull();
        var mine = comments.findAll(interviewId,
                new InterviewCommentFindRequest(null, 5, "LATEST"), authorId).comments().getFirst();
        assertThat(mine.author().userId()).isEqualTo(authorId);
        assertThat(jdbc.queryForObject("SELECT actor_id FROM notifications WHERE interview_comment_id = ?",
                Long.class, root)).isNull();
    }

    @Test
    void 공감은_중복되지_않고_삭제된_댓글에는_남길_수_없다() {
        long root = create(authorId, null, false);
        reactions.add(interviewId, root, otherId, "AGREE");
        reactions.add(interviewId, root, otherId, "AGREE");
        var found = comments.findAll(interviewId,
                new InterviewCommentFindRequest(null, 5, "LATEST"), otherId).comments().getFirst();
        assertThat(found.agreeCount()).isEqualTo(1);
        assertThat(found.agreedByMe()).isTrue();
        reactions.remove(interviewId, root, otherId, "AGREE");
        assertThatThrownBy(() -> reactions.remove(interviewId, root, otherId, "AGREE"))
                .isInstanceOf(EntityNotFoundException.class);
        comments.delete(interviewId, root, authorId);
        assertThatThrownBy(() -> reactions.add(interviewId, root, otherId, "AGREE"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void 최신순과_과거순_커서는_루트_단위로_조회한다() {
        for (int i = 0; i < 3; i++) {
            create(authorId, null, false);
        }
        jdbc.update("UPDATE interview_comments SET created_at = '2026-10-15T00:00:00Z' WHERE interview_id = ?", interviewId);
        entityManager.clear();
        for (String sort : new String[]{"LATEST", "OLDEST"}) {
            var first = comments.findAll(interviewId, new InterviewCommentFindRequest(null, 2, sort), null);
            var second = comments.findAll(interviewId,
                    new InterviewCommentFindRequest(first.meta().nextCursor(), 2, sort), null);
            assertThat(first.comments()).hasSize(2);
            assertThat(second.comments()).hasSize(1);
            assertThat(first.comments()).extracting(c -> c.id()).doesNotContain(second.comments().getFirst().id());
        }
    }

    @Test
    void 초안과_삭제된_인터뷰의_댓글은_읽거나_작성할_수_없다() {
        jdbc.update("UPDATE interviews SET status = 'DRAFT' WHERE id = ?", interviewId);
        assertThatThrownBy(() -> create(authorId, null, false)).isInstanceOf(EntityNotFoundException.class);
        assertThatThrownBy(() -> comments.findAll(interviewId,
                new InterviewCommentFindRequest(null, 5, "LATEST"), null))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void 인터뷰에만_달린_링크도_수집하고_캐시_정리에서_보존한다() {
        long commentId = comments.create(interviewId, authorId,
                new InterviewCommentCreateRequest("링크 https://example.com/interview-" + UUID.randomUUID(), null)).id();
        var job = previews.claimDue(Instant.now().plusSeconds(1), Instant.now().minusSeconds(3600)).orElseThrow();
        previews.complete(job, new LinkPreviewMetadata("제목", "설명", null, "Example"), Instant.now());
        assertThat(previews.findByInterviewCommentIds(java.util.List.of(commentId)).get(commentId).title())
                .isEqualTo("제목");
        previews.deleteUnusedBefore(Instant.now().plus(30, ChronoUnit.DAYS));
        assertThat(previews.findByInterviewCommentIds(java.util.List.of(commentId))).containsKey(commentId);
        comments.update(interviewId, commentId, authorId, new InterviewCommentUpdateRequest("링크 삭제"));
        assertThat(previews.findByInterviewCommentIds(java.util.List.of(commentId))).isEmpty();
    }

    @Test
    void 답글_알림을_우선하고_수신자_중복과_자기_알림을_제외한다() {
        long root = create(authorId, null, false);
        long reply = create(otherId, root, false);
        var page = notifications.findAll(authorId, null, 20);
        assertThat(page.items()).hasSize(1);
        assertThat(page.items().getFirst().notificationType().name()).isEqualTo("INTERVIEW_COMMENT_REPLY");
        assertThat(page.items().getFirst().interviewId()).isEqualTo(interviewId);
        assertThat(page.items().getFirst().interviewCommentId()).isEqualTo(reply);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notifications WHERE recipient_id = ? AND actor_id = ?",
                Long.class, otherId, otherId)).isZero();
    }

    @Test
    void 내_댓글_목록에_인터뷰를_포함하고_익명은_타인에게_숨긴다() {
        create(authorId, null, false);
        create(authorId, null, true);
        var request = new UserCommentFindRequest(null, 20);
        var mine = userComments.findAll(authorId, authorId, request);
        assertThat(mine.comments()).hasSize(2);
        assertThat(mine.comments()).allMatch(c -> c.type() == UserCommentType.INTERVIEW && c.interviewId() == interviewId);
        var other = userComments.findAll(authorId, otherId, request);
        assertThat(other.comments()).hasSize(1);
        assertThat(other.totalCount()).isEqualTo(1);
        jdbc.update("UPDATE interviews SET status = 'DRAFT' WHERE id = ?", interviewId);
        assertThat(userComments.findAll(authorId, authorId, request).comments()).isEmpty();
    }

    private long create(long userId, Long parentId, boolean anonymous) {
        return comments.create(interviewId, userId, new InterviewCommentCreateRequest("댓글", parentId, anonymous)).id();
    }

    private long count() {
        return jdbc.queryForObject("SELECT comment_count FROM interviews WHERE id = ?", Long.class, interviewId);
    }

    private long user(String role) {
        long id = jdbc.queryForObject("INSERT INTO users(handle, role) VALUES (?, ?) RETURNING id", Long.class,
                "@i" + UUID.randomUUID().toString().replace("-", "").substring(0, 20), role);
        jdbc.update("INSERT INTO user_profiles(user_id, display_name) VALUES (?, '사용자')", id);
        return id;
    }
}
