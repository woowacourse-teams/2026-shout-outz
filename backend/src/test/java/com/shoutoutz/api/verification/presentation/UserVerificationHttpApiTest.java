package com.shoutoutz.api.verification.presentation;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.OBJECT;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.shoutoutz.api.auth.presentation.session.AuthenticatedSession;
import com.shoutoutz.api.common.exception.custom.ConflictException;
import com.shoutoutz.api.common.restdocs.RestDocsFields;
import com.shoutoutz.api.user.domain.account.UserRole;
import com.shoutoutz.api.user.domain.profile.UserType;
import com.shoutoutz.api.verification.application.UserVerificationService;
import com.shoutoutz.api.verification.domain.UserVerificationErrorCode;
import com.shoutoutz.api.verification.domain.VerificationRequestStatus;
import com.shoutoutz.api.verification.presentation.dto.request.UserVerificationRequestCreateRequest;
import com.shoutoutz.api.verification.presentation.dto.response.UserVerificationRequestCreateResponse;
import com.shoutoutz.api.verification.presentation.dto.response.UserVerificationRequestResponse;
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

@DisplayName("크루/코치 인증 신청 API")
@WebMvcTest(controllers = UserVerificationHttpApi.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs
class UserVerificationHttpApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserVerificationService userVerificationService;

    @Test
    @DisplayName("로그인한 사용자가 크루 인증을 신청한다")
    void createCrewVerificationRequest() throws Exception {
        Instant requestedAt = Instant.parse("2026-09-16T00:00:00Z");
        given(userVerificationService.create(
                eq(1L),
                any(UserVerificationRequestCreateRequest.class)
        )).willReturn(new UserVerificationRequestCreateResponse(
                10L,
                UserType.WOOWACOURSE_CREW,
                "샤를",
                8,
                "BACKEND",
                VerificationRequestStatus.PENDING,
                requestedAt
        ));

        mockMvc.perform(post("/api/v1/users/me/verification-requests")
                        .header(HttpHeaders.COOKIE, "JSESSIONID=session-id")
                        .header("X-CSRF-Token", "csrf-token")
                        .requestAttr(
                                AuthenticatedSession.class.getName(),
                                new AuthenticatedSession(1L, UserRole.USER)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userType": "WOOWACOURSE_CREW",
                                  "nickname": "샤를",
                                  "cohort": 8,
                                  "track": "BACKEND"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.requestId").value(10))
                .andExpect(jsonPath("$.data.userType").value("WOOWACOURSE_CREW"))
                .andExpect(jsonPath("$.data.nickname").value("샤를"))
                .andExpect(jsonPath("$.data.cohort").value(8))
                .andExpect(jsonPath("$.data.track").value("BACKEND"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.requestedAt").value("2026-09-16T00:00:00Z"))
                .andDo(document(
                        "user-verification-request-create",
                        resource(ResourceSnippetParameters.builder()
                                .tag("User Verification")
                                .summary("크루/코치 인증 신청")
                                .description("로그인한 일반 사용자가 크루 또는 코치 인증을 신청한다.")
                                .requestHeaders(
                                        headerWithName(HttpHeaders.COOKIE)
                                                .description("인증된 사용자의 JSESSIONID"),
                                        headerWithName("X-CSRF-Token")
                                                .description("CSRF 토큰")
                                )
                                .requestSchema(Schema.schema("UserVerificationRequestCreateRequest"))
                                .requestFields(
                                        fieldWithPath("userType").type(STRING)
                                                .description("신청 유형(CREW 또는 COACH)"),
                                        fieldWithPath("nickname").type(STRING)
                                                .description("우테코 닉네임(앞뒤 공백 제거 후 50자 이하)"),
                                        fieldWithPath("cohort").type(NUMBER)
                                                .description("크루 신청 시 기수. 양의 정수이며 실제 유효성은 관리자 확인")
                                                .optional(),
                                        fieldWithPath("track").type(STRING)
                                                .description("크루 신청 시 트랙(BACKEND, FRONTEND, ANDROID)")
                                                .optional()
                                )
                                .responseSchema(Schema.schema("UserVerificationRequestCreateSuccessResponse"))
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data").type(OBJECT).description("생성된 인증 신청"),
                                        fieldWithPath("data.requestId").type(NUMBER)
                                                .description("인증 신청 ID"),
                                        fieldWithPath("data.userType").type(STRING)
                                                .description("신청 유형"),
                                        fieldWithPath("data.nickname").type(STRING)
                                                .description("신청 닉네임"),
                                        fieldWithPath("data.cohort").type(NUMBER)
                                                .description("신청 기수").optional(),
                                        fieldWithPath("data.track").type(STRING)
                                                .description("신청 트랙").optional(),
                                        fieldWithPath("data.status").type(STRING)
                                                .description("신청 상태"),
                                        fieldWithPath("data.requestedAt").type(STRING)
                                                .description("신청 시각(ISO-8601)")
                                )
                                .build())
                ));
    }

    @Test
    @DisplayName("로그인한 사용자의 최신 인증 신청을 조회한다")
    void findLatestVerificationRequest() throws Exception {
        Instant requestedAt = Instant.parse("2026-09-16T02:30:00Z");
        Instant decidedAt = Instant.parse("2026-09-16T03:00:00Z");
        given(userVerificationService.findLatest(1L)).willReturn(
                new UserVerificationRequestResponse(
                        101L,
                        UserType.WOOWACOURSE_CREW,
                        "샤를",
                        8,
                        "BACKEND",
                        VerificationRequestStatus.REJECTED,
                        requestedAt,
                        decidedAt,
                        "Slack 프로필의 기수 정보와 일치하지 않습니다."
                )
        );

        mockMvc.perform(get("/api/v1/users/me/verification-request")
                        .header(HttpHeaders.COOKIE, "JSESSIONID=session-id")
                        .requestAttr(
                                AuthenticatedSession.class.getName(),
                                new AuthenticatedSession(1L, UserRole.USER)
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.requestId").value(101))
                .andExpect(jsonPath("$.data.userType").value("WOOWACOURSE_CREW"))
                .andExpect(jsonPath("$.data.nickname").value("샤를"))
                .andExpect(jsonPath("$.data.cohort").value(8))
                .andExpect(jsonPath("$.data.track").value("BACKEND"))
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.requestedAt").value("2026-09-16T02:30:00Z"))
                .andExpect(jsonPath("$.data.decidedAt").value("2026-09-16T03:00:00Z"))
                .andExpect(jsonPath("$.data.reason")
                        .value("Slack 프로필의 기수 정보와 일치하지 않습니다."))
                .andDo(document(
                        "user-verification-request-get",
                        resource(ResourceSnippetParameters.builder()
                                .tag("User Verification")
                                .summary("내 최신 크루/코치 인증 신청 조회")
                                .description("신청자에게 가장 최근의 크루/코치 인증 신청 1건을 반환한다.")
                                .requestHeaders(
                                        headerWithName(HttpHeaders.COOKIE)
                                                .description("인증된 사용자의 JSESSIONID")
                                )
                                .responseSchema(Schema.schema("UserVerificationRequestSuccessResponse"))
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data").type(OBJECT).description("최신 인증 신청")
                                                .optional(),
                                        fieldWithPath("data.requestId").type(NUMBER)
                                                .description("인증 신청 ID. 기존 인증 사용자는 null")
                                                .optional(),
                                        fieldWithPath("data.userType").type(STRING)
                                                .description("신청 유형"),
                                        fieldWithPath("data.nickname").type(STRING)
                                                .description("신청 닉네임"),
                                        fieldWithPath("data.cohort").type(NUMBER)
                                                .description("신청 기수. 코치 신청은 null")
                                                .optional(),
                                        fieldWithPath("data.track").type(STRING)
                                                .description("신청 트랙. 코치 신청은 null")
                                                .optional(),
                                        fieldWithPath("data.status").type(STRING)
                                                .description("신청 상태"),
                                        fieldWithPath("data.requestedAt").type(STRING)
                                                .description("신청 시각(ISO-8601). 기존 인증 사용자는 null")
                                                .optional(),
                                        fieldWithPath("data.decidedAt").type(STRING)
                                                .description("승인/반려 시각(ISO-8601). PENDING이면 null")
                                                .optional(),
                                        fieldWithPath("data.reason").type(STRING)
                                                .description("반려 사유. REJECTED일 때만 반환")
                                                .optional()
                                )
                                .build())
                ));
    }

    @Test
    @DisplayName("인증 신청 이력이 없으면 data에 null을 반환한다")
    void findLatestVerificationRequestWhenHistoryDoesNotExist() throws Exception {
        given(userVerificationService.findLatest(1L)).willReturn(null);

        mockMvc.perform(get("/api/v1/users/me/verification-request")
                        .requestAttr(
                                AuthenticatedSession.class.getName(),
                                new AuthenticatedSession(1L, UserRole.USER)
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    @DisplayName("로그인하지 않은 사용자는 최신 인증 신청을 조회할 수 없다")
    void rejectUnauthenticatedLatestVerificationRequest() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/verification-request"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andDo(document(
                        "user-verification-request-get-unauthorized",
                        resource(latestVerificationGetErrorResource())
                ));

        verifyNoInteractions(userVerificationService);
    }

    @Test
    @DisplayName("인증 정보가 없으면 인증 신청을 거절한다")
    void rejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(post("/api/v1/users/me/verification-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userType": "WOOWACOURSE_CREW",
                                  "nickname": "샤를",
                                  "cohort": 8,
                                  "track": "BACKEND"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andDo(document(
                        "user-verification-request-create-unauthorized",
                        resource(ResourceSnippetParameters.builder()
                                .tag("User Verification")
                                .summary("크루/코치 인증 신청")
                                .description("로그인한 일반 사용자가 크루 또는 코치 인증을 신청한다.")
                                .responseSchema(Schema.schema("ErrorResponse"))
                                .responseFields(RestDocsFields.errorResponse())
                                .build())
                ));

        verifyNoInteractions(userVerificationService);
    }

    @Test
    @DisplayName("이미 대기 중인 신청이 있으면 인증 신청을 거절한다")
    void rejectDuplicatePendingRequest() throws Exception {
        given(userVerificationService.create(
                eq(1L),
                any(UserVerificationRequestCreateRequest.class)
        )).willThrow(new ConflictException(
                UserVerificationErrorCode.VERIFICATION_REQUEST_ALREADY_PENDING
        ));

        mockMvc.perform(post("/api/v1/users/me/verification-requests")
                        .requestAttr(
                                AuthenticatedSession.class.getName(),
                                new AuthenticatedSession(1L, UserRole.USER)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userType": "WOOWACOURSE_CREW",
                                  "nickname": "샤를",
                                  "cohort": 8,
                                  "track": "BACKEND"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code")
                        .value("VERIFICATION_REQUEST_ALREADY_PENDING"))
                .andDo(document("user-verification-request-create-duplicate", resource(errorResource())));
    }

    @Test
    @DisplayName("필수 입력값이 없으면 인증 신청을 거절한다")
    void rejectInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/v1/users/me/verification-requests")
                        .requestAttr(
                                AuthenticatedSession.class.getName(),
                                new AuthenticatedSession(1L, UserRole.USER)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userType": "WOOWACOURSE_CREW",
                                  "nickname": " ",
                                  "cohort": 0,
                                  "track": "BACKEND"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andDo(document("user-verification-request-create-invalid", resource(errorResource())));

        verifyNoInteractions(userVerificationService);
    }

    private static ResourceSnippetParameters errorResource() {
        return ResourceSnippetParameters.builder()
                .tag("User Verification")
                .summary("크루/코치 인증 신청")
                .description("로그인한 일반 사용자가 크루 또는 코치 인증을 신청한다.")
                .responseSchema(Schema.schema("ErrorResponse"))
                .responseFields(RestDocsFields.errorResponse())
                .build();
    }

    private static ResourceSnippetParameters latestVerificationGetErrorResource() {
        return ResourceSnippetParameters.builder()
                .tag("User Verification")
                .summary("내 최신 크루/코치 인증 신청 조회")
                .description("신청자에게 가장 최근의 크루/코치 인증 신청 1건을 반환한다.")
                .responseSchema(Schema.schema("ErrorResponse"))
                .responseFields(RestDocsFields.errorResponse())
                .build();
    }
}
