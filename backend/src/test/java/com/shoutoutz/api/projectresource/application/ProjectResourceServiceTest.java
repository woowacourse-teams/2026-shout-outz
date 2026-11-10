package com.shoutoutz.api.projectresource.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.project.application.ProjectSlugResolver;
import com.shoutoutz.api.project.domain.ProjectErrorCode;
import com.shoutoutz.api.project.domain.ProjectRepository;
import com.shoutoutz.api.projectresource.domain.ProjectResourceErrorCode;
import com.shoutoutz.api.projectresource.domain.ProjectResourceJobRole;
import com.shoutoutz.api.projectresource.domain.ResourceCount;
import com.shoutoutz.api.projectresource.domain.ResourceTopic;
import com.shoutoutz.api.projectresource.presentation.dto.response.ProjectResourceTopicsResponse;
import com.shoutoutz.api.projectresource.presentation.dto.response.ProjectTopicResourcesResponse;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceCard;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.ResourceCardResponse;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectResourceServiceTest {

    private static final String SLUG = "shout-outz";
    private static final long PROJECT_ID = 1L;
    private static final ResourceTopic CONCURRENCY = new ResourceTopic(
            "concurrency",
            "동시성 제어/락",
            "backend",
            "동시성, 성능"
    );

    @Mock
    private ProjectSlugResolver projectSlugResolver;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectResourceQueryRepository projectResourceQueryRepository;

    private ProjectResourceService projectResourceService;

    @BeforeEach
    void setUp() {
        projectResourceService = new ProjectResourceService(
                projectSlugResolver,
                projectRepository,
                projectResourceQueryRepository
        );
    }

    @Test
    @DisplayName("주제 목록은 이 프로젝트 팀 자료 수로 머리 수, 직군 탭, 고른 직군의 분야별 주제를 만든다")
    void findsTopics() {
        givenPublicProject();
        given(projectResourceQueryRepository.findTopics()).willReturn(List.of(
                CONCURRENCY,
                new ResourceTopic("react_rendering", "React 렌더링/훅", "frontend", "프론트엔드")
        ));
        given(projectResourceQueryRepository.countTeamResources(PROJECT_ID)).willReturn(List.of(
                new ResourceCount("concurrency", "BE", "github-pr", 3),
                new ResourceCount("concurrency", null, "blog-post", 1),
                new ResourceCount("react_rendering", "FE", "github-teamdoc", 2)
        ));

        ProjectResourceTopicsResponse response = projectResourceService.findTopics(SLUG, ProjectResourceJobRole.BE);

        assertThat(response.totalCount()).isEqualTo(6);
        assertThat(response.kindCounts()).isEqualTo(new ProjectResourceTopicsResponse.KindCounts(1, 3, 2));
        assertThat(response.jobRoles()).containsExactly(
                new ProjectResourceTopicsResponse.JobRoleCount(ProjectResourceJobRole.ALL, 6),
                new ProjectResourceTopicsResponse.JobRoleCount(ProjectResourceJobRole.BE, 4),
                new ProjectResourceTopicsResponse.JobRoleCount(ProjectResourceJobRole.FE, 2)
        );
        assertThat(response.fields()).containsExactly(new ProjectResourceTopicsResponse.Field(
                "동시성, 성능",
                List.of(new ProjectResourceTopicsResponse.Topic("concurrency", "동시성 제어/락", 4))
        ));
    }

    @Test
    @DisplayName("공개되지 않은 프로젝트의 주제 목록은 없는 프로젝트로 본다")
    void rejectsTopicsOfNonPublicProject() {
        given(projectSlugResolver.resolveId(SLUG)).willReturn(PROJECT_ID);
        given(projectRepository.existsPublicById(PROJECT_ID)).willReturn(false);

        assertThatThrownBy(() -> projectResourceService.findTopics(SLUG, ProjectResourceJobRole.ALL))
                .isInstanceOf(EntityNotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ProjectErrorCode.PROJECT_NOT_FOUND);
        verify(projectResourceQueryRepository, never()).countTeamResources(anyLong());
    }

    @Test
    @DisplayName("주제별 자료는 이 팀 자료, 테코톡, 다른 팀 자료 상위 20건을 카드로 준다")
    void findsTopicResources() {
        givenPublicProject();
        ResourceCard teamPr = card(11L, "github-pr", new ResourceCard.Team("샤라웃", SLUG, 31L));
        ResourceCard talk = card(21L, "youtube-tekotalk", null);
        ResourceCard otherBlog = card(31L, "blog-post", new ResourceCard.Team("등록 전 팀", null, null));
        given(projectResourceQueryRepository.findTopic("concurrency")).willReturn(Optional.of(CONCURRENCY));
        given(projectResourceQueryRepository.findTeamResources(PROJECT_ID, "concurrency", ProjectResourceJobRole.BE))
                .willReturn(List.of(teamPr));
        given(projectResourceQueryRepository.findTalks("concurrency")).willReturn(List.of(talk));
        given(projectResourceQueryRepository.findOtherTeamResources(
                PROJECT_ID,
                "concurrency",
                ProjectResourceJobRole.BE,
                ProjectResourceService.OTHER_TEAM_RESOURCE_LIMIT
        )).willReturn(List.of(otherBlog));

        ProjectTopicResourcesResponse response = projectResourceService.findResources(
                SLUG,
                "concurrency",
                ProjectResourceJobRole.BE
        );

        assertThat(ProjectResourceService.OTHER_TEAM_RESOURCE_LIMIT).isEqualTo(20);
        assertThat(response.topic()).isEqualTo(new ProjectTopicResourcesResponse.Topic("concurrency", "동시성 제어/락"));
        assertThat(response.resources()).containsExactly(ResourceCardResponse.from(teamPr));
        assertThat(response.talks()).containsExactly(ResourceCardResponse.from(talk));
        assertThat(response.otherTeamResources()).containsExactly(ResourceCardResponse.from(otherBlog));
        assertThat(response.resources().getFirst().team())
                .isEqualTo(new ResourceCardResponse.Team("샤라웃", SLUG, 31L));
        assertThat(response.talks().getFirst().team()).isNull();
    }

    @Test
    @DisplayName("카탈로그에 없는 주제는 자료를 읽지 않고 거부한다")
    void rejectsUnknownTopic() {
        givenPublicProject();
        given(projectResourceQueryRepository.findTopic("unknown")).willReturn(Optional.empty());

        assertThatThrownBy(() -> projectResourceService.findResources(SLUG, "unknown", ProjectResourceJobRole.ALL))
                .isInstanceOf(EntityNotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ProjectResourceErrorCode.RESOURCE_TOPIC_NOT_FOUND);
        verify(projectResourceQueryRepository, never()).findTalks(anyString());
        verify(projectResourceQueryRepository, never())
                .findOtherTeamResources(anyLong(), anyString(), any(), anyInt());
    }

    @Test
    @DisplayName("공개되지 않은 프로젝트의 주제별 자료는 없는 프로젝트로 본다")
    void rejectsResourcesOfNonPublicProject() {
        given(projectSlugResolver.resolveId(SLUG)).willReturn(PROJECT_ID);
        given(projectRepository.existsPublicById(PROJECT_ID)).willReturn(false);

        assertThatThrownBy(() -> projectResourceService.findResources(SLUG, "concurrency", ProjectResourceJobRole.ALL))
                .isInstanceOf(EntityNotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ProjectErrorCode.PROJECT_NOT_FOUND);
        verify(projectResourceQueryRepository, never()).findTopic(anyString());
    }

    private void givenPublicProject() {
        given(projectSlugResolver.resolveId(SLUG)).willReturn(PROJECT_ID);
        given(projectRepository.existsPublicById(PROJECT_ID)).willReturn(true);
    }

    private static ResourceCard card(long resourceId, String source, ResourceCard.Team team) {
        return new ResourceCard(
                resourceId,
                source,
                "제목 " + resourceId,
                "https://example.com/" + resourceId,
                "한 줄 소개",
                Instant.parse("2026-10-20T05:12:00Z"),
                null,
                team
        );
    }
}
