package com.shoutoutz.api.resourcesearch.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.resourcesearch.application.ResourceDetailQueryRepository;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceDetail;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceExperience;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceTeam;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceTopic;
import com.shoutoutz.api.resourcesearch.support.ResourceDocumentFixture;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
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
class ResourceDetailQueryRepositoryIntegrationTest {

    @Autowired
    private ResourceDetailQueryRepository resourceDetailQueryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ResourceDocumentFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new ResourceDocumentFixture(jdbcTemplate);
    }

    @Test
    @DisplayName("자료의 카드 정보와 종류, 기술 주제, 배울 점을 준다")
    void findsDetail() {
        long projectId = fixture.approvedProject("zzol", "쫄");
        long id = fixture.document("github-pr")
                .projectId(projectId)
                .contentType("problem_solving")
                .topic("concurrency")
                .keyPoints("락 범위를 줄이는 방법", "재시도 흐름")
                .save();

        ResourceDetail detail = resourceDetailQueryRepository.findById(id).orElseThrow();

        assertThat(detail.card().resourceId()).isEqualTo(id);
        assertThat(detail.card().team()).isEqualTo(new ResourceTeam("쫄", "zzol", null));
        assertThat(detail.contentType()).isEqualTo("problem_solving");
        assertThat(detail.topic()).isEqualTo(new ResourceTopic("concurrency", "동시성 제어/락"));
        assertThat(detail.keyPoints()).containsExactly("락 범위를 줄이는 방법", "재시도 흐름");
        assertThat(detail.experiences()).isEmpty();
    }

    @Test
    @DisplayName("경험은 순서대로 주고 삭제된 경험은 뺀다")
    void findsExperiencesInOrderExcludingDeleted() {
        long id = fixture.document("blog-post").save();
        insertExperience(id, 1, "두 번째 문제", null, "두 번째 해결", "결과", false, "Redis");
        insertExperience(id, 0, "첫 번째 문제", "원인", "첫 번째 해결", null, false, "MySQL", "InnoDB");
        insertExperience(id, 2, "지운 문제", null, "지운 해결", null, true);

        ResourceDetail detail = resourceDetailQueryRepository.findById(id).orElseThrow();

        assertThat(detail.experiences()).containsExactly(
                new ResourceExperience("첫 번째 문제", "원인", "첫 번째 해결", null, List.of("MySQL", "InnoDB")),
                new ResourceExperience("두 번째 문제", null, "두 번째 해결", "결과", List.of("Redis")));
    }

    @Test
    @DisplayName("주제가 없는 자료는 주제를 null로 둔다")
    void leavesTopicEmptyWhenMissing() {
        long id = fixture.document("youtube-tekotalk").save();

        ResourceDetail detail = resourceDetailQueryRepository.findById(id).orElseThrow();

        assertThat(detail.topic()).isNull();
        assertThat(detail.contentType()).isNull();
        assertThat(detail.keyPoints()).isEmpty();
    }

    @Test
    @DisplayName("삭제된 자료나 없는 자료는 찾지 못한다")
    void returnsEmptyForDeletedOrMissing() {
        long deleted = fixture.document("blog-post").deleted().save();

        assertThat(resourceDetailQueryRepository.findById(deleted)).isEmpty();
        assertThat(resourceDetailQueryRepository.findById(999_999_999L)).isEmpty();
    }

    private void insertExperience(long documentId, int index, String problem, String cause, String solution,
            String result, boolean deleted, String... tech) {
        jdbcTemplate.update("""
                INSERT INTO resource_experiences (
                    document_id, experience_index, problem, cause, solution, result, tech, source, deleted_at
                ) SELECT id, ?, ?, ?, ?, ?, ?, source, ? FROM resource_documents WHERE id = ?
                """, index, problem, cause, solution, result, tech,
                deleted ? Timestamp.from(Instant.now()) : null, documentId);
    }
}
