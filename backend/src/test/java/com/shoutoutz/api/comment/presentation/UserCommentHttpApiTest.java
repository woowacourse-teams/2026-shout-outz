package com.shoutoutz.api.comment.presentation;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.SimpleType.INTEGER;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
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

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.shoutoutz.api.auth.presentation.session.AuthenticatedSession;
import com.shoutoutz.api.comment.application.UserCommentService;
import com.shoutoutz.api.comment.application.dto.UserCommentItem;
import com.shoutoutz.api.comment.application.dto.UserCommentResult;
import com.shoutoutz.api.comment.application.dto.UserCommentType;
import com.shoutoutz.api.comment.domain.CommentErrorCode;
import com.shoutoutz.api.comment.presentation.dto.request.UserCommentFindRequest;
import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.restdocs.RestDocsFields;
import com.shoutoutz.api.user.domain.account.UserRole;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@DisplayName("내 댓글 목록 API")
@WebMvcTest(controllers = UserCommentHttpApi.class)
@AutoConfigureRestDocs
class UserCommentHttpApiTest {

    private static final String SUMMARY = "내 댓글 목록 조회";
    private static final String DESCRIPTION = "로그인한 사용자가 작성한 피드와 프로젝트 댓글을 최신순으로 조회한다. "
            + "삭제되지 않은 공개 대상의 댓글만 반환하며, type과 targetId로 이동할 대상을 구분한다. "
            + "응답의 meta.nextCursor를 다음 요청에 그대로 전달한다.";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserCommentService userCommentService;

    @Test
    @DisplayName("피드와 프로젝트에 작성한 댓글을 함께 조회한다")
    void findsMyComments() throws Exception {
        UserCommentFindRequest request = new UserCommentFindRequest("current-cursor", 20);
        given(userCommentService.findAll(1L, request))
                .willReturn(new UserCommentResult(
                        List.of(
                                comment(11L, UserCommentType.FEED, 101L),
                                comment(12L, UserCommentType.PROJECT, 202L)
                        ),
                        "next-cursor",
                        true
                ));

        mockMvc.perform(get("/api/v1/users/me/comments")
                        .header(HttpHeaders.COOKIE, "JSESSIONID=session-id")
                        .requestAttr(
                                AuthenticatedSession.class.getName(),
                                new AuthenticatedSession(1L, UserRole.USER)
                        )
                        .queryParam("cursor", "current-cursor")
                        .queryParam("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].commentId").value(11))
                .andExpect(jsonPath("$.data[0].type").value("FEED"))
                .andExpect(jsonPath("$.data[0].targetId").value(101))
                .andExpect(jsonPath("$.data[0].content").value("내가 작성한 댓글"))
                .andExpect(jsonPath("$.data[0].createdAt").value("2026-09-16T00:00:00Z"))
                .andExpect(jsonPath("$.data[0].updatedAt").value("2026-09-16T00:05:00Z"))
                .andExpect(jsonPath("$.data[1].type").value("PROJECT"))
                .andExpect(jsonPath("$.meta.nextCursor").value("next-cursor"))
                .andExpect(jsonPath("$.meta.hasNext").value(true))
                .andDo(document(
                        "user-comment-find-all",
                        resource(ResourceSnippetParameters.builder()
                                .tag("User")
                                .summary(SUMMARY)
                                .description(DESCRIPTION)
                                .requestHeaders(
                                        headerWithName(HttpHeaders.COOKIE)
                                                .description("인증된 사용자의 JSESSIONID")
                                )
                                .queryParameters(
                                        parameterWithName("cursor")
                                                .description("다음 페이지 조회용 커서. 첫 요청은 생략")
                                                .optional(),
                                        parameterWithName("size")
                                                .type(INTEGER)
                                                .description("한 번에 가져올 댓글 수. 기본값 20, 1~50")
                                                .optional()
                                )
                                .responseSchema(Schema.schema("UserCommentFindAllSuccessResponse"))
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data").type(ARRAY).description("내가 작성한 댓글 목록"),
                                        fieldWithPath("data[].commentId").type(NUMBER).description("댓글 ID"),
                                        fieldWithPath("data[].type").type(STRING)
                                                .description("댓글 대상 종류(FEED 또는 PROJECT)"),
                                        fieldWithPath("data[].targetId").type(NUMBER)
                                                .description("이동할 피드 또는 프로젝트 ID"),
                                        fieldWithPath("data[].content").type(STRING).description("댓글 내용"),
                                        fieldWithPath("data[].createdAt").type(STRING).description("댓글 작성 시각"),
                                        fieldWithPath("data[].updatedAt").type(STRING).description("댓글 최종 수정 시각"),
                                        fieldWithPath("meta").type(OBJECT).description("페이지네이션 정보"),
                                        fieldWithPath("meta.nextCursor").type(STRING)
                                                .description("다음 페이지 조회용 커서").optional(),
                                        fieldWithPath("meta.hasNext").type(BOOLEAN)
                                                .description("다음 페이지 존재 여부")
                                )
                                .build())
                ));

        verify(userCommentService).findAll(1L, request);
    }

    @Test
    @DisplayName("파라미터를 생략하면 기본 조회 조건을 사용한다")
    void usesDefaultParameters() throws Exception {
        UserCommentFindRequest request = new UserCommentFindRequest(null, null);
        given(userCommentService.findAll(1L, request))
                .willReturn(new UserCommentResult(List.of(), null, false));

        mockMvc.perform(get("/api/v1/users/me/comments")
                        .requestAttr(
                                AuthenticatedSession.class.getName(),
                                new AuthenticatedSession(1L, UserRole.USER)
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.meta.nextCursor").doesNotExist())
                .andExpect(jsonPath("$.meta.hasNext").value(false));

        verify(userCommentService).findAll(1L, request);
    }

    @Test
    @DisplayName("인증 정보가 없으면 내 댓글을 조회할 수 없다")
    void rejectsUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/comments"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andDo(document(
                        "user-comment-find-all-unauthorized",
                        resource(ResourceSnippetParameters.builder()
                                .tag("User")
                                .summary(SUMMARY)
                                .description(DESCRIPTION)
                                .responseSchema(Schema.schema("ErrorResponse"))
                                .responseFields(RestDocsFields.errorResponse())
                                .build())
                ));

        verifyNoInteractions(userCommentService);
    }

    @Test
    @DisplayName("조회 개수가 범위를 벗어나면 조회할 수 없다")
    void rejectsInvalidSize() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/comments")
                        .requestAttr(
                                AuthenticatedSession.class.getName(),
                                new AuthenticatedSession(1L, UserRole.USER)
                        )
                        .queryParam("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(userCommentService);
    }

    @Test
    @DisplayName("커서가 올바르지 않으면 조회할 수 없다")
    void rejectsInvalidCursor() throws Exception {
        UserCommentFindRequest request = new UserCommentFindRequest("broken", null);
        given(userCommentService.findAll(1L, request))
                .willThrow(new BadRequestException(CommentErrorCode.INVALID_COMMENT_CURSOR));

        mockMvc.perform(get("/api/v1/users/me/comments")
                        .requestAttr(
                                AuthenticatedSession.class.getName(),
                                new AuthenticatedSession(1L, UserRole.USER)
                        )
                        .queryParam("cursor", "broken"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_COMMENT_CURSOR"));
    }

    private UserCommentItem comment(long commentId, UserCommentType type, long targetId) {
        return new UserCommentItem(
                commentId,
                type,
                targetId,
                "내가 작성한 댓글",
                Instant.parse("2026-09-16T00:00:00Z"),
                Instant.parse("2026-09-16T00:05:00Z")
        );
    }
}
