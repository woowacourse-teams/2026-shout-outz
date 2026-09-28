package com.shoutoutz.api.verification.presentation;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.SimpleType.INTEGER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
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
import com.shoutoutz.api.auth.presentation.session.AuthenticatedSession;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.common.response.SliceMetaResponse;
import com.shoutoutz.api.common.restdocs.RestDocsFields;
import com.shoutoutz.api.user.domain.account.UserRole;
import com.shoutoutz.api.user.domain.profile.Track;
import com.shoutoutz.api.user.domain.profile.UserType;
import com.shoutoutz.api.verification.application.AdminVerificationRequestService;
import com.shoutoutz.api.verification.domain.UserVerificationErrorCode;
import com.shoutoutz.api.verification.domain.VerificationRequestStatus;
import com.shoutoutz.api.verification.presentation.dto.request.AdminVerificationRequestFindAllRequest;
import com.shoutoutz.api.verification.presentation.dto.response.AdminVerificationRequestFindAllResponse;
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

@DisplayName("관리자 크루/코치 인증 신청 목록 API")
@WebMvcTest(controllers = AdminVerificationRequestHttpApi.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs
class AdminVerificationRequestHttpApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminVerificationRequestService adminVerificationRequestService;

    @Test
    @DisplayName("관리자가 상태별 인증 신청 목록을 조회한다")
    void findAllVerificationRequests() throws Exception {
        Instant requestedAt = Instant.parse("2026-09-16T02:30:00Z");
        given(adminVerificationRequestService.findAll(
                eq(UserRole.ADMIN),
                any(AdminVerificationRequestFindAllRequest.class)
        )).willReturn(new AdminVerificationRequestFindAllResponse(
                List.of(new AdminVerificationRequestFindAllResponse.Item(
                        101L,
                        new AdminVerificationRequestFindAllResponse.Applicant(42L, "charles"),
                        UserType.WOOWACOURSE_CREW,
                        "샤를",
                        8,
                        "BACKEND",
                        VerificationRequestStatus.PENDING,
                        requestedAt
                )),
                new SliceMetaResponse("next-cursor", true, 12L)
        ));

        mockMvc.perform(get("/api/v1/admin/verification-requests")
                        .with(authenticated(UserRole.ADMIN))
                        .header(HttpHeaders.COOKIE, "JSESSIONID=admin-session")
                        .queryParam("status", "PENDING")
                        .queryParam("size", "20")
                        .queryParam("cursor", "cursor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data[0].requestId").value(101))
                .andExpect(jsonPath("$.data[0].applicant.userId").value(42))
                .andExpect(jsonPath("$.data[0].applicant.handle").value("charles"))
                .andExpect(jsonPath("$.data[0].userType").value("WOOWACOURSE_CREW"))
                .andExpect(jsonPath("$.data[0].nickname").value("샤를"))
                .andExpect(jsonPath("$.data[0].cohort").value(8))
                .andExpect(jsonPath("$.data[0].track").value("BACKEND"))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data[0].requestedAt")
                        .value("2026-09-16T02:30:00Z"))
                .andExpect(jsonPath("$.meta.nextCursor").value("next-cursor"))
                .andExpect(jsonPath("$.meta.hasNext").value(true))
                .andExpect(jsonPath("$.meta.totalCount").value(12))
                .andDo(document(
                        "admin-verification-request-find-all",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Admin User Verification")
                                .summary("관리자의 크루/코치 인증 신청 목록 조회")
                                .description("관리자가 상태별 크루/코치 인증 신청 목록을 최신순으로 조회한다.")
                                .requestHeaders(
                                        headerWithName(HttpHeaders.COOKIE)
                                                .description("인증된 관리자의 JSESSIONID")
                                )
                                .queryParameters(
                                        parameterWithName("status")
                                                .type(com.epages.restdocs.apispec.SimpleType.STRING)
                                                .defaultValue("PENDING")
                                                .description("조회할 상태(PENDING, APPROVED, REJECTED)")
                                                .optional(),
                                        parameterWithName("size")
                                                .type(INTEGER)
                                                .defaultValue(20)
                                                .description("조회 개수(1~100), 기본값 20")
                                                .optional(),
                                        parameterWithName("cursor")
                                                .type(com.epages.restdocs.apispec.SimpleType.STRING)
                                                .description("다음 페이지 조회용 커서")
                                                .optional()
                                )
                                .responseSchema(Schema.schema("AdminVerificationRequestFindAllSuccessResponse"))
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data").type(ARRAY).description("인증 신청 목록"),
                                        fieldWithPath("data[].requestId").type(NUMBER)
                                                .description("인증 신청 ID"),
                                        fieldWithPath("data[].applicant").type(OBJECT)
                                                .description("신청자 식별 정보"),
                                        fieldWithPath("data[].applicant.userId").type(NUMBER)
                                                .description("신청자 사용자 ID"),
                                        fieldWithPath("data[].applicant.handle").type(STRING)
                                                .description("신청자 handle"),
                                        new EnumFields(UserType.class).withPath("data[].userType")
                                                .description("신청 유형"),
                                        fieldWithPath("data[].nickname").type(STRING)
                                                .description("신청 닉네임"),
                                        fieldWithPath("data[].cohort").type(NUMBER)
                                                .description("신청 기수. 코치 신청은 null")
                                                .optional(),
                                        new EnumFields(Track.class).withPath("data[].track")
                                                .description("신청 트랙. 코치 신청은 null")
                                                .optional(),
                                        new EnumFields(VerificationRequestStatus.class).withPath("data[].status")
                                                .description("현재 신청 상태"),
                                        fieldWithPath("data[].requestedAt").type(STRING)
                                                .description("신청 시각(ISO-8601)"),
                                        fieldWithPath("meta").type(OBJECT).description("페이지네이션 정보"),
                                        fieldWithPath("meta.nextCursor").type(STRING)
                                                .description("다음 페이지 커서. 다음 페이지가 없으면 null")
                                                .optional(),
                                        fieldWithPath("meta.hasNext").type(BOOLEAN)
                                                .description("다음 페이지 존재 여부"),
                                        fieldWithPath("meta.totalCount").type(NUMBER)
                                                .description("상태 조건을 만족하는 전체 인증 신청 수")
                                )
                                .build())
                ));
    }

    @Test
    @DisplayName("일반 사용자는 관리자 인증 신청 목록을 조회할 수 없다")
    void rejectNonAdminRequest() throws Exception {
        given(adminVerificationRequestService.findAll(
                eq(UserRole.USER),
                any(AdminVerificationRequestFindAllRequest.class)
        )).willThrow(new ForbiddenException(
                UserVerificationErrorCode.VERIFICATION_ADMIN_FORBIDDEN
        ));

        mockMvc.perform(get("/api/v1/admin/verification-requests")
                        .with(authenticated(UserRole.USER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code")
                        .value("VERIFICATION_ADMIN_FORBIDDEN"))
                .andDo(document(
                        "admin-verification-request-find-all-forbidden",
                        resource(errorResource())
                ));
    }

    @Test
    @DisplayName("인증 없이 관리자 인증 신청 목록을 조회하면 401을 반환한다")
    void rejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/v1/admin/verification-requests"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andDo(document(
                        "admin-verification-request-find-all-unauthorized",
                        resource(errorResource())
                ));

        verifyNoInteractions(adminVerificationRequestService);
    }

    @Test
    @DisplayName("허용되지 않은 상태나 크기로 조회하면 400을 반환한다")
    void rejectInvalidQueryParameters() throws Exception {
        mockMvc.perform(get("/api/v1/admin/verification-requests")
                        .with(authenticated(UserRole.ADMIN))
                        .queryParam("status", "PROCESSING"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andDo(document(
                        "admin-verification-request-find-all-invalid",
                        resource(errorResource())
                ));

        mockMvc.perform(get("/api/v1/admin/verification-requests")
                        .with(authenticated(UserRole.ADMIN))
                        .queryParam("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(adminVerificationRequestService);
    }

    private RequestPostProcessor authenticated(UserRole role) {
        return request -> {
            request.setAttribute(
                    AuthenticatedSession.class.getName(),
                    new AuthenticatedSession(1L, role)
            );
            return request;
        };
    }

    private ResourceSnippetParameters errorResource() {
        return ResourceSnippetParameters.builder()
                .tag("Admin User Verification")
                .summary("관리자의 크루/코치 인증 신청 목록 조회")
                .description("관리자가 상태별 크루/코치 인증 신청 목록을 최신순으로 조회한다.")
                .responseSchema(Schema.schema("ErrorResponse"))
                .responseFields(RestDocsFields.errorResponse())
                .build();
    }
}
