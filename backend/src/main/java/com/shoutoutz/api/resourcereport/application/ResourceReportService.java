package com.shoutoutz.api.resourcereport.application;

import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_NOT_FOUND;
import static com.shoutoutz.api.resourcereport.domain.ResourceReportErrorCode.RESOURCE_REPORT_FORBIDDEN;
import static com.shoutoutz.api.resourcereport.domain.ResourceReportErrorCode.RESOURCE_REPORT_RESOURCE_NOT_FOUND;
import static com.shoutoutz.api.resourcereport.domain.ResourceReportErrorCode.RESOURCE_REPORT_RESOURCE_NOT_REMOVABLE;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.project.application.ProjectSlugResolver;
import com.shoutoutz.api.project.domain.ProjectRepository;
import com.shoutoutz.api.resourcereport.application.dto.ReportableResource;
import com.shoutoutz.api.resourcereport.domain.ResourceReport;
import com.shoutoutz.api.resourcereport.domain.ResourceReportRepository;
import com.shoutoutz.api.resourcereport.domain.ResourceReportUrl;
import com.shoutoutz.api.resourcereport.presentation.dto.request.ResourceAdditionReportRequest;
import com.shoutoutz.api.resourcereport.presentation.dto.request.ResourceRemovalReportRequest;
import com.shoutoutz.api.resourcereport.presentation.dto.response.MyResourceReportResponse;
import com.shoutoutz.api.resourcereport.presentation.dto.response.ResourceAdditionReportResponse;
import com.shoutoutz.api.resourcereport.presentation.dto.response.ResourceRemovalReportResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 프로젝트 기술 이야기 자료 제보.
 * 제보는 그 프로젝트 팀원만 보낼 수 있다(프로토타입의 '팀 소속자만 가능합니다' 안내). 크루 인증 여부는 보지 않는다.
 */
@Service
@RequiredArgsConstructor
public class ResourceReportService {

    private final ProjectSlugResolver projectSlugResolver;
    private final ProjectRepository projectRepository;
    private final ResourceReportRepository resourceReportRepository;
    private final ResourceReportQueryRepository resourceReportQueryRepository;
    private final Clock clock;

    /**
     * 고른 자료를 모두 확인한 뒤에 저장하므로, 하나라도 제보할 수 없는 자료가 섞이면 아무것도 저장하지 않는다.
     * 응답은 요청 순서(중복 제외)대로 준다.
     */
    @Transactional
    public List<ResourceRemovalReportResponse> reportRemovals(
            String slug,
            long userId,
            ResourceRemovalReportRequest request
    ) {
        long projectId = findPublicProjectId(slug);
        validateMember(projectId, userId);
        List<Long> resourceIds = request.distinctResourceIds();
        Map<Long, ReportableResource> resources = resourceReportQueryRepository.findResources(resourceIds);
        List<ReportableResource> targets = resourceIds.stream()
                .map(resourceId -> findRemovableResource(resources, resourceId, projectId))
                .toList();

        Instant now = clock.instant();
        return targets.stream()
                .map(target -> new ResourceRemovalReportResponse(
                        resourceReportRepository.saveIfAbsent(ResourceReport.removal(
                                projectId,
                                userId,
                                target.source(),
                                target.externalKey(),
                                request.memo(),
                                now
                        )),
                        target.resourceId()
                ))
                .toList();
    }

    @Transactional
    public ResourceAdditionReportResponse reportAddition(
            String slug,
            long userId,
            ResourceAdditionReportRequest request
    ) {
        long projectId = findPublicProjectId(slug);
        validateMember(projectId, userId);
        String repositoryUrl = projectRepository.findActiveById(projectId)
                .map(project -> project.getGithubRepositoryUrl().value())
                .orElseThrow(() -> new EntityNotFoundException(PROJECT_NOT_FOUND));
        ResourceReport report = ResourceReport.addition(
                projectId,
                userId,
                ResourceReportUrl.of(request.url(), repositoryUrl),
                request.memo(),
                clock.instant()
        );
        return new ResourceAdditionReportResponse(resourceReportRepository.saveIfAbsent(report));
    }

    /**
     * 내가 이 프로젝트에 보낸 제보를 최신순으로 준다. 팀원이 아니어도 부를 수 있고, 보낸 제보가 없으면 빈 목록이다.
     */
    @Transactional(readOnly = true)
    public List<MyResourceReportResponse> findMine(String slug, long userId) {
        long projectId = findPublicProjectId(slug);
        return resourceReportQueryRepository.findMine(projectId, userId).stream()
                .map(MyResourceReportResponse::from)
                .toList();
    }

    /**
     * 승인되고 삭제되지 않은 프로젝트만 제보를 받는다. 그 밖의 프로젝트는 상세 화면처럼 없는 프로젝트로 본다.
     */
    private long findPublicProjectId(String slug) {
        long projectId = projectSlugResolver.resolveId(slug);
        if (!projectRepository.existsPublicById(projectId)) {
            throw new EntityNotFoundException(PROJECT_NOT_FOUND);
        }
        return projectId;
    }

    private void validateMember(long projectId, long userId) {
        if (!resourceReportQueryRepository.isProjectMember(projectId, userId)) {
            throw new ForbiddenException(RESOURCE_REPORT_FORBIDDEN);
        }
    }

    private ReportableResource findRemovableResource(
            Map<Long, ReportableResource> resources,
            long resourceId,
            long projectId
    ) {
        ReportableResource resource = resources.get(resourceId);
        if (resource == null) {
            throw new EntityNotFoundException(RESOURCE_REPORT_RESOURCE_NOT_FOUND);
        }
        if (!resource.belongsTo(projectId) || !ResourceReport.isRemovableSource(resource.source())) {
            throw new BadRequestException(RESOURCE_REPORT_RESOURCE_NOT_REMOVABLE);
        }
        return resource;
    }
}
