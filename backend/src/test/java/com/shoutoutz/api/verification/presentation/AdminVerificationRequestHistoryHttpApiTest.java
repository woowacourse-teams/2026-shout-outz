package com.shoutoutz.api.verification.presentation;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.SimpleType.INTEGER;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.OBJECT;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.shoutoutz.api.auth.presentation.session.AuthenticatedSession;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.common.restdocs.RestDocsFields;
import com.shoutoutz.api.user.domain.account.UserRole;
import com.shoutoutz.api.verification.application.AdminVerificationRequestHistoryService;
import com.shoutoutz.api.verification.domain.UserVerificationErrorCode;
import com.shoutoutz.api.verification.domain.VerificationRequestStatus;
import com.shoutoutz.api.verification.presentation.dto.response.AdminVerificationRequestDecisionActor;
import com.shoutoutz.api.verification.presentation.dto.response.AdminVerificationRequestHistoryResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@DisplayName("관리자 크루/코치 인증 신청 이력 조회 API")
@WebMvcTest(controllers = AdminVerificationRequestHistoryHttpApi.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs
class AdminVerificationRequestHistoryHttpApiTest {

    private static final long REQUEST_ID = 101L;
    private static final long ADMIN_ID = 7L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminVerificationRequestHistoryService historyService;

    @Test
    @DisplayName("관리자가 특정 신청의 전체 이력을 최신순으로 조회한다")
    void findVerificationRequestHistory() throws Exception {
        given(historyService.findHistory(REQUEST_ID, UserRole.ADMIN))
                .willReturn(new AdminVerificationRequestHistoryResponse(
                        REQUEST_ID,
                        List.of(
                                new AdminVerificationRequestHistoryResponse.Item(
                                        202L,
                                        VerificationRequestStatus.PENDING,
                                        VerificationRequestStatus.REJECTED,
                                        new AdminVerificationRequestDecisionActor(ADMIN_ID, "admin"),
                                        "Slack 프로필의 기수 정보와 일치하지 않습니다.",
                                        Instant.parse("2026-09-16T03:00:00Z")
                                ),
                                new AdminVerificationRequestHistoryResponse.Item(
                                        201L,
                                        null,
                                        VerificationRequestStatus.PENDING,
                                        null,
                                        null,
                                        Instant.parse("2026-09-16T02:30:00Z")
                                )
                        )
                ));

        mockMvc.perform(get(
                        "/api/v1/admin/verification-requests/{requestId}/history",
                        REQUEST_ID
                )
                        .with(authenticated(UserRole.ADMIN))
                        .header(HttpHeaders.COOKIE, "JSESSIONID=admin-session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.requestId").value(REQUEST_ID))
                .andExpect(jsonPath("$.data.items[0].historyId").value(202))
                .andExpect(jsonPath("$.data.items[0].fromStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.items[0].toStatus").value("REJECTED"))
                .andExpect(jsonPath("$.data.items[0].changedBy.userId").value(ADMIN_ID))
                .andExpect(jsonPath("$.data.items[0].changedBy.handle").value("admin"))
                .andExpect(jsonPath("$.data.items[0].reason")
                        .value("Slack 프로필의 기수 정보와 일치하지 않습니다."))
                .andExpect(jsonPath("$.data.items[0].changedAt")
                        .value("2026-09-16T03:00:00Z"))
                .andExpect(jsonPath("$.data.items[1].historyId").value(201))
                .andExpect(jsonPath("$.data.items[1].fromStatus").value(nullValue()))
                .andExpect(jsonPath("$.data.items[1].toStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.items[1].changedBy").value(nullValue()))
                .andExpect(jsonPath("$.data.items[1].reason").value(nullValue()))
                .andExpect(jsonPath("$.data.items[1].changedAt")
                        .value("2026-09-16T02:30:00Z"))
                .andDo(document(
                        "admin-verification-request-history",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Admin User Verification")
                                .summary("관리자의 크루/코치 인증 신청 이력 조회")
                                .description("관리자가 특정 크루/코치 인증 신청의 상태 변경 이력을 최신순으로 조회한다.")
                                .requestHeaders(
                                        headerWithName(HttpHeaders.COOKIE)
                                                .description("인증된 관리자의 JSESSIONID")
                                )
                                .pathParameters(
                                        parameterWithName("requestId")
                                                .type(INTEGER)
                                                .description("인증 신청 ID")
                                )
                                .responseSchema(Schema.schema("AdminVerificationRequestHistorySuccessResponse"))
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data").type(OBJECT).description("인증 신청 이력"),
                                        fieldWithPath("data.requestId").type(NUMBER)
                                                .description("인증 신청 ID"),
                                        fieldWithPath("data.items").type(ARRAY)
                                                .description("상태 변경 이력(최신순)"),
                                        fieldWithPath("data.items[].historyId").type(NUMBER)
                                                .description("이력 ID"),
                                        fieldWithPath("data.items[].fromStatus").type(STRING)
                                                .description("변경 전 상태. 최초 신청이면 null")
                                                .optional(),
                                        fieldWithPath("data.items[].toStatus").type(STRING)
                                                .description("변경 후 상태"),
                                        fieldWithPath("data.items[].changedBy").type(OBJECT)
                                                .description("상태를 변경한 관리자. 최초 신청이면 null")
                                                .optional(),
                                        fieldWithPath("data.items[].changedBy.userId").type(NUMBER)
                                                .description("상태를 변경한 관리자 사용자 ID")
                                                .optional(),
                                        fieldWithPath("data.items[].changedBy.handle").type(STRING)
                                                .description("상태를 변경한 관리자 handle")
                                                .optional(),
                                        fieldWithPath("data.items[].reason").type(STRING)
                                                .description("반려 사유. 승인 또는 최초 신청이면 null")
                                                .optional(),
                                        fieldWithPath("data.items[].changedAt").type(STRING)
                                                .description("상태 변경 시각(ISO-8601)")
                                )
                                .build())
                ));

        verify(historyService).findHistory(REQUEST_ID, UserRole.ADMIN);
    }

    @Test
    @DisplayName("일반 사용자는 신청 이력을 조회할 수 없다")
    void rejectNonAdminRequest() throws Exception {
        given(historyService.findHistory(REQUEST_ID, UserRole.USER))
                .willThrow(new ForbiddenException(UserVerificationErrorCode.VERIFICATION_ADMIN_FORBIDDEN));

        mockMvc.perform(get(
                        "/api/v1/admin/verification-requests/{requestId}/history",
                        REQUEST_ID
                ).with(authenticated(UserRole.USER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("VERIFICATION_ADMIN_FORBIDDEN"))
                .andDo(document(
                        "admin-verification-request-history-forbidden",
                        resource(errorResource())
                ));
    }

    @Test
    @DisplayName("존재하지 않는 신청의 이력을 조회하면 404를 반환한다")
    void rejectMissingRequest() throws Exception {
        given(historyService.findHistory(REQUEST_ID, UserRole.ADMIN))
                .willThrow(new EntityNotFoundException(
                        UserVerificationErrorCode.VERIFICATION_REQUEST_NOT_FOUND
                ));

        mockMvc.perform(get(
                        "/api/v1/admin/verification-requests/{requestId}/history",
                        REQUEST_ID
                ).with(authenticated(UserRole.ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("VERIFICATION_REQUEST_NOT_FOUND"))
                .andDo(document(
                        "admin-verification-request-history-not-found",
                        resource(errorResource())
                ));
    }

    @Test
    @DisplayName("인증 없이 신청 이력을 조회하면 401을 반환한다")
    void rejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get(
                "/api/v1/admin/verification-requests/{requestId}/history",
                REQUEST_ID
        ))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andDo(document(
                        "admin-verification-request-history-unauthorized",
                        resource(errorResource())
                ));

        verifyNoInteractions(historyService);
    }

    private RequestPostProcessor authenticated(UserRole role) {
        return request -> {
            request.setAttribute(
                    AuthenticatedSession.class.getName(),
                    new AuthenticatedSession(ADMIN_ID, role)
            );
            return request;
        };
    }

    private ResourceSnippetParameters errorResource() {
        return ResourceSnippetParameters.builder()
                .tag("Admin User Verification")
                .summary("관리자의 크루/코치 인증 신청 이력 조회")
                .description("관리자가 특정 크루/코치 인증 신청의 상태 변경 이력을 최신순으로 조회한다.")
                .pathParameters(
                        parameterWithName("requestId")
                                .type(INTEGER)
                                .description("인증 신청 ID")
                )
                .responseSchema(Schema.schema("ErrorResponse"))
                .responseFields(RestDocsFields.errorResponse())
                .build();
    }
}
