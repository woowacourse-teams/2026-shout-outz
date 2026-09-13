package com.shoutoutz.api.project.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.cohort.domain.Cohort;
import com.shoutoutz.api.common.exception.custom.DuplicateEntityException;
import com.shoutoutz.api.project.domain.GithubRepositoryUrl;
import com.shoutoutz.api.project.domain.Project;
import com.shoutoutz.api.project.domain.ProjectErrorCode;
import com.shoutoutz.api.project.domain.ProjectRepository;
import com.shoutoutz.api.project.domain.TeamName;
import com.shoutoutz.api.project.domain.Title;
import com.shoutoutz.api.user.domain.User;
import com.shoutoutz.api.user.domain.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class ProjectRepositoryIntegrationTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("사전 검사를 거치지 않은 같은 slug 저장이 UNIQUE 제약에 걸리면 slug 중복 예외로 변환한다")
    void convertsSlugUniqueViolationToDuplicateSlugException() {
        Long registeredBy = userRepository.save(User.initialize("slugrace")).getId();
        String repositoryName = "2026-race-" + UUID.randomUUID().toString().substring(0, 8);
        projectRepository.save(project(registeredBy, repositoryName), List.of(), List.of());

        assertThatThrownBy(() -> projectRepository.save(project(registeredBy, repositoryName), List.of(), List.of()))
                .isInstanceOfSatisfying(DuplicateEntityException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ProjectErrorCode.PROJECT_DUPLICATE_SLUG));
    }

    private static Project project(Long registeredBy, String repositoryName) {
        return Project.register(
                Cohort.COHORT_8,
                registeredBy,
                new TeamName("레이스"),
                new Title("동시 등록"),
                "같은 slug 로 두 번 등록한다",
                "설명",
                new GithubRepositoryUrl("https://github.com/woowacourse-teams/" + repositoryName),
                null,
                null
        );
    }
}
