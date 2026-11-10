package com.shoutoutz.api.resourcereport.presentation;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.SimpleType.INTEGER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.BOOLEAN;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.OBJECT;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.EnumFields;
import com.epages.restdocs.apispec.ParameterDescriptorWithType;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.shoutoutz.api.auth.presentation.session.AuthenticatedSession;
import com.shoutoutz.api.common.exception.code.CommonErrorCode;
import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.common.restdocs.RestDocsFields;
import com.shoutoutz.api.common.response.SliceMetaResponse;
import com.shoutoutz.api.resourcereport.application.ResourceReportAdminService;
import com.shoutoutz.api.resourcereport.domain.ResourceReportErrorCode;
import com.shoutoutz.api.resourcereport.domain.ResourceReportKind;
import com.shoutoutz.api.resourcereport.domain.ResourceReportStatus;
import com.shoutoutz.api.resourcereport.presentation.dto.request.ResourceReportAdminFindAllRequest;
import com.shoutoutz.api.resourcereport.presentation.dto.request.ResourceReportStatusUpdateRequest;
import com.shoutoutz.api.resourcereport.presentation.dto.response.ResourceReportAdminFindAllResponse;
import com.shoutoutz.api.resourcereport.presentation.dto.response.ResourceReportStatusUpdateResponse;
import com.shoutoutz.api.user.domain.account.UserRole;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.headers.HeaderDescriptor;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(controllers = AdminResourceReportHttpApi.class)
@AutoConfigureRestDocs
@DisplayName("자료 제보 관리자 API 문서")
class AdminResourceReportApiDocumentationTest {

    private static final String SLUG = "shout-outz";
    private static final long USER_ID = 17L;
    private static final long ADMIN_USER_ID = 7L;
    private static final long REPORT_ID = 41L;
    private static final long RESOURCE_ID = 3412L;
    private static final Instant CREATED_AT = Instant.parse("2026-11-10T02:00:00Z");
    private static final Instant HANDLED_AT = Instant.parse("2026-11-11T01:00:00Z");
    private static final String NEXT_CURSOR = "MjAyNi0xMS0xMFQwMjowMDowMFp8NDE";
    private static final String CSRF_TOKEN = "csrf-token";
    private static final String ADDITION_URL = "https://github.com/woowacourse-teams/2026-shout-outz/pull/123";

    private static final String ADMIN_PATH = "/api/v1/admin/resource-reports";
    private static final String ADMIN_STATUS_PATH = "/api/v1/admin/resource-reports/{reportId}/status";

    private static final String ADMIN_FIND_ALL_DESCRIPTION = "관리자가 받은 자료 제보를 최신순으로 조회한다. status 기본값은 OPEN이며 OPEN, ACCEPTED, REJECTED, ALL을 지원한다. kind로 REMOVE, ADD를 거를 수 있다. size 기본값은 20이고 1~100을 허용한다. 다음 페이지는 응답 meta.nextCursor를 cursor로 전달해 조회한다. totalCount는 상태, 종류 조건에 해당하는 전체 제보 수다. 필요 없어요 제보가 가리키는 자료가 내려갔으면 resource는 null이다. 관리자 로그인 세션이 없으면 401, 관리자 권한이 없으면 403 RESOURCE_REPORT_ADMIN_FORBIDDEN, 조건이 유효하지 않으면 400 VALIDATION_FAILED, cursor가 유효하지 않으면 400 RESOURCE_REPORT_ADMIN_CURSOR_INVALID를 반환한다.";
    private static final String ADMIN_UPDATE_STATUS_DESCRIPTION = "관리자가 자료 제보를 반영함(ACCEPTED), 반영 안 함(REJECTED), 확인 전(OPEN) 상태로 바꾼다. 상태가 실제로 바뀌면 처리 시각과 관리자 ID를 기록하고, 같은 상태로 바꾸면 그대로 둔다. 관리자 로그인 세션이 없으면 401, 관리자 권한이 없으면 403 RESOURCE_REPORT_ADMIN_FORBIDDEN, 상태가 없거나 올바르지 않으면 400 VALIDATION_FAILED, 제보가 없으면 404 RESOURCE_REPORT_NOT_FOUND를 반환한다.";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResourceReportAdminService resourceReportAdminService;

    @Test
    @DisplayName("관리자가 자료 제보를 조건과 커서로 조회한다")
    void findAll() throws Exception {
        given(resourceReportAdminService.findAll(eq(UserRole.ADMIN), any(ResourceReportAdminFindAllRequest.class)))
                .willReturn(new ResourceReportAdminFindAllResponse(
                        List.of(
                                new ResourceReportAdminFindAllResponse.Item(
                                        REPORT_ID,
                                        ResourceReportKind.REMOVE,
                                        new ResourceReportAdminFindAllResponse.Project(SLUG, "샤라웃"),
                                        new ResourceReportAdminFindAllResponse.Resource(
                                                RESOURCE_ID,
                                                "github-pr",
                                                "[BE] 추천 질문 생성을 비동기 작업으로 분리",
                                                "https://github.com/woowacourse-teams/2026-shout-outz/pull/512"
                                        ),
                                        null,
                                        "단순 설정 변경이라 배울 거리가 없어요",
                                        USER_ID,
                                        ResourceReportStatus.OPEN,
                                        CREATED_AT
                                ),
                                new ResourceReportAdminFindAllResponse.Item(
                                        REPORT_ID - 1,
                                        ResourceReportKind.ADD,
                                        new ResourceReportAdminFindAllResponse.Project(SLUG, "샤라웃"),
                                        null,
                                        ADDITION_URL,
                                        null,
                                        USER_ID,
                                        ResourceReportStatus.OPEN,
                                        CREATED_AT.minusSeconds(60)
                                )
                        ),
                        new SliceMetaResponse(NEXT_CURSOR, true, 5L)
                ));

        mockMvc.perform(get(ADMIN_PATH)
                        .with(authenticated(ADMIN_USER_ID, UserRole.ADMIN))
                        .queryParam("status", "OPEN")
                        .queryParam("kind", "REMOVE")
                        .queryParam("size", "2")
                        .queryParam("cursor", NEXT_CURSOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].reportId").value(REPORT_ID))
                .andExpect(jsonPath("$.meta.hasNext").value(true))
                .andDo(document(
                        "resource-report-admin-find-all",
                        resource(findAllSuccessResponse())
                ));
    }

    @Test
    @DisplayName("일반 사용자는 자료 제보 목록을 조회할 수 없다")
    void rejectNonAdminFindAll() throws Exception {
        given(resourceReportAdminService.findAll(eq(UserRole.USER), any(ResourceReportAdminFindAllRequest.class)))
                .willThrow(new ForbiddenException(ResourceReportErrorCode.RESOURCE_REPORT_ADMIN_FORBIDDEN));

        mockMvc.perform(get(ADMIN_PATH)
                        .with(authenticated(USER_ID, UserRole.USER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RESOURCE_REPORT_ADMIN_FORBIDDEN"))
                .andDo(document(
                        "resource-report-admin-find-all-forbidden",
                        resource(findAllErrorResponse())
                ));
    }

    @Test
    @DisplayName("로그인하지 않으면 자료 제보 목록을 조회할 수 없다")
    void rejectAnonymousFindAll() throws Exception {
        mockMvc.perform(get(ADMIN_PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.UNAUTHORIZED.name()))
                .andDo(document(
                        "resource-report-admin-find-all-unauthorized",
                        resource(findAllErrorResponse())
                ));
    }

    @Test
    @DisplayName("잘못된 커서로 자료 제보 목록을 조회하면 거부한다")
    void rejectInvalidCursor() throws Exception {
        given(resourceReportAdminService.findAll(eq(UserRole.ADMIN), any(ResourceReportAdminFindAllRequest.class)))
                .willThrow(new BadRequestException(ResourceReportErrorCode.RESOURCE_REPORT_ADMIN_CURSOR_INVALID));

        mockMvc.perform(get(ADMIN_PATH)
                        .with(authenticated(ADMIN_USER_ID, UserRole.ADMIN))
                        .queryParam("cursor", "invalid-cursor"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RESOURCE_REPORT_ADMIN_CURSOR_INVALID"))
                .andDo(document(
                        "resource-report-admin-find-all-invalid-cursor",
                        resource(findAllErrorResponse())
                ));
    }

    @Test
    @DisplayName("유효하지 않은 조건으로 자료 제보 목록을 조회하면 거부한다")
    void rejectInvalidFilter() throws Exception {
        mockMvc.perform(get(ADMIN_PATH)
                        .with(authenticated(ADMIN_USER_ID, UserRole.ADMIN))
                        .queryParam("kind", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.VALIDATION_FAILED.name()))
                .andDo(document(
                        "resource-report-admin-find-all-invalid-filter",
                        resource(findAllErrorResponse())
                ));
    }

    @Test
    @DisplayName("관리자가 자료 제보 처리 상태를 바꾼다")
    void updateStatus() throws Exception {
        given(resourceReportAdminService.updateStatus(
                eq(REPORT_ID),
                eq(ADMIN_USER_ID),
                eq(UserRole.ADMIN),
                any(ResourceReportStatusUpdateRequest.class)
        )).willReturn(new ResourceReportStatusUpdateResponse(
                REPORT_ID,
                ResourceReportStatus.ACCEPTED,
                HANDLED_AT,
                ADMIN_USER_ID
        ));

        mockMvc.perform(patch(ADMIN_STATUS_PATH, REPORT_ID)
                        .with(authenticated(ADMIN_USER_ID, UserRole.ADMIN))
                        .header("X-CSRF-Token", CSRF_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
                .andDo(document(
                        "resource-report-admin-update-status",
                        resource(updateStatusSuccessResponse())
                ));
    }

    @Test
    @DisplayName("상태가 없으면 자료 제보 처리를 거부한다")
    void rejectMissingStatus() throws Exception {
        mockMvc.perform(patch(ADMIN_STATUS_PATH, REPORT_ID)
                        .with(authenticated(ADMIN_USER_ID, UserRole.ADMIN))
                        .header("X-CSRF-Token", CSRF_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.VALIDATION_FAILED.name()))
                .andDo(document(
                        "resource-report-admin-update-status-invalid",
                        resource(updateStatusErrorResponse())
                ));
    }

    @Test
    @DisplayName("일반 사용자는 자료 제보를 처리할 수 없다")
    void rejectNonAdminUpdateStatus() throws Exception {
        given(resourceReportAdminService.updateStatus(
                eq(REPORT_ID),
                eq(USER_ID),
                eq(UserRole.USER),
                any(ResourceReportStatusUpdateRequest.class)
        )).willThrow(new ForbiddenException(ResourceReportErrorCode.RESOURCE_REPORT_ADMIN_FORBIDDEN));

        mockMvc.perform(patch(ADMIN_STATUS_PATH, REPORT_ID)
                        .with(authenticated(USER_ID, UserRole.USER))
                        .header("X-CSRF-Token", CSRF_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RESOURCE_REPORT_ADMIN_FORBIDDEN"))
                .andDo(document(
                        "resource-report-admin-update-status-forbidden",
                        resource(updateStatusErrorResponse())
                ));
    }

    @Test
    @DisplayName("로그인하지 않으면 자료 제보를 처리할 수 없다")
    void rejectAnonymousUpdateStatus() throws Exception {
        mockMvc.perform(patch(ADMIN_STATUS_PATH, REPORT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.UNAUTHORIZED.name()))
                .andDo(document(
                        "resource-report-admin-update-status-unauthorized",
                        resource(updateStatusErrorResponse())
                ));
    }

    @Test
    @DisplayName("없는 자료 제보는 처리할 수 없다")
    void rejectMissingReport() throws Exception {
        given(resourceReportAdminService.updateStatus(
                eq(REPORT_ID),
                eq(ADMIN_USER_ID),
                eq(UserRole.ADMIN),
                any(ResourceReportStatusUpdateRequest.class)
        )).willThrow(new EntityNotFoundException(ResourceReportErrorCode.RESOURCE_REPORT_NOT_FOUND));

        mockMvc.perform(patch(ADMIN_STATUS_PATH, REPORT_ID)
                        .with(authenticated(ADMIN_USER_ID, UserRole.ADMIN))
                        .header("X-CSRF-Token", CSRF_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_REPORT_NOT_FOUND"))
                .andDo(document(
                        "resource-report-admin-update-status-not-found",
                        resource(updateStatusErrorResponse())
                ));
    }

    private ResourceSnippetParameters findAllSuccessResponse() {
        return ResourceSnippetParameters.builder()
                .tag("ResourceReport")
                .summary("자료 제보 목록 조회")
                .description(ADMIN_FIND_ALL_DESCRIPTION)
                .queryParameters(findAllParameters())
                .responseSchema(Schema.schema("ResourceReportAdminFindAllSuccessResponse"))
                .responseFields(
                        fieldWithPath("status").type(STRING).description("응답 상태"),
                        fieldWithPath("data").type(ARRAY).description("자료 제보 목록. 최신순"),
                        fieldWithPath("data[].reportId").type(NUMBER).description("제보 ID"),
                        new EnumFields(ResourceReportKind.class).withPath("data[].kind")
                                .description("제보 종류. REMOVE는 필요 없어요, ADD는 추가해 주세요"),
                        fieldWithPath("data[].project").type(OBJECT).description("제보한 프로젝트"),
                        fieldWithPath("data[].project.slug").type(STRING).description("프로젝트 slug"),
                        fieldWithPath("data[].project.title").type(STRING).description("프로젝트 이름"),
                        fieldWithPath("data[].resource").type(OBJECT)
                                .description("필요 없어요 제보가 가리키는 자료. 추가해 주세요 제보이거나 자료가 내려갔으면 null")
                                .optional(),
                        fieldWithPath("data[].resource.resourceId").type(NUMBER).description("자료 ID").optional(),
                        fieldWithPath("data[].resource.source").type(STRING)
                                .description("자료 원천(github-pr, github-teamdoc, github-wiki, github-discussion)")
                                .optional(),
                        fieldWithPath("data[].resource.title").type(STRING).description("자료 제목").optional(),
                        fieldWithPath("data[].resource.url").type(STRING).description("자료 원문 주소").optional(),
                        fieldWithPath("data[].url").type(STRING)
                                .description("추가해 주세요 제보로 알려 준 주소. 필요 없어요 제보이면 null")
                                .optional(),
                        fieldWithPath("data[].memo").type(STRING).description("제보 이유. 없으면 null").optional(),
                        fieldWithPath("data[].reporterUserId").type(NUMBER).description("제보한 사용자 ID"),
                        new EnumFields(ResourceReportStatus.class).withPath("data[].status")
                                .description("처리 상태"),
                        fieldWithPath("data[].createdAt").type(STRING).description("ISO-8601 제보 시각"),
                        fieldWithPath("meta").type(OBJECT).description("커서 페이지네이션 정보"),
                        fieldWithPath("meta.nextCursor").type(STRING)
                                .description("다음 페이지 요청에 사용할 커서. 다음 페이지가 없으면 null").optional(),
                        fieldWithPath("meta.hasNext").type(BOOLEAN).description("다음 페이지 존재 여부"),
                        fieldWithPath("meta.totalCount").type(NUMBER).description("상태, 종류 조건에 해당하는 전체 제보 수")
                )
                .build();
    }

    private ResourceSnippetParameters findAllErrorResponse() {
        return ResourceSnippetParameters.builder()
                .tag("ResourceReport")
                .summary("자료 제보 목록 조회")
                .description(ADMIN_FIND_ALL_DESCRIPTION)
                .queryParameters(findAllParameters())
                .responseSchema(Schema.schema("ErrorResponse"))
                .responseFields(RestDocsFields.errorResponse())
                .build();
    }

    private ResourceSnippetParameters updateStatusSuccessResponse() {
        return ResourceSnippetParameters.builder()
                .tag("ResourceReport")
                .summary("자료 제보 처리")
                .description(ADMIN_UPDATE_STATUS_DESCRIPTION)
                .pathParameters(reportIdParameter())
                .requestHeaders(csrfHeader())
                .requestSchema(Schema.schema("ResourceReportStatusUpdateRequest"))
                .responseSchema(Schema.schema("ResourceReportStatusUpdateSuccessResponse"))
                .requestFields(statusRequestFields())
                .responseFields(
                        fieldWithPath("status").type(STRING).description("응답 상태"),
                        fieldWithPath("data").type(OBJECT).description("처리한 자료 제보"),
                        fieldWithPath("data.reportId").type(NUMBER).description("제보 ID"),
                        new EnumFields(ResourceReportStatus.class).withPath("data.status")
                                .description("바뀐 처리 상태"),
                        fieldWithPath("data.handledAt").type(STRING)
                                .description("마지막으로 상태를 바꾼 시각. 바꾼 적이 없으면 null").optional(),
                        fieldWithPath("data.handledByUserId").type(NUMBER)
                                .description("마지막으로 상태를 바꾼 관리자 ID. 바꾼 적이 없으면 null").optional()
                )
                .build();
    }

    private ResourceSnippetParameters updateStatusErrorResponse() {
        return ResourceSnippetParameters.builder()
                .tag("ResourceReport")
                .summary("자료 제보 처리")
                .description(ADMIN_UPDATE_STATUS_DESCRIPTION)
                .pathParameters(reportIdParameter())
                .requestSchema(Schema.schema("ResourceReportStatusUpdateRequest"))
                .responseSchema(Schema.schema("ErrorResponse"))
                .responseFields(RestDocsFields.errorResponse())
                .build();
    }

    private List<FieldDescriptor> statusRequestFields() {
        return List.of(
                new EnumFields(ResourceReportStatus.class).withPath("status")
                        .description("바꿀 처리 상태. OPEN, ACCEPTED, REJECTED")
        );
    }

    private List<ParameterDescriptorWithType> findAllParameters() {
        return List.of(
                parameterWithName("status")
                        .description("조회할 상태. OPEN, ACCEPTED, REJECTED, ALL 중 하나. 생략하면 OPEN").optional(),
                parameterWithName("kind").description("조회할 제보 종류. REMOVE, ADD 중 하나. 생략하면 모두").optional(),
                parameterWithName("size").type(INTEGER)
                        .description("한 페이지의 제보 수. 기본값 20, 최솟값 1, 최댓값 100").optional(),
                parameterWithName("cursor")
                        .description("이전 응답의 meta.nextCursor 값. 첫 페이지에서는 생략").optional()
        );
    }

    private ParameterDescriptorWithType reportIdParameter() {
        return parameterWithName("reportId").type(INTEGER).description("자료 제보 ID");
    }

    private HeaderDescriptor csrfHeader() {
        return headerWithName("X-CSRF-Token").description("세션 조회로 발급받은 CSRF 토큰");
    }

    private String statusBody() {
        return """
                {
                  "status": "ACCEPTED"
                }
                """;
    }

    private RequestPostProcessor authenticated(long userId, UserRole role) {
        return request -> {
            request.setAttribute(
                    AuthenticatedSession.class.getName(),
                    new AuthenticatedSession(userId, role)
            );
            return request;
        };
    }
}
