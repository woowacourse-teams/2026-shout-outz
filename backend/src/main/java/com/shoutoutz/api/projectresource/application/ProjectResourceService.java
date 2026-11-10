package com.shoutoutz.api.projectresource.application;

import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_NOT_FOUND;
import static com.shoutoutz.api.projectresource.domain.ProjectResourceErrorCode.RESOURCE_TOPIC_NOT_FOUND;

import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.project.application.ProjectSlugResolver;
import com.shoutoutz.api.project.domain.ProjectRepository;
import com.shoutoutz.api.projectresource.domain.CareerCard;
import com.shoutoutz.api.projectresource.domain.ProjectResourceJobRole;
import com.shoutoutz.api.projectresource.domain.ProjectResourceTopics;
import com.shoutoutz.api.projectresource.domain.ResourceTopic;
import com.shoutoutz.api.projectresource.presentation.dto.response.ProjectCareerCardResponse;
import com.shoutoutz.api.projectresource.presentation.dto.response.ProjectResourceTopicsResponse;
import com.shoutoutz.api.projectresource.presentation.dto.response.ProjectTopicResourcesResponse;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceCard;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 프로젝트 상세 기술 이야기 탭. 로그인 없이 보며, 공개된 프로젝트만 조회한다.
 */
@Service
@RequiredArgsConstructor
public class ProjectResourceService {

    /**
     * 다른 팀 자료는 점수순 상위 20건까지만 준다. 몇 개를 보여 줄지는 프론트가 정한다(기술 자료 검색의 섹션 20건과 같다).
     */
    static final int OTHER_TEAM_RESOURCE_LIMIT = 20;

    private final ProjectSlugResolver projectSlugResolver;
    private final ProjectRepository projectRepository;
    private final ProjectResourceQueryRepository projectResourceQueryRepository;

    @Transactional(readOnly = true)
    public ProjectResourceTopicsResponse findTopics(String slug, ProjectResourceJobRole jobRole) {
        long projectId = findPublicProjectId(slug);
        ProjectResourceTopics topics = ProjectResourceTopics.of(
                projectResourceQueryRepository.findTopics(),
                projectResourceQueryRepository.countTeamResources(projectId)
        );
        return ProjectResourceTopicsResponse.of(topics, jobRole);
    }

    /**
     * 카탈로그에 없는 주제는 404 로 응답한다. 카탈로그에는 있지만 이 프로젝트 자료가 없는 주제는 이 팀 자료만 빈 목록이다.
     */
    @Transactional(readOnly = true)
    public ProjectTopicResourcesResponse findResources(String slug, String topicId, ProjectResourceJobRole jobRole) {
        long projectId = findPublicProjectId(slug);
        ResourceTopic topic = projectResourceQueryRepository.findTopic(topicId)
                .orElseThrow(() -> new EntityNotFoundException(RESOURCE_TOPIC_NOT_FOUND));
        return ProjectTopicResourcesResponse.of(
                topic,
                projectResourceQueryRepository.findTeamResources(projectId, topicId, jobRole),
                projectResourceQueryRepository.findTalks(topicId),
                projectResourceQueryRepository.findOtherTeamResources(
                        projectId,
                        topicId,
                        jobRole,
                        OTHER_TEAM_RESOURCE_LIMIT
                )
        );
    }

    /**
     * 전체 경험 탭의 경력 카드. 근거가 하나도 남지 않은 카드는 숨긴다.
     * 원천에서 사라진 콘텐츠는 카드 출처까지 내린다는 원칙(2026-11-09)과 같은 방향이다.
     */
    @Transactional(readOnly = true)
    public List<ProjectCareerCardResponse> findCareerCards(String slug) {
        long projectId = findPublicProjectId(slug);
        List<CareerCard> cards = projectResourceQueryRepository.findCareerCards(projectId);
        if (cards.isEmpty()) {
            return List.of();
        }
        Map<Long, List<ResourceCard>> sourcesByCard = projectResourceQueryRepository.findCareerCardSources(
                cards.stream().map(CareerCard::cardId).toList()
        );
        return cards.stream()
                .filter(card -> !sourcesByCard.getOrDefault(card.cardId(), List.of()).isEmpty())
                .map(card -> ProjectCareerCardResponse.of(card, sourcesByCard.get(card.cardId())))
                .toList();
    }

    /**
     * 승인되고 삭제되지 않은 프로젝트만 보여 준다. 그 밖의 프로젝트는 상세 화면처럼 없는 프로젝트로 본다.
     */
    private long findPublicProjectId(String slug) {
        long projectId = projectSlugResolver.resolveId(slug);
        if (!projectRepository.existsPublicById(projectId)) {
            throw new EntityNotFoundException(PROJECT_NOT_FOUND);
        }
        return projectId;
    }
}
