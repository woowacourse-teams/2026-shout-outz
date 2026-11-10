package com.shoutoutz.api.resourcereport.presentation;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.OBJECT;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.shoutoutz.api.project.domain.ProjectErrorCode;
import com.shoutoutz.api.resourcereport.application.ResourceReportService;
import com.shoutoutz.api.resourcereport.domain.ResourceReportErrorCode;
import com.shoutoutz.api.resourcereport.domain.ResourceReportKind;
import com.shoutoutz.api.resourcereport.domain.ResourceReportStatus;
import com.shoutoutz.api.resourcereport.presentation.dto.request.ResourceAdditionReportRequest;
import com.shoutoutz.api.resourcereport.presentation.dto.request.ResourceRemovalReportRequest;
import com.shoutoutz.api.resourcereport.presentation.dto.response.MyResourceReportResponse;
import com.shoutoutz.api.resourcereport.presentation.dto.response.ResourceAdditionReportResponse;
import com.shoutoutz.api.resourcereport.presentation.dto.response.ResourceRemovalReportResponse;
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

@WebMvcTest(controllers = ResourceReportHttpApi.class)
@AutoConfigureRestDocs
@DisplayName("자료 제보 API 문서")
class ResourceReportApiDocumentationTest {

    private static final String SLUG = "shout-outz";
    private static final long USER_ID = 17L;
    private static final long REPORT_ID = 41L;
    private static final long RESOURCE_ID = 3412L;
    private static final Instant CREATED_AT = Instant.parse("2026-11-10T02:00:00Z");
    private static final String CSRF_TOKEN = "csrf-token";
    private static final String ADDITION_URL = "https://github.com/woowacourse-teams/2026-shout-outz/pull/123";

    private static final String REMOVALS_PATH = "/api/v1/projects/@{slug}/resource-reports/removals";
    private static final String ADDITIONS_PATH = "/api/v1/projects/@{slug}/resource-reports/additions";
    private static final String MINE_PATH = "/api/v1/projects/@{slug}/resource-reports/me";

    private static final String REMOVAL_DESCRIPTION = "프로젝트 팀원이 자기 프로젝트 기술 이야기에서 필요 없는 PR이나 팀 문서를 골라 제보한다. 로그인이 필요하며 그 프로젝트 팀원(서비스에 등록된 팀원, 가입해서 맞춰진 이관 프로젝트 팀원)만 보낼 수 있다. 크루 인증 여부는 보지 않는다. resourceIds는 1~100개이며 같은 자료는 한 번만 제보한다. 모든 자료가 이 프로젝트의 삭제되지 않은 PR이나 팀 문서(github-pr, github-teamdoc, github-wiki, github-discussion)여야 하고, 하나라도 아니면 아무것도 저장하지 않는다. 같은 사람이 같은 자료를 다시 보내면 새로 만들지 않고 처음 제보 ID를 돌려준다. 로그인하지 않았으면 401, 팀원이 아니면 403 RESOURCE_REPORT_FORBIDDEN, 프로젝트가 없거나 공개되지 않았으면 404 PROJECT_NOT_FOUND, 자료가 없거나 내려갔으면 404 RESOURCE_REPORT_RESOURCE_NOT_FOUND, 다른 프로젝트 자료나 블로그, 테코톡이면 400 RESOURCE_REPORT_RESOURCE_NOT_REMOVABLE, 입력값이 올바르지 않으면 400 VALIDATION_FAILED를 반환한다.";
    private static final String ADDITION_DESCRIPTION = "프로젝트 팀원이 기술 이야기나 전체 경험 근거에 빠진 PR이나 팀 문서의 GitHub 주소를 알려 준다. 로그인이 필요하며 그 프로젝트 팀원만 보낼 수 있다. 주소는 이 프로젝트 리포지토리(https://github.com/woowacourse-teams/{repo}) 아래여야 하며, 쿼리와 # 뒤, 끝 슬래시를 떼고 저장한다. 같은 사람이 같은 주소를 다시 보내면 새로 만들지 않고 처음 제보 ID를 돌려준다. 로그인하지 않았으면 401, 팀원이 아니면 403 RESOURCE_REPORT_FORBIDDEN, 프로젝트가 없거나 공개되지 않았으면 404 PROJECT_NOT_FOUND, GitHub의 PR이나 문서 주소가 아니면 400 RESOURCE_REPORT_URL_INVALID, 이 프로젝트 리포지토리 주소가 아니면 400 RESOURCE_REPORT_URL_OUTSIDE_REPOSITORY, url이 비었거나 길이가 500자를 넘으면 400 VALIDATION_FAILED를 반환한다.";
    private static final String FIND_MINE_DESCRIPTION = "로그인한 사용자가 이 프로젝트에 보낸 자료 제보를 최신순으로 조회한다. 프론트는 필요 없어요 제보의 resourceId로 '필요 없어요 보냄'을 표시한다. 팀원이 아니어도 부를 수 있고 보낸 제보가 없으면 빈 목록이다. 필요 없어요 제보가 가리키는 자료가 내려갔으면 resourceId는 null이다. 로그인하지 않았으면 401, 프로젝트가 없거나 공개되지 않았으면 404 PROJECT_NOT_FOUND를 반환한다.";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResourceReportService resourceReportService;

    @Test
    @DisplayName("팀원이 필요 없는 자료를 제보한다")
    void reportRemovals() throws Exception {
        given(resourceReportService.reportRemovals(eq(SLUG), eq(USER_ID), any(ResourceRemovalReportRequest.class)))
                .willReturn(List.of(
                        new ResourceRemovalReportResponse(REPORT_ID, RESOURCE_ID),
                        new ResourceRemovalReportResponse(REPORT_ID + 1, RESOURCE_ID + 1)
                ));

        mockMvc.perform(post(REMOVALS_PATH, SLUG)
                        .with(authenticated(USER_ID, UserRole.USER))
                        .header("X-CSRF-Token", CSRF_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(removalBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data[0].reportId").value(REPORT_ID))
                .andExpect(jsonPath("$.data[0].resourceId").value(RESOURCE_ID))
                .andDo(document(
                        "resource-report-removal",
                        resource(removalSuccessResponse())
                ));
    }

    @Test
    @DisplayName("고른 자료가 없으면 필요 없는 자료 제보를 거부한다")
    void rejectEmptyRemovals() throws Exception {
        mockMvc.perform(post(REMOVALS_PATH, SLUG)
                        .with(authenticated(USER_ID, UserRole.USER))
                        .header("X-CSRF-Token", CSRF_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "resourceIds": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.VALIDATION_FAILED.name()))
                .andDo(document(
                        "resource-report-removal-invalid",
                        resource(removalErrorResponse())
                ));
    }

    @Test
    @DisplayName("로그인하지 않으면 필요 없는 자료를 제보할 수 없다")
    void rejectAnonymousRemovals() throws Exception {
        mockMvc.perform(post(REMOVALS_PATH, SLUG)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(removalBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.UNAUTHORIZED.name()))
                .andDo(document(
                        "resource-report-removal-unauthorized",
                        resource(removalErrorResponse())
                ));
    }

    @Test
    @DisplayName("팀원이 아니면 필요 없는 자료를 제보할 수 없다")
    void rejectRemovalsFromNonMember() throws Exception {
        given(resourceReportService.reportRemovals(eq(SLUG), eq(USER_ID), any(ResourceRemovalReportRequest.class)))
                .willThrow(new ForbiddenException(ResourceReportErrorCode.RESOURCE_REPORT_FORBIDDEN));

        mockMvc.perform(post(REMOVALS_PATH, SLUG)
                        .with(authenticated(USER_ID, UserRole.USER))
                        .header("X-CSRF-Token", CSRF_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(removalBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RESOURCE_REPORT_FORBIDDEN"))
                .andDo(document(
                        "resource-report-removal-forbidden",
                        resource(removalErrorResponse())
                ));
    }

    @Test
    @DisplayName("이 프로젝트의 PR이나 팀 문서가 아니면 필요 없는 자료로 제보할 수 없다")
    void rejectNotRemovableResource() throws Exception {
        given(resourceReportService.reportRemovals(eq(SLUG), eq(USER_ID), any(ResourceRemovalReportRequest.class)))
                .willThrow(new BadRequestException(ResourceReportErrorCode.RESOURCE_REPORT_RESOURCE_NOT_REMOVABLE));

        mockMvc.perform(post(REMOVALS_PATH, SLUG)
                        .with(authenticated(USER_ID, UserRole.USER))
                        .header("X-CSRF-Token", CSRF_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(removalBody()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RESOURCE_REPORT_RESOURCE_NOT_REMOVABLE"))
                .andDo(document(
                        "resource-report-removal-not-removable",
                        resource(removalErrorResponse())
                ));
    }

    @Test
    @DisplayName("없거나 내려간 자료는 필요 없는 자료로 제보할 수 없다")
    void rejectMissingResource() throws Exception {
        given(resourceReportService.reportRemovals(eq(SLUG), eq(USER_ID), any(ResourceRemovalReportRequest.class)))
                .willThrow(new EntityNotFoundException(ResourceReportErrorCode.RESOURCE_REPORT_RESOURCE_NOT_FOUND));

        mockMvc.perform(post(REMOVALS_PATH, SLUG)
                        .with(authenticated(USER_ID, UserRole.USER))
                        .header("X-CSRF-Token", CSRF_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(removalBody()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_REPORT_RESOURCE_NOT_FOUND"))
                .andDo(document(
                        "resource-report-removal-resource-not-found",
                        resource(removalErrorResponse())
                ));
    }

    @Test
    @DisplayName("팀원이 빠진 자료 주소를 제보한다")
    void reportAddition() throws Exception {
        given(resourceReportService.reportAddition(eq(SLUG), eq(USER_ID), any(ResourceAdditionReportRequest.class)))
                .willReturn(new ResourceAdditionReportResponse(REPORT_ID));

        mockMvc.perform(post(ADDITIONS_PATH, SLUG)
                        .with(authenticated(USER_ID, UserRole.USER))
                        .header("X-CSRF-Token", CSRF_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(additionBody(ADDITION_URL)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.reportId").value(REPORT_ID))
                .andDo(document(
                        "resource-report-addition",
                        resource(additionSuccessResponse())
                ));
    }

    @Test
    @DisplayName("이 프로젝트 리포지토리 밖의 주소는 빠진 자료로 제보할 수 없다")
    void rejectAdditionOutsideRepository() throws Exception {
        given(resourceReportService.reportAddition(eq(SLUG), eq(USER_ID), any(ResourceAdditionReportRequest.class)))
                .willThrow(new BadRequestException(ResourceReportErrorCode.RESOURCE_REPORT_URL_OUTSIDE_REPOSITORY));

        mockMvc.perform(post(ADDITIONS_PATH, SLUG)
                        .with(authenticated(USER_ID, UserRole.USER))
                        .header("X-CSRF-Token", CSRF_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(additionBody("https://github.com/woowacourse-teams/2026-other/pull/1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RESOURCE_REPORT_URL_OUTSIDE_REPOSITORY"))
                .andDo(document(
                        "resource-report-addition-outside-repository",
                        resource(additionErrorResponse())
                ));
    }

    @Test
    @DisplayName("주소가 없으면 빠진 자료 제보를 거부한다")
    void rejectBlankAddition() throws Exception {
        mockMvc.perform(post(ADDITIONS_PATH, SLUG)
                        .with(authenticated(USER_ID, UserRole.USER))
                        .header("X-CSRF-Token", CSRF_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(additionBody("   ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.VALIDATION_FAILED.name()))
                .andDo(document(
                        "resource-report-addition-invalid",
                        resource(additionErrorResponse())
                ));
    }

    @Test
    @DisplayName("팀원이 아니면 빠진 자료를 제보할 수 없다")
    void rejectAdditionFromNonMember() throws Exception {
        given(resourceReportService.reportAddition(eq(SLUG), eq(USER_ID), any(ResourceAdditionReportRequest.class)))
                .willThrow(new ForbiddenException(ResourceReportErrorCode.RESOURCE_REPORT_FORBIDDEN));

        mockMvc.perform(post(ADDITIONS_PATH, SLUG)
                        .with(authenticated(USER_ID, UserRole.USER))
                        .header("X-CSRF-Token", CSRF_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(additionBody(ADDITION_URL)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RESOURCE_REPORT_FORBIDDEN"))
                .andDo(document(
                        "resource-report-addition-forbidden",
                        resource(additionErrorResponse())
                ));
    }

    @Test
    @DisplayName("로그인하지 않으면 빠진 자료를 제보할 수 없다")
    void rejectAnonymousAddition() throws Exception {
        mockMvc.perform(post(ADDITIONS_PATH, SLUG)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(additionBody(ADDITION_URL)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.UNAUTHORIZED.name()))
                .andDo(document(
                        "resource-report-addition-unauthorized",
                        resource(additionErrorResponse())
                ));
    }

    @Test
    @DisplayName("내가 이 프로젝트에 보낸 제보를 조회한다")
    void findMine() throws Exception {
        given(resourceReportService.findMine(SLUG, USER_ID)).willReturn(List.of(
                new MyResourceReportResponse(
                        REPORT_ID + 1,
                        ResourceReportKind.ADD,
                        null,
                        ADDITION_URL,
                        ResourceReportStatus.OPEN,
                        CREATED_AT.plusSeconds(300)
                ),
                new MyResourceReportResponse(
                        REPORT_ID,
                        ResourceReportKind.REMOVE,
                        RESOURCE_ID,
                        null,
                        ResourceReportStatus.OPEN,
                        CREATED_AT
                )
        ));

        mockMvc.perform(get(MINE_PATH, SLUG)
                        .with(authenticated(USER_ID, UserRole.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[1].resourceId").value(RESOURCE_ID))
                .andDo(document(
                        "resource-report-find-mine",
                        resource(findMineSuccessResponse())
                ));
    }

    @Test
    @DisplayName("로그인하지 않으면 내가 보낸 제보를 조회할 수 없다")
    void rejectAnonymousFindMine() throws Exception {
        mockMvc.perform(get(MINE_PATH, SLUG))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.UNAUTHORIZED.name()))
                .andDo(document(
                        "resource-report-find-mine-unauthorized",
                        resource(findMineErrorResponse())
                ));
    }

    @Test
    @DisplayName("없거나 공개되지 않은 프로젝트에서는 내가 보낸 제보를 조회할 수 없다")
    void rejectFindMineOfMissingProject() throws Exception {
        given(resourceReportService.findMine(SLUG, USER_ID))
                .willThrow(new EntityNotFoundException(ProjectErrorCode.PROJECT_NOT_FOUND));

        mockMvc.perform(get(MINE_PATH, SLUG)
                        .with(authenticated(USER_ID, UserRole.USER)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"))
                .andDo(document(
                        "resource-report-find-mine-project-not-found",
                        resource(findMineErrorResponse())
                ));
    }

    private ResourceSnippetParameters removalSuccessResponse() {
        return ResourceSnippetParameters.builder()
                .tag("ResourceReport")
                .summary("필요 없는 자료 제보")
                .description(REMOVAL_DESCRIPTION)
                .pathParameters(slugParameter())
                .requestHeaders(csrfHeader())
                .requestSchema(Schema.schema("ResourceRemovalReportRequest"))
                .responseSchema(Schema.schema("ResourceRemovalReportSuccessResponse"))
                .requestFields(removalRequestFields())
                .responseFields(
                        fieldWithPath("status").type(STRING).description("응답 상태"),
                        fieldWithPath("data").type(ARRAY).description("접수한 제보. 중복을 뺀 요청 순서"),
                        fieldWithPath("data[].reportId").type(NUMBER)
                                .description("제보 ID. 이미 보낸 자료면 처음 제보 ID"),
                        fieldWithPath("data[].resourceId").type(NUMBER).description("제보한 자료 ID")
                )
                .build();
    }

    private ResourceSnippetParameters removalErrorResponse() {
        return ResourceSnippetParameters.builder()
                .tag("ResourceReport")
                .summary("필요 없는 자료 제보")
                .description(REMOVAL_DESCRIPTION)
                .pathParameters(slugParameter())
                .requestSchema(Schema.schema("ResourceRemovalReportRequest"))
                .responseSchema(Schema.schema("ErrorResponse"))
                .responseFields(RestDocsFields.errorResponse())
                .build();
    }

    private ResourceSnippetParameters additionSuccessResponse() {
        return ResourceSnippetParameters.builder()
                .tag("ResourceReport")
                .summary("빠진 자료 제보")
                .description(ADDITION_DESCRIPTION)
                .pathParameters(slugParameter())
                .requestHeaders(csrfHeader())
                .requestSchema(Schema.schema("ResourceAdditionReportRequest"))
                .responseSchema(Schema.schema("ResourceAdditionReportSuccessResponse"))
                .requestFields(additionRequestFields())
                .responseFields(
                        fieldWithPath("status").type(STRING).description("응답 상태"),
                        fieldWithPath("data").type(OBJECT).description("접수한 제보"),
                        fieldWithPath("data.reportId").type(NUMBER)
                                .description("제보 ID. 이미 보낸 주소면 처음 제보 ID")
                )
                .build();
    }

    private ResourceSnippetParameters additionErrorResponse() {
        return ResourceSnippetParameters.builder()
                .tag("ResourceReport")
                .summary("빠진 자료 제보")
                .description(ADDITION_DESCRIPTION)
                .pathParameters(slugParameter())
                .requestSchema(Schema.schema("ResourceAdditionReportRequest"))
                .responseSchema(Schema.schema("ErrorResponse"))
                .responseFields(RestDocsFields.errorResponse())
                .build();
    }

    private ResourceSnippetParameters findMineSuccessResponse() {
        return ResourceSnippetParameters.builder()
                .tag("ResourceReport")
                .summary("내가 보낸 자료 제보 조회")
                .description(FIND_MINE_DESCRIPTION)
                .pathParameters(slugParameter())
                .responseSchema(Schema.schema("MyResourceReportFindSuccessResponse"))
                .responseFields(
                        fieldWithPath("status").type(STRING).description("응답 상태"),
                        fieldWithPath("data").type(ARRAY).description("내가 보낸 제보. 최신순"),
                        fieldWithPath("data[].reportId").type(NUMBER).description("제보 ID"),
                        new EnumFields(ResourceReportKind.class).withPath("data[].kind")
                                .description("제보 종류. REMOVE는 필요 없어요, ADD는 추가해 주세요"),
                        fieldWithPath("data[].resourceId").type(NUMBER)
                                .description("필요 없어요 제보가 가리키는 자료 ID. 추가해 주세요 제보이거나 자료가 내려갔으면 null")
                                .optional(),
                        fieldWithPath("data[].url").type(STRING)
                                .description("추가해 주세요 제보로 알려 준 주소. 필요 없어요 제보이면 null")
                                .optional(),
                        new EnumFields(ResourceReportStatus.class).withPath("data[].status")
                                .description("처리 상태"),
                        fieldWithPath("data[].createdAt").type(STRING).description("ISO-8601 제보 시각")
                )
                .build();
    }

    private ResourceSnippetParameters findMineErrorResponse() {
        return ResourceSnippetParameters.builder()
                .tag("ResourceReport")
                .summary("내가 보낸 자료 제보 조회")
                .description(FIND_MINE_DESCRIPTION)
                .pathParameters(slugParameter())
                .responseSchema(Schema.schema("ErrorResponse"))
                .responseFields(RestDocsFields.errorResponse())
                .build();
    }

    private List<FieldDescriptor> removalRequestFields() {
        return List.of(
                fieldWithPath("resourceIds").type(ARRAY)
                        .description("필요 없다고 제보할 자료 ID 목록. 1~100개. 같은 ID는 한 번만 제보한다"),
                fieldWithPath("memo").type(STRING)
                        .description("제보 이유. 공백 제거 후 500자 이하. 고른 자료 모두에 붙는다").optional()
        );
    }

    private List<FieldDescriptor> additionRequestFields() {
        return List.of(
                fieldWithPath("url").type(STRING)
                        .description("빠진 PR이나 팀 문서의 GitHub 주소. 이 프로젝트 리포지토리 아래, 500자 이하"),
                fieldWithPath("memo").type(STRING)
                        .description("왜 들어가면 좋은지. 공백 제거 후 500자 이하").optional()
        );
    }

    private ParameterDescriptorWithType slugParameter() {
        return parameterWithName("slug").description("프로젝트 slug. 경로에서는 앞에 @를 붙인다.");
    }

    private HeaderDescriptor csrfHeader() {
        return headerWithName("X-CSRF-Token").description("세션 조회로 발급받은 CSRF 토큰");
    }

    private String removalBody() {
        return """
                {
                  "resourceIds": [%d, %d],
                  "memo": "단순 설정 변경이라 배울 거리가 없어요"
                }
                """.formatted(RESOURCE_ID, RESOURCE_ID + 1);
    }

    private String additionBody(String url) {
        return """
                {
                  "url": "%s",
                  "memo": "캐시 도입 전후 성능을 비교한 PR이에요"
                }
                """.formatted(url);
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
