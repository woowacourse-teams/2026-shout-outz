package com.shoutoutz.api.resourcereport.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.resourcereport.application.ResourceReportQueryRepository;
import com.shoutoutz.api.resourcereport.application.dto.MyResourceReport;
import com.shoutoutz.api.resourcereport.application.dto.ReportableResource;
import com.shoutoutz.api.resourcereport.domain.ResourceReportKind;
import com.shoutoutz.api.resourcereport.domain.ResourceReportStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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
class ResourceReportQueryRepositoryIntegrationTest {

    private static final Instant T1 = Instant.parse("2026-11-10T01:00:00Z");
    private static final Instant T2 = Instant.parse("2026-11-10T02:00:00Z");
    private static final Instant T3 = Instant.parse("2026-11-10T03:00:00Z");

    @Autowired
    private ResourceReportQueryRepository resourceReportQueryRepository;

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
    @DisplayName("서비스에 등록된 팀원과 가입해서 맞춰진 이관 팀원을 팀원으로 본다")
    void findsProjectMembers() {
        long archivedUserId = fixtures.user();
        long outsiderId = fixtures.user();
        long otherProjectId = fixtures.project("다른 팀");
        fixtures.member(projectId, userId);
        fixtures.archivedMember(projectId, archivedUserId);
        fixtures.archivedMember(projectId, null);
        fixtures.member(otherProjectId, outsiderId);

        assertThat(resourceReportQueryRepository.isProjectMember(projectId, userId)).isTrue();
        assertThat(resourceReportQueryRepository.isProjectMember(projectId, archivedUserId)).isTrue();
        assertThat(resourceReportQueryRepository.isProjectMember(projectId, outsiderId)).isFalse();
    }

    @Test
    @DisplayName("삭제되지 않은 자료만 찾고, 프로젝트가 없는 자료는 프로젝트를 비워 둔다")
    void findsActiveResources() {
        long prId = fixtures.document("github-pr", "woowacourse-teams/2026-shout-outz#1", projectId, false);
        long talkId = fixtures.document("youtube-tekotalk", "yt:abc", null, false);
        long deletedId = fixtures.document("github-pr", "woowacourse-teams/2026-shout-outz#2", projectId, true);

        Map<Long, ReportableResource> resources = resourceReportQueryRepository.findResources(
                List.of(prId, talkId, deletedId, Long.MAX_VALUE)
        );

        assertThat(resources).containsOnlyKeys(prId, talkId);
        assertThat(resources.get(prId)).isEqualTo(new ReportableResource(
                prId,
                projectId,
                "github-pr",
                "woowacourse-teams/2026-shout-outz#1"
        ));
        assertThat(resources.get(talkId).projectId()).isNull();
    }

    @Test
    @DisplayName("내가 이 프로젝트에 보낸 제보를 최신순으로 읽고, 필요 없어요 제보에는 지금 자료 id 를 붙인다")
    void findsMine() {
        long prId = fixtures.document("github-pr", "woowacourse-teams/2026-shout-outz#1", projectId, false);
        long goneDocumentId = fixtures.document("github-pr", "woowacourse-teams/2026-shout-outz#2", projectId, false);
        long removal = fixtures.removalReport(projectId, userId, "github-pr", "woowacourse-teams/2026-shout-outz#1", "OPEN", T1);
        long removalOfGone = fixtures.removalReport(projectId, userId, "github-pr", "woowacourse-teams/2026-shout-outz#2", "ACCEPTED", T2);
        long addition = fixtures.additionReport(projectId, userId, "https://github.com/woowacourse-teams/repo/pull/3", "OPEN", T3);
        fixtures.deleteDocument(goneDocumentId);
        long otherUserId = fixtures.user();
        fixtures.removalReport(projectId, otherUserId, "github-pr", "woowacourse-teams/2026-shout-outz#1", "OPEN", T3);
        long otherProjectId = fixtures.project("다른 팀");
        fixtures.removalReport(otherProjectId, userId, "github-pr", "woowacourse-teams/2026-other#1", "OPEN", T3);

        List<MyResourceReport> mine = resourceReportQueryRepository.findMine(projectId, userId);

        assertThat(mine).containsExactly(
                new MyResourceReport(addition, ResourceReportKind.ADD, null,
                        "https://github.com/woowacourse-teams/repo/pull/3", ResourceReportStatus.OPEN, T3),
                new MyResourceReport(removalOfGone, ResourceReportKind.REMOVE, null, null,
                        ResourceReportStatus.ACCEPTED, T2),
                new MyResourceReport(removal, ResourceReportKind.REMOVE, prId, null, ResourceReportStatus.OPEN, T1)
        );
    }
}
