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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.shoutoutz.api.auth.presentation.session.AuthenticatedSession;
import com.shoutoutz.api.comment.application.MagazineCommentReactionService;
import com.shoutoutz.api.comment.domain.CommentErrorCode;
import com.shoutoutz.api.comment.domain.MagazineCommentReactionType;
import com.shoutoutz.api.comment.presentation.dto.response.MagazineCommentReactionResponse;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.common.exception.custom.InvalidInputException;
import com.shoutoutz.api.common.restdocs.RestDocsFields;
import com.shoutoutz.api.magazine.domain.MagazineErrorCode;
import com.shoutoutz.api.user.domain.account.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultHandler;

@WebMvcTest(controllers = MagazineCommentReactionHttpApi.class)
@AutoConfigureRestDocs
class MagazineCommentReactionHttpApiTest {

    private static final long USER_ID = 1L;
    private static final long MAGAZINE_ID = 100L;
    private static final long COMMENT_ID = 501L;
    private static final String AUTHENTICATED_SESSION_ATTRIBUTE = AuthenticatedSession.class.getName();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MagazineCommentReactionService magazineCommentReactionService;

    @Test
    void 매거진_댓글에_공감을_추가한다() throws Exception {
        given(magazineCommentReactionService.add(MAGAZINE_ID, COMMENT_ID, USER_ID, "AGREE"))
                .willReturn(new MagazineCommentReactionResponse(
                        MAGAZINE_ID,
                        COMMENT_ID,
                        MagazineCommentReactionType.AGREE,
                        true,
                        7L
                ));

        mockMvc.perform(put(
                                "/api/v1/magazines/{magazineId}/comments/{commentId}/reactions/{type}",
                                MAGAZINE_ID,
                                COMMENT_ID,
                                "AGREE"
                        )
                        .requestAttr(
                                AUTHENTICATED_SESSION_ATTRIBUTE,
                                new AuthenticatedSession(USER_ID, UserRole.USER)
                        )
                        .header(HttpHeaders.COOKIE, "JSESSIONID=user-session")
                        .header("X-CSRF-Token", "csrf-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.magazineId").value(MAGAZINE_ID))
                .andExpect(jsonPath("$.data.commentId").value(COMMENT_ID))
                .andExpect(jsonPath("$.data.type").value("AGREE"))
                .andExpect(jsonPath("$.data.active").value(true))
                .andExpect(jsonPath("$.data.agreeCount").value(7))
                .andDo(document(
                        "magazine-comment-reaction-add",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Magazine Comment Reaction")
                                .summary("매거진 댓글 반응 추가")
                                .description("발행된 매거진의 삭제되지 않은 댓글에 현재 로그인 사용자의 AGREE 반응을 추가한다. 이미 존재하는 반응이면 현재 상태를 반환한다.")
                                .pathParameters(
                                        parameterWithName("magazineId").type(INTEGER).description("매거진 ID"),
                                        parameterWithName("commentId").type(INTEGER).description("댓글 ID"),
                                        parameterWithName("type").description("반응 타입(현재 AGREE)")
                                )
                                .requestHeaders(
                                        headerWithName(HttpHeaders.COOKIE).description("활성 회원 세션의 JSESSIONID"),
                                        headerWithName("X-CSRF-Token").description("세션 조회로 발급받은 CSRF 토큰")
                                )
                                .responseSchema(Schema.schema("MagazineCommentReactionSuccessResponse"))
                                .responseFields(successResponseFields())
                                .build())
                ));

        verify(magazineCommentReactionService).add(MAGAZINE_ID, COMMENT_ID, USER_ID, "AGREE");
    }

    @Test
    void 매거진_댓글의_공감을_제거한다() throws Exception {
        given(magazineCommentReactionService.remove(MAGAZINE_ID, COMMENT_ID, USER_ID, "AGREE"))
                .willReturn(new MagazineCommentReactionResponse(
                        MAGAZINE_ID,
                        COMMENT_ID,
                        MagazineCommentReactionType.AGREE,
                        false,
                        6L
                ));

        mockMvc.perform(delete(
                                "/api/v1/magazines/{magazineId}/comments/{commentId}/reactions/{type}",
                                MAGAZINE_ID,
                                COMMENT_ID,
                                "AGREE"
                        )
                        .requestAttr(
                                AUTHENTICATED_SESSION_ATTRIBUTE,
                                new AuthenticatedSession(USER_ID, UserRole.USER)
                        )
                        .header(HttpHeaders.COOKIE, "JSESSIONID=user-session")
                        .header("X-CSRF-Token", "csrf-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.magazineId").value(MAGAZINE_ID))
                .andExpect(jsonPath("$.data.commentId").value(COMMENT_ID))
                .andExpect(jsonPath("$.data.type").value("AGREE"))
                .andExpect(jsonPath("$.data.active").value(false))
                .andExpect(jsonPath("$.data.agreeCount").value(6))
                .andDo(document(
                        "magazine-comment-reaction-remove",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Magazine Comment Reaction")
                                .summary("매거진 댓글 반응 제거")
                                .description("발행된 매거진의 삭제되지 않은 댓글에서 현재 로그인 사용자의 AGREE 반응을 제거한다. 반응이 존재하면 삭제 후 현재 상태를 반환하고, 반응이 없으면 REACTION_NOT_FOUND를 반환한다.")
                                .pathParameters(
                                        parameterWithName("magazineId").type(INTEGER).description("매거진 ID"),
                                        parameterWithName("commentId").type(INTEGER).description("댓글 ID"),
                                        parameterWithName("type").description("반응 타입(현재 AGREE)")
                                )
                                .requestHeaders(
                                        headerWithName(HttpHeaders.COOKIE).description("활성 회원 세션의 JSESSIONID"),
                                        headerWithName("X-CSRF-Token").description("세션 조회로 발급받은 CSRF 토큰")
                                )
                                .responseSchema(Schema.schema("MagazineCommentReactionSuccessResponse"))
                                .responseFields(successResponseFields())
                                .build())
                ));

        verify(magazineCommentReactionService).remove(MAGAZINE_ID, COMMENT_ID, USER_ID, "AGREE");
    }

    @Test
    void 매거진_댓글에_반응이_없으면_삭제할_때_404를_반환한다() throws Exception {
        willThrow(new EntityNotFoundException(CommentErrorCode.REACTION_NOT_FOUND))
                .given(magazineCommentReactionService).remove(MAGAZINE_ID, COMMENT_ID, USER_ID, "AGREE");

        mockMvc.perform(delete(
                                "/api/v1/magazines/{magazineId}/comments/{commentId}/reactions/{type}",
                                MAGAZINE_ID,
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
                        "magazine-comment-reaction-remove-reaction-not-found",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Magazine Comment Reaction")
                                .summary("매거진 댓글 반응 제거")
                                .pathParameters(
                                        parameterWithName("magazineId").type(INTEGER).description("매거진 ID"),
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

        verify(magazineCommentReactionService).remove(MAGAZINE_ID, COMMENT_ID, USER_ID, "AGREE");
    }

    @Test
    void 지원하지_않는_반응_타입은_400을_반환한다() throws Exception {
        willThrow(new InvalidInputException(CommentErrorCode.REACTION_TYPE_INVALID))
                .given(magazineCommentReactionService).add(MAGAZINE_ID, COMMENT_ID, USER_ID, "LIKE");

        mockMvc.perform(put(
                                "/api/v1/magazines/{magazineId}/comments/{commentId}/reactions/{type}",
                                MAGAZINE_ID,
                                COMMENT_ID,
                                "LIKE"
                        )
                        .requestAttr(
                                AUTHENTICATED_SESSION_ATTRIBUTE,
                                new AuthenticatedSession(USER_ID, UserRole.USER)
                        )
                        .header("X-CSRF-Token", "csrf-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REACTION_TYPE_INVALID"))
                .andDo(errorDocument("magazine-comment-reaction-add-invalid-type"));
    }

    @Test
    void 로그인하지_않으면_댓글_반응을_변경할_수_없다() throws Exception {
        mockMvc.perform(put(
                        "/api/v1/magazines/{magazineId}/comments/{commentId}/reactions/{type}",
                        MAGAZINE_ID,
                        COMMENT_ID,
                        "AGREE"
                ))
                .andExpect(status().isUnauthorized())
                .andDo(errorDocument("magazine-comment-reaction-add-unauthorized"));

        verifyNoInteractions(magazineCommentReactionService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PUT", "DELETE"})
    void 비활성_사용자의_댓글_공감은_403을_반환한다(String method) throws Exception {
        given(magazineCommentReactionService.add(MAGAZINE_ID, COMMENT_ID, USER_ID, "AGREE"))
                .willThrow(new ForbiddenException(MagazineErrorCode.MAGAZINE_USER_FORBIDDEN));
        given(magazineCommentReactionService.remove(MAGAZINE_ID, COMMENT_ID, USER_ID, "AGREE"))
                .willThrow(new ForbiddenException(MagazineErrorCode.MAGAZINE_USER_FORBIDDEN));
        String operation = "remove";
        if (method.equals("PUT")) {
            operation = "add";
        }
        mockMvc.perform(request(HttpMethod.valueOf(method),
                        "/api/v1/magazines/{magazineId}/comments/{commentId}/reactions/{type}",
                        MAGAZINE_ID, COMMENT_ID, "AGREE")
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE, new AuthenticatedSession(USER_ID, UserRole.USER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MAGAZINE_USER_FORBIDDEN"))
                .andDo(errorDocument("magazine-comment-reaction-" + operation + "-forbidden"));
    }

    @Test
    void 공개되지_않은_매거진의_댓글에_공감하면_404를_반환한다() throws Exception {
        given(magazineCommentReactionService.add(MAGAZINE_ID, COMMENT_ID, USER_ID, "AGREE"))
                .willThrow(new EntityNotFoundException(MagazineErrorCode.MAGAZINE_NOT_FOUND));
        mockMvc.perform(put("/api/v1/magazines/{magazineId}/comments/{commentId}/reactions/{type}",
                        MAGAZINE_ID, COMMENT_ID, "AGREE")
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE, new AuthenticatedSession(USER_ID, UserRole.USER)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MAGAZINE_NOT_FOUND"))
                .andDo(errorDocument("magazine-comment-reaction-add-not-found"));
    }

    @Test
    void 로그인하지_않으면_댓글_공감을_삭제할_수_없다() throws Exception {
        mockMvc.perform(delete("/api/v1/magazines/{magazineId}/comments/{commentId}/reactions/{type}",
                        MAGAZINE_ID, COMMENT_ID, "AGREE"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andDo(errorDocument("magazine-comment-reaction-remove-unauthorized"));
        verifyNoInteractions(magazineCommentReactionService);
    }

    @Test
    void 잘못된_타입으로_댓글_공감을_삭제하면_400을_반환한다() throws Exception {
        given(magazineCommentReactionService.remove(MAGAZINE_ID, COMMENT_ID, USER_ID, "LIKE"))
                .willThrow(new InvalidInputException(CommentErrorCode.REACTION_TYPE_INVALID));
        mockMvc.perform(delete("/api/v1/magazines/{magazineId}/comments/{commentId}/reactions/{type}",
                        MAGAZINE_ID, COMMENT_ID, "LIKE")
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE, new AuthenticatedSession(USER_ID, UserRole.USER)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REACTION_TYPE_INVALID"))
                .andDo(errorDocument("magazine-comment-reaction-remove-invalid-type"));
    }

    private ResultHandler errorDocument(String name) {
        return document(name, resource(ResourceSnippetParameters.builder().tag("Magazine Comment Reaction")
                .pathParameters(
                        parameterWithName("magazineId").type(INTEGER).description("매거진 ID"),
                        parameterWithName("commentId").type(INTEGER).description("댓글 ID"),
                        parameterWithName("type").description("반응 타입(현재 AGREE)"))
                .responseSchema(Schema.schema("ErrorResponse"))
                .responseFields(RestDocsFields.errorResponse()).build()));
    }

    private org.springframework.restdocs.payload.FieldDescriptor[] successResponseFields() {
        return new org.springframework.restdocs.payload.FieldDescriptor[]{
                fieldWithPath("status").type(STRING).description("응답 상태"),
                fieldWithPath("data").type(OBJECT).description("매거진 댓글 반응 변경 결과"),
                fieldWithPath("data.magazineId").type(NUMBER).description("매거진 ID"),
                fieldWithPath("data.commentId").type(NUMBER).description("댓글 ID"),
                fieldWithPath("data.type").type(STRING).description("반응 타입(현재 AGREE)"),
                fieldWithPath("data.active").type(BOOLEAN).description("요청한 반응의 활성 상태"),
                fieldWithPath("data.agreeCount").type(NUMBER).description("댓글 공감 수")
        };
    }
}
