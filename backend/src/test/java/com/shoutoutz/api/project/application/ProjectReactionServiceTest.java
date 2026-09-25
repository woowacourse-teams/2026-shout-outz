package com.shoutoutz.api.project.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.cohort.domain.Cohort;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.InvalidInputException;
import com.shoutoutz.api.project.domain.ApprovalStatus;
import com.shoutoutz.api.project.domain.GithubRepositoryUrl;
import com.shoutoutz.api.project.domain.Project;
import com.shoutoutz.api.project.domain.ProjectErrorCode;
import com.shoutoutz.api.project.domain.ProjectReactionCounts;
import com.shoutoutz.api.project.domain.ProjectReactionRepository;
import com.shoutoutz.api.project.domain.ProjectReactionType;
import com.shoutoutz.api.project.domain.ProjectRepository;
import com.shoutoutz.api.project.domain.ServiceStatus;
import com.shoutoutz.api.project.domain.Slug;
import com.shoutoutz.api.project.domain.TeamName;
import com.shoutoutz.api.project.domain.Title;
import com.shoutoutz.api.project.presentation.dto.response.ProjectReactionResponse;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectReactionServiceTest {

    private static final long PROJECT_ID = 100L;
    private static final long USER_ID = 1L;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectReactionRepository projectReactionRepository;

    private ProjectReactionService projectReactionService;

    @BeforeEach
    void setUp() {
        projectReactionService = new ProjectReactionService(projectRepository, projectReactionRepository);
    }

    @Test
    void 승인된_프로젝트에_좋아요를_멱등하게_추가한다() {
        givenProject(ApprovalStatus.APPROVED);
        givenCounts(84L, 28L);

        ProjectReactionResponse response = projectReactionService.add(PROJECT_ID, USER_ID, "LIKE");

        assertThat(response).isEqualTo(
                new ProjectReactionResponse(PROJECT_ID, ProjectReactionType.LIKE, true, 84L, 28L)
        );
        verify(projectReactionRepository).add(PROJECT_ID, USER_ID, ProjectReactionType.LIKE);
    }

    @Test
    void 승인_대기_프로젝트의_기존_북마크를_제거한다() {
        givenProject(ApprovalStatus.PENDING);
        givenCounts(83L, 27L);

        ProjectReactionResponse response = projectReactionService.remove(
                PROJECT_ID,
                USER_ID,
                "BOOKMARK"
        );

        assertThat(response).isEqualTo(
                new ProjectReactionResponse(PROJECT_ID, ProjectReactionType.BOOKMARK, false, 83L, 27L)
        );
        verify(projectReactionRepository).remove(PROJECT_ID, USER_ID, ProjectReactionType.BOOKMARK);
    }

    @Test
    void 승인되지_않은_프로젝트에는_새로운_반응을_추가할_수_없다() {
        givenProject(ApprovalStatus.PENDING);

        assertThatThrownBy(() -> projectReactionService.add(PROJECT_ID, USER_ID, "LIKE"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", ProjectErrorCode.PROJECT_NOT_FOUND);

        verifyNoInteractions(projectReactionRepository);
    }

    @Test
    void 존재하지_않거나_삭제된_프로젝트는_반응할_수_없다() {
        assertThatThrownBy(() -> projectReactionService.remove(PROJECT_ID, USER_ID, "LIKE"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", ProjectErrorCode.PROJECT_NOT_FOUND);

        verifyNoInteractions(projectReactionRepository);
    }

    @Test
    void 지원하지_않는_반응_타입은_400_오류로_처리한다() {
        assertThatThrownBy(() -> projectReactionService.add(PROJECT_ID, USER_ID, "AGREE"))
                .isInstanceOf(InvalidInputException.class)
                .hasFieldOrPropertyWithValue("errorCode", ProjectErrorCode.REACTION_TYPE_INVALID);

        verify(projectRepository, never()).findActiveById(PROJECT_ID);
        verifyNoInteractions(projectReactionRepository);
    }

    private void givenProject(ApprovalStatus approvalStatus) {
        when(projectRepository.findActiveById(PROJECT_ID))
                .thenReturn(Optional.of(Project.builder()
                        .id(PROJECT_ID)
                        .cohort(Cohort.COHORT_6)
                        .registeredBy(99L)
                        .teamName(new TeamName("루프팀"))
                        .slug(new Slug("loop"))
                        .title(new Title("루프"))
                        .tagline("한 줄 소개")
                        .serviceStatus(ServiceStatus.CLOSED)
                        .approvalStatus(approvalStatus)
                        .descriptionMd("설명")
                        .githubRepositoryUrl(new GithubRepositoryUrl(
                                "https://github.com/woowacourse-teams/2026-loop"
                        ))
                        .deploymentUrl(null)
                        .build()));
    }

    private void givenCounts(long likeCount, long bookmarkCount) {
        when(projectReactionRepository.countByProjectId(PROJECT_ID))
                .thenReturn(new ProjectReactionCounts(likeCount, bookmarkCount));
    }
}
