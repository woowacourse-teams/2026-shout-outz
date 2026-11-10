package com.shoutoutz.api.projectresource.application;

import com.shoutoutz.api.projectresource.domain.ProjectTopicCounts;
import com.shoutoutz.api.projectresource.presentation.dto.response.ProjectTopicsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 탐색 화면 프로젝트 탭의 기술 주제 목록. 로그인 없이 보며, 수는 다른 조건과 상관없는 전체 기준이다.
 */
@Service
@RequiredArgsConstructor
public class ProjectTopicService {

    private final ProjectResourceQueryRepository projectResourceQueryRepository;

    @Transactional(readOnly = true)
    public ProjectTopicsResponse findTopics() {
        ProjectTopicCounts counts = ProjectTopicCounts.of(
                projectResourceQueryRepository.findTopics(),
                projectResourceQueryRepository.findTopicProjects()
        );
        return ProjectTopicsResponse.from(counts);
    }
}
