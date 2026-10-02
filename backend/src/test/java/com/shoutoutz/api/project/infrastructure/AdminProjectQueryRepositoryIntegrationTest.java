package com.shoutoutz.api.project.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.cohort.domain.Cohort;
import com.shoutoutz.api.project.application.AdminProjectQueryRepository;
import com.shoutoutz.api.project.application.dto.AdminProjectPage;
import com.shoutoutz.api.project.domain.ApprovalStatus;
import com.shoutoutz.api.project.domain.GithubRepositoryUrl;
import com.shoutoutz.api.project.domain.Project;
import com.shoutoutz.api.project.domain.ProjectApprovalHistory;
import com.shoutoutz.api.project.domain.ProjectApprovalHistoryRepository;
import com.shoutoutz.api.project.domain.ProjectRepository;
import com.shoutoutz.api.project.domain.Slug;
import com.shoutoutz.api.project.domain.ServiceStatus;
import com.shoutoutz.api.project.domain.TeamName;
import com.shoutoutz.api.project.domain.Title;
import com.shoutoutz.api.user.domain.account.User;
import com.shoutoutz.api.user.domain.account.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class AdminProjectQueryRepositoryIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-09-20T00:00:00Z");

    @Autowired
    private AdminProjectQueryRepository queryRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectApprovalHistoryRepository historyRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void listsProjectsByStatusAndCursor() {
        long ownerId = userRepository.save(User.initialize(uniqueHandle())).getId();
        Project first = projectRepository.save(project(ownerId, ApprovalStatus.PENDING), List.of(), List.of());
        Project second = projectRepository.save(project(ownerId, ApprovalStatus.PENDING), List.of(), List.of());

        AdminProjectPage firstPage = queryRepository.findAll(ApprovalStatus.PENDING, null, 1);
        AdminProjectPage secondPage = queryRepository.findAll(
                ApprovalStatus.PENDING,
                firstPage.items().getLast().toCursor(),
                1
        );

        assertThat(firstPage.items()).hasSize(1);
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(firstPage.items().getFirst().approvalStatus()).isEqualTo(ApprovalStatus.PENDING);
        assertThat(secondPage.items()).hasSize(1);
        assertThat(secondPage.items().getFirst().projectId())
                .isNotEqualTo(firstPage.items().getFirst().projectId());
        assertThat(List.of(first.getId(), second.getId()))
                .contains(firstPage.items().getFirst().projectId(), secondPage.items().getFirst().projectId());
    }

    @Test
    void returnsLatestRejectionReasonForRejectedProject() {
        long ownerId = userRepository.save(User.initialize(uniqueHandle())).getId();
        Project rejected = projectRepository.save(
                project(ownerId, ApprovalStatus.REJECTED),
                List.of(),
                List.of()
        );
        historyRepository.save(ProjectApprovalHistory.initial(rejected.getId(), NOW.minusSeconds(1)));
        historyRepository.save(ProjectApprovalHistory.decision(
                rejected.getId(),
                ownerId,
                ApprovalStatus.REJECTED,
                "설명을 보완해주세요.",
                NOW
        ));

        AdminProjectPage page = queryRepository.findAll(ApprovalStatus.REJECTED, null, 20);

        assertThat(page.items()).anySatisfy(item -> {
            assertThat(item.projectId()).isEqualTo(rejected.getId());
            assertThat(item.rejectReason()).isEqualTo("설명을 보완해주세요.");
        });
    }

    private static Project project(long ownerId, ApprovalStatus status) {
        String name = "2026-admin-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return Project.builder()
                .cohort(Cohort.COHORT_8)
                .registeredBy(ownerId)
                .teamName(new TeamName("심사팀"))
                .slug(new Slug(name))
                .title(new Title("심사 프로젝트"))
                .tagline("심사 목록 테스트")
                .serviceStatus(ServiceStatus.CLOSED)
                .approvalStatus(status)
                .githubRepositoryUrl(new GithubRepositoryUrl(
                        "https://github.com/woowacourse-teams/" + name
                ))
                .build();
    }

    private static String uniqueHandle() {
        return "@admin-project-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }
}
