package com.shoutoutz.api.resourcesearch.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.resourcesearch.application.ResourceCardQueryRepository;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceCard;
import com.shoutoutz.api.resourcesearch.domain.ResourceSection;
import com.shoutoutz.api.resourcesearch.domain.ResourceTrack;
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
class ProjectResourceCardQueryIntegrationTest {

    private static final Instant BASE_TIME = Instant.parse("2026-10-20T01:00:00Z");

    @Autowired
    private ResourceCardQueryRepository resourceCardQueryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ResourceDocumentFixture fixture;
    private long rilog;

    @BeforeEach
    void setUp() {
        fixture = new ResourceDocumentFixture(jdbcTemplate);
        rilog = fixture.approvedProject("rilog", "Rilog.");
    }

    @Test
    @DisplayName("그 프로젝트의 해당 섹션 자료만 최신순으로 주고, 발행 시각이 없는 자료는 뒤에 둔다")
    void findsSectionResourcesOfProjectInLatestOrder() {
        long other = fixture.approvedProject("harustudy", "하루스터디");
        long old = fixture.document("blog-post").projectId(rilog).publishedAt(BASE_TIME.minusSeconds(60)).save();
        long undated = fixture.document("blog-post").projectId(rilog).publishedAt(null).save();
        long latest = fixture.document("blog-post").projectId(rilog).publishedAt(BASE_TIME).save();
        fixture.document("github-pr").projectId(rilog).save();
        fixture.document("blog-post").projectId(other).title("Logback 로깅").save();
        fixture.document("blog-post").title("팀 없는 로깅 글").save();

        List<ResourceCard> cards = resourceCardQueryRepository.findLatestByProjects(
                List.of(rilog), ResourceSection.BLOG, null);

        assertThat(cards).extracting(ResourceCard::resourceId).containsExactly(latest, old, undated);
        assertThat(cards).extracting(card -> card.team().projectSlug()).containsOnly("rilog");
    }

    @Test
    @DisplayName("이름이 같은 프로젝트가 여럿이면 그 프로젝트들의 자료를 함께 준다")
    void findsResourcesOfEveryProject() {
        long another = fixture.approvedProject("rilog-2025", "리로그");
        long first = fixture.document("github-pr").projectId(rilog).publishedAt(BASE_TIME).save();
        long second = fixture.document("github-teamdoc").projectId(another).publishedAt(BASE_TIME.minusSeconds(60)).save();

        List<ResourceCard> cards = resourceCardQueryRepository.findLatestByProjects(
                List.of(rilog, another), ResourceSection.DOCUMENT, null);

        assertThat(cards).extracting(ResourceCard::resourceId).containsExactly(first, second);
    }

    @Test
    @DisplayName("섹션 상한까지만 준다")
    void limitsToSectionLimit() {
        for (int index = 0; index <= ResourceSection.BLOG.limit(); index++) {
            fixture.document("blog-post").projectId(rilog).publishedAt(BASE_TIME.plusSeconds(index)).save();
        }

        List<ResourceCard> cards = resourceCardQueryRepository.findLatestByProjects(
                List.of(rilog), ResourceSection.BLOG, null);

        assertThat(cards).hasSize(ResourceSection.BLOG.limit());
    }

    @Test
    @DisplayName("삭제된 자료와 관리자가 받아들인 필요 없어요 제보가 있는 자료는 뺀다")
    void skipsDeletedAndRemovedResources() {
        long alive = fixture.document("blog-post").projectId(rilog).save();
        long reportedOnly = fixture.document("blog-post").projectId(rilog).save();
        long removed = fixture.document("blog-post").projectId(rilog).save();
        fixture.document("blog-post").projectId(rilog).deleted().save();
        fixture.removalReport(reportedOnly, "OPEN");
        fixture.removalReport(removed, "ACCEPTED");

        List<ResourceCard> cards = resourceCardQueryRepository.findLatestByProjects(
                List.of(rilog), ResourceSection.BLOG, null);

        assertThat(cards).extracting(ResourceCard::resourceId).containsExactlyInAnyOrder(alive, reportedOnly);
    }

    @Test
    @DisplayName("승인 전이거나 삭제된 프로젝트의 자료는 주지 않는다")
    void skipsResourcesOfHiddenProjects() {
        long pending = fixture.pendingProject("pending-team", "대기 팀");
        long deleted = fixture.deletedProject("deleted-team", "삭제 팀");
        fixture.document("blog-post").projectId(pending).save();
        fixture.document("blog-post").projectId(deleted).save();

        List<ResourceCard> cards = resourceCardQueryRepository.findLatestByProjects(
                List.of(pending, deleted), ResourceSection.BLOG, null);

        assertThat(cards).isEmpty();
    }

    @Test
    @DisplayName("트랙을 주면 그 트랙과 공통, 트랙 미분류 자료만 준다")
    void filtersByTrack() {
        long backend = fixture.document("github-pr").projectId(rilog).track("BE").save();
        long common = fixture.document("github-pr").projectId(rilog).track("ALL").save();
        long unclassified = fixture.document("github-pr").projectId(rilog).save();
        fixture.document("github-pr").projectId(rilog).track("FE").save();

        List<ResourceCard> cards = resourceCardQueryRepository.findLatestByProjects(
                List.of(rilog), ResourceSection.DOCUMENT, ResourceTrack.BE);

        assertThat(cards).extracting(ResourceCard::resourceId)
                .containsExactlyInAnyOrder(backend, common, unclassified);
    }

    @Test
    @DisplayName("프로젝트가 없으면 빈 목록을 준다")
    void returnsEmptyWithoutProjects() {
        fixture.document("blog-post").projectId(rilog).save();

        assertThat(resourceCardQueryRepository.findLatestByProjects(List.of(), ResourceSection.BLOG, null)).isEmpty();
    }
}
