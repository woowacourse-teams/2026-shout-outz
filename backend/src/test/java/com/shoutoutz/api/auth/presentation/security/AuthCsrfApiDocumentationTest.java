package com.shoutoutz.api.auth.presentation.security;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.shoutoutz.api.auth.presentation.AuthSessionHttpApi;
import com.shoutoutz.api.auth.presentation.session.AuthSessionAccessor;
import com.shoutoutz.api.auth.presentation.session.AuthSessionManager;
import com.shoutoutz.api.common.restdocs.RestDocsFields;
import com.shoutoutz.api.user.domain.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@DisplayName("인증 CSRF API 문서")
@WebMvcTest(controllers = AuthSessionHttpApi.class)
@Import({AuthFilterConfig.class, AuthSessionAccessor.class, CsrfTokenManager.class})
@AutoConfigureRestDocs
class AuthCsrfApiDocumentationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthSessionAccessor authSessionAccessor;

    @Autowired
    private CsrfTokenManager csrfTokenManager;

    @MockitoBean
    private AuthSessionManager authSessionManager;

    @Test
    @DisplayName("로그아웃 요청의 CSRF 토큰이 유효하지 않으면 공통 오류 형식으로 응답한다")
    void rejectLogoutWithInvalidCsrfToken() throws Exception {
        MockHttpSession session = new MockHttpSession();
        authSessionAccessor.saveAuthentication(session, 1L, UserRole.USER);
        csrfTokenManager.getOrCreate(session);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .session(session)
                        .header(HttpHeaders.COOKIE, "JSESSIONID=session-id")
                        .header(CsrfProtectionFilter.CSRF_HEADER, "csrf-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"))
                .andDo(document(
                        "auth-logout-csrf-invalid",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Auth")
                                .summary("로그아웃")
                                .description("현재 인증 세션을 무효화한다. "
                                        + "CSRF Token이 유효하지 않으면 "
                                        + "403 CSRF_TOKEN_INVALID를 반환한다.")
                                .requestHeaders(
                                        headerWithName(HttpHeaders.COOKIE)
                                                .description("인증된 사용자의 JSESSIONID"),
                                        headerWithName(CsrfProtectionFilter.CSRF_HEADER)
                                                .description("세션 조회 API에서 발급받은 CSRF Token")
                                )
                                .responseSchema(Schema.schema("ErrorResponse"))
                                .responseFields(RestDocsFields.errorResponse())
                                .build())
                ));
    }
}
