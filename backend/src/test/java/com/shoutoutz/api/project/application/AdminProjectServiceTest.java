package com.shoutoutz.api.project.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.shoutoutz.api.cohort.domain.Cohort;
import com.shoutoutz.api.auth.domain.OAuthAccountRepository;
import com.shoutoutz.api.common.exception.custom.ConflictException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.media.application.MediaUrlResolver;
import com.shoutoutz.api.project.domain.ApprovalStatus;
import com.shoutoutz.api.project.domain.Project;
import com.shoutoutz.api.project.domain.ProjectApprovalHistory;
import com.shoutoutz.api.project.domain.ProjectApprovalHistoryRepository;
import com.shoutoutz.api.project.domain.ProjectErrorCode;
import com.shoutoutz.api.project.domain.ProjectRepository;
import com.shoutoutz.api.project.domain.Slug;
import com.shoutoutz.api.project.domain.ServiceStatus;
import com.shoutoutz.api.project.domain.TeamName;
import com.shoutoutz.api.project.domain.Title;
import com.shoutoutz.api.project.infrastructure.jdbc.ProjectTechTagAndMemberJdbcRepository;
import com.shoutoutz.api.project.presentation.dto.response.AdminProjectApproveResponse;
import com.shoutoutz.api.project.presentation.dto.response.AdminProjectHistoryResponse;
import com.shoutoutz.api.project.presentation.dto.response.AdminProjectRejectResponse;
import com.shoutoutz.api.user.domain.account.User;
import com.shoutoutz.api.user.domain.account.UserRepository;
import com.shoutoutz.api.user.domain.account.UserRole;
import com.shoutoutz.api.user.domain.account.UserStatus;
import com.shoutoutz.api.user.application.UserAvatarUrlResolver;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminProjectServiceTest {

    private static final long PROJECT_ID = 100L;
    private static final long ADMIN_ID = 7L;
    private static final Instant NOW = Instant.parse("2026-09-20T00:00:00Z");

    @Mock
    private AdminProjectQueryRepository queryRepository;

    @Mock
    private AdminProjectCursorCodec cursorCodec;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectApprovalHistoryRepository historyRepository;

    @Mock
    private ProjectTechTagAndMemberJdbcRepository techTagAndMemberJdbcRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MediaUrlResolver mediaUrlResolver;

    @Mock
    private OAuthAccountRepository oauthAccountRepository;

    private AdminProjectService service;

    @BeforeEach
    void setUp() {
        service = new AdminProjectService(
                queryRepository,
                cursorCodec,
                projectRepository,
                historyRepository,
                techTagAndMemberJdbcRepository,
                userRepository,
                mediaUrlResolver,
                new UserAvatarUrlResolver(mediaUrlResolver, oauthAccountRepository),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void approvesOnlyPendingProjectAndSavesDecisionHistory() {
        given(projectRepository.findActiveById(PROJECT_ID)).willReturn(Optional.of(project(ApprovalStatus.PENDING)));
        given(userRepository.findById(ADMIN_ID)).willReturn(Optional.of(admin()));
        given(projectRepository.transitionApprovalStatus(
                PROJECT_ID,
                ApprovalStatus.PENDING,
                ApprovalStatus.APPROVED
        )).willReturn(true);

        AdminProjectApproveResponse response = service.approve(PROJECT_ID, ADMIN_ID, UserRole.ADMIN);

        assertThat(response.projectId()).isEqualTo(PROJECT_ID);
        assertThat(response.approvalStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(response.decidedBy().handle()).isEqualTo("@admin");
        assertThat(response.decidedAt()).isEqualTo(NOW);
        ArgumentCaptor<ProjectApprovalHistory> captor = ArgumentCaptor.forClass(ProjectApprovalHistory.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getFromStatus()).isEqualTo(ApprovalStatus.PENDING);
        assertThat(captor.getValue().getToStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(captor.getValue().getReason()).isNull();
    }

    @Test
    void rejectsPendingProjectWithTrimmedReason() {
        given(projectRepository.findActiveById(PROJECT_ID)).willReturn(Optional.of(project(ApprovalStatus.PENDING)));
        given(userRepository.findById(ADMIN_ID)).willReturn(Optional.of(admin()));
        given(projectRepository.transitionApprovalStatus(
                PROJECT_ID,
                ApprovalStatus.PENDING,
                ApprovalStatus.REJECTED
        )).willReturn(true);

        AdminProjectRejectResponse response = service.reject(
                PROJECT_ID,
                ADMIN_ID,
                UserRole.ADMIN,
                "  설명을 보완해주세요.  "
        );

        assertThat(response.approvalStatus()).isEqualTo(ApprovalStatus.REJECTED);
        assertThat(response.reason()).isEqualTo("설명을 보완해주세요.");
        ArgumentCaptor<ProjectApprovalHistory> captor = ArgumentCaptor.forClass(ProjectApprovalHistory.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getReason()).isEqualTo("설명을 보완해주세요.");
        assertThat(captor.getValue().getToStatus()).isEqualTo(ApprovalStatus.REJECTED);
    }

    @Test
    void rejectsNonPendingProject() {
        given(projectRepository.findActiveById(PROJECT_ID)).willReturn(Optional.of(project(ApprovalStatus.APPROVED)));

        assertThatThrownBy(() -> service.approve(PROJECT_ID, ADMIN_ID, UserRole.ADMIN))
                .isInstanceOfSatisfying(ConflictException.class,
                        error -> assertThat(error.getErrorCode())
                                .isEqualTo(ProjectErrorCode.PROJECT_APPROVAL_NOT_PENDING));

        verifyNoInteractions(userRepository, historyRepository);
    }

    @Test
    void rejectsNonAdminBeforeReadingProject() {
        assertThatThrownBy(() -> service.approve(PROJECT_ID, ADMIN_ID, UserRole.USER))
                .isInstanceOfSatisfying(ForbiddenException.class,
                        error -> assertThat(error.getErrorCode())
                                .isEqualTo(ProjectErrorCode.PROJECT_APPROVAL_ADMIN_FORBIDDEN));

        verifyNoInteractions(projectRepository, userRepository, historyRepository);
    }

    @Test
    void returnsHistoryInRepositoryOrderAndResolvesActors() {
        given(projectRepository.findActiveById(PROJECT_ID)).willReturn(Optional.of(project(ApprovalStatus.PENDING)));
        given(historyRepository.findAllByProjectId(PROJECT_ID)).willReturn(List.of(
                ProjectApprovalHistory.reconstitute(
                        2L, PROJECT_ID, ADMIN_ID, ApprovalStatus.PENDING, ApprovalStatus.REJECTED,
                        "사유", NOW
                ),
                ProjectApprovalHistory.reconstitute(
                        1L, PROJECT_ID, null, null, ApprovalStatus.PENDING, null, NOW.minusSeconds(1)
                )
        ));
        given(userRepository.findById(ADMIN_ID)).willReturn(Optional.of(admin()));

        AdminProjectHistoryResponse response = service.findHistory(PROJECT_ID, UserRole.ADMIN);

        assertThat(response.items()).hasSize(2);
        assertThat(response.items().getFirst().toStatus()).isEqualTo(ApprovalStatus.REJECTED);
        assertThat(response.items().getFirst().changedBy().handle()).isEqualTo("@admin");
        assertThat(response.items().getLast().changedBy()).isNull();
    }

    private static Project project(ApprovalStatus status) {
        return Project.builder()
                .id(PROJECT_ID)
                .cohort(Cohort.COHORT_6)
                .registeredBy(ADMIN_ID)
                .teamName(new TeamName("루프팀"))
                .slug(new Slug("loop"))
                .title(new Title("루프"))
                .tagline("한 줄 소개")
                .serviceStatus(ServiceStatus.CLOSED)
                .approvalStatus(status)
                .githubRepositoryUrl(new com.shoutoutz.api.project.domain.GithubRepositoryUrl(
                        "https://github.com/woowacourse-teams/2026-loop"
                ))
                .build();
    }

    private static User admin() {
        return User.builder()
                .id(ADMIN_ID)
                .handle("@admin")
                .status(UserStatus.ACTIVE)
                .role(UserRole.ADMIN)
                .build();
    }
}
