package com.shoutoutz.api.jobposting.presentation;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.BOOLEAN;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.OBJECT;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.EnumFields;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.epages.restdocs.apispec.SimpleType;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.jobposting.application.JobPostingService;
import com.shoutoutz.api.jobposting.domain.JobPostingErrorCode;
import com.shoutoutz.api.jobposting.domain.JobPostingSort;
import com.shoutoutz.api.jobposting.domain.JobRole;
import com.shoutoutz.api.jobposting.domain.RecommendationVerdict;
import com.shoutoutz.api.jobposting.domain.RequirementCategory;
import com.shoutoutz.api.jobposting.presentation.dto.request.JobPostingFindAllRequest;
import com.shoutoutz.api.jobposting.presentation.dto.response.JobPostingDetailResponse;
import com.shoutoutz.api.jobposting.presentation.dto.response.JobPostingFindAllResponse;
import com.shoutoutz.api.jobposting.presentation.dto.response.JobPostingListMetaResponse;
import com.shoutoutz.api.jobposting.presentation.dto.response.JobRequirementRecommendationResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = JobPostingHttpApi.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs
class JobPostingHttpApiTest {

    private static final String TAG = "Job Posting";
    private static final long JOB_POSTING_ID = 100L;
    private static final long REQUIREMENT_ID = 11L;
    private static final String RECOMMENDATIONS_URL =
            "/api/v1/job-postings/{jobPostingId}/requirements/{requirementId}/recommendations";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobPostingService jobPostingService;

    @Test
    void findsJobPostingList() throws Exception {
        given(jobPostingService.findAll(any(JobPostingFindAllRequest.class)))
                .willReturn(new JobPostingFindAllResponse(
                        List.of(new JobPostingFindAllResponse.Item(
                                100L, "토스뱅크", JobRole.BACKEND, "정규직", "경력 3년 이상", "Server Developer",
                                LocalDate.of(2026, 11, 30), 5, List.of("Kotlin, Spring", "데이터 모델링", "대용량 트래픽 처리"))),
                        JobPostingListMetaResponse.of("next-cursor", true, JobRole.BACKEND,
                                Map.of(JobRole.BACKEND, 3L, JobRole.FRONTEND, 2L))
                ));

        mockMvc.perform(get("/api/v1/job-postings")
                        .queryParam("jobRole", "BACKEND")
                        .queryParam("keyword", "토스")
                        .queryParam("sort", "LATEST")
                        .queryParam("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].jobPostingId").value(100L))
                .andExpect(jsonPath("$.data[0].createdAt").doesNotExist())
                .andExpect(jsonPath("$.meta.totalCount").value(3L))
                .andExpect(jsonPath("$.meta.allCount").value(5L))
                .andExpect(jsonPath("$.meta.jobRoleCounts.length()").value(JobRole.values().length))
                .andDo(document(
                        "job-posting-find-all",
                        resource(ResourceSnippetParameters.builder()
                                .tag(TAG)
                                .summary("채용공고 목록 조회")
                                .description("""
                                        분석이 끝났고, 숨기지 않았고, 마감되지 않은(마감일 당일 포함, 한국 날짜 기준) 채용공고를 최신 등록순으로 조회한다. 로그인 없이 볼 수 있다.
                                        직군 탭 개수(jobRoleCounts)와 allCount는 검색어만 반영하고 직군 조건은 반영하지 않는다.
                                        jobRole, sort, size, keyword 값이 올바르지 않으면 400(VALIDATION_FAILED)으로 응답한다.""")
                                .queryParameters(
                                        parameterWithName("jobRole").type(SimpleType.STRING)
                                                .description("직군(BACKEND, FRONTEND, ANDROID, IOS, DEVOPS, DATA, AI, ETC). 없으면 전체 직군").optional(),
                                        parameterWithName("keyword").type(SimpleType.STRING)
                                                .description("검색어(100자 이하). 회사 이름, 공고 제목, 역량 이름에서 대소문자 구분 없이 찾는다").optional(),
                                        parameterWithName("sort").type(SimpleType.STRING).defaultValue("LATEST")
                                                .description("정렬. 지금은 LATEST(최신 등록순)만 있다").optional(),
                                        parameterWithName("cursor").type(SimpleType.STRING)
                                                .description("다음 페이지 조회용 커서. 올바르지 않거나 정렬과 맞지 않으면 400(JOB_POSTING_CURSOR_INVALID)").optional(),
                                        parameterWithName("size").type(SimpleType.INTEGER).defaultValue(20)
                                                .description("조회 개수(1~50), 기본값 20").optional()
                                )
                                .responseSchema(Schema.schema("JobPostingFindAllSuccessResponse"))
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data").type(ARRAY).description("채용공고 목록"),
                                        fieldWithPath("data[].jobPostingId").type(NUMBER).description("채용공고 ID"),
                                        fieldWithPath("data[].companyName").type(STRING).description("회사명"),
                                        new EnumFields(JobRole.class).withPath("data[].jobRole").description("직군"),
                                        fieldWithPath("data[].employmentType").type(STRING).description("고용 형태").optional(),
                                        fieldWithPath("data[].careerRequirement").type(STRING).description("경력 조건").optional(),
                                        fieldWithPath("data[].title").type(STRING).description("공고 제목"),
                                        fieldWithPath("data[].deadline").type(STRING)
                                                .description("마감일(yyyy-MM-dd). 없으면 null이고 상시 채용").optional(),
                                        fieldWithPath("data[].requirementCount").type(NUMBER).description("역량 수"),
                                        fieldWithPath("data[].requirementNames").type(ARRAY)
                                                .description("역량 이름. 역량 순서대로 앞 3개"),
                                        fieldWithPath("meta").type(OBJECT).description("페이지네이션과 직군별 개수"),
                                        fieldWithPath("meta.nextCursor").type(STRING).description("다음 페이지 커서. 마지막 페이지면 null")
                                                .optional(),
                                        fieldWithPath("meta.hasNext").type(BOOLEAN).description("다음 페이지 존재 여부"),
                                        fieldWithPath("meta.totalCount").type(NUMBER)
                                                .description("지금 고른 직군의 공고 수. 직군을 고르지 않았으면 allCount와 같다"),
                                        fieldWithPath("meta.jobRoleCounts").type(ARRAY)
                                                .description("직군별 공고 수. 모든 직군을 담고 공고가 없으면 0"),
                                        new EnumFields(JobRole.class).withPath("meta.jobRoleCounts[].jobRole").description("직군"),
                                        fieldWithPath("meta.jobRoleCounts[].count").type(NUMBER).description("공고 수"),
                                        fieldWithPath("meta.allCount").type(NUMBER).description("직군 조건을 뺀 전체 공고 수")
                                )
                                .build())
                ));

        ArgumentCaptor<JobPostingFindAllRequest> captor = ArgumentCaptor.forClass(JobPostingFindAllRequest.class);
        verify(jobPostingService).findAll(captor.capture());
        assertThat(captor.getValue())
                .isEqualTo(new JobPostingFindAllRequest(JobRole.BACKEND, "토스", JobPostingSort.LATEST, null, 20));
    }

    @Test
    void findsJobPostingDetail() throws Exception {
        given(jobPostingService.findDetail(JOB_POSTING_ID)).willReturn(new JobPostingDetailResponse(
                JOB_POSTING_ID, "토스뱅크", JobRole.BACKEND, "정규직", "경력 3년 이상", "Server Developer",
                LocalDate.of(2026, 11, 30), "https://careers.example.com/jobs/100",
                List.of(
                        new JobPostingDetailResponse.Requirement(11L, 1, "Kotlin, Spring", RequirementCategory.REQUIRED,
                                "Kotlin과 Spring으로 서버를 개발한 경험이 필요해요.", 5L),
                        new JobPostingDetailResponse.Requirement(12L, 2, "Kafka", RequirementCategory.PREFERRED,
                                "메시지 큐를 운영해 본 경험이 있으면 좋아요.", 0L))));

        mockMvc.perform(get("/api/v1/job-postings/{jobPostingId}", JOB_POSTING_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.jobPostingId").value(JOB_POSTING_ID))
                .andExpect(jsonPath("$.data.status").doesNotExist())
                .andExpect(jsonPath("$.data.requirements[0].recommendationCount").value(5L))
                .andExpect(jsonPath("$.data.requirements[1].recommendationCount").value(0L))
                .andDo(document(
                        "job-posting-find-detail",
                        resource(ResourceSnippetParameters.builder()
                                .tag(TAG)
                                .summary("채용공고 상세 조회")
                                .description("""
                                        채용공고 정보와 역량 목록, 역량마다 추천 자료 수를 조회한다. 로그인 없이 볼 수 있다.
                                        역량은 묶지 않고 공고에 적힌 순서(displayOrder)대로 한 목록으로 준다. 필수, 우대, 업무·기술 스택 구분은 category로 한다.
                                        추천 자료 수는 MATCH(딱 맞는 자료), PARTIAL(함께 보면 좋은 자료) 판정 중 지금 볼 수 있는 자료만 센다.
                                        없거나, 분석이 끝나지 않았거나, 숨겼거나, 마감된(마감일 당일은 볼 수 있음, 한국 날짜 기준) 공고는 404(JOB_POSTING_NOT_FOUND)로 응답한다.""")
                                .pathParameters(parameterWithName("jobPostingId").type(SimpleType.INTEGER)
                                        .description("채용공고 ID"))
                                .responseSchema(Schema.schema("JobPostingDetailSuccessResponse"))
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data").type(OBJECT).description("채용공고 상세"),
                                        fieldWithPath("data.jobPostingId").type(NUMBER).description("채용공고 ID"),
                                        fieldWithPath("data.companyName").type(STRING).description("회사명"),
                                        new EnumFields(JobRole.class).withPath("data.jobRole").description("직군"),
                                        fieldWithPath("data.employmentType").type(STRING).description("고용 형태").optional(),
                                        fieldWithPath("data.careerRequirement").type(STRING).description("경력 조건").optional(),
                                        fieldWithPath("data.title").type(STRING).description("공고 제목"),
                                        fieldWithPath("data.deadline").type(STRING)
                                                .description("마감일(yyyy-MM-dd). 없으면 null이고 상시 채용").optional(),
                                        fieldWithPath("data.sourceUrl").type(STRING).description("공고 원문 주소"),
                                        fieldWithPath("data.requirements").type(ARRAY).description("역량 목록. 공고에 적힌 순서대로"),
                                        fieldWithPath("data.requirements[].requirementId").type(NUMBER).description("역량 ID"),
                                        fieldWithPath("data.requirements[].displayOrder").type(NUMBER)
                                                .description("공고에 적힌 순서(1부터)"),
                                        fieldWithPath("data.requirements[].name").type(STRING).description("역량 이름"),
                                        new EnumFields(RequirementCategory.class).withPath("data.requirements[].category")
                                                .description("역량 구분"),
                                        fieldWithPath("data.requirements[].summary").type(STRING).description("역량 한 줄 설명"),
                                        fieldWithPath("data.requirements[].recommendationCount").type(NUMBER)
                                                .description("추천 자료 수. 없으면 0")
                                )
                                .build())
                ));
    }

    @Test
    void findsRequirementRecommendations() throws Exception {
        given(jobPostingService.findRecommendations(JOB_POSTING_ID, REQUIREMENT_ID)).willReturn(List.of(
                new JobRequirementRecommendationResponse(RecommendationVerdict.MATCH, "github-pr",
                        "주문 API 동시성 제어", "https://github.com/woowacourse-teams/example/pull/1",
                        "Spring 트랜잭션과 락으로 동시 주문을 막은 과정을 다룬다.", Instant.parse("2026-07-01T00:00:00Z"),
                        "problem_solving", "샤웃아웃즈", "dahye"),
                new JobRequirementRecommendationResponse(RecommendationVerdict.PARTIAL, "youtube-tekotalk",
                        "Kotlin 코루틴 입문", "https://www.youtube.com/watch?v=example",
                        "Kotlin 문법 일부를 설명한다.", Instant.parse("2025-03-10T00:00:00Z"),
                        null, null, "크루")));

        mockMvc.perform(get(RECOMMENDATIONS_URL, JOB_POSTING_ID, REQUIREMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].verdict").value("MATCH"))
                .andExpect(jsonPath("$.data[0].publishedAt").value("2026-07-01T00:00:00Z"))
                .andExpect(jsonPath("$.data[1].verdict").value("PARTIAL"))
                .andExpect(jsonPath("$.data[1].contentType").isEmpty())
                .andDo(document(
                        "job-posting-find-requirement-recommendations",
                        resource(ResourceSnippetParameters.builder()
                                .tag(TAG)
                                .summary("채용공고 역량별 추천 자료 조회")
                                .description("""
                                        역량 하나에 추천된 자료를 한 번에 모두 조회한다. 페이지를 나누지 않는다. 로그인 없이 볼 수 있다.
                                        MATCH(딱 맞는 자료), PARTIAL(함께 보면 좋은 자료) 판정만 주고, MATCH를 먼저, 같은 판정 안에서는 관련도가 높은 순(같으면 추천 ID 순)으로 준다.
                                        삭제됐거나 자료 정보가 없어 열 수 없는 자료는 뺀다. 자료가 없으면 빈 목록이다.
                                        개수는 상세 조회의 recommendationCount와 같다.
                                        source, contentType은 코드 그대로 주고 화면 문구(출처 이름, 배지)는 프론트가 정한다.
                                        볼 수 없는 공고(없음, 분석이 끝나지 않음, 숨김, 마감)는 404(JOB_POSTING_NOT_FOUND), 이 공고의 역량이 아니면 404(JOB_REQUIREMENT_NOT_FOUND)로 응답한다.""")
                                .pathParameters(
                                        parameterWithName("jobPostingId").type(SimpleType.INTEGER).description("채용공고 ID"),
                                        parameterWithName("requirementId").type(SimpleType.INTEGER)
                                                .description("역량 ID. 이 공고의 역량이어야 한다"))
                                .responseSchema(Schema.schema("JobRequirementRecommendationsSuccessResponse"))
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data").type(ARRAY).description("추천 자료 목록"),
                                        fieldWithPath("data[].verdict").type(STRING)
                                                .description("판정. MATCH(딱 맞는 자료), PARTIAL(함께 보면 좋은 자료)"),
                                        fieldWithPath("data[].source").type(STRING).description(
                                                "출처. github-pr(PR), github-teamdoc(팀 문서), github-wiki(위키), "
                                                        + "github-discussion(디스커션), blog-post(블로그 글), youtube-tekotalk(테코톡)"),
                                        fieldWithPath("data[].title").type(STRING).description("자료 제목"),
                                        fieldWithPath("data[].url").type(STRING).description("자료 주소"),
                                        fieldWithPath("data[].description").type(STRING)
                                                .description("이 자료가 역량과 어떻게 이어지는지 설명"),
                                        fieldWithPath("data[].publishedAt").type(STRING)
                                                .description("ISO-8601 작성 시각. 모르면 null").optional(),
                                        fieldWithPath("data[].contentType").type(STRING).description(
                                                "자료 성격. 없으면 null(블로그·테코톡은 항상 null). problem_solving(문제 해결), "
                                                        + "tech_decision(기술 결정), implementation(구현), chore(잡무), design(설계), "
                                                        + "operation(운영), experiment(실험), convention(컨벤션), plan(계획), other(기타)")
                                                .optional(),
                                        fieldWithPath("data[].teamName").type(STRING)
                                                .description("팀 이름. 연결된 프로젝트가 없거나 삭제됐으면 null").optional(),
                                        fieldWithPath("data[].authorDisplay").type(STRING)
                                                .description("작성자 표시 이름. 없으면 null").optional()
                                )
                                .build())
                ));
    }

    @Test
    void returnsNotFoundForRequirementOfOtherPosting() throws Exception {
        given(jobPostingService.findRecommendations(JOB_POSTING_ID, REQUIREMENT_ID))
                .willThrow(new EntityNotFoundException(JobPostingErrorCode.JOB_REQUIREMENT_NOT_FOUND));

        mockMvc.perform(get(RECOMMENDATIONS_URL, JOB_POSTING_ID, REQUIREMENT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("JOB_REQUIREMENT_NOT_FOUND"));
    }

    @Test
    void rejectsNonNumericRequirementId() throws Exception {
        mockMvc.perform(get(RECOMMENDATIONS_URL, JOB_POSTING_ID, "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(jobPostingService);
    }

    @Test
    void returnsNotFoundForInvisibleJobPosting() throws Exception {
        given(jobPostingService.findDetail(JOB_POSTING_ID))
                .willThrow(new EntityNotFoundException(JobPostingErrorCode.JOB_POSTING_NOT_FOUND));

        mockMvc.perform(get("/api/v1/job-postings/{jobPostingId}", JOB_POSTING_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("JOB_POSTING_NOT_FOUND"));
    }

    @Test
    void rejectsNonNumericJobPostingId() throws Exception {
        mockMvc.perform(get("/api/v1/job-postings/{jobPostingId}", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(jobPostingService);
    }

    @Test
    void rejectsUnknownJobRole() throws Exception {
        mockMvc.perform(get("/api/v1/job-postings").queryParam("jobRole", "DESIGNER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(jobPostingService);
    }

    @Test
    void rejectsUnsupportedSort() throws Exception {
        mockMvc.perform(get("/api/v1/job-postings").queryParam("sort", "DEADLINE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(jobPostingService);
    }

    @Test
    void rejectsTooLargeSize() throws Exception {
        mockMvc.perform(get("/api/v1/job-postings").queryParam("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(jobPostingService);
    }
}
