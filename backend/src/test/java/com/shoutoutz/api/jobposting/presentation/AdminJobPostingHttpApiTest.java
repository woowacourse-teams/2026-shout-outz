package com.shoutoutz.api.jobposting.presentation;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.BOOLEAN;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.OBJECT;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.EnumFields;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.epages.restdocs.apispec.SimpleType;
import com.shoutoutz.api.auth.presentation.session.AuthenticatedSession;
import com.shoutoutz.api.common.response.SliceMetaResponse;
import com.shoutoutz.api.jobposting.application.JobPostingAdminService;
import com.shoutoutz.api.jobposting.domain.JobRole;
import com.shoutoutz.api.jobposting.domain.MatchingStatus;
import com.shoutoutz.api.jobposting.domain.RequirementCategory;
import com.shoutoutz.api.jobposting.presentation.dto.request.AdminJobPostingExtractRequest;
import com.shoutoutz.api.jobposting.presentation.dto.request.AdminJobPostingFindAllRequest;
import com.shoutoutz.api.jobposting.presentation.dto.request.AdminJobPostingSaveRequest;
import com.shoutoutz.api.jobposting.presentation.dto.request.AdminJobPostingVisibilityUpdateRequest;
import com.shoutoutz.api.jobposting.presentation.dto.response.AdminJobPostingDetailResponse;
import com.shoutoutz.api.jobposting.presentation.dto.response.AdminJobPostingExtractResponse;
import com.shoutoutz.api.jobposting.presentation.dto.response.AdminJobPostingFindAllResponse;
import com.shoutoutz.api.jobposting.presentation.dto.response.AdminJobPostingSaveResponse;
import com.shoutoutz.api.jobposting.presentation.dto.response.AdminJobPostingUpdateResponse;
import com.shoutoutz.api.jobposting.presentation.dto.response.AdminJobPostingVisibilityUpdateResponse;
import com.shoutoutz.api.user.domain.account.UserRole;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(controllers = AdminJobPostingHttpApi.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs
class AdminJobPostingHttpApiTest {

    private static final long JOB_POSTING_ID = 100L;
    private static final long ADMIN_ID = 7L;
    private static final Instant NOW = Instant.parse("2026-10-27T00:00:00Z");
    private static final String TAG = "Admin Job Posting";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobPostingAdminService jobPostingAdminService;

    @Test
    void extractsJobPosting() throws Exception {
        given(jobPostingAdminService.extract(eq(UserRole.ADMIN), any(AdminJobPostingExtractRequest.class)))
                .willReturn(new AdminJobPostingExtractResponse(
                        "우아한형제들",
                        JobRole.BACKEND,
                        "백엔드 개발자",
                        "정규직",
                        "신입",
                        LocalDate.of(2026, 12, 31),
                        List.of(new AdminJobPostingExtractResponse.Requirement(
                                "Java, Spring", RequirementCategory.REQUIRED, "Spring으로 API 서버를 개발한 경험이 필요해요.")),
                        null
                ));

        mockMvc.perform(post("/api/v1/admin/job-postings/extractions")
                        .with(authenticated(UserRole.ADMIN))
                        .header(HttpHeaders.COOKIE, "JSESSIONID=admin-session")
                        .header("X-CSRF-Token", "csrf-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sourceUrl":"https://career.example.com/jobs/1","content":"[백엔드 개발자] 자격 요건: Java, Spring 경험"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.companyName").value("우아한형제들"))
                .andExpect(jsonPath("$.data.requirements[0].category").value("REQUIRED"))
                .andDo(document(
                        "admin-job-posting-extract",
                        resource(ResourceSnippetParameters.builder()
                                .tag(TAG)
                                .summary("채용공고 정보와 역량 추출")
                                .description("""
                                        관리자가 붙여넣은 공고 글에서 LLM으로 공고 정보와 역량을 뽑는다. 아무것도 저장하지 않는다.
                                        같은 주소로 등록된 공고가 있으면 registeredJobPostingId에 그 ID를 담는다.
                                        LLM 호출이 실패하거나 시간이 초과되면 503(JOB_POSTING_EXTRACTION_FAILED)으로 응답한다.""")
                                .requestHeaders(
                                        headerWithName(HttpHeaders.COOKIE)
                                                .description("인증된 관리자의 JSESSIONID"),
                                        headerWithName("X-CSRF-Token")
                                                .description("세션 조회로 발급받은 CSRF 토큰")
                                )
                                .requestSchema(Schema.schema("AdminJobPostingExtractRequest"))
                                .requestFields(
                                        fieldWithPath("sourceUrl").type(STRING)
                                                .description("채용공고 주소(http 또는 https, 1,024자 이하)"),
                                        fieldWithPath("content").type(STRING)
                                                .description("공고 글(10,000자 이하). 저장하지 않는다.")
                                )
                                .responseSchema(Schema.schema("AdminJobPostingExtractSuccessResponse"))
                                .responseFields(extractResponseFields())
                                .build())
                ));
    }

    @Test
    void registersJobPosting() throws Exception {
        given(jobPostingAdminService.register(eq(ADMIN_ID), eq(UserRole.ADMIN), any(AdminJobPostingSaveRequest.class)))
                .willReturn(new AdminJobPostingSaveResponse(JOB_POSTING_ID, MatchingStatus.PENDING, NOW));

        mockMvc.perform(post("/api/v1/admin/job-postings")
                        .with(authenticated(UserRole.ADMIN))
                        .header(HttpHeaders.COOKIE, "JSESSIONID=admin-session")
                        .header("X-CSRF-Token", "csrf-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveRequestBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.jobPostingId").value(JOB_POSTING_ID))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andDo(document(
                        "admin-job-posting-register",
                        resource(ResourceSnippetParameters.builder()
                                .tag(TAG)
                                .summary("채용공고 등록")
                                .description("""
                                        관리자가 확인하고 고친 공고 정보와 역량을 저장한다. LLM을 다시 부르지 않는다.
                                        공고 글은 해시만 만들고 저장하지 않는다. 역량 순서는 보낸 순서를 따른다.
                                        같은 주소의 공고가 이미 있으면 409(JOB_POSTING_DUPLICATE_URL)로 응답한다.""")
                                .requestHeaders(
                                        headerWithName(HttpHeaders.COOKIE)
                                                .description("인증된 관리자의 JSESSIONID"),
                                        headerWithName("X-CSRF-Token")
                                                .description("세션 조회로 발급받은 CSRF 토큰")
                                )
                                .requestSchema(Schema.schema("AdminJobPostingSaveRequest"))
                                .requestFields(saveRequestFields())
                                .responseSchema(Schema.schema("AdminJobPostingSaveSuccessResponse"))
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data").type(OBJECT).description("등록한 채용공고"),
                                        fieldWithPath("data.jobPostingId").type(NUMBER).description("채용공고 ID"),
                                        new EnumFields(MatchingStatus.class).withPath("data.status")
                                                .description("자료 분석 상태"),
                                        fieldWithPath("data.createdAt").type(STRING).description("등록 시각")
                                )
                                .build())
                ));

        verify(jobPostingAdminService).register(eq(ADMIN_ID), eq(UserRole.ADMIN), any(AdminJobPostingSaveRequest.class));
    }

    @Test
    void findsJobPostingList() throws Exception {
        given(jobPostingAdminService.findAll(eq(UserRole.ADMIN), any(AdminJobPostingFindAllRequest.class)))
                .willReturn(new AdminJobPostingFindAllResponse(
                        List.of(new AdminJobPostingFindAllResponse.Item(
                                JOB_POSTING_ID, "우아한형제들", "백엔드 개발자", JobRole.BACKEND, MatchingStatus.PENDING, NOW)),
                        new SliceMetaResponse("next-cursor", true, 3L)
                ));

        mockMvc.perform(get("/api/v1/admin/job-postings")
                        .with(authenticated(UserRole.ADMIN))
                        .header(HttpHeaders.COOKIE, "JSESSIONID=admin-session")
                        .queryParam("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].jobPostingId").value(JOB_POSTING_ID))
                .andExpect(jsonPath("$.meta.nextCursor").value("next-cursor"))
                .andDo(document(
                        "admin-job-posting-find-all",
                        resource(ResourceSnippetParameters.builder()
                                .tag(TAG)
                                .summary("채용공고 목록 조회")
                                .description("관리자가 등록한 채용공고를 최신 등록순으로 조회한다.")
                                .requestHeaders(headerWithName(HttpHeaders.COOKIE)
                                        .description("인증된 관리자의 JSESSIONID"))
                                .queryParameters(
                                        parameterWithName("size").type(SimpleType.INTEGER).defaultValue(20)
                                                .description("조회 개수(1~100), 기본값 20").optional(),
                                        parameterWithName("cursor").type(SimpleType.STRING)
                                                .description("다음 페이지 조회용 커서. 올바르지 않으면 400(JOB_POSTING_ADMIN_CURSOR_INVALID)").optional()
                                )
                                .responseSchema(Schema.schema("AdminJobPostingFindAllSuccessResponse"))
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data").type(ARRAY).description("채용공고 목록"),
                                        fieldWithPath("data[].jobPostingId").type(NUMBER).description("채용공고 ID"),
                                        fieldWithPath("data[].companyName").type(STRING).description("회사명"),
                                        fieldWithPath("data[].title").type(STRING).description("공고 제목"),
                                        new EnumFields(JobRole.class).withPath("data[].jobRole").description("직무"),
                                        new EnumFields(MatchingStatus.class).withPath("data[].status")
                                                .description("자료 분석 상태"),
                                        fieldWithPath("data[].createdAt").type(STRING).description("등록 시각"),
                                        fieldWithPath("meta").type(OBJECT).description("페이지네이션 정보"),
                                        fieldWithPath("meta.nextCursor").type(STRING).description("다음 페이지 커서")
                                                .optional(),
                                        fieldWithPath("meta.hasNext").type(BOOLEAN).description("다음 페이지 존재 여부"),
                                        fieldWithPath("meta.totalCount").type(NUMBER).description("전체 채용공고 수")
                                )
                                .build())
                ));
    }

    @Test
    void findsJobPostingDetail() throws Exception {
        given(jobPostingAdminService.findDetail(JOB_POSTING_ID, UserRole.ADMIN))
                .willReturn(new AdminJobPostingDetailResponse(
                        JOB_POSTING_ID,
                        "https://career.example.com/jobs/1",
                        "우아한형제들",
                        JobRole.BACKEND,
                        "신입",
                        "백엔드 개발자",
                        "정규직",
                        LocalDate.of(2026, 12, 31),
                        MatchingStatus.PENDING,
                        ADMIN_ID,
                        NOW,
                        List.of(new AdminJobPostingDetailResponse.Requirement(
                                1000L, 1, "Java, Spring", RequirementCategory.REQUIRED,
                                "Spring으로 API 서버를 개발한 경험이 필요해요.", MatchingStatus.PENDING))
                ));

        mockMvc.perform(get("/api/v1/admin/job-postings/{jobPostingId}", JOB_POSTING_ID)
                        .with(authenticated(UserRole.ADMIN))
                        .header(HttpHeaders.COOKIE, "JSESSIONID=admin-session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.jobPostingId").value(JOB_POSTING_ID))
                .andExpect(jsonPath("$.data.requirements[0].displayOrder").value(1))
                .andDo(document(
                        "admin-job-posting-find-detail",
                        resource(ResourceSnippetParameters.builder()
                                .tag(TAG)
                                .summary("채용공고 상세 조회")
                                .description("관리자가 채용공고 정보와 역량 목록을 조회한다. 없으면 404(JOB_POSTING_NOT_FOUND)로 응답한다.")
                                .requestHeaders(headerWithName(HttpHeaders.COOKIE)
                                        .description("인증된 관리자의 JSESSIONID"))
                                .pathParameters(parameterWithName("jobPostingId").type(SimpleType.INTEGER)
                                        .description("채용공고 ID"))
                                .responseSchema(Schema.schema("AdminJobPostingDetailSuccessResponse"))
                                .responseFields(detailResponseFields())
                                .build())
                ));
    }

    @Test
    void updatesJobPosting() throws Exception {
        given(jobPostingAdminService.update(eq(JOB_POSTING_ID), eq(UserRole.ADMIN), any(AdminJobPostingSaveRequest.class)))
                .willReturn(new AdminJobPostingUpdateResponse(JOB_POSTING_ID, MatchingStatus.PENDING, true));

        mockMvc.perform(put("/api/v1/admin/job-postings/{jobPostingId}", JOB_POSTING_ID)
                        .with(authenticated(UserRole.ADMIN))
                        .header(HttpHeaders.COOKIE, "JSESSIONID=admin-session")
                        .header("X-CSRF-Token", "csrf-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveRequestBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.jobPostingId").value(JOB_POSTING_ID))
                .andExpect(jsonPath("$.data.reanalyzed").value(true))
                .andDo(document(
                        "admin-job-posting-update",
                        resource(ResourceSnippetParameters.builder()
                                .tag(TAG)
                                .summary("채용공고 변경")
                                .description("""
                                        기업이 바꾼 공고를 관리자가 다시 붙여넣어 고친다. 요청은 등록과 같다.
                                        공고 글의 해시가 같으면 공고 정보만 고치고 역량과 분석 상태는 그대로 둔다. 이때 요청의 역량은 반영하지 않는다(reanalyzed=false).
                                        해시가 다르면 역량을 요청 목록으로 바꾸고 분석 상태를 PENDING으로 되돌려 다시 분석한다(reanalyzed=true).
                                        숨김 상태와 등록자, 등록 시각은 바꾸지 않는다.
                                        주소가 공고의 주소와 다르면 400(JOB_POSTING_URL_MISMATCH), 공고가 없으면 404(JOB_POSTING_NOT_FOUND)로 응답한다.
                                        관리자가 아니면 403(JOB_POSTING_ADMIN_FORBIDDEN)으로 응답한다.""")
                                .requestHeaders(
                                        headerWithName(HttpHeaders.COOKIE)
                                                .description("인증된 관리자의 JSESSIONID"),
                                        headerWithName("X-CSRF-Token")
                                                .description("세션 조회로 발급받은 CSRF 토큰")
                                )
                                .pathParameters(parameterWithName("jobPostingId").type(SimpleType.INTEGER)
                                        .description("채용공고 ID"))
                                .requestSchema(Schema.schema("AdminJobPostingSaveRequest"))
                                .requestFields(saveRequestFields())
                                .responseSchema(Schema.schema("AdminJobPostingUpdateSuccessResponse"))
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data").type(OBJECT).description("변경한 채용공고"),
                                        fieldWithPath("data.jobPostingId").type(NUMBER).description("채용공고 ID"),
                                        new EnumFields(MatchingStatus.class).withPath("data.status")
                                                .description("변경 후 자료 분석 상태"),
                                        fieldWithPath("data.reanalyzed").type(BOOLEAN)
                                                .description("공고 글이 바뀌어 역량을 바꾸고 다시 분석하면 true. false면 요청의 역량은 반영되지 않았다.")
                                )
                                .build())
                ));
    }

    @Test
    void updatesJobPostingVisibility() throws Exception {
        given(jobPostingAdminService.updateVisibility(
                eq(JOB_POSTING_ID), eq(UserRole.ADMIN), any(AdminJobPostingVisibilityUpdateRequest.class)))
                .willReturn(new AdminJobPostingVisibilityUpdateResponse(JOB_POSTING_ID, true, NOW));

        mockMvc.perform(patch("/api/v1/admin/job-postings/{jobPostingId}/visibility", JOB_POSTING_ID)
                        .with(authenticated(UserRole.ADMIN))
                        .header(HttpHeaders.COOKIE, "JSESSIONID=admin-session")
                        .header("X-CSRF-Token", "csrf-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"hidden": true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hidden").value(true))
                .andDo(document(
                        "admin-job-posting-update-visibility",
                        resource(ResourceSnippetParameters.builder()
                                .tag(TAG)
                                .summary("채용공고 숨김·숨김 해제")
                                .description("""
                                        채용공고를 숨기거나 숨김을 해제한다. 분석 상태와 역량은 바꾸지 않는다.
                                        이미 숨긴 공고를 다시 숨기면 처음 숨긴 시각을 그대로 두고, 숨기지 않은 공고를 해제해도 성공한다.
                                        hidden이 없으면 400, 공고가 없으면 404(JOB_POSTING_NOT_FOUND)로 응답한다.
                                        관리자가 아니면 403(JOB_POSTING_ADMIN_FORBIDDEN)으로 응답한다.""")
                                .requestHeaders(
                                        headerWithName(HttpHeaders.COOKIE)
                                                .description("인증된 관리자의 JSESSIONID"),
                                        headerWithName("X-CSRF-Token")
                                                .description("세션 조회로 발급받은 CSRF 토큰")
                                )
                                .pathParameters(parameterWithName("jobPostingId").type(SimpleType.INTEGER)
                                        .description("채용공고 ID"))
                                .requestSchema(Schema.schema("AdminJobPostingVisibilityUpdateRequest"))
                                .requestFields(
                                        fieldWithPath("hidden").type(BOOLEAN).description("숨기려면 true, 해제하려면 false")
                                )
                                .responseSchema(Schema.schema("AdminJobPostingVisibilityUpdateSuccessResponse"))
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data").type(OBJECT).description("바뀐 숨김 상태"),
                                        fieldWithPath("data.jobPostingId").type(NUMBER).description("채용공고 ID"),
                                        fieldWithPath("data.hidden").type(BOOLEAN).description("숨김 여부"),
                                        fieldWithPath("data.hiddenAt").type(STRING)
                                                .description("처음 숨긴 시각. 숨기지 않았으면 null").optional()
                                )
                                .build())
                ));
    }

    @Test
    void rejectsVisibilityUpdateWithoutHidden() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/job-postings/{jobPostingId}/visibility", JOB_POSTING_ID)
                        .with(authenticated(UserRole.ADMIN))
                        .header(HttpHeaders.COOKIE, "JSESSIONID=admin-session")
                        .header("X-CSRF-Token", "csrf-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(jobPostingAdminService);
    }

    @Test
    void deletesJobPosting() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/job-postings/{jobPostingId}", JOB_POSTING_ID)
                        .with(authenticated(UserRole.ADMIN))
                        .header(HttpHeaders.COOKIE, "JSESSIONID=admin-session")
                        .header("X-CSRF-Token", "csrf-token"))
                .andExpect(status().isNoContent())
                .andDo(document(
                        "admin-job-posting-delete",
                        resource(ResourceSnippetParameters.builder()
                                .tag(TAG)
                                .summary("채용공고 삭제")
                                .description("""
                                        채용공고와 역량, 추천 자료를 모두 지운다. 분석 중인 공고도 지울 수 있다.
                                        공고가 없으면 404(JOB_POSTING_NOT_FOUND)로 응답한다.
                                        관리자가 아니면 403(JOB_POSTING_ADMIN_FORBIDDEN)으로 응답한다.""")
                                .requestHeaders(
                                        headerWithName(HttpHeaders.COOKIE)
                                                .description("인증된 관리자의 JSESSIONID"),
                                        headerWithName("X-CSRF-Token")
                                                .description("세션 조회로 발급받은 CSRF 토큰")
                                )
                                .pathParameters(parameterWithName("jobPostingId").type(SimpleType.INTEGER)
                                        .description("채용공고 ID"))
                                .build())
                ));

        verify(jobPostingAdminService).delete(JOB_POSTING_ID, UserRole.ADMIN);
    }

    private static RequestPostProcessor authenticated(UserRole role) {
        return request -> {
            request.setAttribute(
                    AuthenticatedSession.class.getName(),
                    new AuthenticatedSession(ADMIN_ID, role)
            );
            return request;
        };
    }

    private static String saveRequestBody() {
        return """
                {
                  "sourceUrl": "https://career.example.com/jobs/1",
                  "content": "[백엔드 개발자] 자격 요건: Java, Spring 경험",
                  "companyName": "우아한형제들",
                  "jobRole": "BACKEND",
                  "careerRequirement": "신입",
                  "title": "백엔드 개발자",
                  "employmentType": "정규직",
                  "deadline": "2026-12-31",
                  "requirements": [
                    {"name": "Java, Spring", "category": "REQUIRED", "summary": "Spring으로 API 서버를 개발한 경험이 필요해요."}
                  ]
                }
                """;
    }

    private static List<FieldDescriptor> extractResponseFields() {
        return List.of(
                fieldWithPath("status").type(STRING).description("응답 상태"),
                fieldWithPath("data").type(OBJECT).description("추출 결과"),
                fieldWithPath("data.companyName").type(STRING).description("회사명. 찾지 못하면 null").optional(),
                new EnumFields(JobRole.class).withPath("data.jobRole")
                        .description("직무. 목록에 없는 직무는 ETC"),
                fieldWithPath("data.title").type(STRING).description("공고 제목. 찾지 못하면 null").optional(),
                fieldWithPath("data.employmentType").type(STRING).description("고용 형태. 찾지 못하면 null").optional(),
                fieldWithPath("data.careerRequirement").type(STRING).description("경력 조건. 찾지 못하면 null")
                        .optional(),
                fieldWithPath("data.deadline").type(STRING).description("마감일(yyyy-MM-dd). 찾지 못하면 null")
                        .optional(),
                fieldWithPath("data.requirements").type(ARRAY)
                        .description("역량 목록(0~50개). 비어 있으면 관리자가 역량을 채운 뒤 등록한다."),
                fieldWithPath("data.requirements[].name").type(STRING).description("역량 이름"),
                new EnumFields(RequirementCategory.class).withPath("data.requirements[].category")
                        .description("역량 구분"),
                fieldWithPath("data.requirements[].summary").type(STRING).description("역량 요약"),
                fieldWithPath("data.registeredJobPostingId").type(NUMBER)
                        .description("같은 주소로 이미 등록된 채용공고 ID. 없으면 null").optional()
        );
    }

    private static List<FieldDescriptor> saveRequestFields() {
        return List.of(
                fieldWithPath("sourceUrl").type(STRING).description("채용공고 주소(http 또는 https, 1,024자 이하)"),
                fieldWithPath("content").type(STRING).description("공고 글(10,000자 이하). 해시만 만들고 저장하지 않는다."),
                fieldWithPath("companyName").type(STRING).description("회사명(100자 이하)"),
                new EnumFields(JobRole.class).withPath("jobRole").description("직무"),
                fieldWithPath("careerRequirement").type(STRING).description("경력 조건(50자 이하)").optional(),
                fieldWithPath("title").type(STRING).description("공고 제목(200자 이하)"),
                fieldWithPath("employmentType").type(STRING).description("고용 형태(30자 이하)").optional(),
                fieldWithPath("deadline").type(STRING).description("마감일(yyyy-MM-dd)").optional(),
                fieldWithPath("requirements").type(ARRAY).description("역량 목록(1~50개)"),
                fieldWithPath("requirements[].name").type(STRING).description("역량 이름(100자 이하)"),
                new EnumFields(RequirementCategory.class).withPath("requirements[].category").description("역량 구분"),
                fieldWithPath("requirements[].summary").type(STRING).description("역량 요약(500자 이하)")
        );
    }

    private static List<FieldDescriptor> detailResponseFields() {
        return List.of(
                fieldWithPath("status").type(STRING).description("응답 상태"),
                fieldWithPath("data").type(OBJECT).description("채용공고 상세"),
                fieldWithPath("data.jobPostingId").type(NUMBER).description("채용공고 ID"),
                fieldWithPath("data.sourceUrl").type(STRING).description("정규화한 채용공고 주소"),
                fieldWithPath("data.companyName").type(STRING).description("회사명"),
                new EnumFields(JobRole.class).withPath("data.jobRole").description("직무"),
                fieldWithPath("data.careerRequirement").type(STRING).description("경력 조건").optional(),
                fieldWithPath("data.title").type(STRING).description("공고 제목"),
                fieldWithPath("data.employmentType").type(STRING).description("고용 형태").optional(),
                fieldWithPath("data.deadline").type(STRING).description("마감일(yyyy-MM-dd)").optional(),
                new EnumFields(MatchingStatus.class).withPath("data.status").description("자료 분석 상태"),
                fieldWithPath("data.registeredBy").type(NUMBER).description("등록한 관리자 ID"),
                fieldWithPath("data.createdAt").type(STRING).description("등록 시각"),
                fieldWithPath("data.requirements").type(ARRAY).description("역량 목록"),
                fieldWithPath("data.requirements[].requirementId").type(NUMBER).description("역량 ID"),
                fieldWithPath("data.requirements[].displayOrder").type(NUMBER).description("표시 순서(1부터)"),
                fieldWithPath("data.requirements[].name").type(STRING).description("역량 이름"),
                new EnumFields(RequirementCategory.class).withPath("data.requirements[].category")
                        .description("역량 구분"),
                fieldWithPath("data.requirements[].summary").type(STRING).description("역량 요약"),
                new EnumFields(MatchingStatus.class).withPath("data.requirements[].status")
                        .description("자료 분석 상태")
        );
    }
}
