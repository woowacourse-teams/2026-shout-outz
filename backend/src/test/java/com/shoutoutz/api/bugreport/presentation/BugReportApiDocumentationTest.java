package com.shoutoutz.api.bugreport.presentation;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.SimpleType.INTEGER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.BOOLEAN;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.OBJECT;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.EnumFields;
import com.epages.restdocs.apispec.ParameterDescriptorWithType;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.shoutoutz.api.auth.presentation.session.AuthenticatedSession;
import com.shoutoutz.api.bugreport.application.BugReportAdminService;
import com.shoutoutz.api.bugreport.application.BugReportService;
import com.shoutoutz.api.bugreport.domain.BugReportErrorCode;
import com.shoutoutz.api.bugreport.domain.BugReportStatus;
import com.shoutoutz.api.bugreport.presentation.dto.request.BugReportAdminFindAllRequest;
import com.shoutoutz.api.bugreport.presentation.dto.request.BugReportCreateRequest;
import com.shoutoutz.api.bugreport.presentation.dto.request.BugReportStatusUpdateRequest;
import com.shoutoutz.api.bugreport.presentation.dto.response.BugReportAdminDetailResponse;
import com.shoutoutz.api.bugreport.presentation.dto.response.BugReportAdminFindAllResponse;
import com.shoutoutz.api.bugreport.presentation.dto.response.BugReportCreateResponse;
import com.shoutoutz.api.bugreport.presentation.dto.response.BugReportStatusUpdateResponse;
import com.shoutoutz.api.common.exception.code.CommonErrorCode;
import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.common.restdocs.RestDocsFields;
import com.shoutoutz.api.common.response.SliceMetaResponse;
import com.shoutoutz.api.user.domain.account.UserRole;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(controllers = {BugReportHttpApi.class, AdminBugReportHttpApi.class})
@AutoConfigureRestDocs
@DisplayName("버그 제보 API 문서")
class BugReportApiDocumentationTest {

    private static final long BUG_REPORT_ID = 42L;
    private static final long REPORTER_USER_ID = 17L;
    private static final long ADMIN_USER_ID = 7L;
    private static final Instant CREATED_AT = Instant.parse("2026-10-08T09:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-10-08T10:00:00Z");
    private static final String NEXT_CURSOR = "MjAyNi0xMC0wOFQwOTowMDowMFp8NDI";
    private static final String CREATE_DESCRIPTION = "로그인한 사용자와 비로그인 사용자 모두 버그를 제보한다. 내용은 공백을 제거한 뒤 저장하며 1자 이상 5,000자 이하여야 한다. 제보 상태는 OPEN으로 시작한다. 내용이 비어 있거나 5,000자를 초과하면 400 VALIDATION_FAILED를 반환한다.";
    private static final String ADMIN_FIND_ALL_DESCRIPTION = "관리자가 버그 제보를 최신순으로 조회한다. status 기본값은 OPEN이며 OPEN, COMPLETED, ALL을 지원한다. size 기본값은 20이고 1~100을 허용한다. 다음 페이지는 응답 meta.nextCursor를 cursor로 전달해 조회한다. totalCount는 상태 필터에 해당하는 전체 제보 수다. 관리자 로그인 세션이 없으면 401, 관리자 권한이 없으면 403을 반환한다. status 또는 size가 유효하지 않으면 400 VALIDATION_FAILED, cursor가 유효하지 않으면 400 BUG_REPORT_ADMIN_CURSOR_INVALID를 반환한다.";
    private static final String ADMIN_DETAIL_DESCRIPTION = "관리자가 버그 제보의 전체 내용과 처리 이력을 조회한다. 관리자 로그인 세션이 필요하다. 세션이 없으면 401, 관리자 권한이 없으면 403, 제보가 없으면 404 BUG_REPORT_NOT_FOUND를 반환한다.";
    private static final String ADMIN_UPDATE_STATUS_DESCRIPTION = "관리자가 버그 제보 상태를 OPEN 또는 COMPLETED로 변경한다. 상태가 실제로 바뀌면 상태 변경 시각과 관리자 ID를 기록한다. 관리자 로그인 세션이 없으면 401, 관리자 권한이 없으면 403, 상태가 없거나 올바르지 않으면 400 VALIDATION_FAILED, 제보가 없으면 404 BUG_REPORT_NOT_FOUND를 반환한다.";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BugReportService bugReportService;

    @MockitoBean
    private BugReportAdminService bugReportAdminService;

    @Test
    @DisplayName("로그인 여부와 관계없이 버그를 제보한다")
    void createBugReport() throws Exception {
        given(bugReportService.create(eq(null), any(BugReportCreateRequest.class)))
                .willReturn(new BugReportCreateResponse(
                        BUG_REPORT_ID,
                        BugReportStatus.OPEN,
                        CREATED_AT
                ));

        mockMvc.perform(post("/api/v1/bug-reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "로그인 버튼을 누르면 오류가 발생합니다."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.bugReportId").value(BUG_REPORT_ID))
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andDo(document(
                        "bug-report-create",
                        resource(createSuccessResponse())
                ));
    }

    @Test
    @DisplayName("내용이 비어 있는 버그 제보는 거부한다")
    void rejectInvalidBugReport() throws Exception {
        mockMvc.perform(post("/api/v1/bug-reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "   "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andDo(document(
                        "bug-report-create-invalid",
                        resource(createErrorResponse())
                ));
    }

    @Test
    @DisplayName("관리자가 버그 제보를 조건과 커서로 조회한다")
    void findAllBugReports() throws Exception {
        given(bugReportAdminService.findAll(
                eq(UserRole.ADMIN),
                any(BugReportAdminFindAllRequest.class)
        )).willReturn(new BugReportAdminFindAllResponse(
                List.of(new BugReportAdminFindAllResponse.Item(
                        BUG_REPORT_ID,
                        "로그인 버튼을 누르면 오류가 발생합니다.",
                        BugReportStatus.OPEN,
                        REPORTER_USER_ID,
                        CREATED_AT,
                        UPDATED_AT,
                        null,
                        null
                )),
                new SliceMetaResponse(NEXT_CURSOR, true, 3L)
        ));

        mockMvc.perform(get("/api/v1/admin/bug-reports")
                        .with(authenticated(UserRole.ADMIN))
                        .queryParam("status", "ALL")
                        .queryParam("size", "1")
                        .queryParam("cursor", NEXT_CURSOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].bugReportId").value(BUG_REPORT_ID))
                .andExpect(jsonPath("$.meta.hasNext").value(true))
                .andDo(document(
                        "bug-report-admin-find-all",
                        resource(adminFindAllSuccessResponse())
                ));
    }

    @Test
    @DisplayName("잘못된 커서로 버그 제보 목록을 조회하면 거부한다")
    void rejectInvalidBugReportCursor() throws Exception {
        given(bugReportAdminService.findAll(
                eq(UserRole.ADMIN),
                any(BugReportAdminFindAllRequest.class)
        )).willThrow(new BadRequestException(
                BugReportErrorCode.BUG_REPORT_ADMIN_CURSOR_INVALID
        ));

        mockMvc.perform(get("/api/v1/admin/bug-reports")
                        .with(authenticated(UserRole.ADMIN))
                        .queryParam("cursor", "invalid-cursor"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUG_REPORT_ADMIN_CURSOR_INVALID"))
                .andDo(document(
                        "bug-report-admin-find-all-invalid-cursor",
                        resource(adminFindAllErrorResponse())
                ));
    }

    @Test
    @DisplayName("유효하지 않은 목록 필터로 버그 제보를 조회하면 거부한다")
    void rejectInvalidBugReportListFilter() throws Exception {
        mockMvc.perform(get("/api/v1/admin/bug-reports")
                        .with(authenticated(UserRole.ADMIN))
                        .queryParam("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andDo(document(
                        "bug-report-admin-find-all-invalid-filter",
                        resource(adminFindAllErrorResponse())
                ));
    }

    @Test
    @DisplayName("로그인하지 않은 관리자는 버그 제보 목록을 조회할 수 없다")
    void rejectAnonymousBugReportList() throws Exception {
        mockMvc.perform(get("/api/v1/admin/bug-reports"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.UNAUTHORIZED.name()))
                .andDo(document(
                        "bug-report-admin-find-all-unauthorized",
                        resource(adminFindAllErrorResponse())
                ));
    }

    @Test
    @DisplayName("일반 사용자는 버그 제보 목록을 조회할 수 없다")
    void rejectNonAdminBugReportList() throws Exception {
        given(bugReportAdminService.findAll(
                eq(UserRole.USER),
                any(BugReportAdminFindAllRequest.class)
        )).willThrow(new ForbiddenException(BugReportErrorCode.BUG_REPORT_ADMIN_FORBIDDEN));

        mockMvc.perform(get("/api/v1/admin/bug-reports")
                        .with(authenticated(UserRole.USER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("BUG_REPORT_ADMIN_FORBIDDEN"))
                .andDo(document(
                        "bug-report-admin-find-all-forbidden",
                        resource(adminFindAllErrorResponse())
                ));
    }

    @Test
    @DisplayName("관리자가 버그 제보 상세 내용을 조회한다")
    void findBugReportDetail() throws Exception {
        given(bugReportAdminService.findDetail(BUG_REPORT_ID, UserRole.ADMIN))
                .willReturn(new BugReportAdminDetailResponse(
                        BUG_REPORT_ID,
                        "로그인 버튼을 누르면 오류가 발생합니다.",
                        BugReportStatus.OPEN,
                        REPORTER_USER_ID,
                        CREATED_AT,
                        UPDATED_AT,
                        null,
                        null
                ));

        mockMvc.perform(get("/api/v1/admin/bug-reports/{bugReportId}", BUG_REPORT_ID)
                        .with(authenticated(UserRole.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("로그인 버튼을 누르면 오류가 발생합니다."))
                .andDo(document(
                        "bug-report-admin-find-detail",
                        resource(adminDetailSuccessResponse())
                ));
    }

    @Test
    @DisplayName("로그인하지 않은 관리자는 버그 제보 상세 내용을 조회할 수 없다")
    void rejectAnonymousBugReportDetail() throws Exception {
        mockMvc.perform(get("/api/v1/admin/bug-reports/{bugReportId}", BUG_REPORT_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.UNAUTHORIZED.name()))
                .andDo(document(
                        "bug-report-admin-find-detail-unauthorized",
                        resource(adminDetailErrorResponse())
                ));
    }

    @Test
    @DisplayName("일반 사용자는 버그 제보 상세 내용을 조회할 수 없다")
    void rejectNonAdminBugReportDetail() throws Exception {
        given(bugReportAdminService.findDetail(BUG_REPORT_ID, UserRole.USER))
                .willThrow(new ForbiddenException(BugReportErrorCode.BUG_REPORT_ADMIN_FORBIDDEN));

        mockMvc.perform(get("/api/v1/admin/bug-reports/{bugReportId}", BUG_REPORT_ID)
                        .with(authenticated(UserRole.USER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("BUG_REPORT_ADMIN_FORBIDDEN"))
                .andDo(document(
                        "bug-report-admin-find-detail-forbidden",
                        resource(adminDetailErrorResponse())
                ));
    }

    @Test
    @DisplayName("존재하지 않는 버그 제보 상세 조회는 404를 반환한다")
    void rejectMissingBugReportDetail() throws Exception {
        given(bugReportAdminService.findDetail(BUG_REPORT_ID, UserRole.ADMIN))
                .willThrow(new EntityNotFoundException(BugReportErrorCode.BUG_REPORT_NOT_FOUND));

        mockMvc.perform(get("/api/v1/admin/bug-reports/{bugReportId}", BUG_REPORT_ID)
                        .with(authenticated(UserRole.ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BUG_REPORT_NOT_FOUND"))
                .andDo(document(
                        "bug-report-admin-find-detail-not-found",
                        resource(adminDetailErrorResponse())
                ));
    }

    @Test
    @DisplayName("관리자가 버그 제보 상태를 변경한다")
    void updateBugReportStatus() throws Exception {
        given(bugReportAdminService.updateStatus(
                eq(BUG_REPORT_ID),
                eq(ADMIN_USER_ID),
                eq(UserRole.ADMIN),
                any(BugReportStatusUpdateRequest.class)
        )).willReturn(new BugReportStatusUpdateResponse(
                BUG_REPORT_ID,
                BugReportStatus.COMPLETED,
                UPDATED_AT,
                ADMIN_USER_ID,
                UPDATED_AT
        ));

        mockMvc.perform(patch("/api/v1/admin/bug-reports/{bugReportId}/status", BUG_REPORT_ID)
                        .with(authenticated(UserRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "COMPLETED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andDo(document(
                        "bug-report-admin-update-status",
                        resource(adminUpdateStatusSuccessResponse())
                ));
    }

    @Test
    @DisplayName("상태가 없는 버그 제보 상태 변경은 거부한다")
    void rejectInvalidBugReportStatusUpdate() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/bug-reports/{bugReportId}/status", BUG_REPORT_ID)
                        .with(authenticated(UserRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andDo(document(
                        "bug-report-admin-update-status-invalid",
                        resource(adminUpdateStatusErrorResponse())
                ));
    }

    @Test
    @DisplayName("로그인하지 않은 관리자는 버그 제보 상태를 변경할 수 없다")
    void rejectAnonymousBugReportStatusUpdate() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/bug-reports/{bugReportId}/status", BUG_REPORT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "COMPLETED"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.UNAUTHORIZED.name()))
                .andDo(document(
                        "bug-report-admin-update-status-unauthorized",
                        resource(adminUpdateStatusErrorResponse())
                ));
    }

    @Test
    @DisplayName("일반 사용자는 버그 제보 상태를 변경할 수 없다")
    void rejectNonAdminBugReportStatusUpdate() throws Exception {
        given(bugReportAdminService.updateStatus(
                eq(BUG_REPORT_ID),
                eq(ADMIN_USER_ID),
                eq(UserRole.USER),
                any(BugReportStatusUpdateRequest.class)
        )).willThrow(new ForbiddenException(BugReportErrorCode.BUG_REPORT_ADMIN_FORBIDDEN));

        mockMvc.perform(patch("/api/v1/admin/bug-reports/{bugReportId}/status", BUG_REPORT_ID)
                        .with(authenticated(UserRole.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "COMPLETED"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("BUG_REPORT_ADMIN_FORBIDDEN"))
                .andDo(document(
                        "bug-report-admin-update-status-forbidden",
                        resource(adminUpdateStatusErrorResponse())
                ));
    }

    @Test
    @DisplayName("존재하지 않는 버그 제보 상태 변경은 404를 반환한다")
    void rejectMissingBugReportStatusUpdate() throws Exception {
        given(bugReportAdminService.updateStatus(
                eq(BUG_REPORT_ID),
                eq(ADMIN_USER_ID),
                eq(UserRole.ADMIN),
                any(BugReportStatusUpdateRequest.class)
        )).willThrow(new EntityNotFoundException(BugReportErrorCode.BUG_REPORT_NOT_FOUND));

        mockMvc.perform(patch("/api/v1/admin/bug-reports/{bugReportId}/status", BUG_REPORT_ID)
                        .with(authenticated(UserRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "COMPLETED"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BUG_REPORT_NOT_FOUND"))
                .andDo(document(
                        "bug-report-admin-update-status-not-found",
                        resource(adminUpdateStatusErrorResponse())
                ));
    }

    private ResourceSnippetParameters createSuccessResponse() {
        return ResourceSnippetParameters.builder()
                .tag("BugReport")
                .summary("버그 제보")
                .description(CREATE_DESCRIPTION)
                .requestSchema(Schema.schema("BugReportCreateRequest"))
                .responseSchema(Schema.schema("BugReportCreateSuccessResponse"))
                .requestFields(createRequestFields())
                .responseFields(
                        fieldWithPath("status").type(STRING).description("응답 상태"),
                        fieldWithPath("data").type(OBJECT).description("생성된 버그 제보"),
                        fieldWithPath("data.bugReportId").type(NUMBER).description("버그 제보 ID"),
                        new EnumFields(BugReportStatus.class).withPath("data.status")
                                .description("제보 상태. 생성 직후 OPEN"),
                        fieldWithPath("data.createdAt").type(STRING).description("ISO-8601 생성 시각")
                )
                .build();
    }

    private ResourceSnippetParameters createErrorResponse() {
        return ResourceSnippetParameters.builder()
                .tag("BugReport")
                .summary("버그 제보")
                .description(CREATE_DESCRIPTION)
                .requestSchema(Schema.schema("BugReportCreateRequest"))
                .responseSchema(Schema.schema("ErrorResponse"))
                .requestFields(createRequestFields())
                .responseFields(RestDocsFields.errorResponse())
                .build();
    }

    private ResourceSnippetParameters adminFindAllSuccessResponse() {
        return ResourceSnippetParameters.builder()
                .tag("BugReport")
                .summary("버그 제보 목록 조회")
                .description(ADMIN_FIND_ALL_DESCRIPTION)
                .queryParameters(adminFindAllParameters())
                .responseSchema(Schema.schema("BugReportAdminFindAllSuccessResponse"))
                .responseFields(
                        fieldWithPath("status").type(STRING).description("응답 상태"),
                        fieldWithPath("data").type(ARRAY).description("버그 제보 미리보기 목록"),
                        fieldWithPath("data[].bugReportId").type(NUMBER).description("버그 제보 ID"),
                        fieldWithPath("data[].contentPreview").type(STRING).description("전체 내용의 미리보기. 최대 200자"),
                        new EnumFields(BugReportStatus.class).withPath("data[].status")
                                .description("제보 상태"),
                        fieldWithPath("data[].reporterUserId").type(NUMBER)
                                .description("제보한 사용자 ID. 비로그인 제보이면 null").optional(),
                        fieldWithPath("data[].createdAt").type(STRING).description("ISO-8601 생성 시각"),
                        fieldWithPath("data[].updatedAt").type(STRING).description("ISO-8601 수정 시각"),
                        fieldWithPath("data[].statusChangedAt").type(STRING)
                                .description("마지막 상태 변경 시각. 변경 전이면 null").optional(),
                        fieldWithPath("data[].statusChangedByUserId").type(NUMBER)
                                .description("마지막으로 상태를 변경한 관리자 ID. 변경 전이면 null").optional(),
                        fieldWithPath("meta").type(OBJECT).description("커서 페이지네이션 정보"),
                        fieldWithPath("meta.nextCursor").type(STRING)
                                .description("다음 페이지 요청에 사용할 커서. 다음 페이지가 없으면 null").optional(),
                        fieldWithPath("meta.hasNext").type(BOOLEAN).description("다음 페이지 존재 여부"),
                        fieldWithPath("meta.totalCount").type(NUMBER).description("상태 필터에 해당하는 전체 제보 수")
                )
                .build();
    }

    private ResourceSnippetParameters adminFindAllErrorResponse() {
        return ResourceSnippetParameters.builder()
                .tag("BugReport")
                .summary("버그 제보 목록 조회")
                .description(ADMIN_FIND_ALL_DESCRIPTION)
                .queryParameters(adminFindAllParameters())
                .responseSchema(Schema.schema("ErrorResponse"))
                .responseFields(RestDocsFields.errorResponse())
                .build();
    }

    private ResourceSnippetParameters adminDetailSuccessResponse() {
        return ResourceSnippetParameters.builder()
                .tag("BugReport")
                .summary("버그 제보 상세 조회")
                .description(ADMIN_DETAIL_DESCRIPTION)
                .pathParameters(bugReportIdParameter())
                .responseSchema(Schema.schema("BugReportAdminDetailSuccessResponse"))
                .responseFields(
                        fieldWithPath("status").type(STRING).description("응답 상태"),
                        fieldWithPath("data").type(OBJECT).description("버그 제보 상세 정보"),
                        fieldWithPath("data.bugReportId").type(NUMBER).description("버그 제보 ID"),
                        fieldWithPath("data.content").type(STRING).description("버그 제보 전체 내용"),
                        new EnumFields(BugReportStatus.class).withPath("data.status")
                                .description("제보 상태"),
                        fieldWithPath("data.reporterUserId").type(NUMBER)
                                .description("제보한 사용자 ID. 비로그인 제보이면 null").optional(),
                        fieldWithPath("data.createdAt").type(STRING).description("ISO-8601 생성 시각"),
                        fieldWithPath("data.updatedAt").type(STRING).description("ISO-8601 수정 시각"),
                        fieldWithPath("data.statusChangedAt").type(STRING)
                                .description("마지막 상태 변경 시각. 변경 전이면 null").optional(),
                        fieldWithPath("data.statusChangedByUserId").type(NUMBER)
                                .description("마지막으로 상태를 변경한 관리자 ID. 변경 전이면 null").optional()
                )
                .build();
    }

    private ResourceSnippetParameters adminDetailErrorResponse() {
        return ResourceSnippetParameters.builder()
                .tag("BugReport")
                .summary("버그 제보 상세 조회")
                .description(ADMIN_DETAIL_DESCRIPTION)
                .pathParameters(bugReportIdParameter())
                .responseSchema(Schema.schema("ErrorResponse"))
                .responseFields(RestDocsFields.errorResponse())
                .build();
    }

    private ResourceSnippetParameters adminUpdateStatusSuccessResponse() {
        return ResourceSnippetParameters.builder()
                .tag("BugReport")
                .summary("버그 제보 상태 변경")
                .description(ADMIN_UPDATE_STATUS_DESCRIPTION)
                .pathParameters(bugReportIdParameter())
                .requestSchema(Schema.schema("BugReportStatusUpdateRequest"))
                .responseSchema(Schema.schema("BugReportStatusUpdateSuccessResponse"))
                .requestFields(statusUpdateRequestFields())
                .responseFields(
                        fieldWithPath("status").type(STRING).description("응답 상태"),
                        fieldWithPath("data").type(OBJECT).description("변경된 버그 제보 상태"),
                        fieldWithPath("data.bugReportId").type(NUMBER).description("버그 제보 ID"),
                        new EnumFields(BugReportStatus.class).withPath("data.status")
                                .description("변경된 제보 상태"),
                        fieldWithPath("data.statusChangedAt").type(STRING)
                                .description("마지막 상태 변경 시각. 상태 변경 이력이 없으면 null").optional(),
                        fieldWithPath("data.statusChangedByUserId").type(NUMBER)
                                .description("마지막으로 상태를 변경한 관리자 ID. 상태 변경 이력이 없으면 null").optional(),
                        fieldWithPath("data.updatedAt").type(STRING).description("ISO-8601 수정 시각")
                )
                .build();
    }

    private ResourceSnippetParameters adminUpdateStatusErrorResponse() {
        return ResourceSnippetParameters.builder()
                .tag("BugReport")
                .summary("버그 제보 상태 변경")
                .description(ADMIN_UPDATE_STATUS_DESCRIPTION)
                .pathParameters(bugReportIdParameter())
                .requestSchema(Schema.schema("BugReportStatusUpdateRequest"))
                .responseSchema(Schema.schema("ErrorResponse"))
                .responseFields(RestDocsFields.errorResponse())
                .build();
    }

    private List<FieldDescriptor> createRequestFields() {
        return List.of(
                fieldWithPath("content").type(STRING)
                        .description("버그 제보 내용. 공백 제거 후 1~5,000자")
        );
    }

    private List<FieldDescriptor> statusUpdateRequestFields() {
        return List.of(
                new EnumFields(BugReportStatus.class).withPath("status")
                        .description("변경할 제보 상태. OPEN 또는 COMPLETED")
        );
    }

    private List<ParameterDescriptorWithType> adminFindAllParameters() {
        return List.of(
                parameterWithName("status").description("조회할 상태. OPEN, COMPLETED, ALL 중 하나. 생략하면 OPEN").optional(),
                parameterWithName("size").type(INTEGER)
                        .description("한 페이지의 제보 수. 기본값 20, 최솟값 1, 최댓값 100").optional(),
                parameterWithName("cursor")
                        .description("이전 응답의 meta.nextCursor 값. 첫 페이지에서는 생략").optional()
        );
    }

    private ParameterDescriptorWithType bugReportIdParameter() {
        return parameterWithName("bugReportId").type(INTEGER).description("버그 제보 ID");
    }

    private RequestPostProcessor authenticated(UserRole role) {
        return request -> {
            request.setAttribute(
                    AuthenticatedSession.class.getName(),
                    new AuthenticatedSession(ADMIN_USER_ID, role)
            );
            return request;
        };
    }
}
