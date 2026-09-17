package com.shoutoutz.api.project.application;

import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_DESCRIPTION_MEDIA_NOT_READY;
import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_DUPLICATE_GITHUB_REPOSITORY;
import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_DUPLICATE_SLUG;
import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_DUPLICATE_TECH_TAG;
import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_INVALID_DESCRIPTION_MEDIA;
import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_INVALID_MEMBER;
import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_INVALID_TECH_TAG;
import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_INVALID_THUMBNAIL;
import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_NOT_FOUND;
import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_RESTORE_DEADLINE_EXPIRED;
import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_THUMBNAIL_NOT_READY;
import static com.shoutoutz.api.user.domain.account.UserErrorCode.USER_NOT_FOUND;

import com.shoutoutz.api.cohort.domain.Cohort;
import com.shoutoutz.api.common.exception.custom.ConflictException;
import com.shoutoutz.api.common.exception.custom.DuplicateEntityException;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.media.application.MediaUrlResolver;
import com.shoutoutz.api.media.domain.MediaMetadata;
import com.shoutoutz.api.media.domain.MediaMetadataRepository;
import com.shoutoutz.api.media.domain.MediaPurpose;
import com.shoutoutz.api.media.domain.MediaStatus;
import com.shoutoutz.api.media.infrastructure.s3.MediaVariant;
import com.shoutoutz.api.project.application.dto.UserProjectResult;
import com.shoutoutz.api.project.domain.ApprovalStatus;
import com.shoutoutz.api.project.domain.DeletedProject;
import com.shoutoutz.api.project.domain.DeploymentUrl;
import com.shoutoutz.api.project.domain.DescriptionMediaReferences;
import com.shoutoutz.api.project.domain.GithubRepositoryUrl;
import com.shoutoutz.api.project.domain.Project;
import com.shoutoutz.api.project.domain.ProjectCursor;
import com.shoutoutz.api.project.domain.ProjectDeletion;
import com.shoutoutz.api.project.domain.ProjectDeletionRepository;
import com.shoutoutz.api.project.domain.ProjectDetail;
import com.shoutoutz.api.project.domain.ProjectFilterCondition;
import com.shoutoutz.api.project.domain.ProjectFilterOptions;
import com.shoutoutz.api.project.domain.ProjectMembers;
import com.shoutoutz.api.project.domain.ProjectPage;
import com.shoutoutz.api.project.domain.ProjectRepository;
import com.shoutoutz.api.project.domain.ProjectSearchCondition;
import com.shoutoutz.api.project.domain.ProjectSort;
import com.shoutoutz.api.project.domain.RestorableProject;
import com.shoutoutz.api.project.domain.RestoredProject;
import com.shoutoutz.api.project.domain.Slug;
import com.shoutoutz.api.project.domain.TeamName;
import com.shoutoutz.api.project.domain.Title;
import com.shoutoutz.api.project.domain.exception.InvalidDescriptionMediaException;
import com.shoutoutz.api.project.domain.exception.InvalidProjectMemberException;
import com.shoutoutz.api.project.domain.exception.InvalidTechTagException;
import com.shoutoutz.api.project.domain.exception.InvalidThumbnailException;
import com.shoutoutz.api.project.domain.exception.ProjectRegistrationForbiddenException;
import com.shoutoutz.api.project.presentation.dto.request.ProjectCreateRequest;
import com.shoutoutz.api.project.presentation.dto.request.ProjectFilterOptionsRequest;
import com.shoutoutz.api.project.presentation.dto.request.ProjectFindAllRequest;
import com.shoutoutz.api.project.presentation.dto.request.ProjectUpdateRequest;
import com.shoutoutz.api.project.presentation.dto.request.UserProjectFindRequest;
import com.shoutoutz.api.project.presentation.dto.response.ProjectCreateResponse;
import com.shoutoutz.api.project.presentation.dto.response.ProjectDetailResponse;
import com.shoutoutz.api.project.presentation.dto.response.ProjectFilterOptionsResponse;
import com.shoutoutz.api.project.presentation.dto.response.ProjectFindAllResponse;
import com.shoutoutz.api.project.presentation.dto.response.ProjectUpdateResponse;
import com.shoutoutz.api.project.presentation.dto.response.UserProjectFindResponse;
import com.shoutoutz.api.techtag.domain.TechTagRepository;
import com.shoutoutz.api.user.domain.account.User;
import com.shoutoutz.api.user.domain.account.UserRepository;
import com.shoutoutz.api.user.domain.account.UserStatus;
import com.shoutoutz.api.user.domain.profile.UserProfile;
import com.shoutoutz.api.user.domain.profile.UserProfileRepository;
import com.shoutoutz.api.user.domain.profile.UserType;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final TechTagRepository techTagRepository;
    private final MediaMetadataRepository mediaMetadataRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;
    private final ProjectDeletionRepository projectDeletionRepository;
    private final UserProjectQueryRepository userProjectQueryRepository;
    private final MediaUrlResolver mediaUrlResolver;
    private final Clock clock;

    @Transactional
    public ProjectCreateResponse create(long registeredBy, ProjectCreateRequest request) {
        validateRegistrant(registeredBy);
        Project project = Project.register(
                Cohort.from(request.cohort()),
                registeredBy,
                new TeamName(request.teamName()),
                new Title(request.title()),
                request.tagline(),
                request.descriptionMd(),
                new GithubRepositoryUrl(request.githubRepositoryUrl()),
                request.deploymentUrl() == null ? null : new DeploymentUrl(request.deploymentUrl()),
                request.thumbnailMediaId()
        );
        validateGithubRepositoryNotDuplicated(project.getGithubRepositoryUrl());
        validateSlugNotDuplicated(project.getSlug());
        validateTechTags(request.techTagIds());
        validateThumbnail(request.thumbnailMediaId(), registeredBy);
        validateDescriptionMedia(request.descriptionMd(), registeredBy);
        List<Long> memberIds = request.memberHandles().stream()
                .map(this::resolveMemberId)
                .toList();
        ProjectMembers members = ProjectMembers.of(registeredBy, memberIds);

        Project savedProject = projectRepository.save(project, request.techTagIds(), members.getUserIds());
        return new ProjectCreateResponse(savedProject.getId(), savedProject.getSlug().value());
    }

    /**
     * 작성자가 프로젝트를 수정한다.
     * 반려된 프로젝트를 수정하면 재심사 요청으로 보고 승인 대기로 되돌린다.
     * 기술 스택과 팀원은 받은 목록으로 통째로 바꾼다.
     */
    @Transactional
    public ProjectUpdateResponse update(long projectId, long loginUserId, ProjectUpdateRequest request) {
        Project project = findOwnedProject(projectId, loginUserId);
        Long thumbnailMediaId = request.isThumbnailImageIdProvided()
                ? request.thumbnailImageId()
                : project.getThumbnailMediaId();
        String descriptionMd = normalizeDescriptionReferences(project, request.descriptionMd());
        Project updated = project.update(
                Cohort.from(request.cohort()),
                new TeamName(request.teamName()),
                new Title(request.title()),
                request.tagline(),
                descriptionMd,
                new GithubRepositoryUrl(request.githubRepositoryUrl()),
                request.deploymentUrl() == null ? null : new DeploymentUrl(request.deploymentUrl()),
                request.serviceStatus(),
                thumbnailMediaId
        );
        validateGithubRepositoryNotDuplicated(updated.getGithubRepositoryUrl(), projectId);
        validateTechTags(request.techTagIds(), projectRepository.findTechTagIds(projectId));
        if (request.isThumbnailImageIdProvided()) {
            validateThumbnail(request.thumbnailImageId(), loginUserId);
        }
        validateDescriptionMedia(descriptionMd, loginUserId);
        List<Long> memberIds = resolveMemberIds(request.memberHandles(), projectRepository.findMemberIds(projectId));
        ProjectMembers members = ProjectMembers.of(project.getRegisteredBy(), memberIds);

        Project savedProject = projectRepository.update(updated, request.techTagIds(), members.getUserIds());
        return ProjectUpdateResponse.from(savedProject);
    }

    /**
     * 승인된 프로젝트 목록을 검색어, 필터, 정렬 조건으로 한 페이지 조회한다.
     * 요청 값은 기본값과 검증을 거친 resolved 메서드로만 꺼내 쓴다.
     */
    @Transactional(readOnly = true)
    public ProjectFindAllResponse findAll(ProjectFindAllRequest request) {
        ProjectSort sort = request.resolvedSort();
        ProjectPage page = projectRepository.findAll(new ProjectSearchCondition(
                request.keyword(),
                request.resolvedCohorts(),
                request.resolvedTechTagIds(),
                sort,
                request.resolvedSize(),
                request.resolvedCursor()
        ));
        ProjectCursor nextCursor = page.nextCursor(sort);
        Map<Long, URI> mediaUrls = resolveProjectMediaUrls(page.items(), MediaVariant.THUMBNAIL);
        return ProjectFindAllResponse.of(
                page,
                nextCursor == null ? null : ProjectCursorCodec.encode(nextCursor),
                mediaUrls
        );
    }

    /**
     * 사용자가 참여한 승인 프로젝트를 최신순으로 조회한다.
     * 탈퇴한 사용자의 프로젝트는 공개하지 않는다.
     */
    @Transactional(readOnly = true)
    public UserProjectFindResponse findAllByUser(String handle, UserProjectFindRequest request) {
        User user = userRepository.findByHandle(handle)
                .orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND));
        if (user.isDeleted()) {
            return UserProjectFindResponse.from(new UserProjectResult(List.of(), false), Map.of());
        }

        UserProjectResult result = userProjectQueryRepository.findAllByUserId(
                user.getId(),
                request.resolvedCursor(),
                request.resolvedSize()
        );
        return UserProjectFindResponse.from(
                result,
                resolveProjectMediaUrls(result.projects(), MediaVariant.THUMBNAIL)
        );
    }

    /**
     * 필터 모달에 보여줄 기수 및 기술 스택 목록과, 각 항목을 골랐을 때 나오는 프로젝트 수를 조회한다.
     * 목록 조회와 숫자가 맞도록, 검색어와 필터는 목록 조회와 같은 방식으로 정리해 넘긴다.
     */
    @Transactional(readOnly = true)
    public ProjectFilterOptionsResponse findFilterOptions(ProjectFilterOptionsRequest request) {
        ProjectFilterOptions options = projectRepository.findFilterOptions(new ProjectFilterCondition(
                request.keyword(),
                request.resolvedCohorts(),
                request.resolvedTechTagIds()
        ));
        return ProjectFilterOptionsResponse.from(options);
    }

    /**
     * 승인된 프로젝트는 누구나, 승인되지 않은 프로젝트는 등록자만 조회할 수 있다.
     */
    @Transactional(readOnly = true)
    public ProjectDetailResponse findDetail(long projectId, Long loginUserId) {
        ProjectDetail detail = projectRepository.findDetailById(projectId, loginUserId)
                .filter(project -> project.isVisibleTo(loginUserId))
                .orElseThrow(() -> new EntityNotFoundException(PROJECT_NOT_FOUND));
        Map<Long, URI> mediaUrls = resolveProjectMediaUrls(detail);
        String descriptionMd = mediaUrlResolver.replaceDescriptionReferences(
                detail.descriptionMd(),
                mediaUrls
        );
        if (descriptionMd == null && detail.descriptionMd() != null) {
            descriptionMd = detail.descriptionMd();
        }
        return ProjectDetailResponse.from(
                detail,
                mediaUrls,
                descriptionMd
        );
    }

    private Map<Long, URI> resolveProjectMediaUrls(
            List<com.shoutoutz.api.project.domain.ProjectSummary> projects,
            MediaVariant variant
    ) {
        Set<Long> mediaIds = new HashSet<>();
        projects.forEach(project -> {
            if (project.thumbnailMediaId() != null) {
                mediaIds.add(project.thumbnailMediaId());
            }
            project.members().stream()
                    .map(com.shoutoutz.api.project.domain.ProjectMemberProfile::avatarImageId)
                    .filter(java.util.Objects::nonNull)
                    .forEach(mediaIds::add);
        });
        return mediaUrlResolver.resolveAll(mediaIds, variant);
    }

    private Map<Long, URI> resolveProjectMediaUrls(ProjectDetail detail) {
        Set<Long> mediaIds = new HashSet<>(
                DescriptionMediaReferences.extractMediaIds(detail.descriptionMd())
        );
        if (detail.thumbnailMediaId() != null) {
            mediaIds.add(detail.thumbnailMediaId());
        }
        detail.members().stream()
                .map(com.shoutoutz.api.project.domain.ProjectMemberProfile::avatarImageId)
                .filter(java.util.Objects::nonNull)
                .forEach(mediaIds::add);
        return mediaUrlResolver.resolveAll(mediaIds);
    }

    private String normalizeDescriptionReferences(Project project, String descriptionMd) {
        List<Long> existingMediaIds = DescriptionMediaReferences.extractMediaIds(project.getDescriptionMd());
        if (existingMediaIds.isEmpty()) {
            return descriptionMd;
        }
        Map<Long, URI> mediaUrls = mediaUrlResolver.resolveAll(existingMediaIds);
        return mediaUrlResolver.replaceDescriptionUrlsWithReferences(descriptionMd, mediaUrls);
    }

    /**
     * 등록자 본인만 자신의 프로젝트를 삭제할 수 있다. (심사 중이어도 삭제 가능)
     * 프로젝트 삭제와 이력 기록은 같은 시각을 쓰고 한 트랜잭션으로 처리한다.
     */
    @Transactional
    public ProjectDeletion delete(long projectId, long registeredBy) {
        Instant deletedAt = clock.instant();
        DeletedProject deletedProject = projectRepository.softDelete(projectId, registeredBy, deletedAt)
                .orElseThrow(() -> new EntityNotFoundException(PROJECT_NOT_FOUND));
        return projectDeletionRepository.save(ProjectDeletion.selfDelete(deletedProject, registeredBy, deletedAt));
    }

    /**
     * 등록자 본인만 복구 기한 안에 자신의 프로젝트를 복구할 수 있다. 승인 상태는 삭제 이전 값을 그대로 유지한다.
     * 프로젝트 복구와 이력 기록은 같은 시각을 쓰고 한 트랜잭션으로 처리한다.
     */
    @Transactional
    public RestoredProject restore(long projectId, long registeredBy) {
        Instant restoredAt = clock.instant();
        RestorableProject restorable = projectRepository.findRestorable(projectId, registeredBy)
                .orElseThrow(() -> new EntityNotFoundException(PROJECT_NOT_FOUND));
        if (!restorable.isRestorable(restoredAt)) {
            throw new ConflictException(PROJECT_RESTORE_DEADLINE_EXPIRED);
        }
        ApprovalStatus approvalStatus = projectRepository.restore(projectId, restoredAt)
                .orElseThrow(() -> new EntityNotFoundException(PROJECT_NOT_FOUND));
        projectDeletionRepository.markRestored(restorable.deletionId(), registeredBy, restoredAt);
        return new RestoredProject(projectId, approvalStatus, restoredAt);
    }

    /**
     * 작성자만 수정할 수 있다.
     */
    private Project findOwnedProject(long projectId, long loginUserId) {
        return projectRepository.findActiveById(projectId)
                .filter(project -> project.isRegisteredBy(loginUserId))
                .orElseThrow(() -> new EntityNotFoundException(PROJECT_NOT_FOUND));
    }

    /**
     * 우아한테크코스 크루와 코치만 프로젝트를 등록할 수 있다.
     */
    private void validateRegistrant(Long registeredBy) {
        userProfileRepository.findByUserId(registeredBy)
                .map(UserProfile::getUserType)
                .filter(ProjectService::isWoowacourseMember)
                .orElseThrow(ProjectRegistrationForbiddenException::new);
    }

    /**
     * 프로젝트 등록자와 팀원은 우아한테크코스 크루나 코치여야 한다.
     */
    private static boolean isWoowacourseMember(UserType userType) {
        return userType == UserType.WOOWACOURSE_CREW || userType == UserType.WOOWACOURSE_COACH;
    }

    /**
     * 같은 리포지토리를 두 프로젝트가 가리킬 수 없다.
     * slug 검사보다 먼저 해, 리포지토리가 겹칠 때 주소 중복이 아니라 리포지토리 중복으로 응답한다.
     */
    private void validateGithubRepositoryNotDuplicated(GithubRepositoryUrl githubRepositoryUrl) {
        if (projectRepository.existsByGithubRepositoryUrl(githubRepositoryUrl)) {
            throw new DuplicateEntityException(PROJECT_DUPLICATE_GITHUB_REPOSITORY);
        }
    }

    /**
     * 수정 중인 프로젝트 자신은 중복으로 보지 않는다. 리포지토리를 그대로 두는 수정이 막히기 때문이다.
     */
    private void validateGithubRepositoryNotDuplicated(GithubRepositoryUrl githubRepositoryUrl, long projectId) {
        if (projectRepository.existsByGithubRepositoryUrlExcluding(githubRepositoryUrl, projectId)) {
            throw new DuplicateEntityException(PROJECT_DUPLICATE_GITHUB_REPOSITORY);
        }
    }

    /**
     * owner가 달라도 리포지토리 이름이 같으면 slug 가 겹치므로, 리포지토리 중복과 따로 검사한다.
     */
    private void validateSlugNotDuplicated(Slug slug) {
        if (projectRepository.existsBySlug(slug)) {
            throw new DuplicateEntityException(PROJECT_DUPLICATE_SLUG);
        }
    }

    /**
     * 중복이 없고, 모든 id가 선택 가능한 태그여야 한다.
     * 없는 id나 비활성 태그는 조회 결과에서 빠지므로 개수로 비교한다.
     */
    private void validateTechTags(List<Long> techTagIds) {
        validateTechTagsNotDuplicated(techTagIds);
        validateTechTagsSelectable(techTagIds);
    }

    /**
     * 이미 달려 있던 태그는 그 사이 비활성화됐더라도 목록에 그대로 둘 수 있다.
     * 비활성화된 태그 하나 때문에 프로젝트 수정 자체가 막히기 때문이다.
     * 새로 추가하는 태그만 선택 가능한지 확인한다.
     */
    private void validateTechTags(List<Long> techTagIds, List<Long> currentTechTagIds) {
        validateTechTagsNotDuplicated(techTagIds);
        validateTechTagsSelectable(techTagIds.stream()
                .filter(techTagId -> !currentTechTagIds.contains(techTagId))
                .toList());
    }

    private void validateTechTagsNotDuplicated(List<Long> techTagIds) {
        if (new HashSet<>(techTagIds).size() != techTagIds.size()) {
            throw new InvalidTechTagException(PROJECT_DUPLICATE_TECH_TAG);
        }
    }

    private void validateTechTagsSelectable(List<Long> techTagIds) {
        if (techTagIds.isEmpty()) {
            return;
        }
        if (techTagRepository.findAllActiveByIds(techTagIds).size() != techTagIds.size()) {
            throw new InvalidTechTagException(PROJECT_INVALID_TECH_TAG);
        }
    }

    /**
     * 썸네일은 선택 입력이다. 입력했다면 본인이 올린 프로젝트 썸네일 용도의 처리 완료 이미지여야 한다.
     * 없는 이미지, 다른 사람의 이미지, 다른 용도의 이미지는 같은 에러로 응답해, 다른 사용자의 미디어 id 존재 여부가 드러나지 않게 한다.
     */
    private void validateThumbnail(Long thumbnailMediaId, Long registeredBy) {
        if (thumbnailMediaId == null) {
            return;
        }
        MediaMetadata thumbnail = findUploadedMedia(thumbnailMediaId, registeredBy, MediaPurpose.PROJECT_THUMBNAIL)
                .orElseThrow(() -> new InvalidThumbnailException(PROJECT_INVALID_THUMBNAIL));
        if (thumbnail.getStatus() != MediaStatus.READY) {
            throw new InvalidThumbnailException(PROJECT_THUMBNAIL_NOT_READY);
        }
    }

    /**
     * 본문 이미지는 Markdown 안에 media://{mediaId} 형식으로 참조한다.
     * 참조한 이미지는 모두 등록자가 본문 용도로 올린 처리 완료 이미지여야 한다.
     * 검증하지 않으면 다른 사용자의 비공개 이미지를 본문에 참조해 노출할 수 있다.
     */
    private void validateDescriptionMedia(String descriptionMd, Long registeredBy) {
        for (Long mediaId : DescriptionMediaReferences.extractMediaIds(descriptionMd)) {
            MediaMetadata media = findUploadedMedia(mediaId, registeredBy, MediaPurpose.PROJECT_DESCRIPTION)
                    .orElseThrow(() -> new InvalidDescriptionMediaException(PROJECT_INVALID_DESCRIPTION_MEDIA));
            if (media.getStatus() != MediaStatus.READY) {
                throw new InvalidDescriptionMediaException(PROJECT_DESCRIPTION_MEDIA_NOT_READY);
            }
        }
    }

    /**
     * 등록자가 해당 용도로 올린 미디어만 찾는다.
     * 없는 이미지, 다른 사람의 이미지, 다른 용도의 이미지는 모두 비어 있는 결과로 돌려, 다른 사용자의 미디어 id 존재 여부가 드러나지 않게 한다.
     */
    private Optional<MediaMetadata> findUploadedMedia(Long mediaId, Long uploadedBy, MediaPurpose purpose) {
        return mediaMetadataRepository.findById(mediaId)
                .filter(media -> uploadedBy.equals(media.getUploadedBy()))
                .filter(media -> media.getPurpose() == purpose);
    }

    /**
     * 팀원 handle을 사용자 id로 바꾼다. 팀원은 활동 중인 우아한테크코스 크루나 코치여야 한다.
     * handle 조회는 대소문자를 구분하지 않으므로, 대소문자만 다른 handle은 같은 사용자로 조회되어 ProjectMembers 에서 중복으로 걸러진다.
     */
    private Long resolveMemberId(String handle) {
        User member = findUser(handle);
        validateSelectableMember(member);
        return member.getId();
    }

    /**
     * 이미 팀원인 사용자는 그 사이 탈퇴했더라도 목록에 그대로 둘 수 있다.
     * 탈퇴한 팀원 한 명 때문에 프로젝트 수정 자체가 막히기 때문이다.
     * 새로 추가하는 팀원만 활동 중인 크루나 코치인지 확인한다.
     */
    private List<Long> resolveMemberIds(List<String> handles, List<Long> currentMemberIds) {
        return handles.stream()
                .map(handle -> resolveMemberId(handle, currentMemberIds))
                .toList();
    }

    private Long resolveMemberId(String handle, List<Long> currentMemberIds) {
        User member = findUser(handle);
        if (currentMemberIds.contains(member.getId())) {
            return member.getId();
        }
        validateSelectableMember(member);
        return member.getId();
    }

    private User findUser(String handle) {
        return userRepository.findByHandle(handle)
                .orElseThrow(() -> new InvalidProjectMemberException(PROJECT_INVALID_MEMBER));
    }

    private void validateSelectableMember(User member) {
        if (member.getStatus() != UserStatus.ACTIVE) {
            throw new InvalidProjectMemberException(PROJECT_INVALID_MEMBER);
        }
        validateWoowacourseMember(member.getId());
    }

    private void validateWoowacourseMember(Long userId) {
        userProfileRepository.findByUserId(userId)
                .map(UserProfile::getUserType)
                .filter(ProjectService::isWoowacourseMember)
                .orElseThrow(() -> new InvalidProjectMemberException(PROJECT_INVALID_MEMBER));
    }
}
