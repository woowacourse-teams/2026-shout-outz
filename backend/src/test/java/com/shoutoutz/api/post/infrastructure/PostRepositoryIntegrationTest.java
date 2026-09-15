package com.shoutoutz.api.post.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.category.domain.CategoryType;
import com.shoutoutz.api.post.application.PostQueryRepository;
import com.shoutoutz.api.post.application.dto.PostCursor;
import com.shoutoutz.api.post.application.dto.PostItem;
import com.shoutoutz.api.post.application.dto.PostMediaReference;
import com.shoutoutz.api.post.application.dto.PostSort;
import com.shoutoutz.api.post.domain.Post;
import com.shoutoutz.api.post.domain.PostRepository;
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
class PostRepositoryIntegrationTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostQueryRepository postQueryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 포스트_검증에_필요한_미디어_데이터를_조회한다() {
        long authorId = insertUser("WOOWACOURSE_CREW", "FRONTEND", (short) 8);
        long otherId = insertUser("WOOWACOURSE_CREW", "BACKEND", (short) 8);
        long readyMediaId = insertMedia(authorId, "POST_CONTENT", "READY");
        long pendingMediaId = insertMedia(authorId, "POST_CONTENT", "PENDING_UPLOAD");
        long otherMediaId = insertMedia(otherId, "POST_CONTENT", "READY");
        long avatarMediaId = insertMedia(authorId, "USER_AVATAR", "READY");

        List<PostMediaReference> media = postQueryRepository.findAllMediaByIds(List.of(
                readyMediaId,
                pendingMediaId,
                otherMediaId,
                avatarMediaId
        ));
        assertThat(media).extracting(PostMediaReference::mediaId)
                .containsExactlyInAnyOrder(
                        readyMediaId,
                        pendingMediaId,
                        otherMediaId,
                        avatarMediaId
                );
    }

    @Test
    void 최신순_슬라이스를_카테고리와_커서로_조회하고_삭제된_포스트는_제외한다() {
        long authorId = insertUser("WOOWACOURSE_CREW", "BACKEND", (short) 8);
        long categoryId = insertCategory(true);
        long mediaId = insertMedia(authorId, "POST_CONTENT", "READY");
        Instant base = Instant.parse("2026-09-11T00:00:00Z");

        Post oldest = savePost(authorId, "첫 번째", base.minus(2, ChronoUnit.HOURS), categoryId, mediaId);
        Post middle = savePost(authorId, "두 번째", base.minus(1, ChronoUnit.HOURS), categoryId);
        Post latest = savePost(authorId, "세 번째", base, categoryId);
        Post deleted = savePost(authorId, "삭제", base.plus(1, ChronoUnit.HOURS), categoryId);
        postRepository.update(deleted.delete(base.plus(2, ChronoUnit.HOURS)));

        List<PostItem> firstSlice = postQueryRepository.findAll(
                PostSort.LATEST,
                null,
                null,
                2
        );
        List<PostItem> secondSlice = postQueryRepository.findAll(
                PostSort.LATEST,
                null,
                new PostCursor(
                        PostSort.LATEST,
                        0L,
                        firstSlice.get(1).createdAt(),
                        firstSlice.get(1).postId()
                ),
                2
        );
        assertThat(firstSlice).extracting(PostItem::postId)
                .containsExactly(latest.getId(), middle.getId());
        assertThat(secondSlice).extracting(PostItem::postId).containsExactly(oldest.getId());
        assertThat(postQueryRepository.findAll(PostSort.LATEST, categoryId, null, 10))
                .extracting(PostItem::postId)
                .containsExactly(latest.getId(), middle.getId(), oldest.getId());
        assertThat(postQueryRepository.findById(deleted.getId())).isEmpty();

        PostItem detail = postQueryRepository.findById(oldest.getId()).orElseThrow();
        assertThat(detail.categories()).extracting(PostItem.Category::categoryId)
                .containsExactly(categoryId);
        assertThat(detail.categories()).extracting(PostItem.Category::type)
                .containsExactly(CategoryType.GENERAL);
        assertThat(detail.media()).extracting(PostItem.Media::mediaId).containsExactly(mediaId);
    }

    @Test
    void 전체_좋아요_수로_인기순_슬라이스를_조회한다() {
        long authorId = insertUser("WOOWACOURSE_CREW", "BACKEND", (short) 8);
        long firstUserId = insertUser("GENERAL", null, null);
        long secondUserId = insertUser("GENERAL", null, null);
        long categoryId = insertCategory(true);
        Instant base = Instant.parse("2026-09-11T00:00:00Z");
        Post noLike = savePost(authorId, "좋아요 없음", base, categoryId);
        Post olderPopular = savePost(authorId, "먼저 작성한 인기 글", base.plus(1, ChronoUnit.HOURS), categoryId);
        Post latestPopular = savePost(authorId, "나중에 작성한 인기 글", base.plus(2, ChronoUnit.HOURS), categoryId);
        insertLike(olderPopular.getId(), firstUserId);
        insertLike(olderPopular.getId(), secondUserId);
        insertLike(latestPopular.getId(), firstUserId);
        insertLike(latestPopular.getId(), secondUserId);

        List<PostItem> firstSlice = postQueryRepository.findAll(
                PostSort.POPULAR,
                null,
                null,
                1
        );
        PostItem firstItem = firstSlice.getFirst();
        List<PostItem> secondSlice = postQueryRepository.findAll(
                PostSort.POPULAR,
                null,
                new PostCursor(
                        PostSort.POPULAR,
                        firstItem.likeCount(),
                        firstItem.createdAt(),
                        firstItem.postId()
                ),
                10
        );

        assertThat(firstSlice).extracting(PostItem::postId)
                .containsExactly(latestPopular.getId());
        assertThat(firstItem.likeCount()).isEqualTo(2L);
        assertThat(secondSlice).extracting(PostItem::postId)
                .containsExactly(olderPopular.getId(), noLike.getId());
    }

    private Post savePost(long authorId, String content, Instant createdAt, long categoryId, long... mediaIds) {
        Post post = postRepository.save(Post.create(authorId, content, createdAt));
        postRepository.saveCategories(post.getId(), List.of(categoryId));
        postRepository.saveMedia(post.getId(), java.util.Arrays.stream(mediaIds).boxed().toList());
        return post;
    }

    private long insertUser(String userType, String track, Short cohort) {
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Long userId = jdbcTemplate.queryForObject(
                "INSERT INTO users (handle) VALUES (?) RETURNING id",
                Long.class,
                "post_" + token
        );
        jdbcTemplate.update(
                "INSERT INTO user_profiles (user_id, display_name, user_type, track, cohort) VALUES (?, ?, ?, ?, ?)",
                userId,
                "포스트 테스트",
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
                "post-" + token,
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
                "media/post-test/" + token,
                status
        );
    }

    private void insertLike(long postId, long userId) {
        jdbcTemplate.update(
                """
                        INSERT INTO post_reactions (post_id, user_id, reaction_type, created_at)
                        VALUES (?, ?, 'LIKE', '2020-01-01T00:00:00Z')
                        """,
                postId,
                userId
        );
    }
}
