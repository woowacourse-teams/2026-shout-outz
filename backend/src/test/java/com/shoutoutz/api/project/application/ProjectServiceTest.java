package com.shoutoutz.api.project.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.cohort.domain.CohortErrorCode;
import com.shoutoutz.api.cohort.domain.InvalidCohortException;
import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.DuplicateEntityException;
import com.shoutoutz.api.media.domain.MediaMetadata;
import com.shoutoutz.api.media.domain.MediaMetadataRepository;
import com.shoutoutz.api.media.domain.MediaPurpose;
import com.shoutoutz.api.media.domain.MediaStatus;
import com.shoutoutz.api.project.application.dto.command.ProjectCreateCommand;
import com.shoutoutz.api.project.application.dto.result.ProjectCreateResult;
import com.shoutoutz.api.project.domain.InvalidProjectMemberException;
import com.shoutoutz.api.project.domain.Project;
import com.shoutoutz.api.project.domain.ProjectErrorCode;
import com.shoutoutz.api.project.domain.ProjectRegistrationForbiddenException;
import com.shoutoutz.api.project.domain.ProjectRepository;
import com.shoutoutz.api.project.domain.ServiceStatus;
import com.shoutoutz.api.project.domain.Slug;
import com.shoutoutz.api.techtag.domain.TechTag;
import com.shoutoutz.api.techtag.domain.TechTagRepository;
import com.shoutoutz.api.user.domain.account.User;
import com.shoutoutz.api.user.domain.account.UserRepository;
import com.shoutoutz.api.user.domain.account.UserRole;
import com.shoutoutz.api.user.domain.account.UserStatus;
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
class ProjectServiceTest {

    private static final long REGISTERED_BY = 7L;
    private static final long MEMBER_ID = 8L;
    private static final String MEMBER_HANDLE = "zzaekkii";
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

    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        projectService = new ProjectService(
                projectRepository,
                techTagRepository,
                mediaMetadataRepository,
                userProfileRepository,
                userRepository
        );
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

        ProjectCreateResult result = projectService.create(command(6, THUMBNAIL_ID, TECH_TAG_IDS));

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

        assertThatThrownBy(() -> projectService.create(command(99, null, TECH_TAG_IDS)))
                .isInstanceOfSatisfying(InvalidCohortException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(CohortErrorCode.INVALID_COHORT));

        verifyNoInteractions(projectRepository, techTagRepository, mediaMetadataRepository);
    }

    @Test
    @DisplayName("이미 등록된 리포지토리면 409를 던지고 저장하지 않는다.")
    void rejectsDuplicateSlug() {
        givenRegistrant(UserType.WOOWACOURSE_CREW);
        when(projectRepository.existsBySlug(new Slug("loop"))).thenReturn(true);

        assertThatThrownBy(() -> projectService.create(command(6, null, TECH_TAG_IDS)))
                .isInstanceOfSatisfying(DuplicateEntityException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ProjectErrorCode.PROJECT_DUPLICATE_SLUG));

        verify(projectRepository, never()).save(any(), anyList(), anyList());
    }

    @Test
    @DisplayName("기술 태그 id가 중복되면 태그를 조회하지 않고 400을 던진다.")
    void rejectsDuplicateTechTags() {
        givenRegistrant(UserType.WOOWACOURSE_CREW);
        when(projectRepository.existsBySlug(new Slug("loop"))).thenReturn(false);

        assertThatThrownBy(() -> projectService.create(command(6, null, List.of(1L, 1L))))
                .isInstanceOfSatisfying(BadRequestException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ProjectErrorCode.PROJECT_DUPLICATE_TECH_TAG));

        verifyNoInteractions(techTagRepository);
    }

    @Test
    @DisplayName("없거나 비활성인 기술 태그가 섞여 있으면 400을 던진다.")
    void rejectsUnselectableTechTags() {
        givenRegistrant(UserType.WOOWACOURSE_CREW);
        when(projectRepository.existsBySlug(new Slug("loop"))).thenReturn(false);
        when(techTagRepository.findAllActiveByIds(TECH_TAG_IDS)).thenReturn(activeTags(1L));

        assertThatThrownBy(() -> projectService.create(command(6, null, TECH_TAG_IDS)))
                .isInstanceOfSatisfying(BadRequestException.class,
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

        projectService.create(command(6, null, TECH_TAG_IDS));

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
                .thenReturn(Optional.of(thumbnail(REGISTERED_BY, MediaPurpose.POST_CONTENT, MediaStatus.READY)));

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

        ProjectCreateResult result = projectService.create(command(6, null, TECH_TAG_IDS));

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

        projectService.create(command(List.of("coach-jack", MEMBER_HANDLE)));

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
        assertThatThrownBy(() -> projectService.create(command(memberHandles)))
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
                .track(crew ? "BACKEND" : null)
                .cohort(crew ? (short) 6 : null)
                .build();
    }

    private void assertRegistrationForbidden() {
        assertThatThrownBy(() -> projectService.create(command(6, null, TECH_TAG_IDS)))
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
        assertThatThrownBy(() -> projectService.create(command(6, THUMBNAIL_ID, TECH_TAG_IDS)))
                .isInstanceOfSatisfying(BadRequestException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(expected));
        verify(projectRepository, never()).save(any(), anyList(), anyList());
    }

    private static ProjectCreateCommand command(List<String> memberHandles) {
        return command(6, null, TECH_TAG_IDS, memberHandles);
    }

    private static ProjectCreateCommand command(int cohort, Long thumbnailMediaId, List<Long> techTagIds) {
        return command(cohort, thumbnailMediaId, techTagIds, List.of(MEMBER_HANDLE));
    }

    private static ProjectCreateCommand command(
            int cohort,
            Long thumbnailMediaId,
            List<Long> techTagIds,
            List<String> memberHandles
    ) {
        return new ProjectCreateCommand(
                "루프 (Loop)",
                "루프팀",
                "스프린트 회고와 액션 아이템을 하나로 엮은 실시간 협업 도구",
                cohort,
                thumbnailMediaId,
                "https://github.com/woowacourse-teams/2026-loop",
                "https://loop.team",
                "## 문제",
                techTagIds,
                memberHandles,
                REGISTERED_BY
        );
    }

    private static List<TechTag> activeTags(Long... ids) {
        return java.util.Arrays.stream(ids)
                .map(id -> TechTag.builder().id(id).slug("tag-" + id).displayName("Tag" + id).active(true).build())
                .toList();
    }

    private static MediaMetadata thumbnail(Long uploadedBy, MediaPurpose purpose, MediaStatus status) {
        return MediaMetadata.reconstitute(
                THUMBNAIL_ID,
                uploadedBy,
                purpose,
                "media/project-thumbnail/12.webp",
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
