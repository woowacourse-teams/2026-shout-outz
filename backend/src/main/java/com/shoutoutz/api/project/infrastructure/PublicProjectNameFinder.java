package com.shoutoutz.api.project.infrastructure;

import com.shoutoutz.api.project.domain.NormalizedKeyword;
import com.shoutoutz.api.project.domain.ProjectRepository;
import com.shoutoutz.api.project.domain.PublicProjectName;
import com.shoutoutz.api.resourcesearch.application.ProjectNameFinder;
import com.shoutoutz.api.resourcesearch.application.dto.MatchedProject;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 프로젝트 탭 검색과 같은 이름 비교 규칙으로 자료 검색어가 가리키는 프로젝트를 찾는다.
 */
@Component
@RequiredArgsConstructor
public class PublicProjectNameFinder implements ProjectNameFinder {

    private final ProjectRepository projectRepository;

    @Override
    public List<MatchedProject> findNamed(String keyword) {
        NormalizedKeyword normalized = NormalizedKeyword.of(keyword);
        if (normalized.isEmpty()) {
            return List.of();
        }
        return PublicProjectName.pointedBy(projectRepository.findPublicProjectNames(), normalized).stream()
                .map(project -> new MatchedProject(project.id(), project.title(), project.slug()))
                .toList();
    }
}
