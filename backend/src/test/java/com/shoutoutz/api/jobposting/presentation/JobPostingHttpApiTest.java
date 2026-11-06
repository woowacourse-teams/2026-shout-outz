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
import com.shoutoutz.api.jobposting.domain.RequirementCategory;
import com.shoutoutz.api.jobposting.presentation.dto.request.JobPostingFindAllRequest;
import com.shoutoutz.api.jobposting.presentation.dto.response.JobPostingDetailResponse;
import com.shoutoutz.api.jobposting.presentation.dto.response.JobPostingFindAllResponse;
import com.shoutoutz.api.jobposting.presentation.dto.response.JobPostingListMetaResponse;
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
