package com.shoutoutz.api.comment.application;

import static com.shoutoutz.api.comment.domain.CommentErrorCode.COMMENT_DEPTH_EXCEEDED;
import static com.shoutoutz.api.comment.domain.CommentErrorCode.COMMENT_NOT_FOUND;
import static com.shoutoutz.api.comment.domain.CommentErrorCode.MISMATCHED_COMMENT_SORT_AND_CURSOR_SORT;
import static com.shoutoutz.api.common.exception.code.CommonErrorCode.FORBIDDEN;
import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.comment.application.dto.ProjectCommentCursor;
import com.shoutoutz.api.comment.application.dto.ProjectCommentPage;
import com.shoutoutz.api.comment.domain.ProjectComment;
import com.shoutoutz.api.comment.domain.ProjectCommentRepository;
import com.shoutoutz.api.comment.domain.ProjectCommentSort;
import com.shoutoutz.api.comment.presentation.dto.request.ProjectCommentCreateRequest;
import com.shoutoutz.api.comment.presentation.dto.request.ProjectCommentFindRequest;
import com.shoutoutz.api.comment.presentation.dto.request.ProjectCommentUpdateRequest;
import com.shoutoutz.api.comment.presentation.dto.response.ProjectCommentCreateResponse;
import com.shoutoutz.api.comment.presentation.dto.response.ProjectCommentDeleteResponse;
import com.shoutoutz.api.comment.presentation.dto.response.ProjectCommentFindResponse;
import com.shoutoutz.api.comment.presentation.dto.response.ProjectCommentFindResponse.Comment;
import com.shoutoutz.api.comment.presentation.dto.response.ProjectCommentUpdateResponse;
import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.common.exception.custom.InvalidInputException;
import com.shoutoutz.api.project.domain.ProjectRepository;
import com.shoutoutz.api.user.domain.profile.UserProfile;
import com.shoutoutz.api.user.domain.profile.UserProfileRepository;
import com.shoutoutz.api.user.domain.profile.UserType;
import java.time.Instant;
import java.util.List;
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
    private static final long COMMENT_ID = 501L;
    private static final Instant NOW = Instant.parse("2026-09-14T00:00:00Z");
    private static final Instant EDITED_AT = Instant.parse("2026-09-14T00:30:00Z");

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectCommentRepository projectCommentRepository;

    @Mock
    private ProjectCommentQueryRepository projectCommentQueryRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    private ProjectCommentService projectCommentService;

    @BeforeEach
    void setUp() {
        projectCommentService = new ProjectCommentService(
                projectRepository,
                projectCommentRepository,
                projectCommentQueryRepository,
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
    @DisplayName("비로그인 사용자는 댓글 목록을 조회하고 삭제된 댓글의 원문은 받지 않는다.")
    void findsCommentsForAnonymousUser() {
        givenPublicProject();
        ProjectComment root = comment(COMMENT_ID, AUTHOR_ID, "루트 댓글", NOW, NOW, null);
        ProjectComment reply = ProjectComment.reconstitute(
                502L,
                PROJECT_ID,
                AUTHOR_ID + 1,
                COMMENT_ID,
                "대댓글",
                NOW.plusSeconds(60),
                NOW.plusSeconds(60),
                null
        );
        ProjectComment deletedRoot = ProjectComment.reconstitute(
                503L,
                PROJECT_ID,
                AUTHOR_ID,
                null,
                "삭제된 원문",
                NOW.plusSeconds(120),
                NOW.plusSeconds(120),
                NOW.plusSeconds(180)
        );
        when(projectCommentQueryRepository.findRootCommentsPage(
                PROJECT_ID,
                null,
                ProjectCommentSort.LATEST,
                5
        )).thenReturn(new ProjectCommentPage(List.of(root, deletedRoot), false));
        when(projectCommentQueryRepository.findReplies(PROJECT_ID, List.of(COMMENT_ID, 503L)))
                .thenReturn(List.of(reply));
        givenAuthor(AUTHOR_ID, "작성자", 10L);
        givenAuthor(AUTHOR_ID + 1, "답글 작성자", 11L);

        ProjectCommentFindResponse result = projectCommentService.findAll(
                PROJECT_ID,
                new ProjectCommentFindRequest(null, 5, "LATEST"),
                null
        );

        assertThat(result.comments()).extracting(Comment::id)
                .containsExactly(COMMENT_ID, 502L, 503L);
        assertThat(result.comments().get(0).content()).isEqualTo("루트 댓글");
        assertThat(result.comments().get(0).editable()).isFalse();
        assertThat(result.comments().get(1).parentId()).isEqualTo(COMMENT_ID);
        assertThat(result.comments().get(2).content()).isNull();
        assertThat(result.comments().get(2).deleted()).isTrue();
        assertThat(result.comments().get(2).editable()).isFalse();
        assertThat(result.meta().nextCursor()).isNull();
        assertThat(result.meta().hasNext()).isFalse();
    }

    @Test
    @DisplayName("로그인 사용자는 본인 댓글만 수정 가능 상태로 조회한다.")
    void marksOnlyLoggedInUsersCommentsAsEditable() {
        givenPublicProject();
        ProjectComment ownComment = comment(COMMENT_ID, AUTHOR_ID, "내 댓글", NOW, NOW, null);
        ProjectComment otherComment = comment(502L, AUTHOR_ID + 1, "다른 댓글", NOW.plusSeconds(1),
                NOW.plusSeconds(1), null);
        when(projectCommentQueryRepository.findRootCommentsPage(
                PROJECT_ID,
                null,
                ProjectCommentSort.LATEST,
                5
        )).thenReturn(new ProjectCommentPage(List.of(ownComment, otherComment), false));
        when(projectCommentQueryRepository.findReplies(PROJECT_ID, List.of(COMMENT_ID, 502L)))
                .thenReturn(List.of());
        givenAuthor(AUTHOR_ID, "내 이름", 10L);
        givenAuthor(AUTHOR_ID + 1, "다른 이름", 11L);

        ProjectCommentFindResponse result = projectCommentService.findAll(
                PROJECT_ID,
                new ProjectCommentFindRequest(null, 5, "LATEST"),
                AUTHOR_ID
        );

        assertThat(result.comments()).extracting(Comment::editable)
                .containsExactly(true, false);
    }

    @Test
    @DisplayName("다음 페이지가 있으면 마지막 루트 댓글 기준 커서를 반환한다.")
    void createsNextCursorFromLastRootComment() {
        givenPublicProject();
        ProjectComment root = comment(COMMENT_ID, AUTHOR_ID, "첫 번째 댓글", NOW, NOW, null);
        when(projectCommentQueryRepository.findRootCommentsPage(
                PROJECT_ID,
                null,
                ProjectCommentSort.OLDEST,
                1
        )).thenReturn(new ProjectCommentPage(List.of(root), true));
        when(projectCommentQueryRepository.findReplies(PROJECT_ID, List.of(COMMENT_ID)))
                .thenReturn(List.of());
        givenAuthor();

        ProjectCommentFindResponse result = projectCommentService.findAll(
                PROJECT_ID,
                new ProjectCommentFindRequest(null, 1, "OLDEST"),
                null
        );

        assertThat(result.meta().hasNext()).isTrue();
        assertThat(ProjectCommentCursorCodec.decode(result.meta().nextCursor()))
                .isEqualTo(new ProjectCommentCursor(NOW, COMMENT_ID, ProjectCommentSort.OLDEST));
    }

    @Test
    @DisplayName("커서의 정렬 기준이 요청 정렬 기준과 다르면 조회하지 않고 400을 던진다.")
    void rejectsCursorWithDifferentSort() {
        givenPublicProject();
        String cursor = ProjectCommentCursorCodec.encode(
                new ProjectCommentCursor(NOW, COMMENT_ID, ProjectCommentSort.LATEST)
        );

        assertThatThrownBy(() -> projectCommentService.findAll(
                PROJECT_ID,
                new ProjectCommentFindRequest(cursor, 5, "OLDEST"),
                null
        )).isInstanceOfSatisfying(InvalidInputException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(
                        MISMATCHED_COMMENT_SORT_AND_CURSOR_SORT
                ));

        verifyNoInteractions(projectCommentQueryRepository, userProfileRepository);
    }

    @Test
    @DisplayName("공개 프로젝트가 아니면 댓글 목록을 조회하지 않고 404를 던진다.")
    void rejectsFindAllForNonPublicProject() {
        when(projectRepository.existsPublicById(PROJECT_ID)).thenReturn(false);

        assertThatThrownBy(() -> projectCommentService.findAll(
                PROJECT_ID,
                new ProjectCommentFindRequest(null, 5, "LATEST"),
                null
        )).isInstanceOfSatisfying(EntityNotFoundException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(PROJECT_NOT_FOUND));

        verifyNoInteractions(projectCommentQueryRepository, userProfileRepository);
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

    @Test
    @DisplayName("댓글 작성자 본인이 댓글 내용을 수정하고 수정 이력을 포함한 응답을 반환한다.")
    void updatesCommentContent() {
        givenPublicProject();
        when(projectCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment(
                COMMENT_ID,
                AUTHOR_ID,
                "기존 댓글",
                NOW,
                NOW,
                null
        )));
        givenAuthor();
        when(projectCommentRepository.save(any(ProjectComment.class))).thenReturn(comment(
                COMMENT_ID,
                AUTHOR_ID,
                "수정된 댓글",
                NOW,
                EDITED_AT,
                null
        ));

        ProjectCommentUpdateResponse result = projectCommentService.update(
                PROJECT_ID,
                COMMENT_ID,
                AUTHOR_ID,
                new ProjectCommentUpdateRequest("  수정된 댓글  ")
        );

        assertThat(result.id()).isEqualTo(COMMENT_ID);
        assertThat(result.content()).isEqualTo("수정된 댓글");
        assertThat(result.author().userId()).isEqualTo(AUTHOR_ID);
        assertThat(result.author().displayName()).isEqualTo("샤라웃 운영팀");
        assertThat(result.author().avatarImageId()).isEqualTo(10L);
        assertThat(result.parentId()).isNull();
        assertThat(result.createdAt()).isEqualTo(NOW);
        assertThat(result.updatedAt()).isEqualTo(EDITED_AT);
        assertThat(result.editable()).isTrue();
        assertThat(result.edited()).isTrue();

        ArgumentCaptor<ProjectComment> captor = ArgumentCaptor.forClass(ProjectComment.class);
        verify(projectCommentRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(COMMENT_ID);
        assertThat(captor.getValue().getContent()).isEqualTo("수정된 댓글");
        assertThat(captor.getValue().getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(captor.getValue().getAuthorId()).isEqualTo(AUTHOR_ID);
    }

    @Test
    @DisplayName("댓글 작성자 본인이 댓글을 soft delete하고 삭제 상태 응답을 반환한다.")
    void deletesComment() {
        givenPublicProject();
        when(projectCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment(
                COMMENT_ID,
                AUTHOR_ID,
                "삭제할 댓글",
                NOW,
                NOW,
                null
        )));
        when(projectCommentRepository.save(any(ProjectComment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProjectCommentDeleteResponse result = projectCommentService.delete(
                PROJECT_ID,
                COMMENT_ID,
                AUTHOR_ID
        );

        assertThat(result.id()).isEqualTo(COMMENT_ID);
        assertThat(result.deleted()).isTrue();

        ArgumentCaptor<ProjectComment> captor = ArgumentCaptor.forClass(ProjectComment.class);
        verify(projectCommentRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(COMMENT_ID);
        assertThat(captor.getValue().getDeletedAt()).isNotNull();
        assertThat(captor.getValue().getContent()).isEqualTo("삭제할 댓글");
    }

    @Test
    @DisplayName("트림 후 기존 내용과 같으면 저장하지 않고 기존 수정 시각을 반환한다.")
    void doesNotUpdateWhenContentIsUnchanged() {
        givenPublicProject();
        when(projectCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment(
                COMMENT_ID,
                AUTHOR_ID,
                "기존 댓글",
                NOW,
                NOW,
                null
        )));
        givenAuthor();

        ProjectCommentUpdateResponse result = projectCommentService.update(
                PROJECT_ID,
                COMMENT_ID,
                AUTHOR_ID,
                new ProjectCommentUpdateRequest("  기존 댓글  ")
        );

        assertThat(result.content()).isEqualTo("기존 댓글");
        assertThat(result.createdAt()).isEqualTo(NOW);
        assertThat(result.updatedAt()).isEqualTo(NOW);
        assertThat(result.edited()).isFalse();
        verify(projectCommentRepository, never()).save(any());
    }

    @Test
    @DisplayName("승인되지 않았거나 삭제된 프로젝트의 댓글은 수정하지 않고 404를 던진다.")
    void rejectsUpdateForNonPublicProject() {
        when(projectRepository.existsPublicById(PROJECT_ID)).thenReturn(false);

        assertThatThrownBy(() -> projectCommentService.update(
                PROJECT_ID,
                COMMENT_ID,
                AUTHOR_ID,
                new ProjectCommentUpdateRequest("수정된 댓글")
        )).isInstanceOfSatisfying(EntityNotFoundException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(PROJECT_NOT_FOUND));

        verifyNoInteractions(projectCommentRepository, userProfileRepository);
    }

    @Test
    @DisplayName("존재하지 않거나 다른 프로젝트에 속한 댓글은 수정하지 않고 404를 던진다.")
    void rejectsMissingOrDifferentProjectComment() {
        givenPublicProject();
        when(projectCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment(
                COMMENT_ID,
                AUTHOR_ID,
                "다른 프로젝트 댓글",
                NOW,
                NOW,
                null,
                PROJECT_ID + 1
        )));

        assertThatThrownBy(() -> projectCommentService.update(
                PROJECT_ID,
                COMMENT_ID,
                AUTHOR_ID,
                new ProjectCommentUpdateRequest("수정된 댓글")
        )).isInstanceOfSatisfying(EntityNotFoundException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(COMMENT_NOT_FOUND));

        verify(projectCommentRepository, never()).save(any());
        verifyNoInteractions(userProfileRepository);
    }

    @Test
    @DisplayName("삭제된 댓글은 수정하지 않고 404를 던진다.")
    void rejectsDeletedComment() {
        givenPublicProject();
        when(projectCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment(
                COMMENT_ID,
                AUTHOR_ID,
                "삭제된 댓글",
                NOW,
                NOW,
                NOW
        )));

        assertThatThrownBy(() -> projectCommentService.update(
                PROJECT_ID,
                COMMENT_ID,
                AUTHOR_ID,
                new ProjectCommentUpdateRequest("수정된 댓글")
        )).isInstanceOfSatisfying(EntityNotFoundException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(COMMENT_NOT_FOUND));

        verify(projectCommentRepository, never()).save(any());
        verifyNoInteractions(userProfileRepository);
    }

    @Test
    @DisplayName("댓글 작성자가 아니면 수정하지 않고 403을 던진다.")
    void rejectsUpdateFromAnotherAuthor() {
        givenPublicProject();
        when(projectCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment(
                COMMENT_ID,
                AUTHOR_ID + 1,
                "기존 댓글",
                NOW,
                NOW,
                null
        )));

        assertThatThrownBy(() -> projectCommentService.update(
                PROJECT_ID,
                COMMENT_ID,
                AUTHOR_ID,
                new ProjectCommentUpdateRequest("수정된 댓글")
        )).isInstanceOfSatisfying(ForbiddenException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(FORBIDDEN));

        verify(projectCommentRepository, never()).save(any());
        verifyNoInteractions(userProfileRepository);
    }

    @Test
    @DisplayName("공개 프로젝트가 아니면 댓글을 삭제하지 않고 404를 던진다.")
    void rejectsDeleteForNonPublicProject() {
        when(projectRepository.existsPublicById(PROJECT_ID)).thenReturn(false);

        assertThatThrownBy(() -> projectCommentService.delete(
                PROJECT_ID,
                COMMENT_ID,
                AUTHOR_ID
        )).isInstanceOfSatisfying(EntityNotFoundException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(PROJECT_NOT_FOUND));

        verifyNoInteractions(projectCommentRepository);
    }

    @Test
    @DisplayName("이미 삭제된 댓글은 다시 삭제하지 않고 404를 던진다.")
    void rejectsAlreadyDeletedComment() {
        givenPublicProject();
        when(projectCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment(
                COMMENT_ID,
                AUTHOR_ID,
                "삭제된 댓글",
                NOW,
                NOW,
                NOW
        )));

        assertThatThrownBy(() -> projectCommentService.delete(
                PROJECT_ID,
                COMMENT_ID,
                AUTHOR_ID
        )).isInstanceOfSatisfying(EntityNotFoundException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(COMMENT_NOT_FOUND));

        verify(projectCommentRepository, never()).save(any());
    }

    @Test
    @DisplayName("다른 프로젝트에 속한 댓글은 삭제하지 않고 404를 던진다.")
    void rejectsDeleteOfCommentFromDifferentProject() {
        givenPublicProject();
        when(projectCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment(
                COMMENT_ID,
                AUTHOR_ID,
                "다른 프로젝트 댓글",
                NOW,
                NOW,
                null,
                PROJECT_ID + 1
        )));

        assertThatThrownBy(() -> projectCommentService.delete(
                PROJECT_ID,
                COMMENT_ID,
                AUTHOR_ID
        )).isInstanceOfSatisfying(EntityNotFoundException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(COMMENT_NOT_FOUND));

        verify(projectCommentRepository, never()).save(any());
    }

    @Test
    @DisplayName("댓글 작성자가 아니면 댓글을 삭제하지 않고 403을 던진다.")
    void rejectsDeleteFromAnotherAuthor() {
        givenPublicProject();
        when(projectCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment(
                COMMENT_ID,
                AUTHOR_ID + 1,
                "다른 사용자의 댓글",
                NOW,
                NOW,
                null
        )));

        assertThatThrownBy(() -> projectCommentService.delete(
                PROJECT_ID,
                COMMENT_ID,
                AUTHOR_ID
        )).isInstanceOfSatisfying(ForbiddenException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(FORBIDDEN));

        verify(projectCommentRepository, never()).save(any());
    }

    private void givenPublicProject() {
        when(projectRepository.existsPublicById(PROJECT_ID)).thenReturn(true);
    }

    private void givenAuthor() {
        givenAuthor(AUTHOR_ID, "샤라웃 운영팀", 10L);
    }

    private void givenAuthor(long authorId, String displayName, Long avatarImageId) {
        when(userProfileRepository.findByUserId(authorId)).thenReturn(Optional.of(
                UserProfile.builder()
                        .userId(authorId)
                        .displayName(displayName)
                        .userType(UserType.GENERAL)
                        .avatarImageId(avatarImageId)
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

    private ProjectComment comment(
            long commentId,
            long authorId,
            String content,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt
    ) {
        return comment(commentId, authorId, content, createdAt, updatedAt, deletedAt, PROJECT_ID);
    }

    private ProjectComment comment(
            long commentId,
            long authorId,
            String content,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt,
            long projectId
    ) {
        return ProjectComment.reconstitute(
                commentId,
                projectId,
                authorId,
                null,
                content,
                createdAt,
                updatedAt,
                deletedAt
        );
    }
}
