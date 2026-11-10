package com.shoutoutz.api.resourcereport.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.resourcereport.application.ResourceReportAdminQueryRepository;
import com.shoutoutz.api.resourcereport.application.dto.ResourceReportAdminCursor;
import com.shoutoutz.api.resourcereport.application.dto.ResourceReportAdminItem;
import com.shoutoutz.api.resourcereport.application.dto.ResourceReportAdminPage;
import com.shoutoutz.api.resourcereport.domain.ResourceReportKind;
import com.shoutoutz.api.resourcereport.domain.ResourceReportStatus;
import java.time.Instant;
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
class ResourceReportAdminQueryRepositoryIntegrationTest {

    private static final Instant T1 = Instant.parse("2026-11-10T01:00:00Z");
    private static final Instant T2 = Instant.parse("2026-11-10T02:00:00Z");
    private static final Instant T3 = Instant.parse("2026-11-10T03:00:00Z");

    @Autowired
    private ResourceReportAdminQueryRepository resourceReportAdminQueryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ResourceReportFixtures fixtures;
    private long projectId;
    private long userId;

    @BeforeEach
    void setUp() {
        fixtures = new ResourceReportFixtures(jdbcTemplate);
        projectId = fixtures.project("샤라웃");
        userId = fixtures.user();
    }

    @Test
    @DisplayName("상태와 종류로 거르고, 최신순으로 프로젝트와 자료 정보를 함께 읽는다")
    void findsWithFilters() {
        long prId = fixtures.document("github-pr", "woowacourse-teams/2026-shout-outz#1", projectId, false);
        long open = fixtures.removalReport(projectId, userId, "github-pr", "woowacourse-teams/2026-shout-outz#1", "OPEN", T1);
        long openAddition = fixtures.additionReport(projectId, userId, "https://github.com/woowacourse-teams/repo/pull/3", "OPEN", T2);
        long rejected = fixtures.removalReport(projectId, fixtures.user(), "github-pr", "woowacourse-teams/2026-shout-outz#1", "REJECTED", T3);

        ResourceReportAdminPage openPage = resourceReportAdminQueryRepository.findAll(ResourceReportStatus.OPEN, null, null, 20);
        ResourceReportAdminPage removalPage = resourceReportAdminQueryRepository.findAll(null, ResourceReportKind.REMOVE, null, 20);

        assertThat(openPage.items()).extracting(ResourceReportAdminItem::reportId).containsExactly(openAddition, open);
        assertThat(openPage.totalCount()).isEqualTo(2);
        assertThat(removalPage.items()).extracting(ResourceReportAdminItem::reportId).containsExactly(rejected, open);
        assertThat(removalPage.totalCount()).isEqualTo(2);

        ResourceReportAdminItem removalItem = openPage.items().getLast();
        assertThat(removalItem.projectSlug()).isEqualTo(fixtures.slugOf(projectId));
        assertThat(removalItem.projectTitle()).isEqualTo("샤라웃");
        assertThat(removalItem.resourceId()).isEqualTo(prId);
        assertThat(removalItem.resourceSource()).isEqualTo("github-pr");
        assertThat(removalItem.resourceTitle()).isEqualTo("제목 woowacourse-teams/2026-shout-outz#1");
        assertThat(removalItem.resourceUrl()).isEqualTo("https://example.com/woowacourse-teams/2026-shout-outz#1");
        assertThat(removalItem.url()).isNull();
        assertThat(removalItem.memo()).isEqualTo("필요 없어요");
        assertThat(removalItem.reporterUserId()).isEqualTo(userId);

        ResourceReportAdminItem additionItem = openPage.items().getFirst();
        assertThat(additionItem.resourceId()).isNull();
        assertThat(additionItem.url()).isEqualTo("https://github.com/woowacourse-teams/repo/pull/3");
    }

    @Test
    @DisplayName("필요 없어요 제보의 자료가 내려갔으면 자료 정보를 비운다")
    void leavesGoneResourceEmpty() {
        long documentId = fixtures.document("github-pr", "woowacourse-teams/2026-shout-outz#1", projectId, false);
        long report = fixtures.removalReport(projectId, userId, "github-pr", "woowacourse-teams/2026-shout-outz#1", "OPEN", T1);
        fixtures.deleteDocument(documentId);

        ResourceReportAdminItem item = resourceReportAdminQueryRepository.findAll(null, null, null, 20).items().getFirst();

        assertThat(item.reportId()).isEqualTo(report);
        assertThat(item.resourceId()).isNull();
        assertThat(item.resourceTitle()).isNull();
    }

    @Test
    @DisplayName("커서 다음 제보부터 읽고, 전체 수는 커서와 상관없이 센다")
    void findsByCursor() {
        long first = fixtures.removalReport(projectId, userId, "github-pr", "key-1", "OPEN", T1);
        long second = fixtures.removalReport(projectId, userId, "github-pr", "key-2", "OPEN", T2);
        long third = fixtures.removalReport(projectId, userId, "github-pr", "key-3", "OPEN", T3);

        ResourceReportAdminPage firstPage = resourceReportAdminQueryRepository.findAll(null, null, null, 2);
        ResourceReportAdminItem last = firstPage.items().getLast();
        ResourceReportAdminPage secondPage = resourceReportAdminQueryRepository.findAll(
                null,
                null,
                new ResourceReportAdminCursor(last.createdAt(), last.reportId()),
                2
        );

        assertThat(firstPage.items()).extracting(ResourceReportAdminItem::reportId).containsExactly(third, second);
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(firstPage.totalCount()).isEqualTo(3);
        assertThat(secondPage.items()).extracting(ResourceReportAdminItem::reportId).containsExactly(first);
        assertThat(secondPage.hasNext()).isFalse();
        assertThat(secondPage.totalCount()).isEqualTo(3);
    }
}
