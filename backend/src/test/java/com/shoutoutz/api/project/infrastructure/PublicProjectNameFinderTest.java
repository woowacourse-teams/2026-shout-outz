package com.shoutoutz.api.project.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import com.shoutoutz.api.project.domain.ProjectRepository;
import com.shoutoutz.api.project.domain.PublicProjectName;
import com.shoutoutz.api.resourcesearch.application.dto.MatchedProject;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PublicProjectNameFinderTest {

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private PublicProjectNameFinder publicProjectNameFinder;

    private void givenPublicProjects() {
        given(projectRepository.findPublicProjectNames()).willReturn(List.of(
                new PublicProjectName(1L, "Rilog.", "rilog", "https://github.com/woowacourse-teams/2026-rilog"),
                new PublicProjectName(2L, "Rilog 블로그", "rilog-blog", "https://github.com/woowacourse-teams/2026-rilog-blog"),
                new PublicProjectName(13L, "히어릿", "hearit", "https://github.com/woowacourse-teams/2025-hearit"),
                new PublicProjectName(20L, "리로그", "rilog-2025", "https://github.com/woowacourse-teams/2025-rilog")
        ));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Rilog", "rilog.", "RILOG"})
    @DisplayName("표기만 다른 검색어는 같은 이름의 프로젝트를 가리키고, 이름이 같은 프로젝트가 여럿이면 모두 찾는다")
    void findsEveryProjectWithSameName(String keyword) {
        givenPublicProjects();

        assertThat(publicProjectNameFinder.findNamed(keyword)).containsExactly(
                new MatchedProject(1L, "Rilog.", "rilog"),
                new MatchedProject(20L, "리로그", "rilog-2025")
        );
    }

    @Test
    @DisplayName("저장소 이름으로도 프로젝트를 찾고, 프로젝트 이름과 slug를 담는다")
    void findsByRepositoryName() {
        givenPublicProjects();

        assertThat(publicProjectNameFinder.findNamed("hearit"))
                .containsExactly(new MatchedProject(13L, "히어릿", "hearit"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"log", "Rilog 배포", "로깅"})
    @DisplayName("이름의 일부이거나 다른 단어가 붙은 검색어는 프로젝트를 가리키지 않는다")
    void findsNothingForPartialName(String keyword) {
        givenPublicProjects();

        assertThat(publicProjectNameFinder.findNamed(keyword)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"?!", "  ", "..."})
    @DisplayName("글자나 숫자가 없는 검색어는 프로젝트 이름과 비교하지 않는다")
    void findsNothingWithoutLetters(String keyword) {
        assertThat(publicProjectNameFinder.findNamed(keyword)).isEmpty();

        verifyNoInteractions(projectRepository);
    }
}
