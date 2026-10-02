package com.shoutoutz.api.comment.presentation;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.SimpleType.INTEGER;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.shoutoutz.api.auth.presentation.session.AuthenticatedSession;
import com.shoutoutz.api.comment.application.ProjectCommentReactionService;
import com.shoutoutz.api.comment.domain.CommentErrorCode;
import com.shoutoutz.api.comment.domain.ProjectCommentReactionType;
import com.shoutoutz.api.comment.presentation.dto.response.ProjectCommentReactionResponse;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.InvalidInputException;
import com.shoutoutz.api.common.restdocs.RestDocsFields;
import com.shoutoutz.api.user.domain.account.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ProjectCommentReactionHttpApi.class)
@AutoConfigureRestDocs
class ProjectCommentReactionHttpApiTest {

    private static final long USER_ID = 1L;
    private static final String SLUG = "loop";
    private static final long COMMENT_ID = 501L;
    private static final String AUTHENTICATED_SESSION_ATTRIBUTE = AuthenticatedSession.class.getName();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectCommentReactionService projectCommentReactionService;

    @Test
    void 프로젝트_댓글에_공감을_추가한다() throws Exception {
        given(projectCommentReactionService.add(SLUG, COMMENT_ID, USER_ID, "AGREE"))
                .willReturn(new ProjectCommentReactionResponse(
                        SLUG,
                        COMMENT_ID,
                        ProjectCommentReactionType.AGREE,
                        true,
                        7L
                ));

        mockMvc.perform(put(
                                "/api/v1/projects/@{slug}/comments/{commentId}/reactions/{type}",
                                SLUG,
                                COMMENT_ID,
                                "AGREE"
                        )
                        .requestAttr(
                                AUTHENTICATED_SESSION_ATTRIBUTE,
                                new AuthenticatedSession(USER_ID, UserRole.USER)
                        )
                        .header("X-CSRF-Token", "csrf-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.slug").value(SLUG))
                .andExpect(jsonPath("$.data.commentId").value(COMMENT_ID))
                .andExpect(jsonPath("$.data.type").value("AGREE"))
                .andExpect(jsonPath("$.data.active").value(true))
                .andExpect(jsonPath("$.data.agreeCount").value(7))
                .andDo(document(
                        "project-comment-reaction-add",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Project Comment Reaction")
                                .summary("프로젝트 댓글 반응 추가")
                                .description("승인 완료되고 삭제되지 않은 프로젝트 댓글에 현재 로그인 사용자의 AGREE 반응을 추가한다. 이미 존재하는 반응이면 현재 상태를 반환한다.")
                                .pathParameters(
                                        parameterWithName("slug").description("프로젝트 slug. 경로에서는 앞에 @를 붙인다."),
                                        parameterWithName("commentId").type(INTEGER).description("댓글 ID"),
                                        parameterWithName("type").description("반응 타입(현재 AGREE)")
                                )
                                .requestHeaders(
                                        headerWithName("X-CSRF-Token").description("세션 조회로 발급받은 CSRF 토큰")
                                )
                                .responseSchema(Schema.schema("ProjectCommentReactionSuccessResponse"))
                                .responseFields(successResponseFields())
                                .build())
                ));

        verify(projectCommentReactionService).add(SLUG, COMMENT_ID, USER_ID, "AGREE");
    }

    @Test
    void 프로젝트_댓글의_공감을_제거한다() throws Exception {
        given(projectCommentReactionService.remove(SLUG, COMMENT_ID, USER_ID, "AGREE"))
                .willReturn(new ProjectCommentReactionResponse(
                        SLUG,
                        COMMENT_ID,
                        ProjectCommentReactionType.AGREE,
                        false,
                        6L
                ));

        mockMvc.perform(delete(
                                "/api/v1/projects/@{slug}/comments/{commentId}/reactions/{type}",
                                SLUG,
                                COMMENT_ID,
                                "AGREE"
                        )
                        .requestAttr(
                                AUTHENTICATED_SESSION_ATTRIBUTE,
                                new AuthenticatedSession(USER_ID, UserRole.USER)
                        )
                        .header("X-CSRF-Token", "csrf-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slug").value(SLUG))
                .andExpect(jsonPath("$.data.commentId").value(COMMENT_ID))
                .andExpect(jsonPath("$.data.type").value("AGREE"))
                .andExpect(jsonPath("$.data.active").value(false))
                .andExpect(jsonPath("$.data.agreeCount").value(6))
                .andDo(document(
                        "project-comment-reaction-remove",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Project Comment Reaction")
                                .summary("프로젝트 댓글 반응 제거")
                                .description("삭제되지 않은 프로젝트 댓글에서 현재 로그인 사용자의 AGREE 반응을 제거한다. 반응이 존재하면 삭제 후 현재 상태를 반환하고, 반응이 없으면 REACTION_NOT_FOUND를 반환한다.")
                                .pathParameters(
                                        parameterWithName("slug").description("프로젝트 slug. 경로에서는 앞에 @를 붙인다."),
                                        parameterWithName("commentId").type(INTEGER).description("댓글 ID"),
                                        parameterWithName("type").description("반응 타입(현재 AGREE)")
                                )
                                .requestHeaders(
                                        headerWithName("X-CSRF-Token").description("세션 조회로 발급받은 CSRF 토큰")
                                )
                                .responseSchema(Schema.schema("ProjectCommentReactionSuccessResponse"))
                                .responseFields(successResponseFields())
                                .build())
                ));

        verify(projectCommentReactionService).remove(SLUG, COMMENT_ID, USER_ID, "AGREE");
    }

    @Test
    void 프로젝트_댓글에_반응이_없으면_삭제할_때_404를_반환한다() throws Exception {
        willThrow(new EntityNotFoundException(CommentErrorCode.REACTION_NOT_FOUND))
                .given(projectCommentReactionService).remove(SLUG, COMMENT_ID, USER_ID, "AGREE");

        mockMvc.perform(delete(
                                "/api/v1/projects/@{slug}/comments/{commentId}/reactions/{type}",
                                SLUG,
                                COMMENT_ID,
                                "AGREE"
                        )
                        .requestAttr(
                                AUTHENTICATED_SESSION_ATTRIBUTE,
                                new AuthenticatedSession(USER_ID, UserRole.USER)
                        )
                        .header("X-CSRF-Token", "csrf-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value(CommentErrorCode.REACTION_NOT_FOUND.name()))
                .andExpect(jsonPath("$.message").value("요청한 반응을 찾을 수 없습니다."))
                .andDo(document(
                        "project-comment-reaction-remove-reaction-not-found",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Project Comment Reaction")
                                .summary("프로젝트 댓글 반응 제거")
                                .description("현재 로그인 사용자의 해당 프로젝트 댓글 반응이 없으면 REACTION_NOT_FOUND를 반환한다.")
                                .pathParameters(
                                        parameterWithName("slug").description("프로젝트 slug. 경로에서는 앞에 @를 붙인다."),
                                        parameterWithName("commentId").type(INTEGER).description("댓글 ID"),
                                        parameterWithName("type").description("반응 타입(현재 AGREE)")
                                )
                                .requestHeaders(
                                        headerWithName("X-CSRF-Token").description("세션 조회로 발급받은 CSRF 토큰")
                                )
                                .responseSchema(Schema.schema("ErrorResponse"))
                                .responseFields(RestDocsFields.errorResponse())
                                .build())
                ));

        verify(projectCommentReactionService).remove(SLUG, COMMENT_ID, USER_ID, "AGREE");
    }

    @Test
    void 지원하지_않는_반응_타입은_400을_반환한다() throws Exception {
        willThrow(new InvalidInputException(CommentErrorCode.REACTION_TYPE_INVALID))
                .given(projectCommentReactionService).add(SLUG, COMMENT_ID, USER_ID, "LIKE");

        mockMvc.perform(put(
                                "/api/v1/projects/@{slug}/comments/{commentId}/reactions/{type}",
                                SLUG,
                                COMMENT_ID,
                                "LIKE"
                        )
                        .requestAttr(
                                AUTHENTICATED_SESSION_ATTRIBUTE,
                                new AuthenticatedSession(USER_ID, UserRole.USER)
                        )
                        .header("X-CSRF-Token", "csrf-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REACTION_TYPE_INVALID"));
    }

    @Test
    void 로그인하지_않으면_댓글_반응을_변경할_수_없다() throws Exception {
        mockMvc.perform(put(
                        "/api/v1/projects/@{slug}/comments/{commentId}/reactions/{type}",
                        SLUG,
                        COMMENT_ID,
                        "AGREE"
                ))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(projectCommentReactionService);
    }

    private org.springframework.restdocs.payload.FieldDescriptor[] successResponseFields() {
        return new org.springframework.restdocs.payload.FieldDescriptor[]{
                fieldWithPath("status").type(STRING).description("응답 상태"),
                fieldWithPath("data").type(OBJECT).description("프로젝트 댓글 반응 변경 결과"),
                fieldWithPath("data.slug").type(STRING).description("프로젝트 slug"),
                fieldWithPath("data.commentId").type(NUMBER).description("댓글 ID"),
                fieldWithPath("data.type").type(STRING).description("반응 타입(현재 AGREE)"),
                fieldWithPath("data.active").type(BOOLEAN).description("요청한 반응의 활성 상태"),
                fieldWithPath("data.agreeCount").type(NUMBER).description("댓글 공감 수")
        };
    }
}
