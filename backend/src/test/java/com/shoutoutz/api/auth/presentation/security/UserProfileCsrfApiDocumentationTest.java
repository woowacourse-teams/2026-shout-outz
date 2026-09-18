package com.shoutoutz.api.auth.presentation.security;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.shoutoutz.api.auth.presentation.session.AuthSessionAccessor;
import com.shoutoutz.api.auth.presentation.session.AuthSessionManager;
import com.shoutoutz.api.common.restdocs.RestDocsFields;
import com.shoutoutz.api.user.application.UserService;
import com.shoutoutz.api.user.domain.account.UserRole;
import com.shoutoutz.api.user.presentation.UserHttpApi;
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

@DisplayName("사용자 프로필 수정 CSRF API 문서")
@WebMvcTest(controllers = UserHttpApi.class)
@Import({AuthFilterConfig.class, AuthSessionAccessor.class, CsrfTokenManager.class})
@AutoConfigureRestDocs
class UserProfileCsrfApiDocumentationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthSessionAccessor authSessionAccessor;

    @Autowired
    private CsrfTokenManager csrfTokenManager;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthSessionManager authSessionManager;

    @Test
    @DisplayName("CSRF 토큰이 유효하지 않으면 프로필 수정에 실패한다")
    void rejectProfileUpdateWithInvalidCsrfToken() throws Exception {
        MockHttpSession session = new MockHttpSession();
        authSessionAccessor.saveAuthentication(session, 1L, UserRole.USER);
        csrfTokenManager.getOrCreate(session);

        mockMvc.perform(put("/api/v1/users/me")
                        .session(session)
                        .header(HttpHeaders.COOKIE, "JSESSIONID=session-id")
                        .header(CsrfProtectionFilter.CSRF_HEADER, "invalid-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"))
                .andDo(document(
                        "user-profile-update-csrf-invalid",
                        resource(ResourceSnippetParameters.builder()
                                .tag("User")
                                .summary("내 프로필 수정")
                                .description("로그인한 사용자의 수정 가능한 프로필 정보를 저장한다.")
                                .requestHeaders(
                                        headerWithName(HttpHeaders.COOKIE)
                                                .description("인증된 사용자의 JSESSIONID"),
                                        headerWithName(CsrfProtectionFilter.CSRF_HEADER)
                                                .description("세션 조회 API에서 발급받은 CSRF Token")
                                )
                                .requestSchema(Schema.schema("UserProfileUpdateRequest"))
                                .responseSchema(Schema.schema("ErrorResponse"))
                                .responseFields(RestDocsFields.errorResponse())
                                .build())
                ));
    }
}
