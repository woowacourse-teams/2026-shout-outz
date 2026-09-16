package com.shoutoutz.api.project.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.cohort.domain.Cohort;
import com.shoutoutz.api.cohort.domain.CohortErrorCode;
import com.shoutoutz.api.cohort.domain.InvalidCohortException;
import com.shoutoutz.api.common.exception.custom.ConflictException;
import com.shoutoutz.api.common.exception.custom.DuplicateEntityException;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.media.domain.MediaMetadata;
import com.shoutoutz.api.media.domain.MediaMetadataRepository;
import com.shoutoutz.api.media.domain.MediaPurpose;
import com.shoutoutz.api.media.domain.MediaStatus;
import com.shoutoutz.api.project.application.dto.UserProjectResult;
import com.shoutoutz.api.project.domain.ApprovalStatus;
import com.shoutoutz.api.project.domain.DeletedProject;
import com.shoutoutz.api.project.domain.DeletionType;
import com.shoutoutz.api.project.domain.Project;
import com.shoutoutz.api.project.domain.ProjectCursor;
import com.shoutoutz.api.project.domain.ProjectDeletion;
import com.shoutoutz.api.project.domain.ProjectDeletionRepository;
import com.shoutoutz.api.project.domain.ProjectDetail;
import com.shoutoutz.api.project.domain.ProjectErrorCode;
import com.shoutoutz.api.project.domain.ProjectFilterCondition;
import com.shoutoutz.api.project.domain.ProjectFilterOptions;
import com.shoutoutz.api.project.domain.ProjectFilterOptions.CohortCount;
import com.shoutoutz.api.project.domain.ProjectFilterOptions.TechTagCount;
import com.shoutoutz.api.project.domain.ProjectMemberProfile;
import com.shoutoutz.api.project.domain.ProjectPage;
import com.shoutoutz.api.project.domain.ProjectRepository;
import com.shoutoutz.api.project.domain.ProjectSearchCondition;
import com.shoutoutz.api.project.domain.ProjectSort;
import com.shoutoutz.api.project.domain.ProjectSummary;
import com.shoutoutz.api.project.domain.RestorableProject;
import com.shoutoutz.api.project.domain.RestoredProject;
import com.shoutoutz.api.project.domain.ProjectTechTag;
import com.shoutoutz.api.project.domain.ServiceStatus;
import com.shoutoutz.api.user.domain.profile.Track;
import com.shoutoutz.api.project.domain.Slug;
import com.shoutoutz.api.project.domain.exception.InvalidDescriptionMediaException;
import com.shoutoutz.api.project.domain.exception.InvalidProjectCursorException;
import com.shoutoutz.api.project.domain.exception.InvalidProjectMemberException;
import com.shoutoutz.api.project.domain.exception.InvalidTechTagException;
import com.shoutoutz.api.project.domain.exception.InvalidThumbnailException;
import com.shoutoutz.api.project.domain.exception.ProjectRegistrationForbiddenException;
import com.shoutoutz.api.project.presentation.dto.request.ProjectCreateRequest;
import com.shoutoutz.api.project.presentation.dto.request.ProjectFilterOptionsRequest;
import com.shoutoutz.api.project.presentation.dto.request.ProjectFindAllRequest;
import com.shoutoutz.api.project.presentation.dto.request.UserProjectFindRequest;
import com.shoutoutz.api.project.presentation.dto.response.ProjectCreateResponse;
import com.shoutoutz.api.project.presentation.dto.response.ProjectDetailResponse;
import com.shoutoutz.api.project.presentation.dto.response.ProjectFilterOptionsResponse;
import com.shoutoutz.api.project.presentation.dto.response.ProjectFindAllResponse;
import com.shoutoutz.api.project.presentation.dto.response.ProjectMemberProfileResponse;
import com.shoutoutz.api.project.presentation.dto.response.ProjectTechTagResponse;
import com.shoutoutz.api.project.presentation.dto.response.UserProjectFindResponse;
import com.shoutoutz.api.techtag.domain.TechTag;
import com.shoutoutz.api.techtag.domain.TechTagRepository;
import com.shoutoutz.api.user.domain.account.User;
import com.shoutoutz.api.user.domain.account.UserErrorCode;
import com.shoutoutz.api.user.domain.account.UserRepository;
import com.shoutoutz.api.user.domain.account.UserRole;
import com.shoutoutz.api.user.domain.account.UserStatus;
import com.shoutoutz.api.user.domain.profile.UserProfile;
import com.shoutoutz.api.user.domain.profile.UserProfileRepository;
import com.shoutoutz.api.user.domain.profile.UserType;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    private static final long REGISTERED_BY = 7L;
    private static final long MEMBER_ID = 8L;
    private static final String MEMBER_HANDLE = "zzaekkii";
    private static final String DESCRIPTION = "## 문제";
    private static final long THUMBNAIL_ID = 12L;
    private static final List<Long> TECH_TAG_IDS = List.of(1L, 2L);
    private static final Instant NOW = Instant.parse("2026-09-10T00:00:00Z");

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TechTagRepository techTagRepository;

    @Mock
    private MediaMetadataRepository mediaMetadataRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectDeletionRepository projectDeletionRepository;

    @Mock
    private UserProjectQueryRepository userProjectQueryRepository;

    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        projectService = new ProjectService(
                projectRepository,
                techTagRepository,
                mediaMetadataRepository,
                userProfileRepository,
                userRepository,
                projectDeletionRepository,
                userProjectQueryRepository,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    @DisplayName("프로젝트를 삭제하면 같은 시각으로 삭제하고 복구 기한이 담긴 삭제 이력을 남긴다.")
    void deleteSoftDeletesProjectAndSavesDeletionHistory() {
        DeletedProject deletedProject = new DeletedProject(1L, "2026-moamoa", "모아모아");
        when(projectRepository.softDelete(1L, REGISTERED_BY, NOW)).thenReturn(Optional.of(deletedProject));
        when(projectDeletionRepository.save(any(ProjectDeletion.class))).thenAnswer(answer -> answer.getArgument(0));

        ProjectDeletion deletion = projectService.delete(1L, REGISTERED_BY);

        ArgumentCaptor<ProjectDeletion> captor = ArgumentCaptor.forClass(ProjectDeletion.class);
        verify(projectDeletionRepository).save(captor.capture());
        ProjectDeletion saved = captor.getValue();
        assertThat(saved.getProjectId()).isEqualTo(1L);
        assertThat(saved.getProjectSlug()).isEqualTo("2026-moamoa");
        assertThat(saved.getProjectTitle()).isEqualTo("모아모아");
        assertThat(saved.getDeletedBy()).isEqualTo(REGISTERED_BY);
        assertThat(saved.getDeletionType()).isEqualTo(DeletionType.SELF_DELETE);
        assertThat(saved.getDeletedAt()).isEqualTo(NOW);
        assertThat(saved.getRestoreDeadlineAt()).isEqualTo(NOW.plus(Duration.ofDays(30)));
        assertThat(deletion.getDeletedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("삭제할 프로젝트가 없으면 삭제 이력을 남기지 않고 프로젝트를 찾을 수 없다고 응답한다.")
    void deleteThrowsNotFoundWhenNothingDeleted() {
        when(projectRepository.softDelete(1L, REGISTERED_BY, NOW)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.delete(1L, REGISTERED_BY))
                .isInstanceOfSatisfying(EntityNotFoundException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ProjectErrorCode.PROJECT_NOT_FOUND));
        verifyNoInteractions(projectDeletionRepository);
    }

    @Test
    @DisplayName("복구 기한 안에 복구하면 프로젝트를 되살리고 삭제 이력에 복구 사실을 남긴다.")
    void restoreRestoresProjectAndMarksDeletionRestored() {
        RestorableProject restorable = new RestorableProject(5L, NOW.plusSeconds(1));
        when(projectRepository.findRestorable(1L, REGISTERED_BY)).thenReturn(Optional.of(restorable));
        when(projectRepository.restore(1L, NOW)).thenReturn(Optional.of(ApprovalStatus.APPROVED));

        RestoredProject restored = projectService.restore(1L, REGISTERED_BY);

        assertThat(restored).isEqualTo(new RestoredProject(1L, ApprovalStatus.APPROVED, NOW));
        verify(projectDeletionRepository).markRestored(5L, REGISTERED_BY, NOW);
    }

    @Test
    @DisplayName("복구 기한과 같은 시각에는 복구할 수 있다.")
    void restoreSucceedsAtRestoreDeadline() {
        RestorableProject restorable = new RestorableProject(5L, NOW);
        when(projectRepository.findRestorable(1L, REGISTERED_BY)).thenReturn(Optional.of(restorable));
        when(projectRepository.restore(1L, NOW)).thenReturn(Optional.of(ApprovalStatus.PENDING));

        assertThat(projectService.restore(1L, REGISTERED_BY).approvalStatus()).isEqualTo(ApprovalStatus.PENDING);
    }

    @Test
    @DisplayName("복구 기한이 지나면 아무것도 바꾸지 않고 복구할 수 없다고 응답한다.")
    void restoreThrowsConflictAfterRestoreDeadline() {
        RestorableProject restorable = new RestorableProject(5L, NOW.minusSeconds(1));
        when(projectRepository.findRestorable(1L, REGISTERED_BY)).thenReturn(Optional.of(restorable));

        assertThatThrownBy(() -> projectService.restore(1L, REGISTERED_BY))
                .isInstanceOfSatisfying(ConflictException.class, error ->
                        assertThat(error.getErrorCode()).isEqualTo(ProjectErrorCode.PROJECT_RESTORE_DEADLINE_EXPIRED));
        verify(projectRepository, never()).restore(anyLong(), any());
        verifyNoInteractions(projectDeletionRepository);
    }

    @Test
    @DisplayName("복구할 프로젝트가 없으면 프로젝트를 찾을 수 없다고 응답한다.")
    void restoreThrowsNotFoundWhenNoRestorableProject() {
        when(projectRepository.findRestorable(1L, REGISTERED_BY)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.restore(1L, REGISTERED_BY))
                .isInstanceOfSatisfying(EntityNotFoundException.class, error ->
                        assertThat(error.getErrorCode()).isEqualTo(ProjectErrorCode.PROJECT_NOT_FOUND));
        verifyNoInteractions(projectDeletionRepository);
    }

    @Test
    @DisplayName("조회한 뒤 다른 요청이 먼저 복구해 버리면 이력을 남기지 않고 프로젝트를 찾을 수 없다고 응답한다.")
    void restoreThrowsNotFoundWhenAlreadyRestoredByAnotherRequest() {
        RestorableProject restorable = new RestorableProject(5L, NOW.plusSeconds(1));
        when(projectRepository.findRestorable(1L, REGISTERED_BY)).thenReturn(Optional.of(restorable));
        when(projectRepository.restore(1L, NOW)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.restore(1L, REGISTERED_BY))
                .isInstanceOfSatisfying(EntityNotFoundException.class, error ->
                        assertThat(error.getErrorCode()).isEqualTo(ProjectErrorCode.PROJECT_NOT_FOUND));
        verifyNoInteractions(projectDeletionRepository);
    }

    @Test
    @DisplayName("검증을 통과하면 등록자를 첫 팀원으로, 이어서 입력한 팀원을 두고 프로젝트를 저장한다.")
    void createsProject() {
        givenRegistrant(UserType.WOOWACOURSE_CREW);
        givenMember(MEMBER_HANDLE, MEMBER_ID, UserType.WOOWACOURSE_CREW);
        when(projectRepository.existsBySlug(new Slug("loop"))).thenReturn(false);
        when(techTagRepository.findAllActiveByIds(TECH_TAG_IDS)).thenReturn(activeTags(1L, 2L));
        when(mediaMetadataRepository.findById(THUMBNAIL_ID))
                .thenReturn(Optional.of(thumbnail(REGISTERED_BY, MediaPurpose.PROJECT_THUMBNAIL, MediaStatus.READY)));
        when(projectRepository.save(any(Project.class), eq(TECH_TAG_IDS), anyList()))
                .thenAnswer(invocation -> withId(invocation.getArgument(0), 100L));

        ProjectCreateResponse result = projectService.create(REGISTERED_BY, request(6, THUMBNAIL_ID, TECH_TAG_IDS));

        assertThat(result.projectId()).isEqualTo(100L);
        assertThat(result.slug()).isEqualTo("loop");

        ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Long>> memberIdsCaptor = ArgumentCaptor.forClass(List.class);
        verify(projectRepository).save(projectCaptor.capture(), eq(TECH_TAG_IDS), memberIdsCaptor.capture());
        assertThat(projectCaptor.getValue().getRegisteredBy()).isEqualTo(REGISTERED_BY);
        assertThat(projectCaptor.getValue().getServiceStatus()).isEqualTo(ServiceStatus.OPERATING);
        assertThat(memberIdsCaptor.getValue()).containsExactly(REGISTERED_BY, MEMBER_ID);
    }

    @Test
    @DisplayName("정의되지 않은 기수면 조회 없이 400을 던진다.")
    void rejectsUndefinedCohort() {
        givenRegistrant(UserType.WOOWACOURSE_CREW);

        assertThatThrownBy(() -> projectService.create(REGISTERED_BY, request(99, null, TECH_TAG_IDS)))
                .isInstanceOfSatisfying(InvalidCohortException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(CohortErrorCode.INVALID_COHORT));

        verifyNoInteractions(projectRepository, techTagRepository, mediaMetadataRepository);
    }

    @Test
    @DisplayName("이미 등록된 리포지토리면 409를 던지고 저장하지 않는다.")
    void rejectsDuplicateSlug() {
        givenRegistrant(UserType.WOOWACOURSE_CREW);
        when(projectRepository.existsBySlug(new Slug("loop"))).thenReturn(true);

        assertThatThrownBy(() -> projectService.create(REGISTERED_BY, request(6, null, TECH_TAG_IDS)))
                .isInstanceOfSatisfying(DuplicateEntityException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ProjectErrorCode.PROJECT_DUPLICATE_SLUG));

        verify(projectRepository, never()).save(any(), anyList(), anyList());
    }

    @Test
    @DisplayName("기술 태그 id가 중복되면 태그를 조회하지 않고 400을 던진다.")
    void rejectsDuplicateTechTags() {
        givenRegistrant(UserType.WOOWACOURSE_CREW);
        when(projectRepository.existsBySlug(new Slug("loop"))).thenReturn(false);

        assertThatThrownBy(() -> projectService.create(REGISTERED_BY, request(6, null, List.of(1L, 1L))))
                .isInstanceOfSatisfying(InvalidTechTagException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ProjectErrorCode.PROJECT_DUPLICATE_TECH_TAG));

        verifyNoInteractions(techTagRepository);
    }

    @Test
    @DisplayName("없거나 비활성인 기술 태그가 섞여 있으면 400을 던진다.")
    void rejectsUnselectableTechTags() {
        givenRegistrant(UserType.WOOWACOURSE_CREW);
        when(projectRepository.existsBySlug(new Slug("loop"))).thenReturn(false);
        when(techTagRepository.findAllActiveByIds(TECH_TAG_IDS)).thenReturn(activeTags(1L));

        assertThatThrownBy(() -> projectService.create(REGISTERED_BY, request(6, null, TECH_TAG_IDS)))
                .isInstanceOfSatisfying(InvalidTechTagException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ProjectErrorCode.PROJECT_INVALID_TECH_TAG));
    }

    @Test
    @DisplayName("썸네일을 입력하지 않으면 미디어를 조회하지 않는다.")
    void skipsThumbnailValidationWhenAbsent() {
        givenRegistrant(UserType.WOOWACOURSE_CREW);
        givenMember(MEMBER_HANDLE, MEMBER_ID, UserType.WOOWACOURSE_CREW);
        when(projectRepository.existsBySlug(new Slug("loop"))).thenReturn(false);
        when(techTagRepository.findAllActiveByIds(TECH_TAG_IDS)).thenReturn(activeTags(1L, 2L));
        when(projectRepository.save(any(Project.class), eq(TECH_TAG_IDS), anyList()))
                .thenAnswer(invocation -> withId(invocation.getArgument(0), 100L));

        projectService.create(REGISTERED_BY, request(6, null, TECH_TAG_IDS));

        verifyNoInteractions(mediaMetadataRepository);
    }

    @Test
    @DisplayName("없는 이미지를 썸네일로 쓰면 400을 던진다.")
    void rejectsMissingThumbnail() {
        givenRegistrant(UserType.WOOWACOURSE_CREW);
        givenValidSlugAndTags();
        when(mediaMetadataRepository.findById(THUMBNAIL_ID)).thenReturn(Optional.empty());

        assertInvalidThumbnail(ProjectErrorCode.PROJECT_INVALID_THUMBNAIL);
    }

    @Test
    @DisplayName("다른 사용자의 이미지를 썸네일로 쓰면 없는 이미지와 같은 400을 던진다.")
    void rejectsOthersThumbnail() {
        givenRegistrant(UserType.WOOWACOURSE_CREW);
        givenValidSlugAndTags();
        when(mediaMetadataRepository.findById(THUMBNAIL_ID))
                .thenReturn(Optional.of(thumbnail(99L, MediaPurpose.PROJECT_THUMBNAIL, MediaStatus.READY)));

        assertInvalidThumbnail(ProjectErrorCode.PROJECT_INVALID_THUMBNAIL);
    }

    @Test
    @DisplayName("썸네일 용도가 아닌 이미지를 쓰면 400을 던진다.")
    void rejectsNonThumbnailPurpose() {
        givenRegistrant(UserType.WOOWACOURSE_CREW);
        givenValidSlugAndTags();
        when(mediaMetadataRepository.findById(THUMBNAIL_ID))
                .thenReturn(Optional.of(thumbnail(REGISTERED_BY, MediaPurpose.FEED_CONTENT, MediaStatus.READY)));

        assertInvalidThumbnail(ProjectErrorCode.PROJECT_INVALID_THUMBNAIL);
    }

    @Test
    @DisplayName("처리가 끝나지 않은 썸네일이면 처리 중 400을 던진다.")
    void rejectsThumbnailNotReady() {
        givenRegistrant(UserType.WOOWACOURSE_CREW);
        givenValidSlugAndTags();
        when(mediaMetadataRepository.findById(THUMBNAIL_ID))
                .thenReturn(Optional.of(thumbnail(REGISTERED_BY, MediaPurpose.PROJECT_THUMBNAIL, MediaStatus.PROCESSING)));

        assertInvalidThumbnail(ProjectErrorCode.PROJECT_THUMBNAIL_NOT_READY);
    }

    @Test
    @DisplayName("코치도 프로젝트를 등록할 수 있다.")
    void createsProjectByCoach() {
        givenRegistrant(UserType.WOOWACOURSE_COACH);
        givenValidSlugAndTags();
        givenMember(MEMBER_HANDLE, MEMBER_ID, UserType.WOOWACOURSE_CREW);
        when(projectRepository.save(any(Project.class), eq(TECH_TAG_IDS), anyList()))
                .thenAnswer(invocation -> withId(invocation.getArgument(0), 100L));

        ProjectCreateResponse result = projectService.create(REGISTERED_BY, request(6, null, TECH_TAG_IDS));

        assertThat(result.projectId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("크루나 코치가 아닌 사용자가 등록하면 다른 검증 없이 403을 던진다.")
    void rejectsGeneralUser() {
        givenRegistrant(UserType.GENERAL);

        assertRegistrationForbidden();
    }

    @Test
    @DisplayName("등록자의 프로필이 없으면 403을 던진다.")
    void rejectsRegistrantWithoutProfile() {
        when(userProfileRepository.findByUserId(REGISTERED_BY)).thenReturn(Optional.empty());

        assertRegistrationForbidden();
    }

    @Test
    @DisplayName("팀원은 입력한 순서대로 저장되고, 코치도 팀원이 될 수 있다.")
    void keepsMemberOrderAndAcceptsCoach() {
        givenValidProjectExceptMembers();
        givenMember(MEMBER_HANDLE, MEMBER_ID, UserType.WOOWACOURSE_CREW);
        givenMember("coach-jack", 9L, UserType.WOOWACOURSE_COACH);
        when(projectRepository.save(any(Project.class), eq(TECH_TAG_IDS), anyList()))
                .thenAnswer(invocation -> withId(invocation.getArgument(0), 100L));

        projectService.create(REGISTERED_BY, request(List.of("coach-jack", MEMBER_HANDLE)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Long>> memberIdsCaptor = ArgumentCaptor.forClass(List.class);
        verify(projectRepository).save(any(Project.class), eq(TECH_TAG_IDS), memberIdsCaptor.capture());
        assertThat(memberIdsCaptor.getValue()).containsExactly(REGISTERED_BY, 9L, MEMBER_ID);
    }

    @Test
    @DisplayName("대소문자만 다른 handle 로 같은 사용자를 두 번 넣으면 400을 던진다.")
    void rejectsDuplicateMemberIgnoringCase() {
        givenValidProjectExceptMembers();
        givenMember(MEMBER_HANDLE, MEMBER_ID, UserType.WOOWACOURSE_CREW);
        givenMemberAccount("ZzaeKKii", MEMBER_ID, UserStatus.ACTIVE);

        assertInvalidMember(List.of(MEMBER_HANDLE, "ZzaeKKii"), ProjectErrorCode.PROJECT_DUPLICATE_MEMBER);
    }

    @Test
    @DisplayName("존재하지 않는 사용자를 팀원으로 넣으면 400을 던진다.")
    void rejectsUnknownMember() {
        givenValidProjectExceptMembers();
        when(userRepository.findByHandle(MEMBER_HANDLE)).thenReturn(Optional.empty());

        assertInvalidMember(List.of(MEMBER_HANDLE), ProjectErrorCode.PROJECT_INVALID_MEMBER);
    }

    @Test
    @DisplayName("활동 중이 아닌 사용자를 팀원으로 넣으면 400을 던진다.")
    void rejectsInactiveMember() {
        givenValidProjectExceptMembers();
        givenMemberAccount(MEMBER_HANDLE, MEMBER_ID, UserStatus.BANNED);

        assertInvalidMember(List.of(MEMBER_HANDLE), ProjectErrorCode.PROJECT_INVALID_MEMBER);
    }

    @Test
    @DisplayName("크루나 코치가 아닌 사용자를 팀원으로 넣으면 400을 던진다.")
    void rejectsGeneralMember() {
        givenValidProjectExceptMembers();
        givenMember(MEMBER_HANDLE, MEMBER_ID, UserType.GENERAL);

        assertInvalidMember(List.of(MEMBER_HANDLE), ProjectErrorCode.PROJECT_INVALID_MEMBER);
    }

    @Test
    @DisplayName("등록자 본인을 팀원으로 넣으면 400을 던진다.")
    void rejectsRegistrantAsMember() {
        givenValidProjectExceptMembers();
        givenMemberAccount("dhyepark", REGISTERED_BY, UserStatus.ACTIVE);

        assertInvalidMember(List.of("dhyepark"), ProjectErrorCode.PROJECT_MEMBER_INCLUDES_REGISTRANT);
    }

    @Test
    @DisplayName("본문이 참조한 이미지가 모두 등록자가 올린 처리 완료 본문 이미지면 등록한다. 같은 이미지는 한 번만 조회한다.")
    void acceptsOwnReadyDescriptionMedia() {
        givenValidProjectExceptMembers();
        givenMember(MEMBER_HANDLE, MEMBER_ID, UserType.WOOWACOURSE_CREW);
        when(mediaMetadataRepository.findById(21L))
                .thenReturn(Optional.of(media(21L, REGISTERED_BY, MediaPurpose.PROJECT_DESCRIPTION, MediaStatus.READY)));
        when(mediaMetadataRepository.findById(22L))
                .thenReturn(Optional.of(media(22L, REGISTERED_BY, MediaPurpose.PROJECT_DESCRIPTION, MediaStatus.READY)));
        when(projectRepository.save(any(Project.class), eq(TECH_TAG_IDS), anyList()))
                .thenAnswer(invocation -> withId(invocation.getArgument(0), 100L));

        projectService.create(REGISTERED_BY, requestWithDescription(
                "## 화면\n![목록](media://21)\n![상세](media://22)\n![목록 확대](media://21)"));

        verify(mediaMetadataRepository, times(1)).findById(21L);
        verify(projectRepository).save(any(Project.class), eq(TECH_TAG_IDS), anyList());
    }

    @Test
    @DisplayName("본문이 없는 이미지를 참조하면 400을 던진다.")
    void rejectsMissingDescriptionMedia() {
        givenValidProjectExceptMembers();
        when(mediaMetadataRepository.findById(21L)).thenReturn(Optional.empty());

        assertInvalidDescriptionMedia("![화면](media://21)", ProjectErrorCode.PROJECT_INVALID_DESCRIPTION_MEDIA);
    }

    @Test
    @DisplayName("본문이 다른 사용자의 이미지를 참조하면 없는 이미지와 같은 400을 던진다.")
    void rejectsOthersDescriptionMedia() {
        givenValidProjectExceptMembers();
        when(mediaMetadataRepository.findById(21L))
                .thenReturn(Optional.of(media(21L, 99L, MediaPurpose.PROJECT_DESCRIPTION, MediaStatus.READY)));

        assertInvalidDescriptionMedia("![화면](media://21)", ProjectErrorCode.PROJECT_INVALID_DESCRIPTION_MEDIA);
    }

    @Test
    @DisplayName("본문이 본문 용도가 아닌 이미지를 참조하면 400을 던진다.")
    void rejectsNonDescriptionPurposeMedia() {
        givenValidProjectExceptMembers();
        when(mediaMetadataRepository.findById(21L))
                .thenReturn(Optional.of(media(21L, REGISTERED_BY, MediaPurpose.PROJECT_THUMBNAIL, MediaStatus.READY)));

        assertInvalidDescriptionMedia("![화면](media://21)", ProjectErrorCode.PROJECT_INVALID_DESCRIPTION_MEDIA);
    }

    @Test
    @DisplayName("본문이 처리가 끝나지 않은 이미지를 참조하면 처리 중 400을 던진다.")
    void rejectsDescriptionMediaNotReady() {
        givenValidProjectExceptMembers();
        when(mediaMetadataRepository.findById(21L))
                .thenReturn(Optional.of(media(21L, REGISTERED_BY, MediaPurpose.PROJECT_DESCRIPTION, MediaStatus.PROCESSING)));

        assertInvalidDescriptionMedia("![화면](media://21)", ProjectErrorCode.PROJECT_DESCRIPTION_MEDIA_NOT_READY);
    }

    @Test
    @DisplayName("목록 요청의 검색어, 필터, 정렬, 조회 개수, 커서를 정리해 조회 조건으로 넘긴다.")
    void findsProjectsWithResolvedCondition() {
        ProjectCursor cursor = ProjectCursor.popular(3L, NOW, 50L);
        when(projectRepository.findAll(any(ProjectSearchCondition.class)))
                .thenReturn(new ProjectPage(List.of(), false, 0));

        projectService.findAll(new ProjectFindAllRequest(
                " 모아 ", List.of(7, 6, 7), List.of(2L, 1L, 2L), "POPULAR", 20, ProjectCursorCodec.encode(cursor)));

        ArgumentCaptor<ProjectSearchCondition> conditionCaptor = ArgumentCaptor.forClass(ProjectSearchCondition.class);
        verify(projectRepository).findAll(conditionCaptor.capture());
        assertThat(conditionCaptor.getValue()).isEqualTo(new ProjectSearchCondition(
                "모아", List.of(7, 6), List.of(2L, 1L), ProjectSort.POPULAR, 20, cursor));
    }

    @Test
    @DisplayName("다음 페이지가 있으면 이번 페이지 마지막 프로젝트의 위치를 다음 커서로 내려준다.")
    void returnsNextCursorOfLastProject() {
        ProjectSummary first = summary(10L, 5L, NOW);
        ProjectSummary last = summary(9L, 3L, NOW.minusSeconds(60));
        when(projectRepository.findAll(any(ProjectSearchCondition.class)))
                .thenReturn(new ProjectPage(List.of(first, last), true, 48));

        ProjectFindAllResponse response = projectService.findAll(
                new ProjectFindAllRequest(null, null, null, "POPULAR", 2, null));

        assertThat(response.items()).extracting(ProjectFindAllResponse.Item::id).containsExactly(10L, 9L);
        assertThat(response.meta().hasNext()).isTrue();
        assertThat(response.meta().totalCount()).isEqualTo(48);
        assertThat(ProjectCursorCodec.decode(response.meta().nextCursor(), ProjectSort.POPULAR))
                .isEqualTo(ProjectCursor.popular(3L, NOW.minusSeconds(60), 9L));
    }

    @Test
    @DisplayName("마지막 페이지면 다음 커서를 내려주지 않는다.")
    void returnsNullCursorOnLastPage() {
        when(projectRepository.findAll(any(ProjectSearchCondition.class)))
                .thenReturn(new ProjectPage(List.of(summary(10L, 0L, NOW)), false, 1));

        ProjectFindAllResponse response = projectService.findAll(
                new ProjectFindAllRequest(null, null, null, null, null, null));

        assertThat(response.meta().hasNext()).isFalse();
        assertThat(response.meta().nextCursor()).isNull();
        assertThat(response.meta().totalCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("사용자가 참여한 프로젝트를 최신순 커서 조건으로 조회한다.")
    void findsProjectsByUser() {
        ProjectCursor cursor = ProjectCursor.latest(NOW, 10L);
        User user = user(REGISTERED_BY, MEMBER_HANDLE, UserStatus.ACTIVE);
        ProjectSummary project = summary(9L, 3L, NOW.minusSeconds(60));
        when(userRepository.findByHandle(MEMBER_HANDLE)).thenReturn(Optional.of(user));
        when(userProjectQueryRepository.findAllByUserId(REGISTERED_BY, cursor, 20))
                .thenReturn(new UserProjectResult(List.of(project), true));

        UserProjectFindResponse response = projectService.findAllByUser(
                MEMBER_HANDLE,
                new UserProjectFindRequest(20, ProjectCursorCodec.encode(cursor))
        );

        assertThat(response.projects()).extracting(ProjectFindAllResponse.Item::id).containsExactly(9L);
        assertThat(response.meta().hasNext()).isTrue();
        assertThat(ProjectCursorCodec.decode(response.meta().nextCursor(), ProjectSort.LATEST))
                .isEqualTo(ProjectCursor.latest(NOW.minusSeconds(60), 9L));
        verify(userProjectQueryRepository).findAllByUserId(REGISTERED_BY, cursor, 20);
    }

    @Test
    @DisplayName("탈퇴한 사용자의 프로젝트 목록은 공개하지 않는다.")
    void returnsEmptyProjectsForDeletedUser() {
        when(userRepository.findByHandle(MEMBER_HANDLE))
                .thenReturn(Optional.of(user(REGISTERED_BY, MEMBER_HANDLE, UserStatus.DELETED)));

        UserProjectFindResponse response = projectService.findAllByUser(
                MEMBER_HANDLE,
                new UserProjectFindRequest(null, null)
        );

        assertThat(response.projects()).isEmpty();
        assertThat(response.meta().hasNext()).isFalse();
        assertThat(response.meta().nextCursor()).isNull();
        verifyNoInteractions(userProjectQueryRepository);
    }

    @Test
    @DisplayName("존재하지 않는 handle의 프로젝트 목록은 조회할 수 없다.")
    void rejectsUnknownUserProjects() {
        when(userRepository.findByHandle(MEMBER_HANDLE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.findAllByUser(
                MEMBER_HANDLE,
                new UserProjectFindRequest(null, null)
        )).isInstanceOfSatisfying(EntityNotFoundException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND));

        verifyNoInteractions(userProjectQueryRepository);
    }

    @Test
    @DisplayName("정의되지 않은 기수로 필터링하면 목록을 조회하지 않고 400을 던진다.")
    void rejectsUndefinedCohortFilter() {
        assertThatThrownBy(() -> projectService.findAll(
                new ProjectFindAllRequest(null, List.of(99), null, null, null, null)))
                .isInstanceOfSatisfying(InvalidCohortException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(CohortErrorCode.INVALID_COHORT));

        verifyNoInteractions(projectRepository);
    }

    @Test
    @DisplayName("정렬을 바꾸고 이전 정렬의 커서를 보내면 목록을 조회하지 않고 400을 던진다.")
    void rejectsCursorOfDifferentSort() {
        String latestCursor = ProjectCursorCodec.encode(ProjectCursor.latest(NOW, 10L));

        assertThatThrownBy(() -> projectService.findAll(
                new ProjectFindAllRequest(null, null, null, "POPULAR", null, latestCursor)))
                .isInstanceOf(InvalidProjectCursorException.class);

        verifyNoInteractions(projectRepository);
    }

    @Test
    @DisplayName("필터 옵션 요청의 검색어와 필터를 목록 조회와 같은 방식으로 정리해 조회 조건으로 넘긴다.")
    void findsFilterOptionsWithResolvedCondition() {
        when(projectRepository.findFilterOptions(any(ProjectFilterCondition.class)))
                .thenReturn(new ProjectFilterOptions(List.of(), List.of(), 0));

        projectService.findFilterOptions(new ProjectFilterOptionsRequest(" 모아 ", List.of(7, 6, 7), List.of(2L, 1L, 2L)));

        verify(projectRepository).findFilterOptions(new ProjectFilterCondition("모아", List.of(7, 6), List.of(2L, 1L)));
    }

    @Test
    @DisplayName("기수는 기수 번호와 연도를, 기술 스택은 id와 이름을 선택지별 프로젝트 수와 함께 응답으로 옮긴다.")
    void mapsFilterOptionsToResponse() {
        when(projectRepository.findFilterOptions(any(ProjectFilterCondition.class)))
                .thenReturn(new ProjectFilterOptions(
                        List.of(new CohortCount(Cohort.COHORT_6, 28L), new CohortCount(Cohort.COHORT_5, 0L)),
                        List.of(new TechTagCount(1L, "Spring Boot", 51L)),
                        8L
                ));

        ProjectFilterOptionsResponse response = projectService.findFilterOptions(
                new ProjectFilterOptionsRequest(null, null, null));

        assertThat(response.cohorts()).containsExactly(
                new ProjectFilterOptionsResponse.CohortItem(6, 2024, 28L),
                new ProjectFilterOptionsResponse.CohortItem(5, 2023, 0L));
        assertThat(response.techTags()).containsExactly(
                new ProjectFilterOptionsResponse.TechTagItem(1L, "Spring Boot", 51L));
        assertThat(response.matchedProjectCount()).isEqualTo(8L);
    }

    @Test
    @DisplayName("정의되지 않은 기수를 선택하면 필터 옵션을 조회하지 않고 400을 던진다.")
    void rejectsUndefinedCohortForFilterOptions() {
        assertThatThrownBy(() -> projectService.findFilterOptions(
                new ProjectFilterOptionsRequest(null, List.of(99), null)))
                .isInstanceOf(InvalidCohortException.class);

        verifyNoInteractions(projectRepository);
    }

    private static ProjectSummary summary(long id, long likeCount, Instant createdAt) {
        return new ProjectSummary(
                id, "loop-" + id, "루프", "한 줄 소개", 6, null, REGISTERED_BY, 128, likeCount, 0L,
                List.of(), List.of(), createdAt);
    }

    private static User user(long id, String handle, UserStatus status) {
        return User.builder()
                .id(id)
                .handle(handle)
                .status(status)
                .role(UserRole.USER)
                .deletedAt(status == UserStatus.DELETED ? NOW : null)
                .build();
    }

    @Test
    @DisplayName("승인된 프로젝트는 비로그인 사용자도 상세 조회할 수 있고, 기술 스택과 팀원을 응답으로 옮긴다.")
    void findsApprovedProjectDetailForAnonymous() {
        when(projectRepository.findDetailById(100L, null))
                .thenReturn(Optional.of(projectDetail(ApprovalStatus.APPROVED)));

        ProjectDetailResponse response = projectService.findDetail(100L, null);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.registeredBy()).isEqualTo(REGISTERED_BY);
        assertThat(response.techTags()).containsExactly(new ProjectTechTagResponse(1L, "React"));
        assertThat(response.members()).containsExactly(new ProjectMemberProfileResponse(
                REGISTERED_BY, "dhyepark", "박다혜", 6, "BACKEND", 101L, null, null
        ));
    }

    @Test
    @DisplayName("승인되지 않은 프로젝트도 등록자 본인은 상세 조회할 수 있다.")
    void findsUnapprovedProjectDetailForRegistrant() {
        when(projectRepository.findDetailById(100L, REGISTERED_BY))
                .thenReturn(Optional.of(projectDetail(ApprovalStatus.REJECTED)));

        ProjectDetailResponse response = projectService.findDetail(100L, REGISTERED_BY);

        assertThat(response.approvalStatus()).isEqualTo(ApprovalStatus.REJECTED);
    }

    @Test
    @DisplayName("승인되지 않은 프로젝트를 등록자가 아닌 사용자가 조회하면 존재 여부를 숨기고 404를 던진다.")
    void hidesUnapprovedProjectFromOthers() {
        when(projectRepository.findDetailById(100L, MEMBER_ID))
                .thenReturn(Optional.of(projectDetail(ApprovalStatus.PENDING)));

        assertProjectNotFound(() -> projectService.findDetail(100L, MEMBER_ID));
    }

    @Test
    @DisplayName("없거나 삭제된 프로젝트를 조회하면 404를 던진다.")
    void rejectsMissingProject() {
        when(projectRepository.findDetailById(100L, null)).thenReturn(Optional.empty());

        assertProjectNotFound(() -> projectService.findDetail(100L, null));
    }

    private static void assertProjectNotFound(ThrowingCallable callable) {
        assertThatThrownBy(callable)
                .isInstanceOfSatisfying(EntityNotFoundException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ProjectErrorCode.PROJECT_NOT_FOUND));
    }

    private static ProjectDetail projectDetail(ApprovalStatus approvalStatus) {
        return new ProjectDetail(
                100L,
                "loop",
                "루프 (Loop)",
                "루프팀",
                "스프린트 회고와 액션 아이템을 하나로 엮은 실시간 협업 도구",
                6,
                THUMBNAIL_ID,
                DESCRIPTION,
                "https://github.com/woowacourse-teams/2026-loop",
                "https://loop.team",
                ServiceStatus.OPERATING,
                approvalStatus,
                null,
                REGISTERED_BY,
                0,
                null,
                0,
                0,
                false,
                false,
                0,
                List.of(new ProjectTechTag(1L, "React")),
                List.of(ProjectMemberProfile.user(
                        REGISTERED_BY,
                        "dhyepark",
                        "박다혜",
                        Cohort.COHORT_6,
                        Track.BACKEND,
                        101L
                )),
                NOW,
                NOW
        );
    }

    private void assertInvalidDescriptionMedia(String descriptionMd, ProjectErrorCode expected) {
        assertThatThrownBy(() -> projectService.create(REGISTERED_BY, requestWithDescription(descriptionMd)))
                .isInstanceOfSatisfying(InvalidDescriptionMediaException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(expected));
        verify(projectRepository, never()).save(any(), anyList(), anyList());
    }

    private void givenRegistrant(UserType userType) {
        when(userProfileRepository.findByUserId(REGISTERED_BY)).thenReturn(Optional.of(profile(REGISTERED_BY, userType)));
    }

    private void givenMember(String handle, long userId, UserType userType) {
        givenMemberAccount(handle, userId, UserStatus.ACTIVE);
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile(userId, userType)));
    }

    private void givenMemberAccount(String handle, long userId, UserStatus status) {
        User user = User.builder()
                .id(userId)
                .handle(handle)
                .status(status)
                .role(UserRole.USER)
                .build();
        when(userRepository.findByHandle(handle)).thenReturn(Optional.of(user));
    }

    /**
     * 팀원 검증 직전까지의 검증을 모두 통과하는 상황을 만든다.
     */
    private void givenValidProjectExceptMembers() {
        givenRegistrant(UserType.WOOWACOURSE_CREW);
        givenValidSlugAndTags();
    }

    private void assertInvalidMember(List<String> memberHandles, ProjectErrorCode expected) {
        assertThatThrownBy(() -> projectService.create(REGISTERED_BY, request(memberHandles)))
                .isInstanceOfSatisfying(InvalidProjectMemberException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(expected));
        verify(projectRepository, never()).save(any(), anyList(), anyList());
    }

    private static UserProfile profile(long userId, UserType userType) {
        boolean crew = userType == UserType.WOOWACOURSE_CREW;
        return UserProfile.builder()
                .userId(userId)
                .displayName("사용자" + userId)
                .userType(userType)
                .track(crew ? Track.BACKEND : null)
                .cohort(crew ? Cohort.COHORT_6 : null)
                .build();
    }

    private void assertRegistrationForbidden() {
        assertThatThrownBy(() -> projectService.create(REGISTERED_BY, request(6, null, TECH_TAG_IDS)))
                .isInstanceOfSatisfying(ProjectRegistrationForbiddenException.class,
                        error -> assertThat(error.getErrorCode())
                                .isEqualTo(ProjectErrorCode.PROJECT_REGISTRATION_FORBIDDEN));
        verifyNoInteractions(projectRepository, techTagRepository, mediaMetadataRepository);
    }

    private void givenValidSlugAndTags() {
        when(projectRepository.existsBySlug(new Slug("loop"))).thenReturn(false);
        when(techTagRepository.findAllActiveByIds(TECH_TAG_IDS)).thenReturn(activeTags(1L, 2L));
    }

    private void assertInvalidThumbnail(ProjectErrorCode expected) {
        assertThatThrownBy(() -> projectService.create(REGISTERED_BY, request(6, THUMBNAIL_ID, TECH_TAG_IDS)))
                .isInstanceOfSatisfying(InvalidThumbnailException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(expected));
        verify(projectRepository, never()).save(any(), anyList(), anyList());
    }

    private static ProjectCreateRequest request(List<String> memberHandles) {
        return request(6, null, TECH_TAG_IDS, memberHandles, DESCRIPTION);
    }

    private static ProjectCreateRequest requestWithDescription(String descriptionMd) {
        return request(6, null, TECH_TAG_IDS, List.of(MEMBER_HANDLE), descriptionMd);
    }

    private static ProjectCreateRequest request(int cohort, Long thumbnailMediaId, List<Long> techTagIds) {
        return request(cohort, thumbnailMediaId, techTagIds, List.of(MEMBER_HANDLE), DESCRIPTION);
    }

    private static ProjectCreateRequest request(
            int cohort,
            Long thumbnailMediaId,
            List<Long> techTagIds,
            List<String> memberHandles,
            String descriptionMd
    ) {
        return new ProjectCreateRequest(
                "루프 (Loop)",
                "루프팀",
                "스프린트 회고와 액션 아이템을 하나로 엮은 실시간 협업 도구",
                cohort,
                thumbnailMediaId,
                "https://github.com/woowacourse-teams/2026-loop",
                "https://loop.team",
                descriptionMd,
                techTagIds,
                memberHandles
        );
    }

    private static List<TechTag> activeTags(Long... ids) {
        return java.util.Arrays.stream(ids)
                .map(id -> TechTag.builder().id(id).slug("tag-" + id).displayName("Tag" + id).active(true).build())
                .toList();
    }

    private static MediaMetadata thumbnail(Long uploadedBy, MediaPurpose purpose, MediaStatus status) {
        return media(THUMBNAIL_ID, uploadedBy, purpose, status);
    }

    private static MediaMetadata media(Long id, Long uploadedBy, MediaPurpose purpose, MediaStatus status) {
        return MediaMetadata.reconstitute(
                id,
                uploadedBy,
                purpose,
                "media/test/" + id + ".webp",
                "thumbnail.webp",
                "image/webp",
                1024L,
                status,
                NOW.plusSeconds(600),
                null,
                NOW,
                NOW,
                NOW
        );
    }

    private static Project withId(Project project, Long id) {
        return Project.builder()
                .id(id)
                .cohort(project.getCohort())
                .registeredBy(project.getRegisteredBy())
                .teamName(project.getTeamName())
                .slug(project.getSlug())
                .title(project.getTitle())
                .tagline(project.getTagline())
                .serviceStatus(project.getServiceStatus())
                .approvalStatus(project.getApprovalStatus())
                .descriptionMd(project.getDescriptionMd())
                .githubRepositoryUrl(project.getGithubRepositoryUrl())
                .deploymentUrl(project.getDeploymentUrl())
                .thumbnailMediaId(project.getThumbnailMediaId())
                .build();
    }
}
