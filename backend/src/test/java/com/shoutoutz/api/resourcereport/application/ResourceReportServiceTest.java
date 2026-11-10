package com.shoutoutz.api.resourcereport.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.project.application.ProjectSlugResolver;
import com.shoutoutz.api.project.domain.GithubRepositoryUrl;
import com.shoutoutz.api.project.domain.Project;
import com.shoutoutz.api.project.domain.ProjectErrorCode;
import com.shoutoutz.api.project.domain.ProjectRepository;
import com.shoutoutz.api.resourcereport.application.dto.MyResourceReport;
import com.shoutoutz.api.resourcereport.application.dto.ReportableResource;
import com.shoutoutz.api.resourcereport.domain.ResourceReport;
import com.shoutoutz.api.resourcereport.domain.ResourceReportErrorCode;
import com.shoutoutz.api.resourcereport.domain.ResourceReportKind;
import com.shoutoutz.api.resourcereport.domain.ResourceReportRepository;
import com.shoutoutz.api.resourcereport.domain.ResourceReportStatus;
import com.shoutoutz.api.resourcereport.presentation.dto.request.ResourceAdditionReportRequest;
import com.shoutoutz.api.resourcereport.presentation.dto.request.ResourceRemovalReportRequest;
import com.shoutoutz.api.resourcereport.presentation.dto.response.MyResourceReportResponse;
import com.shoutoutz.api.resourcereport.presentation.dto.response.ResourceAdditionReportResponse;
import com.shoutoutz.api.resourcereport.presentation.dto.response.ResourceRemovalReportResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResourceReportServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-10T02:00:00Z");
    private static final String SLUG = "shout-outz";
    private static final long PROJECT_ID = 1L;
    private static final long OTHER_PROJECT_ID = 2L;
    private static final long USER_ID = 7L;
    private static final String REPOSITORY_URL = "https://github.com/woowacourse-teams/2026-shout-outz";

    @Mock
    private ProjectSlugResolver projectSlugResolver;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ResourceReportRepository resourceReportRepository;

    @Mock
    private ResourceReportQueryRepository resourceReportQueryRepository;

    private ResourceReportService resourceReportService;

    @BeforeEach
    void setUp() {
        resourceReportService = new ResourceReportService(
                projectSlugResolver,
                projectRepository,
                resourceReportRepository,
                resourceReportQueryRepository,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    @DisplayName("고른 자료를 요청 순서대로 중복 없이 제보하고 제보 id 를 돌려준다")
    void reportsRemovalsInRequestOrder() {
        givenPublicProject();
        givenMember(true);
        given(resourceReportQueryRepository.findResources(List.of(11L, 12L))).willReturn(Map.of(
                11L, new ReportableResource(11L, PROJECT_ID, "github-pr", "woowacourse-teams/2026-shout-outz#512"),
                12L, new ReportableResource(12L, PROJECT_ID, "github-teamdoc", "woowacourse-teams/2026-shout-outz:docs/adr.md")
        ));
        given(resourceReportRepository.saveIfAbsent(any(ResourceReport.class))).willReturn(101L, 102L);

        List<ResourceRemovalReportResponse> responses = resourceReportService.reportRemovals(
                SLUG,
                USER_ID,
                new ResourceRemovalReportRequest(List.of(11L, 12L, 11L), "  단순 설정 변경이에요  ")
        );

        assertThat(responses).containsExactly(
                new ResourceRemovalReportResponse(101L, 11L),
                new ResourceRemovalReportResponse(102L, 12L)
        );
        ArgumentCaptor<ResourceReport> captor = ArgumentCaptor.forClass(ResourceReport.class);
        verify(resourceReportRepository, times(2)).saveIfAbsent(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(ResourceReport::getResourceExternalKey)
                .containsExactly("woowacourse-teams/2026-shout-outz#512", "woowacourse-teams/2026-shout-outz:docs/adr.md");
        assertThat(captor.getAllValues())
                .allSatisfy(report -> {
                    assertThat(report.getKind()).isEqualTo(ResourceReportKind.REMOVE);
                    assertThat(report.getProjectId()).isEqualTo(PROJECT_ID);
                    assertThat(report.getUserId()).isEqualTo(USER_ID);
                    assertThat(report.getMemo()).isEqualTo("단순 설정 변경이에요");
                    assertThat(report.getCreatedAt()).isEqualTo(NOW);
                });
    }

    @Test
    @DisplayName("공개되지 않았거나 삭제된 프로젝트에는 제보할 수 없다")
    void rejectsNonPublicProject() {
        given(projectSlugResolver.resolveId(SLUG)).willReturn(PROJECT_ID);
        given(projectRepository.existsPublicById(PROJECT_ID)).willReturn(false);

        assertThatThrownBy(() -> resourceReportService.reportRemovals(
                SLUG,
                USER_ID,
                new ResourceRemovalReportRequest(List.of(11L), null)
        ))
                .isInstanceOf(EntityNotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ProjectErrorCode.PROJECT_NOT_FOUND);
        verify(resourceReportRepository, never()).saveIfAbsent(any());
    }

    @Test
    @DisplayName("프로젝트 팀원이 아니면 제보할 수 없고 자료도 찾지 않는다")
    void rejectsNonMember() {
        givenPublicProject();
        givenMember(false);

        assertThatThrownBy(() -> resourceReportService.reportRemovals(
                SLUG,
                USER_ID,
                new ResourceRemovalReportRequest(List.of(11L), null)
        ))
                .isInstanceOf(ForbiddenException.class)
                .extracting("errorCode")
                .isEqualTo(ResourceReportErrorCode.RESOURCE_REPORT_FORBIDDEN);
        verify(resourceReportQueryRepository, never()).findResources(any());
        verify(resourceReportRepository, never()).saveIfAbsent(any());
    }

    @Test
    @DisplayName("없거나 내려간 자료가 섞이면 아무것도 저장하지 않는다")
    void rejectsMissingResource() {
        givenPublicProject();
        givenMember(true);
        given(resourceReportQueryRepository.findResources(List.of(11L, 99L))).willReturn(Map.of(
                11L, new ReportableResource(11L, PROJECT_ID, "github-pr", "woowacourse-teams/2026-shout-outz#512")
        ));

        assertThatThrownBy(() -> resourceReportService.reportRemovals(
                SLUG,
                USER_ID,
                new ResourceRemovalReportRequest(List.of(11L, 99L), null)
        ))
                .isInstanceOf(EntityNotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ResourceReportErrorCode.RESOURCE_REPORT_RESOURCE_NOT_FOUND);
        verify(resourceReportRepository, never()).saveIfAbsent(any());
    }

    @Test
    @DisplayName("다른 프로젝트의 자료는 제보할 수 없다")
    void rejectsOtherProjectResource() {
        givenPublicProject();
        givenMember(true);
        given(resourceReportQueryRepository.findResources(List.of(11L))).willReturn(Map.of(
                11L, new ReportableResource(11L, OTHER_PROJECT_ID, "github-pr", "woowacourse-teams/2026-other#1")
        ));

        assertThatThrownBy(() -> resourceReportService.reportRemovals(
                SLUG,
                USER_ID,
                new ResourceRemovalReportRequest(List.of(11L), null)
        ))
                .isInstanceOf(BadRequestException.class)
                .extracting("errorCode")
                .isEqualTo(ResourceReportErrorCode.RESOURCE_REPORT_RESOURCE_NOT_REMOVABLE);
        verify(resourceReportRepository, never()).saveIfAbsent(any());
    }

    @Test
    @DisplayName("이 프로젝트의 블로그 글은 필요 없다고 제보할 수 없다")
    void rejectsBlogResource() {
        givenPublicProject();
        givenMember(true);
        given(resourceReportQueryRepository.findResources(List.of(11L, 12L))).willReturn(Map.of(
                11L, new ReportableResource(11L, PROJECT_ID, "github-pr", "woowacourse-teams/2026-shout-outz#512"),
                12L, new ReportableResource(12L, PROJECT_ID, "blog-post", "post:8738139d847566747c42198b6b03dba5c4df2952")
        ));

        assertThatThrownBy(() -> resourceReportService.reportRemovals(
                SLUG,
                USER_ID,
                new ResourceRemovalReportRequest(List.of(11L, 12L), null)
        ))
                .isInstanceOf(BadRequestException.class)
                .extracting("errorCode")
                .isEqualTo(ResourceReportErrorCode.RESOURCE_REPORT_RESOURCE_NOT_REMOVABLE);
        verify(resourceReportRepository, never()).saveIfAbsent(any());
    }

    @Test
    @DisplayName("빠진 자료 주소를 정리해서 제보하고 제보 id 를 돌려준다")
    void reportsAddition() {
        givenPublicProject();
        givenMember(true);
        givenRepository();
        given(resourceReportRepository.saveIfAbsent(any(ResourceReport.class))).willReturn(103L);

        ResourceAdditionReportResponse response = resourceReportService.reportAddition(
                SLUG,
                USER_ID,
                new ResourceAdditionReportRequest(REPOSITORY_URL + "/pull/123/files?w=1#diff", "캐시 도입 전후를 비교했어요")
        );

        assertThat(response.reportId()).isEqualTo(103L);
        ArgumentCaptor<ResourceReport> captor = ArgumentCaptor.forClass(ResourceReport.class);
        verify(resourceReportRepository).saveIfAbsent(captor.capture());
        assertThat(captor.getValue().getKind()).isEqualTo(ResourceReportKind.ADD);
        assertThat(captor.getValue().getUrl()).isEqualTo(REPOSITORY_URL + "/pull/123/files");
        assertThat(captor.getValue().getMemo()).isEqualTo("캐시 도입 전후를 비교했어요");
    }

    @Test
    @DisplayName("이 프로젝트 리포지토리 밖의 주소는 제보할 수 없다")
    void rejectsAdditionOutsideRepository() {
        givenPublicProject();
        givenMember(true);
        givenRepository();

        assertThatThrownBy(() -> resourceReportService.reportAddition(
                SLUG,
                USER_ID,
                new ResourceAdditionReportRequest("https://github.com/woowacourse-teams/2026-other/pull/1", null)
        ))
                .isInstanceOf(BadRequestException.class)
                .extracting("errorCode")
                .isEqualTo(ResourceReportErrorCode.RESOURCE_REPORT_URL_OUTSIDE_REPOSITORY);
        verify(resourceReportRepository, never()).saveIfAbsent(any());
    }

    @Test
    @DisplayName("팀원이 아니면 빠진 자료도 제보할 수 없다")
    void rejectsAdditionFromNonMember() {
        givenPublicProject();
        givenMember(false);

        assertThatThrownBy(() -> resourceReportService.reportAddition(
                SLUG,
                USER_ID,
                new ResourceAdditionReportRequest(REPOSITORY_URL + "/pull/1", null)
        ))
                .isInstanceOf(ForbiddenException.class)
                .extracting("errorCode")
                .isEqualTo(ResourceReportErrorCode.RESOURCE_REPORT_FORBIDDEN);
        verify(resourceReportRepository, never()).saveIfAbsent(any());
    }

    @Test
    @DisplayName("내가 보낸 제보는 팀원 여부와 상관없이 조회한다")
    void findsMine() {
        givenPublicProject();
        Instant createdAt = Instant.parse("2026-11-10T01:00:00Z");
        given(resourceReportQueryRepository.findMine(PROJECT_ID, USER_ID)).willReturn(List.of(
                new MyResourceReport(41L, ResourceReportKind.REMOVE, 11L, null, ResourceReportStatus.OPEN, createdAt)
        ));

        List<MyResourceReportResponse> responses = resourceReportService.findMine(SLUG, USER_ID);

        assertThat(responses).containsExactly(
                new MyResourceReportResponse(41L, ResourceReportKind.REMOVE, 11L, null, ResourceReportStatus.OPEN, createdAt)
        );
        verify(resourceReportQueryRepository, never()).isProjectMember(PROJECT_ID, USER_ID);
    }

    private void givenPublicProject() {
        given(projectSlugResolver.resolveId(SLUG)).willReturn(PROJECT_ID);
        given(projectRepository.existsPublicById(PROJECT_ID)).willReturn(true);
    }

    private void givenMember(boolean member) {
        given(resourceReportQueryRepository.isProjectMember(PROJECT_ID, USER_ID)).willReturn(member);
    }

    private void givenRepository() {
        Project project = mock(Project.class);
        given(project.getGithubRepositoryUrl()).willReturn(new GithubRepositoryUrl(REPOSITORY_URL));
        given(projectRepository.findActiveById(PROJECT_ID)).willReturn(Optional.of(project));
    }
}
