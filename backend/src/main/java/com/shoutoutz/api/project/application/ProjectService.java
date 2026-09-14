package com.shoutoutz.api.project.application;

import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_DESCRIPTION_MEDIA_NOT_READY;
import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_DUPLICATE_SLUG;
import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_DUPLICATE_TECH_TAG;
import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_INVALID_DESCRIPTION_MEDIA;
import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_INVALID_MEMBER;
import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_INVALID_TECH_TAG;
import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_INVALID_THUMBNAIL;
import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_THUMBNAIL_NOT_READY;

import com.shoutoutz.api.cohort.domain.Cohort;
import com.shoutoutz.api.common.exception.custom.DuplicateEntityException;
import com.shoutoutz.api.media.domain.MediaMetadata;
import com.shoutoutz.api.media.domain.MediaMetadataRepository;
import com.shoutoutz.api.media.domain.MediaPurpose;
import com.shoutoutz.api.media.domain.MediaStatus;
import com.shoutoutz.api.project.application.dto.command.ProjectCreateCommand;
import com.shoutoutz.api.project.application.dto.result.ProjectCreateResult;
import com.shoutoutz.api.project.domain.DeploymentUrl;
import com.shoutoutz.api.project.domain.DescriptionMediaReferences;
import com.shoutoutz.api.project.domain.GithubRepositoryUrl;
import com.shoutoutz.api.project.domain.Project;
import com.shoutoutz.api.project.domain.ProjectMembers;
import com.shoutoutz.api.project.domain.ProjectRepository;
import com.shoutoutz.api.project.domain.Slug;
import com.shoutoutz.api.project.domain.TeamName;
import com.shoutoutz.api.project.domain.Title;
import com.shoutoutz.api.project.domain.exception.InvalidDescriptionMediaException;
import com.shoutoutz.api.project.domain.exception.InvalidProjectMemberException;
import com.shoutoutz.api.project.domain.exception.InvalidTechTagException;
import com.shoutoutz.api.project.domain.exception.InvalidThumbnailException;
import com.shoutoutz.api.project.domain.exception.ProjectRegistrationForbiddenException;
import com.shoutoutz.api.techtag.domain.TechTagRepository;
import com.shoutoutz.api.user.domain.account.User;
import com.shoutoutz.api.user.domain.account.UserRepository;
import com.shoutoutz.api.user.domain.account.UserStatus;
import com.shoutoutz.api.user.domain.profile.UserProfile;
import com.shoutoutz.api.user.domain.profile.UserProfileRepository;
import com.shoutoutz.api.user.domain.profile.UserType;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
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

    @Transactional
    public ProjectCreateResult create(ProjectCreateCommand command) {
        validateRegistrant(command.registeredBy());
        Project project = Project.register(
                Cohort.from(command.cohort()),
                command.registeredBy(),
                new TeamName(command.teamName()),
                new Title(command.title()),
                command.tagline(),
                command.descriptionMd(),
                new GithubRepositoryUrl(command.githubRepositoryUrl()),
                command.deploymentUrl() == null ? null : new DeploymentUrl(command.deploymentUrl()),
                command.thumbnailMediaId()
        );
        validateSlugNotDuplicated(project.getSlug());
        validateTechTags(command.techTagIds());
        validateThumbnail(command.thumbnailMediaId(), command.registeredBy());
        validateDescriptionMedia(command.descriptionMd(), command.registeredBy());
        List<Long> memberIds = command.memberHandles().stream()
                .map(this::resolveMemberId)
                .toList();
        ProjectMembers members = ProjectMembers.of(command.registeredBy(), memberIds);

        Project savedProject = projectRepository.save(project, command.techTagIds(), members.getUserIds());
        return new ProjectCreateResult(savedProject.getId(), savedProject.getSlug().value());
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
        if (new HashSet<>(techTagIds).size() != techTagIds.size()) {
            throw new InvalidTechTagException(PROJECT_DUPLICATE_TECH_TAG);
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
        User member = findActiveUser(handle);
        validateWoowacourseMember(member.getId());
        return member.getId();
    }

    private User findActiveUser(String handle) {
        return userRepository.findByHandle(handle)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new InvalidProjectMemberException(PROJECT_INVALID_MEMBER));
    }

    private void validateWoowacourseMember(Long userId) {
        userProfileRepository.findByUserId(userId)
                .map(UserProfile::getUserType)
                .filter(ProjectService::isWoowacourseMember)
                .orElseThrow(() -> new InvalidProjectMemberException(PROJECT_INVALID_MEMBER));
    }
}
