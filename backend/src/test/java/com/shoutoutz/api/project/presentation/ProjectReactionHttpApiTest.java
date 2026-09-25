package com.shoutoutz.api.project.presentation;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.payload.JsonFieldType.BOOLEAN;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.OBJECT;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.shoutoutz.api.auth.presentation.session.AuthenticatedSession;
import com.shoutoutz.api.common.exception.custom.InvalidInputException;
import com.shoutoutz.api.project.application.ProjectReactionService;
import com.shoutoutz.api.project.domain.ProjectErrorCode;
import com.shoutoutz.api.project.domain.ProjectReactionType;
import com.shoutoutz.api.project.presentation.dto.response.ProjectReactionResponse;
import com.shoutoutz.api.user.domain.account.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ProjectReactionHttpApi.class)
@AutoConfigureRestDocs
class ProjectReactionHttpApiTest {

    private static final long USER_ID = 1L;
    private static final long PROJECT_ID = 100L;
    private static final String AUTHENTICATED_SESSION_ATTRIBUTE = AuthenticatedSession.class.getName();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectReactionService projectReactionService;

    @Test
    void 프로젝트_좋아요를_추가한다() throws Exception {
        given(projectReactionService.add(PROJECT_ID, USER_ID, "LIKE"))
                .willReturn(new ProjectReactionResponse(PROJECT_ID, ProjectReactionType.LIKE, true, 84L, 28L));

        mockMvc.perform(put("/api/v1/projects/{projectId}/reactions/{type}", PROJECT_ID, "LIKE")
                        .requestAttr(
                                AUTHENTICATED_SESSION_ATTRIBUTE,
                                new AuthenticatedSession(USER_ID, UserRole.USER)
                        )
                        .header("X-CSRF-Token", "csrf-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.projectId").value(PROJECT_ID))
                .andExpect(jsonPath("$.data.type").value("LIKE"))
                .andExpect(jsonPath("$.data.active").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(84))
                .andExpect(jsonPath("$.data.bookmarkCount").value(28))
                .andDo(document(
                        "project-reaction-add",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Project Reaction")
                                .summary("프로젝트 반응 추가")
                                .description("승인 완료되고 삭제되지 않은 프로젝트에 현재 로그인 사용자의 좋아요 또는 북마크를 추가한다.")
                                .pathParameters(
                                        parameterWithName("projectId").description("프로젝트 ID"),
                                        parameterWithName("type").description("반응 타입(LIKE 또는 BOOKMARK)")
                                )
                                .requestHeaders(
                                        headerWithName("X-CSRF-Token").description("세션 조회로 발급받은 CSRF 토큰")
                                )
                                .responseSchema(Schema.schema("ProjectReactionSuccessResponse"))
                                .responseFields(successResponseFields())
                                .build())
                ));

        verify(projectReactionService).add(PROJECT_ID, USER_ID, "LIKE");
    }

    @Test
    void 프로젝트_북마크를_제거한다() throws Exception {
        given(projectReactionService.remove(PROJECT_ID, USER_ID, "BOOKMARK"))
                .willReturn(new ProjectReactionResponse(
                        PROJECT_ID,
                        ProjectReactionType.BOOKMARK,
                        false,
                        83L,
                        27L
                ));

        mockMvc.perform(delete("/api/v1/projects/{projectId}/reactions/{type}", PROJECT_ID, "BOOKMARK")
                        .requestAttr(
                                AUTHENTICATED_SESSION_ATTRIBUTE,
                                new AuthenticatedSession(USER_ID, UserRole.USER)
                        )
                        .header("X-CSRF-Token", "csrf-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.projectId").value(PROJECT_ID))
                .andExpect(jsonPath("$.data.type").value("BOOKMARK"))
                .andExpect(jsonPath("$.data.active").value(false))
                .andExpect(jsonPath("$.data.likeCount").value(83))
                .andExpect(jsonPath("$.data.bookmarkCount").value(27))
                .andDo(document(
                        "project-reaction-remove",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Project Reaction")
                                .summary("프로젝트 반응 제거")
                                .description("삭제되지 않은 프로젝트에서 현재 로그인 사용자의 좋아요 또는 북마크를 제거한다. 승인 상태와 무관하게 처리한다.")
                                .pathParameters(
                                        parameterWithName("projectId").description("프로젝트 ID"),
                                        parameterWithName("type").description("반응 타입(LIKE 또는 BOOKMARK)")
                                )
                                .requestHeaders(
                                        headerWithName("X-CSRF-Token").description("세션 조회로 발급받은 CSRF 토큰")
                                )
                                .responseSchema(Schema.schema("ProjectReactionSuccessResponse"))
                                .responseFields(successResponseFields())
                                .build())
                ));

        verify(projectReactionService).remove(PROJECT_ID, USER_ID, "BOOKMARK");
    }

    @Test
    void 지원하지_않는_반응_타입은_400을_반환한다() throws Exception {
        willThrow(new InvalidInputException(ProjectErrorCode.REACTION_TYPE_INVALID))
                .given(projectReactionService).add(PROJECT_ID, USER_ID, "AGREE");

        mockMvc.perform(put("/api/v1/projects/{projectId}/reactions/{type}", PROJECT_ID, "AGREE")
                        .requestAttr(
                                AUTHENTICATED_SESSION_ATTRIBUTE,
                                new AuthenticatedSession(USER_ID, UserRole.USER)
                        )
                        .header("X-CSRF-Token", "csrf-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REACTION_TYPE_INVALID"));
    }

    @Test
    void 로그인하지_않으면_반응을_변경할_수_없다() throws Exception {
        mockMvc.perform(put("/api/v1/projects/{projectId}/reactions/{type}", PROJECT_ID, "LIKE"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(projectReactionService);
    }

    private org.springframework.restdocs.payload.FieldDescriptor[] successResponseFields() {
        return new org.springframework.restdocs.payload.FieldDescriptor[]{
                fieldWithPath("status").type(STRING).description("응답 상태"),
                fieldWithPath("data").type(OBJECT).description("프로젝트 반응 변경 결과"),
                fieldWithPath("data.projectId").type(NUMBER).description("프로젝트 ID"),
                fieldWithPath("data.type").type(STRING).description("반응 타입(LIKE 또는 BOOKMARK)"),
                fieldWithPath("data.active").type(BOOLEAN).description("요청한 반응의 활성 상태"),
                fieldWithPath("data.likeCount").type(NUMBER).description("프로젝트 좋아요 수"),
                fieldWithPath("data.bookmarkCount").type(NUMBER).description("프로젝트 북마크 수")
        };
    }
}
