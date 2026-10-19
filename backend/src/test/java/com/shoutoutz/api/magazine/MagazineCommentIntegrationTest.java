package com.shoutoutz.api.magazine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.cohort.domain.Cohort;
import com.shoutoutz.api.comment.application.MagazineCommentReactionService;
import com.shoutoutz.api.comment.application.MagazineCommentService;
import com.shoutoutz.api.comment.application.UserCommentService;
import com.shoutoutz.api.comment.application.dto.UserCommentType;
import com.shoutoutz.api.comment.presentation.dto.request.MagazineCommentCreateRequest;
import com.shoutoutz.api.comment.presentation.dto.request.MagazineCommentFindRequest;
import com.shoutoutz.api.comment.presentation.dto.request.MagazineCommentUpdateRequest;
import com.shoutoutz.api.comment.presentation.dto.request.UserCommentFindRequest;
import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.feed.application.FeedLinkPreviewRepository;
import com.shoutoutz.api.feed.application.dto.LinkPreviewMetadata;
import com.shoutoutz.api.notification.application.NotificationService;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MagazineCommentIntegrationTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private MagazineCommentService magazineCommentService;

    @Autowired
    private MagazineCommentReactionService magazineCommentReactionService;

    @Autowired
    private FeedLinkPreviewRepository linkPreviewRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserCommentService userCommentService;

    private long magazineId;
    private long adminId;
    private long authorId;
    private long otherId;

    @BeforeEach
    void setUp() {
        adminId = user("ADMIN");
        authorId = user("USER");
        otherId = user("USER");
        magazineId = jdbcTemplate.queryForObject("""
                INSERT INTO magazines(title, summary, featured_user_id, author_id, status, published_at)
                VALUES ('매거진', '요약', ?, ?, 'PUBLISHED', now()) RETURNING id
                """, Long.class, authorId, adminId);
    }

    @Test
    void 댓글과_대댓글을_집계하고_중복_삭제는_차감하지_않는다() {
        long root = create(authorId, null, false);
        long reply = create(otherId, root, false);
        assertThat(count()).isEqualTo(2);
        magazineCommentService.update(magazineId, root, authorId, new MagazineCommentUpdateRequest("수정"));
        assertThat(count()).isEqualTo(2);
        var edited = magazineCommentService.findAll(magazineId,
                new MagazineCommentFindRequest(null, 5, "LATEST"), authorId).comments().getFirst();
        assertThat(edited.edited()).isTrue();
        magazineCommentService.delete(magazineId, root, authorId);
        assertThat(count()).isEqualTo(1);
        assertThatThrownBy(() -> magazineCommentService.delete(magazineId, root, authorId))
                .isInstanceOf(EntityNotFoundException.class);
        assertThat(count()).isEqualTo(1);
        var page = magazineCommentService.findAll(magazineId, new MagazineCommentFindRequest(null, 5, "LATEST"), null);
        assertThat(page.comments()).hasSize(2);
        assertThat(page.comments().getFirst().deleted()).isTrue();
        assertThat(page.comments().getFirst().content()).isNull();
        assertThat(page.comments().getFirst().edited()).isFalse();
        assertThat(page.comments().getLast().id()).isEqualTo(reply);
        assertThat(page.meta().totalCount()).isEqualTo(1);
    }

    @Test
    void 대댓글의_대댓글과_다른_매거진의_부모를_거부한다() {
        long root = create(authorId, null, false);
        long reply = create(otherId, root, false);
        assertThatThrownBy(() -> create(otherId, reply, false)).isInstanceOf(BadRequestException.class);
        long second = jdbcTemplate.queryForObject("""
                INSERT INTO magazines(title, featured_user_id, author_id, status, published_at)
                VALUES ('다른 매거진', ?, ?, 'PUBLISHED', now()) RETURNING id
                """, Long.class, authorId, adminId);
        assertThatThrownBy(() -> magazineCommentService.create(second, authorId,
                new MagazineCommentCreateRequest("잘못된 답글", root, false)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void 본인만_수정과_삭제할_수_있다() {
        long root = create(authorId, null, false);
        assertThatThrownBy(() -> magazineCommentService.update(magazineId, root, otherId,
                new MagazineCommentUpdateRequest("수정"))).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> magazineCommentService.delete(magazineId, root, otherId))
                .isInstanceOf(ForbiddenException.class);
        assertThat(count()).isEqualTo(1);
    }

    @Test
    void 익명_작성자는_본인에게만_보이고_알림에서도_숨긴다() {
        short cohort = (short) Cohort.current().getValue();
        jdbcTemplate.update("UPDATE user_profiles SET user_type = 'WOOWACOURSE_CREW', track = 'BACKEND', cohort = ? WHERE user_id = ?",
                cohort, authorId);
        entityManager.clear();
        var created = magazineCommentService.create(magazineId, authorId, new MagazineCommentCreateRequest("익명 댓글", null, true));
        long root = created.id();
        assertThat(created.author().track()).isEqualTo("BACKEND");
        assertThat(created.author().cohort()).isEqualTo(cohort);
        assertThat(created.author().isCurrent()).isTrue();
        var updated = magazineCommentService.update(magazineId, root, authorId, new MagazineCommentUpdateRequest("수정"));
        assertThat(updated.author()).isEqualTo(created.author());
        var publicComment = magazineCommentService.findAll(magazineId,
                new MagazineCommentFindRequest(null, 5, "LATEST"), otherId).comments().getFirst();
        assertThat(publicComment.author().userId()).isNull();
        assertThat(publicComment.author().handle()).isNull();
        assertThat(publicComment.author().displayName()).isNull();
        assertThat(publicComment.author().track()).isNull();
        assertThat(publicComment.author().cohort()).isNull();
        assertThat(publicComment.author().isCurrent()).isTrue();
        var mine = magazineCommentService.findAll(magazineId,
                new MagazineCommentFindRequest(null, 5, "LATEST"), authorId).comments().getFirst();
        assertThat(mine.author().userId()).isEqualTo(authorId);
        assertThat(mine.author().track()).isEqualTo("BACKEND");
        assertThat(mine.author().cohort()).isEqualTo(cohort);
        assertThat(jdbcTemplate.queryForObject("SELECT actor_id FROM notifications WHERE magazine_comment_id = ?",
                Long.class, root)).isNull();
    }

    @Test
    void 공감은_중복되지_않고_삭제된_댓글에는_남길_수_없다() {
        long root = create(authorId, null, false);
        magazineCommentReactionService.add(magazineId, root, otherId, "AGREE");
        magazineCommentReactionService.add(magazineId, root, otherId, "AGREE");
        var found = magazineCommentService.findAll(magazineId,
                new MagazineCommentFindRequest(null, 5, "LATEST"), otherId).comments().getFirst();
        assertThat(found.agreeCount()).isEqualTo(1);
        assertThat(found.agreedByMe()).isTrue();
        magazineCommentReactionService.remove(magazineId, root, otherId, "AGREE");
        assertThatThrownBy(() -> magazineCommentReactionService.remove(magazineId, root, otherId, "AGREE"))
                .isInstanceOf(EntityNotFoundException.class);
        magazineCommentService.delete(magazineId, root, authorId);
        assertThatThrownBy(() -> magazineCommentReactionService.add(magazineId, root, otherId, "AGREE"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void 최신순과_과거순_커서는_루트_단위로_조회한다() {
        for (int i = 0; i < 3; i++) {
            create(authorId, null, false);
        }
        jdbcTemplate.update("UPDATE magazine_comments SET created_at = '2026-10-15T00:00:00Z' WHERE magazine_id = ?", magazineId);
        entityManager.clear();
        for (String sort : new String[]{"LATEST", "OLDEST"}) {
            var first = magazineCommentService.findAll(magazineId, new MagazineCommentFindRequest(null, 2, sort), null);
            var second = magazineCommentService.findAll(magazineId,
                    new MagazineCommentFindRequest(first.meta().nextCursor(), 2, sort), null);
            assertThat(first.comments()).hasSize(2);
            assertThat(second.comments()).hasSize(1);
            assertThat(first.comments()).extracting(c -> c.id()).doesNotContain(second.comments().getFirst().id());
        }
    }

    @Test
    void 초안과_삭제된_매거진의_댓글은_읽거나_작성할_수_없다() {
        jdbcTemplate.update("UPDATE magazines SET status = 'DRAFT' WHERE id = ?", magazineId);
        assertThatThrownBy(() -> create(authorId, null, false)).isInstanceOf(EntityNotFoundException.class);
        assertThatThrownBy(() -> magazineCommentService.findAll(magazineId,
                new MagazineCommentFindRequest(null, 5, "LATEST"), null))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void 매거진에만_달린_링크도_수집하고_캐시_정리에서_보존한다() {
        long commentId = magazineCommentService.create(magazineId, authorId,
                new MagazineCommentCreateRequest("링크 https://example.com/magazine-" + UUID.randomUUID(), null)).id();
        var job = linkPreviewRepository.claimDue(Instant.now().plusSeconds(1), Instant.now().minusSeconds(3600)).orElseThrow();
        linkPreviewRepository.complete(job, new LinkPreviewMetadata("제목", "설명", null, "Example"), Instant.now());
        assertThat(linkPreviewRepository.findByMagazineCommentIds(List.of(commentId)).get(commentId).title())
                .isEqualTo("제목");
        linkPreviewRepository.deleteUnusedBefore(Instant.now().plus(30, ChronoUnit.DAYS));
        assertThat(linkPreviewRepository.findByMagazineCommentIds(List.of(commentId))).containsKey(commentId);
        magazineCommentService.update(magazineId, commentId, authorId, new MagazineCommentUpdateRequest("링크 삭제"));
        assertThat(linkPreviewRepository.findByMagazineCommentIds(List.of(commentId))).isEmpty();
    }

    @Test
    void 답글_알림을_우선하고_수신자_중복과_자기_알림을_제외한다() {
        long root = create(authorId, null, false);
        long reply = create(otherId, root, false);
        var page = notificationService.findAll(authorId, null, 20);
        assertThat(page.items()).hasSize(1);
        assertThat(page.items().getFirst().notificationType().name()).isEqualTo("MAGAZINE_COMMENT_REPLY");
        assertThat(page.items().getFirst().magazineId()).isEqualTo(magazineId);
        assertThat(page.items().getFirst().magazineCommentId()).isEqualTo(reply);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM notifications WHERE recipient_id = ? AND actor_id = ?",
                Long.class, otherId, otherId)).isZero();
    }

    @Test
    void 내_댓글_목록에_매거진을_포함하고_익명은_타인에게_숨긴다() {
        create(authorId, null, false);
        create(authorId, null, true);
        var request = new UserCommentFindRequest(null, 20);
        var mine = userCommentService.findAll(authorId, authorId, request);
        assertThat(mine.comments()).hasSize(2);
        assertThat(mine.comments()).allMatch(c -> c.type() == UserCommentType.MAGAZINE && c.magazineId() == magazineId);
        var other = userCommentService.findAll(authorId, otherId, request);
        assertThat(other.comments()).hasSize(1);
        assertThat(other.totalCount()).isEqualTo(1);
        jdbcTemplate.update("UPDATE magazines SET status = 'DRAFT' WHERE id = ?", magazineId);
        assertThat(userCommentService.findAll(authorId, authorId, request).comments()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"BANNED", "DELETED"})
    void 비활성_사용자는_댓글과_공감을_변경할_수_없다(String userStatus) {
        long root = create(authorId, null, false);
        magazineCommentReactionService.add(magazineId, root, authorId, "AGREE");
        jdbcTemplate.update("""
                UPDATE users SET status = ?,
                    deleted_at = CASE WHEN ? = 'DELETED' THEN now() ELSE NULL END
                WHERE id = ?
                """, userStatus, userStatus, authorId);
        entityManager.clear();

        assertThatThrownBy(() -> create(authorId, null, false)).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> magazineCommentService.update(magazineId, root, authorId,
                new MagazineCommentUpdateRequest("정지 후 수정"))).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> magazineCommentService.delete(magazineId, root, authorId))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> magazineCommentReactionService.add(magazineId, root, authorId, "AGREE"))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> magazineCommentReactionService.remove(magazineId, root, authorId, "AGREE"))
                .isInstanceOf(ForbiddenException.class);
        assertThat(count()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT content FROM magazine_comments WHERE id = ?", String.class, root))
                .isEqualTo("댓글");
    }

    private long create(long userId, Long parentId, boolean anonymous) {
        return magazineCommentService.create(magazineId, userId, new MagazineCommentCreateRequest("댓글", parentId, anonymous)).id();
    }

    private long count() {
        return jdbcTemplate.queryForObject("SELECT comment_count FROM magazines WHERE id = ?", Long.class, magazineId);
    }

    private long user(String role) {
        long id = jdbcTemplate.queryForObject("INSERT INTO users(handle, role) VALUES (?, ?) RETURNING id", Long.class,
                "@i" + UUID.randomUUID().toString().replace("-", "").substring(0, 20), role);
        jdbcTemplate.update("INSERT INTO user_profiles(user_id, display_name) VALUES (?, '사용자')", id);
        return id;
    }
}
