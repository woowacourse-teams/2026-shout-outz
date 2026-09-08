package com.shoutoutz.api.auth.presentation;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.OBJECT;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.shoutoutz.api.auth.application.OAuthLoginAttempt;
import com.shoutoutz.api.auth.application.OAuthLoginService;
import com.shoutoutz.api.auth.application.OAuthSignupService;
import com.shoutoutz.api.auth.application.dto.result.OAuthLoginCallbackResult;
import com.shoutoutz.api.auth.application.dto.result.OAuthLoginStartResult;
import com.shoutoutz.api.auth.application.dto.result.OAuthSignupResult;
import com.shoutoutz.api.auth.domain.OAuthIdentity;
import com.shoutoutz.api.auth.domain.OAuthProvider;
import com.shoutoutz.api.auth.exception.AuthErrorCode;
import com.shoutoutz.api.auth.presentation.security.CsrfTokenManager;
import com.shoutoutz.api.auth.presentation.session.AuthSessionAccessor;
import com.shoutoutz.api.auth.presentation.session.AuthSessionManager;
import com.shoutoutz.api.auth.presentation.session.AuthenticatedSession;
import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.restdocs.RestDocsFields;
import com.shoutoutz.api.user.domain.account.UserRole;
import java.net.URI;
import java.time.Instant;
import java.util.Optional;
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

@DisplayName("인증 API 문서")
@WebMvcTest(controllers = {
        OAuthLoginHttpApi.class,
        OAuthSignupHttpApi.class,
        AuthSessionHttpApi.class
})
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs
class AuthApiDocumentationTest {

    private static final URI GITHUB_AUTHORIZATION_URI =
            URI.create("https://github.com/login/oauth/authorize?client_id=client-id");
    private static final URI COMPLETION_URI = URI.create("http://localhost:5173");
    private static final String CALLBACK_DESCRIPTION =
            "GitHub 승인 시 code와 state, 거절 시 error=access_denied와 state를 검증한다. "
                    + "처리 후 로그인 또는 가입 대기 상태를 만들고 프론트엔드로 이동한다.";
    private static final String SIGNUP_DESCRIPTION =
            "가입 대기 OAuth 신원에 서비스 사용자와 프로필을 생성하고 인증 세션을 설정한다. "
                    + "가입 정보가 유효하지 않으면 400 VALIDATION_FAILED, 가입 대기 신원이 없으면 "
                    + "400 OAUTH_SIGNUP_SESSION_NOT_FOUND, CSRF Token이 유효하지 않으면 "
                    + "403 CSRF_TOKEN_INVALID를 반환한다.";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OAuthLoginService oauthLoginService;

    @MockitoBean
    private OAuthSignupService oauthSignupService;

    @MockitoBean
    private OAuthLoginProperties oauthLoginProperties;

    @MockitoBean
    private AuthSessionAccessor authSessionAccessor;

    @MockitoBean
    private AuthSessionManager authSessionManager;

    @MockitoBean
    private CsrfTokenManager csrfTokenManager;

    @Test
    @DisplayName("GitHub 로그인을 시작하면 GitHub 인가 화면으로 이동한다")
    void authorizeGitHub() throws Exception {
        given(oauthLoginService.startGitHubLogin())
                .willReturn(new OAuthLoginStartResult(GITHUB_AUTHORIZATION_URI, loginAttempt()));

        mockMvc.perform(get("/oauth2/authorization/github"))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, GITHUB_AUTHORIZATION_URI.toString()))
                .andDo(document(
                        "auth-github-authorization",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Auth")
                                .summary("GitHub 로그인 시작")
                                .description("state와 PKCE 값을 생성해 세션에 저장하고 GitHub 인가 화면으로 이동한다.")
                                .responseHeaders(
                                        headerWithName(HttpHeaders.LOCATION)
                                                .description("GitHub OAuth 인가 화면 URI"),
                                        headerWithName(HttpHeaders.SET_COOKIE)
                                                .description("OAuth 로그인 시도를 저장한 JSESSIONID")
                                                .optional()
                                )
                                .build())
                ));
    }

    @Test
    @DisplayName("GitHub Callback을 처리하면 서비스 완료 화면으로 이동한다")
    void callbackGitHub() throws Exception {
        given(authSessionAccessor.consumeLoginAttempt(any(), any())).willReturn(loginAttempt());
        given(oauthLoginService.completeGitHubLogin("authorization-code", "state", loginAttempt()))
                .willReturn(OAuthLoginCallbackResult.signupRequired(githubIdentity()));
        given(authSessionManager.rotateSessionId(any())).willReturn(new org.springframework.mock.web.MockHttpSession());
        given(oauthLoginProperties.completionUri()).willReturn(COMPLETION_URI);

        mockMvc.perform(get("/login/oauth2/code/github")
                        .queryParam("code", "authorization-code")
                        .queryParam("state", "state")
                        .header(HttpHeaders.COOKIE, "JSESSIONID=session-id"))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, COMPLETION_URI.toString()))
                .andDo(document(
                        "auth-github-callback",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Auth")
                                .summary("GitHub 로그인 Callback")
                                .description(CALLBACK_DESCRIPTION)
                                .privateResource(true)
                                .requestHeaders(
                                        headerWithName(HttpHeaders.COOKIE)
                                                .description("로그인 시작 시 발급된 JSESSIONID")
                                )
                                .queryParameters(
                                        parameterWithName("code")
                                                .description("GitHub 승인 시 발급되는 Authorization Code")
                                                .optional(),
                                        parameterWithName("state")
                                                .description("로그인 시작 요청과 Callback을 연결하는 검증값"),
                                        parameterWithName("error")
                                                .description("GitHub 거절 시 전달되는 access_denied")
                                                .optional()
                                )
                                .responseHeaders(
                                        headerWithName(HttpHeaders.LOCATION)
                                                .description("OAuth 처리 후 이동할 프론트엔드 URI"),
                                        headerWithName(HttpHeaders.SET_COOKIE)
                                                .description("인증 또는 가입 대기 상태로 회전된 JSESSIONID")
                                                .optional()
                                )
                                .build())
                ));
    }

    @Test
    @DisplayName("OAuth 로그인 세션이 없으면 Callback을 거부한다")
    void rejectCallbackWithoutOAuthLoginSession() throws Exception {
        given(authSessionAccessor.consumeLoginAttempt(any(), any()))
                .willThrow(new BadRequestException(
                        AuthErrorCode.OAUTH_LOGIN_SESSION_NOT_FOUND
                ));

        mockMvc.perform(get("/login/oauth2/code/github")
                        .queryParam("code", "authorization-code")
                        .queryParam("state", "state")
                        .header(HttpHeaders.COOKIE, "JSESSIONID=expired-session-id"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("OAUTH_LOGIN_SESSION_NOT_FOUND"))
                .andDo(document(
                        "auth-github-callback-session-missing",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Auth")
                                .summary("GitHub 로그인 Callback")
                                .description(CALLBACK_DESCRIPTION)
                                .privateResource(true)
                                .requestHeaders(
                                        headerWithName(HttpHeaders.COOKIE)
                                                .description("로그인 시작 시 발급된 JSESSIONID")
                                )
                                .queryParameters(
                                        parameterWithName("code")
                                                .description("GitHub 승인 시 발급되는 Authorization Code")
                                                .optional(),
                                        parameterWithName("state")
                                                .description("로그인 시작 요청과 Callback을 연결하는 검증값"),
                                        parameterWithName("error")
                                                .description("GitHub 거절 시 전달되는 access_denied")
                                                .optional()
                                )
                                .responseSchema(Schema.schema("ErrorResponse"))
                                .responseFields(RestDocsFields.errorResponse())
                                .build())
                ));
    }

    @Test
    @DisplayName("현재 인증 세션과 CSRF 토큰을 조회한다")
    void getAuthSession() throws Exception {
        given(csrfTokenManager.getOrCreate(any())).willReturn("csrf-token");
        given(authSessionAccessor.findAuthentication(any()))
                .willReturn(Optional.of(new AuthenticatedSession(1L, UserRole.USER)));

        mockMvc.perform(get("/api/v1/auth/session")
                        .header(HttpHeaders.COOKIE, "JSESSIONID=session-id"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.status").value("AUTHENTICATED"))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.csrfToken").value("csrf-token"))
                .andDo(document(
                        "auth-session-get",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Auth")
                                .summary("인증 세션 조회")
                                .description("현재 로그인 상태와 상태 변경 요청에 사용할 CSRF Token을 조회한다.")
                                .requestHeaders(
                                        headerWithName(HttpHeaders.COOKIE)
                                                .description("기존 세션이 있을 때 브라우저가 전송하는 JSESSIONID")
                                                .optional()
                                )
                                .responseHeaders(
                                        headerWithName(HttpHeaders.CACHE_CONTROL)
                                                .description("인증 정보와 CSRF Token의 캐시 저장 방지"),
                                        headerWithName(HttpHeaders.SET_COOKIE)
                                                .description("새 세션을 생성한 경우 발급되는 JSESSIONID")
                                                .optional()
                                )
                                .responseSchema(Schema.schema("AuthSessionSuccessResponse"))
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data").type(OBJECT).description("인증 세션 정보"),
                                        fieldWithPath("data.status").type(STRING)
                                                .description("UNAUTHENTICATED, SIGNUP_REQUIRED, AUTHENTICATED"),
                                        fieldWithPath("data.userId").type(NUMBER)
                                                .description("인증된 사용자 ID").optional(),
                                        fieldWithPath("data.role").type(STRING)
                                                .description("인증된 사용자 권한").optional(),
                                        fieldWithPath("data.csrfToken").type(STRING)
                                                .description("상태 변경 요청에 사용할 CSRF Token")
                                )
                                .build())
                ));
    }

    @Test
    @DisplayName("가입 대기 사용자가 프로필을 입력하면 가입을 완료한다")
    void signup() throws Exception {
        given(authSessionAccessor.findPendingIdentity(any()))
                .willReturn(Optional.of(githubIdentity()));
        given(oauthSignupService.signup(any()))
                .willReturn(new OAuthSignupResult(1L, UserRole.USER));

        mockMvc.perform(post("/api/v1/auth/signup")
                        .header(HttpHeaders.COOKIE, "JSESSIONID=session-id")
                        .header("X-CSRF-Token", "csrf-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "handle": "zzaekkii",
                                  "displayName": "재키",
                                  "userType": "WOOWACOURSE_CREW",
                                  "track": "BACKEND",
                                  "cohort": 8
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andDo(document(
                        "auth-signup",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Auth")
                                .summary("OAuth 사용자 가입")
                                .description(SIGNUP_DESCRIPTION)
                                .requestHeaders(
                                        headerWithName(HttpHeaders.COOKIE)
                                                .description("가입 대기 신원이 저장된 JSESSIONID"),
                                        headerWithName("X-CSRF-Token")
                                                .description("세션 조회 API에서 발급받은 CSRF Token"),
                                        headerWithName(HttpHeaders.CONTENT_TYPE)
                                                .description("application/json")
                                )
                                .requestSchema(Schema.schema("OAuthSignupRequest"))
                                .requestFields(
                                        fieldWithPath("handle").type(STRING)
                                                .description("영구 공개 핸들"),
                                        fieldWithPath("displayName").type(STRING)
                                                .description("프로필 표시 이름"),
                                        fieldWithPath("userType").type(STRING)
                                                .description("GENERAL, WOOWACOURSE_CREW, WOOWACOURSE_COACH"),
                                        fieldWithPath("track").type(STRING)
                                                .description("우테코 크루의 트랙").optional(),
                                        fieldWithPath("cohort").type(NUMBER)
                                                .description("우테코 크루의 기수").optional()
                                )
                                .responseSchema(Schema.schema("OAuthSignupSuccessResponse"))
                                .responseHeaders(
                                        headerWithName(HttpHeaders.SET_COOKIE)
                                                .description("가입 완료 후 인증 상태로 회전된 JSESSIONID")
                                                .optional()
                                )
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data").type(OBJECT).description("가입 결과"),
                                        fieldWithPath("data.userId").type(NUMBER)
                                                .description("생성된 사용자 ID")
                                )
                                .build())
                ));
    }

    @Test
    @DisplayName("가입 요청값이 유효하지 않으면 공통 오류 형식으로 응답한다")
    void rejectInvalidSignupRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "handle": "",
                                  "displayName": "",
                                  "userType": "GENERAL"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details").isArray())
                .andDo(document(
                        "auth-signup-invalid",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Auth")
                                .summary("OAuth 사용자 가입")
                                .description(SIGNUP_DESCRIPTION)
                                .requestSchema(Schema.schema("OAuthSignupRequest"))
                                .requestFields(
                                        fieldWithPath("handle").type(STRING)
                                                .description("영구 공개 핸들"),
                                        fieldWithPath("displayName").type(STRING)
                                                .description("프로필 표시 이름"),
                                        fieldWithPath("userType").type(STRING)
                                                .description("사용자 유형")
                                )
                                .responseSchema(Schema.schema("ErrorResponse"))
                                .responseFields(RestDocsFields.errorResponse())
                                .build())
                ));
    }

    @Test
    @DisplayName("우테코 크루 가입 정보가 불완전하면 400으로 응답한다")
    void rejectIncompleteCrewSignupProfile() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "handle": "zzaekkii",
                                  "displayName": "재키",
                                  "userType": "WOOWACOURSE_CREW"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details[0].field").value("track"));
    }

    @Test
    @DisplayName("가입 대기 OAuth 신원이 없으면 가입 요청을 거부한다")
    void rejectSignupWithoutPendingOAuthIdentity() throws Exception {
        given(authSessionAccessor.findPendingIdentity(any())).willReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/auth/signup")
                        .header(HttpHeaders.COOKIE, "JSESSIONID=expired-session-id")
                        .header("X-CSRF-Token", "csrf-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "handle": "zzaekkii",
                                  "displayName": "재키",
                                  "userType": "GENERAL"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("OAUTH_SIGNUP_SESSION_NOT_FOUND"))
                .andDo(document(
                        "auth-signup-session-missing",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Auth")
                                .summary("OAuth 사용자 가입")
                                .description(SIGNUP_DESCRIPTION)
                                .requestHeaders(
                                        headerWithName(HttpHeaders.COOKIE)
                                                .description("가입 대기 신원이 저장된 JSESSIONID"),
                                        headerWithName("X-CSRF-Token")
                                                .description("세션 조회 API에서 발급받은 CSRF Token"),
                                        headerWithName(HttpHeaders.CONTENT_TYPE)
                                                .description("application/json")
                                )
                                .requestSchema(Schema.schema("OAuthSignupRequest"))
                                .requestFields(
                                        fieldWithPath("handle").type(STRING)
                                                .description("영구 공개 핸들"),
                                        fieldWithPath("displayName").type(STRING)
                                                .description("프로필 표시 이름"),
                                        fieldWithPath("userType").type(STRING)
                                                .description("GENERAL, WOOWACOURSE_CREW, WOOWACOURSE_COACH"),
                                        fieldWithPath("track").type(STRING)
                                                .description("우테코 크루의 트랙").optional(),
                                        fieldWithPath("cohort").type(NUMBER)
                                                .description("우테코 크루의 기수").optional()
                                )
                                .responseSchema(Schema.schema("ErrorResponse"))
                                .responseFields(RestDocsFields.errorResponse())
                                .build())
                ));
    }

    @Test
    @DisplayName("로그아웃하면 인증 세션을 무효화한다")
    void logout() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header(HttpHeaders.COOKIE, "JSESSIONID=session-id")
                        .header("X-CSRF-Token", "csrf-token"))
                .andExpect(status().isNoContent())
                .andDo(document(
                        "auth-logout",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Auth")
                                .summary("로그아웃")
                                .description("현재 인증 세션을 무효화한다. "
                                        + "CSRF Token이 유효하지 않으면 403 CSRF_TOKEN_INVALID를 반환한다.")
                                .requestHeaders(
                                        headerWithName(HttpHeaders.COOKIE)
                                                .description("인증된 사용자의 JSESSIONID"),
                                        headerWithName("X-CSRF-Token")
                                                .description("세션 조회 API에서 발급받은 CSRF Token")
                                )
                                .build())
                ));
    }

    private OAuthLoginAttempt loginAttempt() {
        return new OAuthLoginAttempt(
                "state",
                "code-verifier",
                Instant.parse("2026-09-03T00:00:00Z")
        );
    }

    private OAuthIdentity githubIdentity() {
        return new OAuthIdentity(
                OAuthProvider.GITHUB,
                "12345678",
                "https://avatars.githubusercontent.com/u/12345678",
                "https://github.com/zzaekkii"
        );
    }
}
