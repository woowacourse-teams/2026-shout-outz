package com.shoutoutz.api.resourcesearch.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.resourcesearch.application.ResourceCardQueryRepository;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceCard;
import com.shoutoutz.api.resourcesearch.support.ResourceDocumentFixture;
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
class ResourceCardQueryRepositoryIntegrationTest {

    @Autowired
    private ResourceCardQueryRepository resourceCardQueryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ResourceDocumentFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new ResourceDocumentFixture(jdbcTemplate);
    }

    @Test
    @DisplayName("요청한 자료 ID 순서 그대로 카드를 준다")
    void keepsRequestedOrder() {
        long first = fixture.document("blog-post").save();
        long second = fixture.document("github-pr").save();
        long third = fixture.document("youtube-tekotalk").save();

        List<ResourceCard> cards = resourceCardQueryRepository.findByIds(List.of(third, first, second));

        assertThat(cards).extracting(ResourceCard::resourceId).containsExactly(third, first, second);
    }

    @Test
    @DisplayName("자료의 기본 정보와 블로그 플랫폼을 담는다")
    void mapsDocumentFields() {
        Instant publishedAt = Instant.parse("2025-10-06T03:00:00Z");
        long id = fixture.document("blog-post")
                .title("[Database] MySQL(InnoDB) 락(Lock) (2)")
                .summary("락의 잠금 범위를 살펴봐요.")
                .publishedAt(publishedAt)
                .extra("{\"platform\": \"tistory\"}")
                .save();

        ResourceCard card = resourceCardQueryRepository.findByIds(List.of(id)).getFirst();

        assertThat(card.source()).isEqualTo("blog-post");
        assertThat(card.title()).isEqualTo("[Database] MySQL(InnoDB) 락(Lock) (2)");
        assertThat(card.url()).startsWith("https://example.com/");
        assertThat(card.summary()).isEqualTo("락의 잠금 범위를 살펴봐요.");
        assertThat(card.publishedAt()).isEqualTo(publishedAt);
        assertThat(card.platform()).isEqualTo("tistory");
    }

    @Test
    @DisplayName("승인된 프로젝트의 자료는 팀 이름과 slug와 로고 미디어 ID를 담는다")
    void mapsRegisteredTeam() {
        long projectId = fixture.approvedProjectWithLogo("festabook", "페스타북");
        long id = fixture.document("blog-post").projectId(projectId).save();

        ResourceCard.Team team = resourceCardQueryRepository.findByIds(List.of(id)).getFirst().team();

        assertThat(team).isEqualTo(new ResourceCard.Team("페스타북", "festabook", fixture.logoMediaIdOf(projectId)));
    }

    @Test
    @DisplayName("프로젝트 썸네일은 팀 로고로 쓰지 않는다. 로고가 없으면 로고 미디어 ID는 null 이다")
    void doesNotUseThumbnailAsLogo() {
        long projectId = fixture.approvedProjectWithLogo("festabook", "페스타북");
        jdbcTemplate.update(
                "UPDATE projects SET thumbnail_media_id = logo_media_id, logo_media_id = NULL WHERE id = ?",
                projectId
        );
        long id = fixture.document("blog-post").projectId(projectId).save();

        ResourceCard.Team team = resourceCardQueryRepository.findByIds(List.of(id)).getFirst().team();

        assertThat(team).isEqualTo(new ResourceCard.Team("페스타북", "festabook", null));
    }

    @Test
    @DisplayName("프로젝트 등록 전 팀의 블로그는 수집 때 저장한 팀 이름만 담는다")
    void mapsUnregisteredBlogTeamFromExtra() {
        long id = fixture.document("blog-post").extra("{\"project_name\": \"모모\"}").save();

        ResourceCard.Team team = resourceCardQueryRepository.findByIds(List.of(id)).getFirst().team();

        assertThat(team).isEqualTo(new ResourceCard.Team("모모", null, null));
    }

    @Test
    @DisplayName("승인되지 않았거나 삭제된 프로젝트의 자료는 수집 때 저장한 팀 이름도 보여주지 않는다")
    void hidesTeamOfHiddenProjects() {
        long pending = fixture.pendingProject("pending-team", "대기 팀");
        long deleted = fixture.deletedProject("deleted-team", "삭제 팀");
        long pendingDoc = fixture.document("blog-post").projectId(pending).extra("{\"project_name\": \"대기 팀\"}").save();
        long deletedDoc = fixture.document("github-pr").projectId(deleted).extra("{\"project_name\": \"삭제 팀\"}").save();

        List<ResourceCard> cards = resourceCardQueryRepository.findByIds(List.of(pendingDoc, deletedDoc));

        assertThat(cards.get(0).team()).isNull();
        assertThat(cards.get(1).team()).isNull();
    }

    @Test
    @DisplayName("팀이 없는 테코톡은 팀을 null로 둔다")
    void leavesTeamEmptyForTalk() {
        long id = fixture.document("youtube-tekotalk").extra("{\"speaker\": \"벨로\"}").save();

        ResourceCard card = resourceCardQueryRepository.findByIds(List.of(id)).getFirst();

        assertThat(card.team()).isNull();
        assertThat(card.platform()).isNull();
    }

    @Test
    @DisplayName("삭제된 자료와 없는 자료는 빼고 준다")
    void skipsDeletedAndMissingDocuments() {
        long alive = fixture.document("blog-post").save();
        long deleted = fixture.document("blog-post").deleted().save();

        List<ResourceCard> cards = resourceCardQueryRepository.findByIds(List.of(deleted, alive, 999_999_999L));

        assertThat(cards).extracting(ResourceCard::resourceId).containsExactly(alive);
    }

    @Test
    @DisplayName("빈 ID 목록이면 빈 목록을 준다")
    void returnsEmptyForEmptyIds() {
        assertThat(resourceCardQueryRepository.findByIds(List.of())).isEmpty();
    }
}
