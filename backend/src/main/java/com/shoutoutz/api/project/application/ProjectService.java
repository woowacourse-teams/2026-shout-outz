package com.shoutoutz.api.project.application;

import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_DUPLICATE_SLUG;
import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_DUPLICATE_TECH_TAG;
import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_INVALID_TECH_TAG;
import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_INVALID_THUMBNAIL;
import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_THUMBNAIL_NOT_READY;

import com.shoutoutz.api.cohort.domain.Cohort;
import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.DuplicateEntityException;
import com.shoutoutz.api.media.domain.MediaMetadata;
import com.shoutoutz.api.media.domain.MediaMetadataRepository;
import com.shoutoutz.api.media.domain.MediaPurpose;
import com.shoutoutz.api.media.domain.MediaStatus;
import com.shoutoutz.api.project.application.dto.command.ProjectCreateCommand;
import com.shoutoutz.api.project.application.dto.result.ProjectCreateResult;
import com.shoutoutz.api.project.domain.DeploymentUrl;
import com.shoutoutz.api.project.domain.GithubRepositoryUrl;
import com.shoutoutz.api.project.domain.Project;
import com.shoutoutz.api.project.domain.ProjectRepository;
import com.shoutoutz.api.project.domain.Slug;
import com.shoutoutz.api.project.domain.TeamName;
import com.shoutoutz.api.project.domain.Title;
import com.shoutoutz.api.techtag.domain.TechTagRepository;
import java.util.HashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final TechTagRepository techTagRepository;
    private final MediaMetadataRepository mediaMetadataRepository;

    @Transactional
    public ProjectCreateResult create(ProjectCreateCommand command) {
        Project project = Project.register(
                Cohort.from(command.cohort()),
                command.registeredBy(),
                new TeamName(command.teamName()),
                new Title(command.title()),
                command.tagline(),
                command.descriptionMd(),
                new GithubRepositoryUrl(command.githubRepositoryUrl()),
                DeploymentUrl.fromNullable(command.deploymentUrl()),
                command.thumbnailMediaId()
        );
        validateSlugNotDuplicated(project.getSlug());
        validateTechTags(command.techTagIds());
        validateThumbnail(command.thumbnailMediaId(), command.registeredBy());
        List<Long> memberIds = resolveMemberIds(command.registeredBy(), command.memberHandles());

        Project savedProject = projectRepository.save(project, command.techTagIds(), memberIds);
        return new ProjectCreateResult(savedProject.getId(), savedProject.getSlug().value());
    }

    private void validateSlugNotDuplicated(Slug slug) {
        if (projectRepository.existsBySlug(slug)) {
            throw new DuplicateEntityException(PROJECT_DUPLICATE_SLUG);
        }
    }

    /**
     * 중복이 없고, 모든 id 가 선택 가능한 태그여야 한다.
     * 없는 id 나 비활성 태그는 조회 결과에서 빠지므로 개수로 비교한다.
     */
    private void validateTechTags(List<Long> techTagIds) {
        if (new HashSet<>(techTagIds).size() != techTagIds.size()) {
            throw new BadRequestException(PROJECT_DUPLICATE_TECH_TAG);
        }
        if (techTagRepository.findAllActiveByIds(techTagIds).size() != techTagIds.size()) {
            throw new BadRequestException(PROJECT_INVALID_TECH_TAG);
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
        MediaMetadata thumbnail = mediaMetadataRepository.findById(thumbnailMediaId)
                .filter(media -> registeredBy.equals(media.getUploadedBy()))
                .filter(media -> media.getPurpose() == MediaPurpose.PROJECT_THUMBNAIL)
                .orElseThrow(() -> new BadRequestException(PROJECT_INVALID_THUMBNAIL));
        if (thumbnail.getStatus() != MediaStatus.READY) {
            throw new BadRequestException(PROJECT_THUMBNAIL_NOT_READY);
        }
    }

    /**
     * TODO: 사용자 프로필 API(PR #91) 머지 후 memberHandles 를 사용자 id 로 변환하고 검증한다.
     *  - 존재하는 ACTIVE 사용자이고 WOOWACOURSE_CREW 여야 한다
     *  - 대소문자를 무시하고 중복이면 400, 등록자 본인이 포함되면 400
     *  - 등록자를 0 번에 두고 memberHandles 순서대로 이어 붙인다
     * 현재는 등록자만 팀원으로 저장한다.
     */
    private List<Long> resolveMemberIds(Long registeredBy, List<String> memberHandles) {
        return List.of(registeredBy);
    }
}
