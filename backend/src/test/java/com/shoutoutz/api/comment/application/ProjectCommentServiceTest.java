package com.shoutoutz.api.comment.application;

import static com.shoutoutz.api.comment.domain.CommentErrorCode.COMMENT_DEPTH_EXCEEDED;
import static com.shoutoutz.api.comment.domain.CommentErrorCode.COMMENT_NOT_FOUND;
import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.comment.domain.ProjectComment;
import com.shoutoutz.api.comment.domain.ProjectCommentRepository;
import com.shoutoutz.api.comment.presentation.dto.request.ProjectCommentCreateRequest;
import com.shoutoutz.api.comment.presentation.dto.response.ProjectCommentCreateResponse;
import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.project.domain.ProjectRepository;
import com.shoutoutz.api.user.domain.profile.UserProfile;
import com.shoutoutz.api.user.domain.profile.UserProfileRepository;
import com.shoutoutz.api.user.domain.profile.UserType;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectCommentServiceTest {

    private static final long PROJECT_ID = 100L;
    private static final long AUTHOR_ID = 7L;
    private static final long PARENT_ID = 301L;
    private static final Instant NOW = Instant.parse("2026-09-14T00:00:00Z");

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectCommentRepository projectCommentRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    private ProjectCommentService projectCommentService;

    @BeforeEach
    void setUp() {
        projectCommentService = new ProjectCommentService(
                projectRepository,
                projectCommentRepository,
                userProfileRepository
        );
    }

    @Test
    @DisplayName("공개 프로젝트에 루트 댓글을 저장하고 작성자 정보를 포함한 응답을 반환한다.")
    void createsRootComment() {
        givenPublicProject();
        givenAuthor();
        when(projectCommentRepository.save(any(ProjectComment.class)))
                .thenReturn(savedComment(null));

        ProjectCommentCreateResponse result = projectCommentService.create(
                PROJECT_ID,
                AUTHOR_ID,
                new ProjectCommentCreateRequest("  좋은 프로젝트네요.  ", null)
        );

        assertThat(result.id()).isEqualTo(501L);
        assertThat(result.content()).isEqualTo("좋은 프로젝트네요.");
        assertThat(result.author().userId()).isEqualTo(AUTHOR_ID);
        assertThat(result.author().displayName()).isEqualTo("샤라웃 운영팀");
        assertThat(result.author().avatarImageId()).isEqualTo(10L);
        assertThat(result.parentId()).isNull();
        assertThat(result.createdAt()).isEqualTo(NOW);
        assertThat(result.updatedAt()).isEqualTo(NOW);
        assertThat(result.editable()).isTrue();

        ArgumentCaptor<ProjectComment> captor = ArgumentCaptor.forClass(ProjectComment.class);
        verify(projectCommentRepository).save(captor.capture());
        assertThat(captor.getValue().getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(captor.getValue().getAuthorId()).isEqualTo(AUTHOR_ID);
        assertThat(captor.getValue().getContent()).isEqualTo("좋은 프로젝트네요.");
        assertThat(captor.getValue().getParentId()).isNull();
    }

    @Test
    @DisplayName("같은 프로젝트의 삭제되지 않은 루트 댓글을 부모로 대댓글을 저장한다.")
    void createsReplyToRootComment() {
        givenPublicProject();
        givenAuthor();
        when(projectCommentRepository.findById(PARENT_ID)).thenReturn(Optional.of(rootComment()));
        when(projectCommentRepository.save(any(ProjectComment.class)))
                .thenReturn(savedComment(PARENT_ID));

        ProjectCommentCreateResponse result = projectCommentService.create(
                PROJECT_ID,
                AUTHOR_ID,
                new ProjectCommentCreateRequest("답글입니다.", PARENT_ID)
        );

        assertThat(result.parentId()).isEqualTo(PARENT_ID);
        ArgumentCaptor<ProjectComment> captor = ArgumentCaptor.forClass(ProjectComment.class);
        verify(projectCommentRepository).save(captor.capture());
        assertThat(captor.getValue().getParentId()).isEqualTo(PARENT_ID);
    }

    @Test
    @DisplayName("승인되지 않았거나 삭제된 프로젝트에는 댓글을 작성하지 않고 404를 던진다.")
    void rejectsNonPublicProject() {
        when(projectRepository.existsPublicById(PROJECT_ID)).thenReturn(false);

        assertThatThrownBy(() -> projectCommentService.create(
                PROJECT_ID,
                AUTHOR_ID,
                new ProjectCommentCreateRequest("댓글", null)
        )).isInstanceOfSatisfying(EntityNotFoundException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(PROJECT_NOT_FOUND));

        verifyNoInteractions(projectCommentRepository, userProfileRepository);
    }

    @Test
    @DisplayName("존재하지 않거나 다른 프로젝트의 부모 댓글이면 404를 던진다.")
    void rejectsMissingOrDifferentProjectParent() {
        givenPublicProject();
        when(projectCommentRepository.findById(PARENT_ID)).thenReturn(Optional.of(
                ProjectComment.reconstitute(
                        PARENT_ID,
                        PROJECT_ID + 1,
                        9L,
                        null,
                        "다른 프로젝트 댓글",
                        NOW,
                        NOW,
                        null
                )
        ));

        assertThatThrownBy(() -> projectCommentService.create(
                PROJECT_ID,
                AUTHOR_ID,
                new ProjectCommentCreateRequest("답글", PARENT_ID)
        )).isInstanceOfSatisfying(EntityNotFoundException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(COMMENT_NOT_FOUND));

        verify(projectCommentRepository, never()).save(any());
        verifyNoInteractions(userProfileRepository);
    }

    @Test
    @DisplayName("삭제된 부모 댓글이면 404를 던져 새 대댓글을 막는다.")
    void rejectsDeletedParent() {
        givenPublicProject();
        when(projectCommentRepository.findById(PARENT_ID)).thenReturn(Optional.of(
                ProjectComment.reconstitute(
                        PARENT_ID,
                        PROJECT_ID,
                        9L,
                        null,
                        "삭제된 댓글",
                        NOW,
                        NOW,
                        NOW
                )
        ));

        assertThatThrownBy(() -> projectCommentService.create(
                PROJECT_ID,
                AUTHOR_ID,
                new ProjectCommentCreateRequest("답글", PARENT_ID)
        )).isInstanceOfSatisfying(EntityNotFoundException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(COMMENT_NOT_FOUND));

        verify(projectCommentRepository, never()).save(any());
        verifyNoInteractions(userProfileRepository);
    }

    @Test
    @DisplayName("이미 대댓글인 댓글을 부모로 지정하면 400을 던진다.")
    void rejectsReplyAsParent() {
        givenPublicProject();
        when(projectCommentRepository.findById(PARENT_ID)).thenReturn(Optional.of(
                ProjectComment.reconstitute(
                        PARENT_ID,
                        PROJECT_ID,
                        9L,
                        300L,
                        "이미 대댓글인 댓글",
                        NOW,
                        NOW,
                        null
                )
        ));

        assertThatThrownBy(() -> projectCommentService.create(
                PROJECT_ID,
                AUTHOR_ID,
                new ProjectCommentCreateRequest("답글", PARENT_ID)
        )).isInstanceOfSatisfying(BadRequestException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(COMMENT_DEPTH_EXCEEDED));

        verify(projectCommentRepository, never()).save(any());
        verifyNoInteractions(userProfileRepository);
    }

    private void givenPublicProject() {
        when(projectRepository.existsPublicById(PROJECT_ID)).thenReturn(true);
    }

    private void givenAuthor() {
        when(userProfileRepository.findByUserId(AUTHOR_ID)).thenReturn(Optional.of(
                UserProfile.builder()
                        .userId(AUTHOR_ID)
                        .displayName("샤라웃 운영팀")
                        .userType(UserType.GENERAL)
                        .avatarImageId(10L)
                        .build()
        ));
    }

    private ProjectComment rootComment() {
        return ProjectComment.reconstitute(
                PARENT_ID,
                PROJECT_ID,
                9L,
                null,
                "부모 댓글",
                NOW,
                NOW,
                null
        );
    }

    private ProjectComment savedComment(Long parentId) {
        return ProjectComment.reconstitute(
                501L,
                PROJECT_ID,
                AUTHOR_ID,
                parentId,
                parentId == null ? "좋은 프로젝트네요." : "답글입니다.",
                NOW,
                NOW,
                null
        );
    }
}
