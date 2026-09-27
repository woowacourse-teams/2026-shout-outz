package com.shoutoutz.api.comment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.cohort.domain.Cohort;
import com.shoutoutz.api.comment.domain.CommentErrorCode;
import com.shoutoutz.api.comment.domain.ProjectComment;
import com.shoutoutz.api.comment.domain.ProjectCommentReactionCounts;
import com.shoutoutz.api.comment.domain.ProjectCommentReactionRepository;
import com.shoutoutz.api.comment.domain.ProjectCommentReactionType;
import com.shoutoutz.api.comment.domain.ProjectCommentRepository;
import com.shoutoutz.api.comment.presentation.dto.response.ProjectCommentReactionResponse;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.InvalidInputException;
import com.shoutoutz.api.project.domain.ApprovalStatus;
import com.shoutoutz.api.project.domain.GithubRepositoryUrl;
import com.shoutoutz.api.project.domain.Project;
import com.shoutoutz.api.project.domain.ProjectErrorCode;
import com.shoutoutz.api.project.domain.ProjectRepository;
import com.shoutoutz.api.project.domain.ServiceStatus;
import com.shoutoutz.api.project.domain.Slug;
import com.shoutoutz.api.project.domain.TeamName;
import com.shoutoutz.api.project.domain.Title;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectCommentReactionServiceTest {

    private static final long PROJECT_ID = 100L;
    private static final long COMMENT_ID = 501L;
    private static final long USER_ID = 1L;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectCommentRepository projectCommentRepository;

    @Mock
    private ProjectCommentReactionRepository projectCommentReactionRepository;

    private ProjectCommentReactionService projectCommentReactionService;

    @BeforeEach
    void setUp() {
        projectCommentReactionService = new ProjectCommentReactionService(
                projectRepository,
                projectCommentRepository,
                projectCommentReactionRepository
        );
    }

    @Test
    void 승인된_프로젝트의_댓글에_공감을_멱등하게_추가한다() {
        givenProject(ApprovalStatus.APPROVED);
        givenComment(PROJECT_ID, false);
        givenCounts(7L);

        ProjectCommentReactionResponse response = projectCommentReactionService.add(
                PROJECT_ID,
                COMMENT_ID,
                USER_ID,
                "AGREE"
        );

        assertThat(response).isEqualTo(new ProjectCommentReactionResponse(
                PROJECT_ID,
                COMMENT_ID,
                ProjectCommentReactionType.AGREE,
                true,
                7L
        ));
        verify(projectCommentReactionRepository)
                .add(COMMENT_ID, USER_ID, ProjectCommentReactionType.AGREE);
    }

    @Test
    void 승인_대기_프로젝트에는_새로운_공감을_추가할_수_없다() {
        givenProject(ApprovalStatus.PENDING);

        assertThatThrownBy(() -> projectCommentReactionService.add(
                PROJECT_ID,
                COMMENT_ID,
                USER_ID,
                "AGREE"
        ))
                .isInstanceOf(EntityNotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", ProjectErrorCode.PROJECT_NOT_FOUND);

        verifyNoInteractions(projectCommentRepository, projectCommentReactionRepository);
    }

    @Test
    void 승인_대기_프로젝트에서도_기존_댓글_공감을_제거한다() {
        givenProject(ApprovalStatus.PENDING);
        givenComment(PROJECT_ID, false);
        givenCounts(6L);

        ProjectCommentReactionResponse response = projectCommentReactionService.remove(
                PROJECT_ID,
                COMMENT_ID,
                USER_ID,
                "AGREE"
        );

        assertThat(response).isEqualTo(new ProjectCommentReactionResponse(
                PROJECT_ID,
                COMMENT_ID,
                ProjectCommentReactionType.AGREE,
                false,
                6L
        ));
        verify(projectCommentReactionRepository)
                .remove(COMMENT_ID, USER_ID, ProjectCommentReactionType.AGREE);
    }

    @Test
    void 다른_프로젝트에_속한_댓글에는_반응할_수_없다() {
        givenProject(ApprovalStatus.APPROVED);
        givenComment(PROJECT_ID + 1, false);

        assertThatThrownBy(() -> projectCommentReactionService.add(
                PROJECT_ID,
                COMMENT_ID,
                USER_ID,
                "AGREE"
        ))
                .isInstanceOf(EntityNotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", CommentErrorCode.COMMENT_NOT_FOUND);

        verifyNoInteractions(projectCommentReactionRepository);
    }

    @Test
    void 삭제된_댓글에는_반응할_수_없다() {
        givenProject(ApprovalStatus.APPROVED);
        givenComment(PROJECT_ID, true);

        assertThatThrownBy(() -> projectCommentReactionService.remove(
                PROJECT_ID,
                COMMENT_ID,
                USER_ID,
                "AGREE"
        ))
                .isInstanceOf(EntityNotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", CommentErrorCode.COMMENT_NOT_FOUND);

        verifyNoInteractions(projectCommentReactionRepository);
    }

    @Test
    void 지원하지_않는_반응_타입은_400_오류로_처리한다() {
        assertThatThrownBy(() -> projectCommentReactionService.add(
                PROJECT_ID,
                COMMENT_ID,
                USER_ID,
                "LIKE"
        ))
                .isInstanceOf(InvalidInputException.class)
                .hasFieldOrPropertyWithValue("errorCode", CommentErrorCode.REACTION_TYPE_INVALID);

        verify(projectRepository, never()).findActiveById(PROJECT_ID);
        verifyNoInteractions(projectCommentRepository, projectCommentReactionRepository);
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

    private void givenComment(long projectId, boolean deleted) {
        Instant createdAt = Instant.parse("2026-09-14T00:00:00Z");
        when(projectCommentRepository.findById(COMMENT_ID))
                .thenReturn(Optional.of(ProjectComment.reconstitute(
                        COMMENT_ID,
                        projectId,
                        2L,
                        null,
                        "공감할 댓글",
                        createdAt,
                        createdAt,
                        deleted ? createdAt : null
                )));
    }

    private void givenCounts(long agreeCount) {
        when(projectCommentReactionRepository.countByCommentId(COMMENT_ID))
                .thenReturn(new ProjectCommentReactionCounts(agreeCount));
    }
}
