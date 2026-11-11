package com.shoutoutz.api.projectresource.presentation;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.OBJECT;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.EnumFields;
import com.epages.restdocs.apispec.ParameterDescriptorWithType;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.shoutoutz.api.common.exception.code.CommonErrorCode;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.restdocs.RestDocsFields;
import com.shoutoutz.api.project.domain.ProjectErrorCode;
import com.shoutoutz.api.projectresource.application.ProjectResourceService;
import com.shoutoutz.api.projectresource.domain.ProjectResourceErrorCode;
import com.shoutoutz.api.projectresource.domain.ProjectResourceJobRole;
import com.shoutoutz.api.projectresource.presentation.dto.response.ProjectCareerCardResponse;
import com.shoutoutz.api.projectresource.presentation.dto.response.ProjectResourceTopicsResponse;
import com.shoutoutz.api.projectresource.presentation.dto.response.ProjectTopicResourcesResponse;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.ResourceCardResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ProjectResourceHttpApi.class)
@AutoConfigureRestDocs
@DisplayName("프로젝트 기술 이야기 API 문서")
class ProjectResourceApiDocumentationTest {

    private static final String SLUG = "shout-outz";
    private static final String TOPICS_PATH = "/api/v1/projects/@{slug}/resource-topics";
    private static final String RESOURCES_PATH = "/api/v1/projects/@{slug}/resources";
    private static final String CAREER_CARDS_PATH = "/api/v1/projects/@{slug}/career-cards";

    private static final String CAREER_CARDS_DESCRIPTION = "프로젝트 상세 전체 경험 탭의 경력 카드를 조회한다. 로그인 없이 부를 수 있다. 직군 BE, FE, AN 순이고 직군 안에서는 이력서에서 강한 순서(sort_order)다. 카드마다 분류, 제목, 이력서 문장과 포트폴리오 글(sections), 근거 자료를 준다. sections는 template별 소제목 순서이고 빈 칸은 빠진다(problem_solving: 해결하려던 문제, 원인, 해결 과정, 확인한 결과, 측정 범위와 고려한 점 / tech_decision: 해결하려던 문제, 원인, 고려한 방법, 선택한 방법과 이유, 적용한 내용, 확인한 결과, 측정 범위와 고려한 점 / build: 목표, 구현한 내용, 확인한 결과, 측정 범위와 고려한 점). 근거 자료는 기술 자료 검색 결과 카드와 같고 source_order 순(PR, 팀 문서, 블로그)이다. 삭제된 카드, 경험, 자료와 관리자가 받아들인 필요 없어요 제보 자료는 빼며, 근거가 하나도 남지 않은 카드는 주지 않는다. 프로젝트가 없거나 공개되지 않았으면 404 PROJECT_NOT_FOUND를 반환한다.";

    private static final String TOPICS_DESCRIPTION = "프로젝트 상세 기술 이야기 탭의 자료 수, 직군 탭, 분야별 기술 주제 목록을 조회한다. 로그인 없이 부를 수 있다. jobRole 기본값은 ALL이며 ALL, BE, FE, AN을 지원한다. totalCount, kindCounts, jobRoles는 jobRole과 상관없이 이 프로젝트 전체 기준이고, fields만 고른 직군 탭에 보일 주제다. 직군 탭은 그 직군 주제와 공통 주제에서 자료 직군이 그 직군, ALL, 없음인 자료를 센다. 관리자가 받아들인 필요 없어요 제보가 있는 자료는 세지 않는다. 분야와 주제는 자료가 많은 순, 같으면 이름순이다. jobRole이 올바르지 않으면 400 VALIDATION_FAILED, 프로젝트가 없거나 공개되지 않았으면 404 PROJECT_NOT_FOUND를 반환한다.";
    private static final String RESOURCES_DESCRIPTION = "기술 이야기 탭에서 고른 주제의 이 팀 자료, 테코톡, 다른 팀 자료를 조회한다. 로그인 없이 부를 수 있다. topicId는 주제 목록 응답의 topicId다. jobRole을 주면 이 팀 자료와 다른 팀 자료를 그 직군, ALL, 직군 없는 자료로 거르고 테코톡은 거르지 않는다. resources는 전부이며 블로그 먼저, 그다음 점수 높은 순, 같으면 최신순이다. talks는 전부이며 최신 발표순이다. otherTeamResources는 공개된 다른 프로젝트와 프로젝트에 이어지지 않은 자료 중 점수 높은 순(점수가 없으면 85점) 상위 20건이다. 몇 개를 보여 줄지는 프론트가 정한다. 관리자가 받아들인 필요 없어요 제보가 있는 자료는 뺀다. 자료 카드는 기술 자료 검색 결과 카드와 같다. topicId가 없거나 jobRole이 올바르지 않으면 400 VALIDATION_FAILED, 프로젝트가 없거나 공개되지 않았으면 404 PROJECT_NOT_FOUND, 카탈로그에 없는 주제면 404 RESOURCE_TOPIC_NOT_FOUND를 반환한다.";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectResourceService projectResourceService;

    @Test
    @DisplayName("전체 경험 탭의 경력 카드를 포트폴리오 글, 근거 자료와 함께 조회한다")
    void findCareerCards() throws Exception {
        given(projectResourceService.findCareerCards(SLUG)).willReturn(List.of(new ProjectCareerCardResponse(
                12L,
                "BE",
                "장애 대응",
                "운영 링크 미리보기 오수집과 캐시 복구",
                List.of("봇 확인 페이지가 성공으로 캐시되는 원인을 규명하고 oEmbed 우선 수집으로 정상 제목과 썸네일 반환을 확인함"),
                List.of("oEmbed", "Jsoup", "Flyway"),
                List.of(
                        new ProjectCareerCardResponse.Section("해결하려던 문제", "운영에서만 유튜브 미리보기가 일반 문구로 저장됐다."),
                        new ProjectCareerCardResponse.Section("원인", "봇 확인 페이지가 성공 응답으로 캐시됐다."),
                        new ProjectCareerCardResponse.Section("해결 과정", "유튜브 링크는 oEmbed API를 먼저 쓰도록 바꿨다."),
                        new ProjectCareerCardResponse.Section("확인한 결과", "운영에서 정상 제목과 썸네일을 받았다.")
                ),
                List.of(
                        new ResourceCardResponse(
                                3412L,
                                "github-pr",
                                "[BE/fix] 유튜브 링크 미리보기를 oEmbed로 먼저 수집 (#512)",
                                "https://github.com/woowacourse-teams/2026-shout-outz/pull/513",
                                "oEmbed로 유튜브 미리보기를 먼저 수집할 때 참고하기 좋아요.",
                                Instant.parse("2026-10-20T05:12:00Z"),
                                null,
                                null,
                                new ResourceCardResponse.Team("샤라웃", SLUG, 31L, "https://cdn.example.com/media/project/31/thumbnail")
                        ),
                        new ResourceCardResponse(
                                3420L,
                                "blog-post",
                                "운영에서만 깨지는 링크 미리보기 추적기",
                                "https://example.com/link-preview",
                                null,
                                null,
                                "tistory",
                                "https://t1.daumcdn.net/tistory_admin/top_v2/bi-tistory-favicon.svg",
                                new ResourceCardResponse.Team("샤라웃", SLUG, 31L, "https://cdn.example.com/media/project/31/thumbnail")
                        )
                )
        )));

        mockMvc.perform(get(CAREER_CARDS_PATH, SLUG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].cardId").value(12))
                .andExpect(jsonPath("$.data[0].sections[0].heading").value("해결하려던 문제"))
                .andExpect(jsonPath("$.data[0].sources[0].resourceId").value(3412))
                .andDo(document(
                        "project-career-cards",
                        resource(careerCardsSuccessResponse())
                ));
    }

    @Test
    @DisplayName("없거나 공개되지 않은 프로젝트의 경력 카드는 조회할 수 없다")
    void rejectCareerCardsOfMissingProject() throws Exception {
        given(projectResourceService.findCareerCards(SLUG))
                .willThrow(new EntityNotFoundException(ProjectErrorCode.PROJECT_NOT_FOUND));

        mockMvc.perform(get(CAREER_CARDS_PATH, SLUG))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"))
                .andDo(document(
                        "project-career-cards-project-not-found",
                        resource(ResourceSnippetParameters.builder()
                                .tag("ProjectResource")
                                .summary("프로젝트 전체 경험 조회")
                                .description(CAREER_CARDS_DESCRIPTION)
                                .pathParameters(slugParameter())
                                .responseSchema(Schema.schema("ErrorResponse"))
                                .responseFields(RestDocsFields.errorResponse())
                                .build())
                ));
    }

    private ResourceSnippetParameters careerCardsSuccessResponse() {
        List<FieldDescriptor> fields = new ArrayList<>(List.of(
                fieldWithPath("status").type(STRING).description("응답 상태"),
                fieldWithPath("data").type(ARRAY)
                        .description("경력 카드. 직군 BE, FE, AN 순, 직군 안에서는 sort_order 순. 카드가 없으면 빈 배열"),
                fieldWithPath("data[].cardId").type(NUMBER).description("경력 카드 ID"),
                fieldWithPath("data[].jobRole").type(STRING).description("직군. BE, FE, AN"),
                fieldWithPath("data[].category").type(STRING).description("분류(예: 장애 대응, 쿼리 최적화)"),
                fieldWithPath("data[].title").type(STRING).description("카드 제목"),
                fieldWithPath("data[].resumeBullets").type(ARRAY).description("이력서 문장"),
                fieldWithPath("data[].techs").type(ARRAY).description(
                        "카드에 쓴 기술. 이력서 머리의 기술 태그는 프로젝트 기술 스택 중 그 직군 카드들의 기술과 이름이 겹치는 것을 프론트가 고른다(겹치는 게 없으면 전부)"),
                fieldWithPath("data[].sections").type(ARRAY)
                        .description("포트폴리오 글. template별 소제목 순서이고 빈 칸은 빠진다"),
                fieldWithPath("data[].sections[].heading").type(STRING).description("소제목"),
                fieldWithPath("data[].sections[].body").type(STRING)
                        .description("본문. 고려한 방법은 방법들을 쉼표로 이어 붙인다"),
                fieldWithPath("data[].sources").type(ARRAY)
                        .description("근거 자료. source_order 순(PR, 팀 문서, 블로그). PR 번호는 PR 주소 끝(/pull/{번호})에서 읽는다")
        ));
        fields.addAll(cardFields("data[].sources[]"));
        return ResourceSnippetParameters.builder()
                .tag("ProjectResource")
                .summary("프로젝트 전체 경험 조회")
                .description(CAREER_CARDS_DESCRIPTION)
                .pathParameters(slugParameter())
                .responseSchema(Schema.schema("ProjectCareerCardsSuccessResponse"))
                .responseFields(fields)
                .build();
    }

    @Test
    @DisplayName("기술 이야기 탭의 자료 수, 직군 탭, 분야별 주제 목록을 조회한다")
    void findTopics() throws Exception {
        given(projectResourceService.findTopics(SLUG, ProjectResourceJobRole.BE))
                .willReturn(new ProjectResourceTopicsResponse(
                        42,
                        new ProjectResourceTopicsResponse.KindCounts(3, 30, 9),
                        List.of(
                                new ProjectResourceTopicsResponse.JobRoleCount(ProjectResourceJobRole.ALL, 42),
                                new ProjectResourceTopicsResponse.JobRoleCount(ProjectResourceJobRole.BE, 31),
                                new ProjectResourceTopicsResponse.JobRoleCount(ProjectResourceJobRole.FE, 15)
                        ),
                        List.of(
                                new ProjectResourceTopicsResponse.Field("데이터", List.of(
                                        new ProjectResourceTopicsResponse.Topic("n_plus_one", "N+1 문제", 7),
                                        new ProjectResourceTopicsResponse.Topic("db_index", "인덱스/쿼리 최적화", 5)
                                )),
                                new ProjectResourceTopicsResponse.Field("동시성, 성능", List.of(
                                        new ProjectResourceTopicsResponse.Topic("concurrency", "동시성 제어/락", 6)
                                ))
                        )
                ));

        mockMvc.perform(get(TOPICS_PATH, SLUG).queryParam("jobRole", "BE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fields[0].topics[0].topicId").value("n_plus_one"))
                .andDo(document(
                        "project-resource-topics",
                        resource(topicsSuccessResponse())
                ));
    }

    @Test
    @DisplayName("직군이 올바르지 않으면 주제 목록 조회를 거부한다")
    void rejectTopicsWithInvalidJobRole() throws Exception {
        mockMvc.perform(get(TOPICS_PATH, SLUG).queryParam("jobRole", "BACKEND"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.VALIDATION_FAILED.name()))
                .andDo(document(
                        "project-resource-topics-invalid-job-role",
                        resource(topicsErrorResponse())
                ));
        verify(projectResourceService, never()).findTopics(anyString(), any());
    }

    @Test
    @DisplayName("없거나 공개되지 않은 프로젝트의 주제 목록은 조회할 수 없다")
    void rejectTopicsOfMissingProject() throws Exception {
        given(projectResourceService.findTopics(SLUG, ProjectResourceJobRole.ALL))
                .willThrow(new EntityNotFoundException(ProjectErrorCode.PROJECT_NOT_FOUND));

        mockMvc.perform(get(TOPICS_PATH, SLUG))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"))
                .andDo(document(
                        "project-resource-topics-project-not-found",
                        resource(topicsErrorResponse())
                ));
    }

    @Test
    @DisplayName("고른 주제의 이 팀 자료, 테코톡, 다른 팀 자료를 조회한다")
    void findResources() throws Exception {
        given(projectResourceService.findResources(SLUG, "concurrency", ProjectResourceJobRole.BE))
                .willReturn(new ProjectTopicResourcesResponse(
                        new ProjectTopicResourcesResponse.Topic("concurrency", "동시성 제어/락"),
                        List.of(new ResourceCardResponse(
                                3412L,
                                "github-pr",
                                "동시 예약 요청에 비관적 락 적용",
                                "https://github.com/woowacourse-teams/2026-shout-outz/pull/512",
                                "비관적 락으로 중복 예약을 막을 때 참고하기 좋아요.",
                                Instant.parse("2026-10-20T05:12:00Z"),
                                null,
                                null,
                                new ResourceCardResponse.Team("샤라웃", SLUG, 31L, "https://cdn.example.com/media/project/31/thumbnail")
                        )),
                        List.of(new ResourceCardResponse(
                                2301L,
                                "youtube-tekotalk",
                                "[10분 테코톡] 벨로의 Race Condition 이해하기",
                                "https://www.youtube.com/watch?v=At4Gyezw8WA",
                                "Race Condition이 생기는 원리를 이해하기 좋아요.",
                                Instant.parse("2025-09-05T09:00:00Z"),
                                null,
                                null,
                                null
                        )),
                        List.of(new ResourceCardResponse(
                                101L,
                                "blog-post",
                                "[Database] MySQL(InnoDB) 락(Lock) (2)",
                                "https://example.com/mysql-lock",
                                "레코드 락과 갭 락의 잠금 범위를 살펴볼 때 참고하기 좋아요.",
                                Instant.parse("2025-10-06T03:00:00Z"),
                                "tistory",
                                "https://t1.daumcdn.net/tistory_admin/top_v2/bi-tistory-favicon.svg",
                                new ResourceCardResponse.Team("등록 전 팀", null, null, null)
                        ))
                ));

        mockMvc.perform(get(RESOURCES_PATH, SLUG)
                        .queryParam("topicId", "concurrency")
                        .queryParam("jobRole", "BE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resources[0].resourceId").value(3412))
                .andExpect(jsonPath("$.data.otherTeamResources[0].team.name").value("등록 전 팀"))
                .andDo(document(
                        "project-resources",
                        resource(resourcesSuccessResponse())
                ));
    }

    @Test
    @DisplayName("주제가 없으면 주제별 자료 조회를 거부한다")
    void rejectResourcesWithoutTopic() throws Exception {
        mockMvc.perform(get(RESOURCES_PATH, SLUG))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.VALIDATION_FAILED.name()))
                .andDo(document(
                        "project-resources-missing-topic",
                        resource(missingTopicErrorResponse())
                ));
        verify(projectResourceService, never()).findResources(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("직군이 올바르지 않으면 주제별 자료 조회를 거부한다")
    void rejectResourcesWithInvalidJobRole() throws Exception {
        mockMvc.perform(get(RESOURCES_PATH, SLUG)
                        .queryParam("topicId", "concurrency")
                        .queryParam("jobRole", "ANDROID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.VALIDATION_FAILED.name()))
                .andDo(document(
                        "project-resources-invalid-job-role",
                        resource(resourcesErrorResponse())
                ));
    }

    @Test
    @DisplayName("카탈로그에 없는 주제의 자료는 조회할 수 없다")
    void rejectResourcesOfUnknownTopic() throws Exception {
        given(projectResourceService.findResources(eq(SLUG), eq("unknown"), eq(ProjectResourceJobRole.ALL)))
                .willThrow(new EntityNotFoundException(ProjectResourceErrorCode.RESOURCE_TOPIC_NOT_FOUND));

        mockMvc.perform(get(RESOURCES_PATH, SLUG).queryParam("topicId", "unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_TOPIC_NOT_FOUND"))
                .andDo(document(
                        "project-resources-topic-not-found",
                        resource(resourcesErrorResponse())
                ));
    }

    @Test
    @DisplayName("없거나 공개되지 않은 프로젝트의 주제별 자료는 조회할 수 없다")
    void rejectResourcesOfMissingProject() throws Exception {
        given(projectResourceService.findResources(SLUG, "concurrency", ProjectResourceJobRole.ALL))
                .willThrow(new EntityNotFoundException(ProjectErrorCode.PROJECT_NOT_FOUND));

        mockMvc.perform(get(RESOURCES_PATH, SLUG).queryParam("topicId", "concurrency"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"))
                .andDo(document(
                        "project-resources-project-not-found",
                        resource(resourcesErrorResponse())
                ));
    }

    private ResourceSnippetParameters topicsSuccessResponse() {
        return ResourceSnippetParameters.builder()
                .tag("ProjectResource")
                .summary("프로젝트 기술 이야기 주제 목록 조회")
                .description(TOPICS_DESCRIPTION)
                .pathParameters(slugParameter())
                .queryParameters(jobRoleParameter())
                .responseSchema(Schema.schema("ProjectResourceTopicsSuccessResponse"))
                .responseFields(
                        fieldWithPath("status").type(STRING).description("응답 상태"),
                        fieldWithPath("data").type(OBJECT).description("기술 이야기 탭의 자료 수와 주제 목록"),
                        fieldWithPath("data.totalCount").type(NUMBER).description("직군과 상관없는 이 프로젝트 자료 수"),
                        fieldWithPath("data.kindCounts").type(OBJECT).description("직군과 상관없는 종류별 자료 수"),
                        fieldWithPath("data.kindCounts.blog").type(NUMBER).description("블로그 수"),
                        fieldWithPath("data.kindCounts.pr").type(NUMBER).description("PR 수"),
                        fieldWithPath("data.kindCounts.teamDoc").type(NUMBER)
                                .description("팀 문서 수. 레포 문서, Wiki, Discussion을 합한 수"),
                        fieldWithPath("data.jobRoles").type(ARRAY)
                                .description("직군 탭. ALL은 늘 있고 BE, FE, AN은 그 직군 자료가 있을 때만 있다"),
                        new EnumFields(ProjectResourceJobRole.class).withPath("data.jobRoles[].jobRole")
                                .description("직군"),
                        fieldWithPath("data.jobRoles[].count").type(NUMBER)
                                .description("그 탭에 보이는 자료 수. 공통 자료는 여러 탭에 들어가 합이 전체보다 클 수 있다"),
                        fieldWithPath("data.fields").type(ARRAY)
                                .description("고른 직군 탭에 보일 주제를 분야로 묶은 것. 자료가 많은 분야부터"),
                        fieldWithPath("data.fields[].name").type(STRING).description("분야 이름"),
                        fieldWithPath("data.fields[].topics").type(ARRAY).description("분야 안 주제. 자료가 많은 순, 같으면 이름순"),
                        fieldWithPath("data.fields[].topics[].topicId").type(STRING)
                                .description("주제 id. 주제별 자료 조회의 topicId로 쓴다"),
                        fieldWithPath("data.fields[].topics[].name").type(STRING).description("주제 이름"),
                        fieldWithPath("data.fields[].topics[].count").type(NUMBER).description("그 주제의 자료 수")
                )
                .build();
    }

    private ResourceSnippetParameters topicsErrorResponse() {
        return ResourceSnippetParameters.builder()
                .tag("ProjectResource")
                .summary("프로젝트 기술 이야기 주제 목록 조회")
                .description(TOPICS_DESCRIPTION)
                .pathParameters(slugParameter())
                .queryParameters(jobRoleParameter())
                .responseSchema(Schema.schema("ErrorResponse"))
                .responseFields(RestDocsFields.errorResponse())
                .build();
    }

    private ResourceSnippetParameters resourcesSuccessResponse() {
        List<FieldDescriptor> fields = new ArrayList<>(List.of(
                fieldWithPath("status").type(STRING).description("응답 상태"),
                fieldWithPath("data").type(OBJECT).description("고른 주제의 자료"),
                fieldWithPath("data.topic").type(OBJECT).description("고른 주제"),
                fieldWithPath("data.topic.topicId").type(STRING).description("주제 id"),
                fieldWithPath("data.topic.name").type(STRING).description("주제 이름"),
                fieldWithPath("data.resources").type(ARRAY)
                        .description("이 팀 자료 전부. 블로그 먼저, 그다음 점수 높은 순, 같으면 최신순"),
                fieldWithPath("data.talks").type(ARRAY).description("그 주제의 테코톡 전부. 최신 발표순"),
                fieldWithPath("data.otherTeamResources").type(ARRAY)
                        .description("같은 주제의 다른 팀 자료. 점수 높은 순(점수가 없으면 85점) 상위 20건")
        ));
        fields.addAll(cardFields("data.resources[]"));
        fields.addAll(cardFields("data.talks[]"));
        fields.addAll(cardFields("data.otherTeamResources[]"));
        return ResourceSnippetParameters.builder()
                .tag("ProjectResource")
                .summary("프로젝트 기술 이야기 주제별 자료 조회")
                .description(RESOURCES_DESCRIPTION)
                .pathParameters(slugParameter())
                .queryParameters(resourcesParameters())
                .responseSchema(Schema.schema("ProjectTopicResourcesSuccessResponse"))
                .responseFields(fields)
                .build();
    }

    private ResourceSnippetParameters resourcesErrorResponse() {
        return ResourceSnippetParameters.builder()
                .tag("ProjectResource")
                .summary("프로젝트 기술 이야기 주제별 자료 조회")
                .description(RESOURCES_DESCRIPTION)
                .pathParameters(slugParameter())
                .queryParameters(resourcesParameters())
                .responseSchema(Schema.schema("ErrorResponse"))
                .responseFields(RestDocsFields.errorResponse())
                .build();
    }

    /**
     * topicId 를 빼고 보낸 요청이라 조회 조건은 적지 않는다. 조건 설명은 다른 문서 조각과 합쳐진다.
     */
    private ResourceSnippetParameters missingTopicErrorResponse() {
        return ResourceSnippetParameters.builder()
                .tag("ProjectResource")
                .summary("프로젝트 기술 이야기 주제별 자료 조회")
                .description(RESOURCES_DESCRIPTION)
                .pathParameters(slugParameter())
                .responseSchema(Schema.schema("ErrorResponse"))
                .responseFields(RestDocsFields.errorResponse())
                .build();
    }

    /**
     * 자료 카드 칸. 기술 자료 검색 결과 카드와 같다.
     */
    private List<FieldDescriptor> cardFields(String prefix) {
        return List.of(
                fieldWithPath(prefix + ".resourceId").type(NUMBER)
                        .description("자료 ID. 자료 상세 조회와 필요 없는 자료 제보에 쓴다"),
                fieldWithPath(prefix + ".source").type(STRING).description(
                        "자료 원천. blog-post, github-pr, github-teamdoc, github-wiki, github-discussion, youtube-tekotalk"),
                fieldWithPath(prefix + ".title").type(STRING).description("자료 제목"),
                fieldWithPath(prefix + ".url").type(STRING).description("원문 주소"),
                fieldWithPath(prefix + ".summary").type(STRING).description("한 줄 소개. 없으면 null").optional(),
                fieldWithPath(prefix + ".publishedAt").type(STRING)
                        .description("ISO-8601 원문 게시 시각. 없으면 null").optional(),
                fieldWithPath(prefix + ".platform").type(STRING)
                        .description("블로그 플랫폼. 블로그가 아니면 null").optional(),
                fieldWithPath(prefix + ".faviconUrl").type(STRING)
                        .description("블로그 출처 아이콘 주소. tistory, velog, rilog 는 플랫폼 공식 아이콘, 그 밖의 블로그(개인 도메인 등)는 원문 페이지의 파비콘. 블로그가 아니거나 아직 수집 전이면 null").optional(),
                fieldWithPath(prefix + ".team").type(OBJECT)
                        .description("자료를 만든 팀. 테코톡처럼 팀이 없거나 팀을 알 수 없으면 null").optional(),
                fieldWithPath(prefix + ".team.name").type(STRING)
                        .description("등록된 프로젝트면 팀 이름, 등록 전 팀이면 수집 때 저장한 팀 이름").optional(),
                fieldWithPath(prefix + ".team.projectSlug").type(STRING)
                        .description("등록된 프로젝트의 slug. 등록 전이면 null").optional(),
                fieldWithPath(prefix + ".team.logoMediaId").type(NUMBER)
                        .description("프로젝트 로고 미디어 ID. 등록 전이거나 로고가 없으면 null").optional(),
                fieldWithPath(prefix + ".team.logoUrl").type(STRING)
                        .description("팀 로고 이미지 주소(작은 크기). logoMediaId가 없거나 이미지가 준비되지 않았으면 null").optional()
        );
    }

    private ParameterDescriptorWithType slugParameter() {
        return parameterWithName("slug").description("프로젝트 slug. 경로에서는 앞에 @를 붙인다.");
    }

    private List<ParameterDescriptorWithType> jobRoleParameter() {
        return List.of(
                parameterWithName("jobRole").description("직군 탭. ALL, BE, FE, AN 중 하나. 생략하면 ALL").optional()
        );
    }

    private List<ParameterDescriptorWithType> resourcesParameters() {
        return List.of(
                parameterWithName("topicId").description("주제 id. 주제 목록 응답의 topicId"),
                parameterWithName("jobRole").description("직군 탭. ALL, BE, FE, AN 중 하나. 생략하면 ALL").optional()
        );
    }
}
