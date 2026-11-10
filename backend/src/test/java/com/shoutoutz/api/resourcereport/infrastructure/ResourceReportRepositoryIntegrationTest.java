package com.shoutoutz.api.resourcereport.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.resourcereport.domain.ResourceReport;
import com.shoutoutz.api.resourcereport.domain.ResourceReportKind;
import com.shoutoutz.api.resourcereport.domain.ResourceReportRepository;
import com.shoutoutz.api.resourcereport.domain.ResourceReportStatus;
import com.shoutoutz.api.resourcereport.domain.ResourceReportUrl;
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
class ResourceReportRepositoryIntegrationTest {

    private static final Instant CREATED_AT = Instant.parse("2026-11-10T02:00:00Z");
    private static final Instant HANDLED_AT = Instant.parse("2026-11-11T01:00:00Z");
    private static final String PR_KEY = "woowacourse-teams/2026-shout-outz#512";

    @Autowired
    private ResourceReportRepository resourceReportRepository;

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
    @DisplayName("같은 사람이 같은 자료를 다시 제보하면 새로 만들지 않고 처음 제보의 id 를 돌려준다")
    void keepsFirstRemovalReport() {
        long firstId = resourceReportRepository.saveIfAbsent(removal(userId, "처음 이유"));
        long secondId = resourceReportRepository.saveIfAbsent(removal(userId, "다른 이유"));

        assertThat(secondId).isEqualTo(firstId);
        assertThat(fixtures.countReports(projectId)).isEqualTo(1);
        assertThat(resourceReportRepository.findById(firstId).orElseThrow().getMemo()).isEqualTo("처음 이유");
    }

    @Test
    @DisplayName("다른 사람이 같은 자료를 제보하면 따로 남긴다")
    void keepsRemovalReportsPerUser() {
        long otherUserId = fixtures.user();

        long firstId = resourceReportRepository.saveIfAbsent(removal(userId, null));
        long otherId = resourceReportRepository.saveIfAbsent(removal(otherUserId, null));

        assertThat(otherId).isNotEqualTo(firstId);
        assertThat(fixtures.countReports(projectId)).isEqualTo(2);
    }

    @Test
    @DisplayName("같은 사람이 같은 주소를 다시 알려 주면 처음 제보의 id 를 돌려준다")
    void keepsFirstAdditionReport() {
        long firstId = resourceReportRepository.saveIfAbsent(addition("/pull/123"));
        long secondId = resourceReportRepository.saveIfAbsent(addition("/pull/123"));
        long otherUrlId = resourceReportRepository.saveIfAbsent(addition("/pull/124"));

        assertThat(secondId).isEqualTo(firstId);
        assertThat(otherUrlId).isNotEqualTo(firstId);
        assertThat(fixtures.countReports(projectId)).isEqualTo(2);
    }

    @Test
    @DisplayName("저장한 제보를 그대로 읽는다")
    void findsSavedReport() {
        long reportId = resourceReportRepository.saveIfAbsent(removal(userId, "설정 변경뿐이에요"));

        ResourceReport found = resourceReportRepository.findById(reportId).orElseThrow();

        assertThat(found.getId()).isEqualTo(reportId);
        assertThat(found.getProjectId()).isEqualTo(projectId);
        assertThat(found.getUserId()).isEqualTo(userId);
        assertThat(found.getKind()).isEqualTo(ResourceReportKind.REMOVE);
        assertThat(found.getResourceSource()).isEqualTo("github-pr");
        assertThat(found.getResourceExternalKey()).isEqualTo(PR_KEY);
        assertThat(found.getUrl()).isNull();
        assertThat(found.getMemo()).isEqualTo("설정 변경뿐이에요");
        assertThat(found.getStatus()).isEqualTo(ResourceReportStatus.OPEN);
        assertThat(found.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(found.getHandledAt()).isNull();
        assertThat(found.getHandledByUserId()).isNull();
    }

    @Test
    @DisplayName("처리 상태와 처리한 관리자, 처리 시각을 저장한다")
    void updatesStatus() {
        long adminId = fixtures.user();
        long reportId = resourceReportRepository.saveIfAbsent(removal(userId, null));
        ResourceReport report = resourceReportRepository.findById(reportId).orElseThrow();

        resourceReportRepository.updateStatus(report.changeStatus(ResourceReportStatus.REJECTED, adminId, HANDLED_AT));

        ResourceReport updated = resourceReportRepository.findById(reportId).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ResourceReportStatus.REJECTED);
        assertThat(updated.getHandledByUserId()).isEqualTo(adminId);
        assertThat(updated.getHandledAt()).isEqualTo(HANDLED_AT);
        assertThat(updated.getUpdatedAt()).isEqualTo(HANDLED_AT);
    }

    @Test
    @DisplayName("없는 제보는 빈 값이다")
    void findsNothingForMissingReport() {
        assertThat(resourceReportRepository.findById(Long.MAX_VALUE)).isEmpty();
    }

    private ResourceReport removal(long reporterId, String memo) {
        return ResourceReport.removal(projectId, reporterId, "github-pr", PR_KEY, memo, CREATED_AT);
    }

    private ResourceReport addition(String path) {
        String repositoryUrl = "https://github.com/woowacourse-teams/" + fixtures.slugOf(projectId);
        return ResourceReport.addition(
                projectId,
                userId,
                ResourceReportUrl.of(repositoryUrl + path, repositoryUrl),
                null,
                CREATED_AT
        );
    }
}
