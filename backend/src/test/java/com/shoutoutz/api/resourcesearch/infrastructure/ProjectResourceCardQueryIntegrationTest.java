package com.shoutoutz.api.resourcesearch.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.resourcesearch.application.ResourceCardQueryRepository;
import com.shoutoutz.api.resourcesearch.application.dto.ProjectResourceCursor;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceCard;
import com.shoutoutz.api.resourcesearch.domain.ResourceSection;
import com.shoutoutz.api.resourcesearch.domain.ResourceTrack;
import com.shoutoutz.api.resourcesearch.support.ResourceDocumentFixture;
import java.time.Instant;
import java.util.ArrayList;
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
                List.of(rilog), ResourceSection.BLOG, null, null, ResourceSection.BLOG.limit());

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
                List.of(rilog, another), ResourceSection.DOCUMENT, null, null, ResourceSection.DOCUMENT.limit());

        assertThat(cards).extracting(ResourceCard::resourceId).containsExactly(first, second);
    }

    @Test
    @DisplayName("섹션 상한까지만 준다")
    void limitsToSectionLimit() {
        for (int index = 0; index <= ResourceSection.BLOG.limit(); index++) {
            fixture.document("blog-post").projectId(rilog).publishedAt(BASE_TIME.plusSeconds(index)).save();
        }

        List<ResourceCard> cards = resourceCardQueryRepository.findLatestByProjects(
                List.of(rilog), ResourceSection.BLOG, null, null, ResourceSection.BLOG.limit());

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
                List.of(rilog), ResourceSection.BLOG, null, null, ResourceSection.BLOG.limit());

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
                List.of(pending, deleted), ResourceSection.BLOG, null, null, ResourceSection.BLOG.limit());

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
                List.of(rilog), ResourceSection.DOCUMENT, ResourceTrack.BE, null, ResourceSection.DOCUMENT.limit());

        assertThat(cards).extracting(ResourceCard::resourceId)
                .containsExactlyInAnyOrder(backend, common, unclassified);
    }

    @Test
    @DisplayName("요청한 개수만큼만 준다")
    void limitsToRequestedSize() {
        fixture.document("blog-post").projectId(rilog).publishedAt(BASE_TIME).save();
        long latest = fixture.document("blog-post").projectId(rilog).publishedAt(BASE_TIME.plusSeconds(60)).save();

        List<ResourceCard> cards = resourceCardQueryRepository.findLatestByProjects(
                List.of(rilog), ResourceSection.BLOG, null, null, 1);

        assertThat(cards).extracting(ResourceCard::resourceId).containsExactly(latest);
    }

    @Test
    @DisplayName("커서를 주면 그 자료 다음부터 이어서 주고, 발행 시각이 같은 자료와 없는 자료도 빠뜨리거나 겹치지 않는다")
    void continuesAfterCursor() {
        long undatedLow = fixture.document("github-pr").projectId(rilog).publishedAt(null).save();
        long undatedHigh = fixture.document("github-pr").projectId(rilog).publishedAt(null).save();
        long old = fixture.document("github-pr").projectId(rilog).publishedAt(BASE_TIME.minusSeconds(60)).save();
        long sameTimeLow = fixture.document("github-pr").projectId(rilog).publishedAt(BASE_TIME).save();
        long sameTimeHigh = fixture.document("github-teamdoc").projectId(rilog).publishedAt(BASE_TIME).save();
        List<Long> expectedOrder = List.of(sameTimeHigh, sameTimeLow, old, undatedHigh, undatedLow);

        List<Long> readByOne = new ArrayList<>();
        ProjectResourceCursor cursor = null;
        for (int page = 0; page < expectedOrder.size(); page++) {
            ResourceCard card = resourceCardQueryRepository.findLatestByProjects(
                    List.of(rilog), ResourceSection.DOCUMENT, null, cursor, 1).getFirst();
            readByOne.add(card.resourceId());
            cursor = ProjectResourceCursor.after(card);
        }

        assertThat(readByOne).isEqualTo(expectedOrder);
        assertThat(resourceCardQueryRepository.findLatestByProjects(
                List.of(rilog), ResourceSection.DOCUMENT, null, cursor, 1)).isEmpty();
    }

    @Test
    @DisplayName("조회와 같은 조건으로 프로젝트의 섹션 자료 수를 센다")
    void countsSectionResourcesOfProjects() {
        long other = fixture.approvedProject("harustudy", "하루스터디");
        fixture.document("github-pr").projectId(rilog).track("BE").save();
        fixture.document("github-teamdoc").projectId(rilog).save();
        fixture.document("github-pr").projectId(rilog).track("FE").save();
        fixture.document("github-pr").projectId(rilog).deleted().save();
        fixture.document("blog-post").projectId(rilog).save();
        fixture.document("github-pr").projectId(other).save();
        fixture.removalReport(fixture.document("github-pr").projectId(rilog).save(), "ACCEPTED");

        assertThat(resourceCardQueryRepository.countByProjects(List.of(rilog), ResourceSection.DOCUMENT, null))
                .isEqualTo(3);
        assertThat(resourceCardQueryRepository.countByProjects(List.of(rilog), ResourceSection.DOCUMENT, ResourceTrack.BE))
                .isEqualTo(2);
        assertThat(resourceCardQueryRepository.countByProjects(List.of(), ResourceSection.DOCUMENT, null)).isZero();
    }

    @Test
    @DisplayName("프로젝트가 없으면 빈 목록을 준다")
    void returnsEmptyWithoutProjects() {
        fixture.document("blog-post").projectId(rilog).save();

        assertThat(resourceCardQueryRepository.findLatestByProjects(
                List.of(), ResourceSection.BLOG, null, null, ResourceSection.BLOG.limit())).isEmpty();
    }
}
