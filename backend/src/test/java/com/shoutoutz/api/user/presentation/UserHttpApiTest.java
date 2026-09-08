package com.shoutoutz.api.user.presentation;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
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
import com.shoutoutz.api.user.application.UserQueryService;
import com.shoutoutz.api.user.application.dto.result.UserProfileResult;
import com.shoutoutz.api.user.application.dto.result.UserProfileSummaryResult;
import com.shoutoutz.api.user.domain.UserProfileCounts;
import com.shoutoutz.api.user.domain.UserRole;
import com.shoutoutz.api.user.domain.UserType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@DisplayName("사용자 API")
@WebMvcTest(controllers = UserHttpApi.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs
class UserHttpApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserQueryService userQueryService;

    @Test
    @DisplayName("내 프로필 요약을 조회한다")
    void getMyProfileSummary() throws Exception {
        given(userQueryService.getMyProfileSummary(1L))
                .willReturn(new UserProfileSummaryResult(
                        1L,
                        "zzaekkii",
                        "재키",
                        21L
                ));

        mockMvc.perform(get("/api/v1/users/me/summary")
                        .header(HttpHeaders.COOKIE, "JSESSIONID=session-id")
                        .requestAttr(
                                AuthenticatedSession.class.getName(),
                                new AuthenticatedSession(1L, UserRole.USER)
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.handle").value("zzaekkii"))
                .andExpect(jsonPath("$.data.displayName").value("재키"))
                .andExpect(jsonPath("$.data.avatarImageId").value(21))
                .andDo(document(
                        "user-profile-summary-get",
                        resource(ResourceSnippetParameters.builder()
                                .tag("User")
                                .summary("내 프로필 요약 조회")
                                .description("로그인 후 공통 헤더에 표시할 최소 사용자 정보를 조회한다.")
                                .privateResource(true)
                                .requestHeaders(
                                        headerWithName(HttpHeaders.COOKIE)
                                                .description("인증된 사용자의 JSESSIONID")
                                )
                                .responseSchema(Schema.schema("UserProfileSummarySuccessResponse"))
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data").type(OBJECT).description("프로필 요약 정보"),
                                        fieldWithPath("data.userId").type(NUMBER).description("사용자 ID"),
                                        fieldWithPath("data.handle").type(STRING).description("사용자 handle"),
                                        fieldWithPath("data.displayName").type(STRING).description("표시 이름"),
                                        fieldWithPath("data.avatarImageId").type(NUMBER)
                                                .description("프로필 이미지 미디어 ID").optional()
                                )
                                .build())
                ));
    }

    @Test
    @DisplayName("인증 정보가 없으면 내 프로필 요약 조회에 실패한다")
    void rejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/summary"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("마이페이지 프로필을 조회한다")
    void getMyProfile() throws Exception {
        given(userQueryService.getMyProfile(1L))
                .willReturn(new UserProfileResult(
                        1L,
                        "zzaekkii",
                        "재키",
                        UserType.WOOWACOURSE_CREW,
                        "BACKEND",
                        (short) 8,
                        "백엔드 개발자입니다.",
                        21L,
                        "https://github.com/zzaekkii",
                        "https://zzaekkii.dev",
                        new UserProfileCounts(2L, 18L)
                ));

        mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.COOKIE, "JSESSIONID=session-id")
                        .requestAttr(
                                AuthenticatedSession.class.getName(),
                                new AuthenticatedSession(1L, UserRole.USER)
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.handle").value("zzaekkii"))
                .andExpect(jsonPath("$.data.displayName").value("재키"))
                .andExpect(jsonPath("$.data.userType").value("WOOWACOURSE_CREW"))
                .andExpect(jsonPath("$.data.track").value("BACKEND"))
                .andExpect(jsonPath("$.data.cohort").value(8))
                .andExpect(jsonPath("$.data.bio").value("백엔드 개발자입니다."))
                .andExpect(jsonPath("$.data.avatarImageId").value(21))
                .andExpect(jsonPath("$.data.githubProfileUrl")
                        .value("https://github.com/zzaekkii"))
                .andExpect(jsonPath("$.data.blogUrl").value("https://zzaekkii.dev"))
                .andExpect(jsonPath("$.data.counts.projects").value(2))
                .andExpect(jsonPath("$.data.counts.posts").value(18))
                .andDo(document(
                        "user-profile-get",
                        resource(ResourceSnippetParameters.builder()
                                .tag("User")
                                .summary("마이페이지 조회")
                                .description("로그인한 사용자의 프로필과 프로젝트·피드 개수를 조회한다.")
                                .privateResource(true)
                                .requestHeaders(
                                        headerWithName(HttpHeaders.COOKIE)
                                                .description("인증된 사용자의 JSESSIONID")
                                )
                                .responseSchema(Schema.schema("UserProfileSuccessResponse"))
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data").type(OBJECT).description("사용자 프로필"),
                                        fieldWithPath("data.userId").type(NUMBER).description("사용자 ID"),
                                        fieldWithPath("data.handle").type(STRING).description("사용자 handle"),
                                        fieldWithPath("data.displayName").type(STRING).description("표시 이름"),
                                        fieldWithPath("data.userType").type(STRING).description("사용자 유형"),
                                        fieldWithPath("data.track").type(STRING).description("우테코 트랙").optional(),
                                        fieldWithPath("data.cohort").type(NUMBER).description("우테코 기수").optional(),
                                        fieldWithPath("data.bio").type(STRING).description("한 줄 소개").optional(),
                                        fieldWithPath("data.avatarImageId").type(NUMBER)
                                                .description("프로필 이미지 미디어 ID").optional(),
                                        fieldWithPath("data.githubProfileUrl").type(STRING)
                                                .description("GitHub 프로필 URL").optional(),
                                        fieldWithPath("data.blogUrl").type(STRING)
                                                .description("블로그 URL").optional(),
                                        fieldWithPath("data.counts").type(OBJECT).description("프로필 항목 개수"),
                                        fieldWithPath("data.counts.projects").type(NUMBER)
                                                .description("삭제되지 않은 참여 프로젝트 개수"),
                                        fieldWithPath("data.counts.posts").type(NUMBER)
                                                .description("삭제되지 않은 작성 피드 개수")
                                )
                                .build())
                ));
    }
}
