package com.shoutoutz.api.resourcesearch.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.resourcereport.application.ResourceReportAdminService;
import com.shoutoutz.api.resourcereport.application.ResourceReportStatusChanged;
import com.shoutoutz.api.resourcereport.domain.ResourceReportKind;
import com.shoutoutz.api.resourcereport.domain.ResourceReportStatus;
import com.shoutoutz.api.resourcereport.presentation.dto.request.ResourceReportStatusUpdateRequest;
import com.shoutoutz.api.resourcesearch.presentation.dto.request.ResourceTopicPopularRequest;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.PopularTopicResponse;
import com.shoutoutz.api.resourcesearch.support.ResourceDocumentFixture;
import com.shoutoutz.api.user.domain.account.UserRole;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * 캐시 비우기는 관리자 상태 변경 트랜잭션이 커밋된 뒤에 일어나므로, 이 테스트는 테스트 트랜잭션으로 감싸지 않고 실제로 커밋한다.
 * 다른 테스트와 DB를 함께 쓰므로 넣은 행은 끝날 때 지운다.
 */
@ActiveProfiles("test")
@SpringBootTest
class PopularTopicsCacheEvictionIntegrationTest {

    private static final String TOPIC = "android";
    private static final ResourceTopicPopularRequest ALL_TOPICS = new ResourceTopicPopularRequest(20);

    @Autowired
    private ResourceTopicQueryService resourceTopicQueryService;

    @Autowired
    private ResourceReportAdminService resourceReportAdminService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long documentId;
    private long reportId;
    private long reporterId;
    private long projectId;
    private long adminId;

    @BeforeEach
    void setUp() {
        ResourceDocumentFixture fixture = new ResourceDocumentFixture(jdbcTemplate);
        documentId = fixture.document("blog-post").topic(TOPIC).save();
        fixture.removalReport(documentId, "OPEN");
        Map<String, Object> report = jdbcTemplate.queryForMap("""
                SELECT report.id, report.user_id, report.project_id
                FROM resource_reports report
                JOIN resource_documents document
                  ON document.source = report.resource_source AND document.external_key = report.resource_external_key
                WHERE document.id = ?
                """, documentId);
        reportId = ((Number) report.get("id")).longValue();
        reporterId = ((Number) report.get("user_id")).longValue();
        projectId = ((Number) report.get("project_id")).longValue();
        adminId = jdbcTemplate.queryForObject("INSERT INTO users (handle, role) VALUES (?, 'ADMIN') RETURNING id",
                Long.class, "@admin-" + UUID.randomUUID().toString().substring(0, 12));
        resourceTopicQueryService.evictPopularTopics(new ResourceReportStatusChanged(ResourceReportKind.REMOVE));
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM resource_reports WHERE id = ?", reportId);
        jdbcTemplate.update("DELETE FROM resource_documents WHERE id = ?", documentId);
        jdbcTemplate.update("DELETE FROM projects WHERE id = ?", projectId);
        jdbcTemplate.update("DELETE FROM users WHERE id IN (?, ?)", reporterId, adminId);
        resourceTopicQueryService.evictPopularTopics(new ResourceReportStatusChanged(ResourceReportKind.REMOVE));
    }

    @Test
    @DisplayName("관리자가 필요 없어요 제보를 승인해 커밋되면 많이 다룬 주제 캐시가 비어 새 집계를 준다")
    void evictsPopularTopicsAfterApprovalCommits() {
        long before = countOf(TOPIC);

        resourceReportAdminService.updateStatus(reportId, adminId, UserRole.ADMIN,
                new ResourceReportStatusUpdateRequest(ResourceReportStatus.ACCEPTED));

        assertThat(countOf(TOPIC)).isEqualTo(before - 1);
    }

    private long countOf(String topicId) {
        return resourceTopicQueryService.findPopular(ALL_TOPICS).stream()
                .filter(topic -> topic.topicId().equals(topicId))
                .mapToLong(PopularTopicResponse::resourceCount)
                .findFirst()
                .orElse(0);
    }
}
