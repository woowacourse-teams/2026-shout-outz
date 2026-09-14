package com.shoutoutz.api.comment.presentation;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.payload.JsonFieldType.BOOLEAN;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.OBJECT;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.shoutoutz.api.auth.presentation.session.AuthenticatedSession;
import com.shoutoutz.api.comment.application.ProjectCommentService;
import com.shoutoutz.api.comment.domain.CommentErrorCode;
import com.shoutoutz.api.comment.presentation.dto.request.ProjectCommentCreateRequest;
import com.shoutoutz.api.comment.presentation.dto.response.ProjectCommentCreateResponse;
import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.restdocs.RestDocsFields;
import com.shoutoutz.api.project.domain.ProjectErrorCode;
import com.shoutoutz.api.user.domain.account.UserRole;
import java.time.Instant;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ProjectCommentHttpApi.class)
@AutoConfigureRestDocs
class ProjectCommentHttpApiTest {

    private static final String AUTHENTICATED_SESSION_ATTRIBUTE = AuthenticatedSession.class.getName();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectCommentService projectCommentService;

    @Test
    @DisplayName("공개 프로젝트에 댓글을 작성하면 201과 작성자 정보를 포함한 댓글을 반환한다.")
    void createsComment() throws Exception {
        given(projectCommentService.create(eq(100L), eq(7L), any(ProjectCommentCreateRequest.class)))
                .willReturn(new ProjectCommentCreateResponse(
                        501L,
                        "좋은 프로젝트네요.",
                        new ProjectCommentCreateResponse.Author(7L, "샤라웃 운영팀", 10L),
                        null,
                        Instant.parse("2026-09-14T00:00:00Z"),
                        Instant.parse("2026-09-14T00:00:00Z"),
                        true
                ));

        mockMvc.perform(post("/api/v1/projects/100/comments")
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE, new AuthenticatedSession(7L, UserRole.USER))
                        .header("X-CSRF-Token", "csrf-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "  좋은 프로젝트네요.  "
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(501))
                .andExpect(jsonPath("$.data.content").value("좋은 프로젝트네요."))
                .andExpect(jsonPath("$.data.author.userId").value(7))
                .andExpect(jsonPath("$.data.author.displayName").value("샤라웃 운영팀"))
                .andExpect(jsonPath("$.data.author.avatarImageId").value(10))
                .andExpect(jsonPath("$.data.parentId").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.data.createdAt").value("2026-09-14T00:00:00Z"))
                .andExpect(jsonPath("$.data.updatedAt").value("2026-09-14T00:00:00Z"))
                .andExpect(jsonPath("$.data.editable").value(true))
                .andDo(document(
                        "project-comment-create",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Project Comment")
                                .summary("프로젝트 댓글 작성")
                                .description("로그인 사용자가 공개 프로젝트에 댓글 또는 1단계 대댓글을 작성한다.")
                                .requestHeaders(
                                        headerWithName("X-CSRF-Token").description("세션 조회로 발급받은 CSRF 토큰")
                                )
                                .requestSchema(Schema.schema("ProjectCommentCreateRequest"))
                                .responseSchema(Schema.schema("ProjectCommentCreateSuccessResponse"))
                                .requestFields(
                                        fieldWithPath("content")
                                                .type(STRING)
                                                .description("앞뒤 공백을 제거한 뒤 저장하는 댓글 내용 (1~500자, Unicode code point 기준)"),
                                        fieldWithPath("parentId")
                                                .type(NUMBER)
                                                .description("같은 프로젝트의 삭제되지 않은 루트 댓글 ID. 없으면 루트 댓글")
                                                .optional()
                                )
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data.id").type(NUMBER).description("댓글 ID"),
                                        fieldWithPath("data.content").type(STRING).description("저장된 댓글 내용"),
                                        fieldWithPath("data.author").type(OBJECT).description("댓글 작성자"),
                                        fieldWithPath("data.author.userId").type(NUMBER).description("작성자 ID"),
                                        fieldWithPath("data.author.displayName").type(STRING).description("작성자 표시 이름"),
                                        fieldWithPath("data.author.avatarImageId").type(NUMBER)
                                                .description("작성자 프로필 이미지 ID")
                                                .optional(),
                                        fieldWithPath("data.parentId").type(NUMBER).description("부모 댓글 ID")
                                                .optional(),
                                        fieldWithPath("data.createdAt").type(STRING).description("생성 시각 (UTC ISO-8601)"),
                                        fieldWithPath("data.updatedAt").type(STRING).description("수정 시각 (UTC ISO-8601)"),
                                        fieldWithPath("data.editable").type(BOOLEAN).description("현재 사용자가 수정할 수 있는지 여부")
                                )
                                .build())
                ));

        verify(projectCommentService).create(eq(100L), eq(7L), any(ProjectCommentCreateRequest.class));
    }

    @Test
    @DisplayName("댓글 내용이 500자를 초과하면 400과 필드 오류를 반환하고 서비스를 호출하지 않는다.")
    void rejectsContentOver500CodePoints() throws Exception {
        mockMvc.perform(post("/api/v1/projects/100/comments")
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE, new AuthenticatedSession(7L, UserRole.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"" + "😀".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details[0].field").value("content"))
                .andDo(document(
                        "project-comment-create-invalid",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Project Comment")
                                .summary("프로젝트 댓글 작성")
                                .description("프로젝트 댓글 작성 요청을 검증한다.")
                                .responseSchema(Schema.schema("ErrorResponse"))
                                .responseFields(RestDocsFields.errorResponse())
                                .build())
                ));

        verifyNoInteractions(projectCommentService);
    }

    @Test
    @DisplayName("로그인하지 않고 댓글을 작성하면 401을 반환한다.")
    void rejectsUnauthenticatedUser() throws Exception {
        mockMvc.perform(post("/api/v1/projects/100/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"댓글\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        verifyNoInteractions(projectCommentService);
    }

    @Test
    @DisplayName("공개 프로젝트가 아니면 프로젝트를 찾을 수 없다는 404를 반환한다.")
    void rejectsNonPublicProject() throws Exception {
        given(projectCommentService.create(eq(100L), eq(7L), any(ProjectCommentCreateRequest.class)))
                .willThrow(new EntityNotFoundException(ProjectErrorCode.PROJECT_NOT_FOUND));

        mockMvc.perform(post("/api/v1/projects/100/comments")
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE, new AuthenticatedSession(7L, UserRole.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"댓글\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
    }

    @Test
    @DisplayName("대댓글 깊이를 초과하면 400을 반환한다.")
    void rejectsExceededCommentDepth() throws Exception {
        given(projectCommentService.create(eq(100L), eq(7L), any(ProjectCommentCreateRequest.class)))
                .willThrow(new BadRequestException(CommentErrorCode.COMMENT_DEPTH_EXCEEDED));

        mockMvc.perform(post("/api/v1/projects/100/comments")
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE, new AuthenticatedSession(7L, UserRole.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"댓글\",\"parentId\":301}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMENT_DEPTH_EXCEEDED"));
    }
}
