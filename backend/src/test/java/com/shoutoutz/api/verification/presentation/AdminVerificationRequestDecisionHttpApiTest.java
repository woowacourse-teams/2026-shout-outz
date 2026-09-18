package com.shoutoutz.api.verification.presentation;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.SimpleType.INTEGER;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.OBJECT;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.EnumFields;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.shoutoutz.api.auth.presentation.session.AuthenticatedSession;
import com.shoutoutz.api.common.exception.custom.ConflictException;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.common.restdocs.RestDocsFields;
import com.shoutoutz.api.user.domain.account.UserRole;
import com.shoutoutz.api.verification.application.AdminVerificationRequestDecisionService;
import com.shoutoutz.api.verification.domain.UserVerificationErrorCode;
import com.shoutoutz.api.verification.domain.VerificationRequestStatus;
import com.shoutoutz.api.verification.presentation.dto.response.AdminVerificationRequestApproveResponse;
import com.shoutoutz.api.verification.presentation.dto.response.AdminVerificationRequestDecisionActor;
import com.shoutoutz.api.verification.presentation.dto.response.AdminVerificationRequestRejectResponse;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@DisplayName("관리자 크루/코치 인증 신청 승인/반려 API")
@WebMvcTest(controllers = AdminVerificationRequestDecisionHttpApi.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs
class AdminVerificationRequestDecisionHttpApiTest {

    private static final long REQUEST_ID = 101L;
    private static final long ADMIN_ID = 7L;
    private static final Instant DECIDED_AT = Instant.parse("2026-09-16T03:10:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminVerificationRequestDecisionService decisionService;

    @Test
    @DisplayName("관리자가 PENDING 신청을 승인한다")
    void approveVerificationRequest() throws Exception {
        given(decisionService.approve(REQUEST_ID, ADMIN_ID, UserRole.ADMIN))
                .willReturn(new AdminVerificationRequestApproveResponse(
                        REQUEST_ID,
                        VerificationRequestStatus.APPROVED,
                        new AdminVerificationRequestDecisionActor(ADMIN_ID, "admin"),
                        DECIDED_AT
                ));

        mockMvc.perform(post("/api/v1/admin/verification-requests/{requestId}/approve", REQUEST_ID)
                        .with(authenticated(UserRole.ADMIN))
                        .header(HttpHeaders.COOKIE, "JSESSIONID=admin-session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.requestId").value(REQUEST_ID))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.decidedBy.userId").value(ADMIN_ID))
                .andExpect(jsonPath("$.data.decidedBy.handle").value("admin"))
                .andExpect(jsonPath("$.data.decidedAt").value("2026-09-16T03:10:00Z"))
                .andDo(document(
                        "admin-verification-request-approve",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Admin User Verification")
                                .summary("크루/코치 인증 신청 승인")
                                .description("관리자가 PENDING 상태의 크루/코치 인증 신청을 승인한다.")
                                .requestHeaders(
                                        headerWithName(HttpHeaders.COOKIE)
                                                .description("인증된 관리자의 JSESSIONID")
                                )
                                .pathParameters(
                                        parameterWithName("requestId")
                                                .type(INTEGER)
                                                .description("인증 신청 ID")
                                )
                                .responseSchema(Schema.schema("AdminVerificationRequestApproveSuccessResponse"))
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data").type(OBJECT).description("승인 결과"),
                                        fieldWithPath("data.requestId").type(NUMBER)
                                                .description("인증 신청 ID"),
                                        new EnumFields(VerificationRequestStatus.class).withPath("data.status")
                                                .description("변경된 신청 상태"),
                                        fieldWithPath("data.decidedBy").type(OBJECT)
                                                .description("승인한 관리자"),
                                        fieldWithPath("data.decidedBy.userId").type(NUMBER)
                                                .description("승인한 관리자 사용자 ID"),
                                        fieldWithPath("data.decidedBy.handle").type(STRING)
                                                .description("승인한 관리자 handle"),
                                        fieldWithPath("data.decidedAt").type(STRING)
                                                .description("승인 시각(ISO-8601)")
                                )
                                .build())
                ));

        verify(decisionService).approve(REQUEST_ID, ADMIN_ID, UserRole.ADMIN);
    }

    @Test
    @DisplayName("관리자가 PENDING 신청을 반려한다")
    void rejectVerificationRequest() throws Exception {
        String reason = "Slack 프로필의 기수 정보와 일치하지 않습니다.";
        given(decisionService.reject(REQUEST_ID, ADMIN_ID, UserRole.ADMIN, reason))
                .willReturn(new AdminVerificationRequestRejectResponse(
                        REQUEST_ID,
                        VerificationRequestStatus.REJECTED,
                        reason,
                        new AdminVerificationRequestDecisionActor(ADMIN_ID, "admin"),
                        DECIDED_AT
                ));

        mockMvc.perform(post("/api/v1/admin/verification-requests/{requestId}/reject", REQUEST_ID)
                        .with(authenticated(UserRole.ADMIN))
                        .header(HttpHeaders.COOKIE, "JSESSIONID=admin-session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "  Slack 프로필의 기수 정보와 일치하지 않습니다.  "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.requestId").value(REQUEST_ID))
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.reason").value(reason))
                .andExpect(jsonPath("$.data.decidedBy.userId").value(ADMIN_ID))
                .andExpect(jsonPath("$.data.decidedBy.handle").value("admin"))
                .andExpect(jsonPath("$.data.decidedAt").value("2026-09-16T03:10:00Z"))
                .andDo(document(
                        "admin-verification-request-reject",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Admin User Verification")
                                .summary("크루/코치 인증 신청 반려")
                                .description("관리자가 PENDING 상태의 크루/코치 인증 신청을 반려한다.")
                                .requestHeaders(
                                        headerWithName(HttpHeaders.COOKIE)
                                                .description("인증된 관리자의 JSESSIONID"),
                                        headerWithName(HttpHeaders.CONTENT_TYPE)
                                                .description("application/json")
                                )
                                .pathParameters(
                                        parameterWithName("requestId")
                                                .type(INTEGER)
                                                .description("인증 신청 ID")
                                )
                                .requestSchema(Schema.schema("AdminVerificationRequestRejectRequest"))
                                .requestFields(
                                        fieldWithPath("reason").type(STRING)
                                                .description("반려 사유(앞뒤 공백 제거 후 1~100자)")
                                )
                                .responseSchema(Schema.schema("AdminVerificationRequestRejectSuccessResponse"))
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data").type(OBJECT).description("반려 결과"),
                                        fieldWithPath("data.requestId").type(NUMBER)
                                                .description("인증 신청 ID"),
                                        new EnumFields(VerificationRequestStatus.class).withPath("data.status")
                                                .description("변경된 신청 상태"),
                                        fieldWithPath("data.reason").type(STRING)
                                                .description("반려 사유"),
                                        fieldWithPath("data.decidedBy").type(OBJECT)
                                                .description("반려한 관리자"),
                                        fieldWithPath("data.decidedBy.userId").type(NUMBER)
                                                .description("반려한 관리자 사용자 ID"),
                                        fieldWithPath("data.decidedBy.handle").type(STRING)
                                                .description("반려한 관리자 handle"),
                                        fieldWithPath("data.decidedAt").type(STRING)
                                                .description("반려 시각(ISO-8601)")
                                )
                                .build())
                ));

        verify(decisionService).reject(REQUEST_ID, ADMIN_ID, UserRole.ADMIN, reason);
    }

    @Test
    @DisplayName("반려 사유가 비어 있으면 400을 반환한다")
    void rejectVerificationRequestWithBlankReason() throws Exception {
        mockMvc.perform(post("/api/v1/admin/verification-requests/{requestId}/reject", REQUEST_ID)
                        .with(authenticated(UserRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "  "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andDo(document(
                        "admin-verification-request-reject-invalid",
                        resource(rejectErrorResource())
                ));

        verifyNoInteractions(decisionService);
    }

    @Test
    @DisplayName("일반 사용자는 인증 신청을 승인할 수 없다")
    void rejectNonAdminRequest() throws Exception {
        given(decisionService.approve(REQUEST_ID, ADMIN_ID, UserRole.USER))
                .willThrow(new ForbiddenException(UserVerificationErrorCode.VERIFICATION_ADMIN_FORBIDDEN));

        mockMvc.perform(post("/api/v1/admin/verification-requests/{requestId}/approve", REQUEST_ID)
                        .with(authenticated(UserRole.USER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("VERIFICATION_ADMIN_FORBIDDEN"))
                .andDo(document(
                        "admin-verification-request-approve-forbidden",
                        resource(errorResource())
                ));
    }

    @Test
    @DisplayName("존재하지 않는 신청은 승인할 수 없다")
    void rejectMissingRequest() throws Exception {
        given(decisionService.approve(REQUEST_ID, ADMIN_ID, UserRole.ADMIN))
                .willThrow(new EntityNotFoundException(
                        UserVerificationErrorCode.VERIFICATION_REQUEST_NOT_FOUND
                ));

        mockMvc.perform(post("/api/v1/admin/verification-requests/{requestId}/approve", REQUEST_ID)
                        .with(authenticated(UserRole.ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("VERIFICATION_REQUEST_NOT_FOUND"))
                .andDo(document(
                        "admin-verification-request-approve-not-found",
                        resource(errorResource())
                ));
    }

    @Test
    @DisplayName("PENDING이 아닌 신청은 승인할 수 없다")
    void rejectNonPendingRequest() throws Exception {
        given(decisionService.approve(REQUEST_ID, ADMIN_ID, UserRole.ADMIN))
                .willThrow(new ConflictException(
                        UserVerificationErrorCode.VERIFICATION_REQUEST_NOT_PENDING
                ));

        mockMvc.perform(post("/api/v1/admin/verification-requests/{requestId}/approve", REQUEST_ID)
                        .with(authenticated(UserRole.ADMIN)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("VERIFICATION_REQUEST_NOT_PENDING"))
                .andDo(document(
                        "admin-verification-request-approve-conflict",
                        resource(errorResource())
                ));
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 신청을 승인할 수 없다")
    void rejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(post("/api/v1/admin/verification-requests/{requestId}/approve", REQUEST_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andDo(document(
                        "admin-verification-request-approve-unauthorized",
                        resource(errorResource())
                ));

        verifyNoInteractions(decisionService);
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
                .summary("크루/코치 인증 신청 승인")
                .description("관리자가 PENDING 상태의 크루/코치 인증 신청을 승인한다.")
                .pathParameters(
                        parameterWithName("requestId")
                                .type(INTEGER)
                                .description("인증 신청 ID")
                )
                .responseSchema(Schema.schema("ErrorResponse"))
                .responseFields(RestDocsFields.errorResponse())
                .build();
    }

    private ResourceSnippetParameters rejectErrorResource() {
        return ResourceSnippetParameters.builder()
                .tag("Admin User Verification")
                .summary("크루/코치 인증 신청 반려")
                .description("관리자가 PENDING 상태의 크루/코치 인증 신청을 반려한다.")
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
