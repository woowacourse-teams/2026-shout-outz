package com.shoutoutz.api.comment.presentation;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.shoutoutz.api.auth.presentation.session.AuthenticatedSession;
import com.shoutoutz.api.comment.application.ProjectCommentService;
import com.shoutoutz.api.comment.domain.CommentErrorCode;
import com.shoutoutz.api.comment.presentation.dto.request.ProjectCommentCreateRequest;
import com.shoutoutz.api.comment.presentation.dto.request.ProjectCommentFindRequest;
import com.shoutoutz.api.comment.presentation.dto.request.ProjectCommentUpdateRequest;
import com.shoutoutz.api.comment.presentation.dto.response.ProjectCommentCreateResponse;
import com.shoutoutz.api.comment.presentation.dto.response.ProjectCommentDeleteResponse;
import com.shoutoutz.api.comment.presentation.dto.response.ProjectCommentFindResponse;
import com.shoutoutz.api.comment.presentation.dto.response.ProjectCommentFindResponse.Comment;
import com.shoutoutz.api.comment.presentation.dto.response.ProjectCommentUpdateResponse;
import com.shoutoutz.api.common.exception.code.CommonErrorCode;
import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.common.restdocs.RestDocsFields;
import com.shoutoutz.api.project.domain.ProjectErrorCode;
import com.shoutoutz.api.user.domain.account.UserRole;
import java.time.Instant;
import java.util.List;
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
    @DisplayName("비로그인 사용자가 댓글 목록을 조회하면 댓글 목록과 페이지 정보를 반환한다.")
    void findsCommentsForAnonymousUser() throws Exception {
        given(projectCommentService.findAll(
                eq(100L),
                any(ProjectCommentFindRequest.class),
                org.mockito.ArgumentMatchers.isNull()
        )).willReturn(new ProjectCommentFindResponse(
                List.of(
                        new Comment(
                                501L,
                                "좋은 프로젝트네요.",
                                new ProjectCommentFindResponse.Author(
                                        7L, "샤라웃 운영팀", "https://cdn.example.com/media/10/display"),
                                null,
                                Instant.parse("2026-09-14T00:00:00Z"),
                                Instant.parse("2026-09-14T00:00:00Z"),
                                false,
                                false,
                                false
                        ),
                        new Comment(
                                502L,
                                "저도 그렇게 생각합니다.",
                                new ProjectCommentFindResponse.Author(
                                        8L, "재키", "https://cdn.example.com/media/11/display"),
                                501L,
                                Instant.parse("2026-09-14T00:05:00Z"),
                                Instant.parse("2026-09-14T00:05:00Z"),
                                false,
                                false,
                                false
                        )
                ),
                new ProjectCommentFindResponse.Meta("next-cursor", true)
        ));

        mockMvc.perform(get("/api/v1/projects/{projectId}/comments", 100L)
                        .queryParam("size", "5")
                        .queryParam("sort", "LATEST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(501))
                .andExpect(jsonPath("$.data[0].author.userId").value(7))
                .andExpect(jsonPath("$.data[0].editable").value(false))
                .andExpect(jsonPath("$.data[0].edited").value(false))
                .andExpect(jsonPath("$.data[0].deleted").value(false))
                .andExpect(jsonPath("$.data[1].parentId").value(501))
                .andExpect(jsonPath("$.meta.nextCursor").value("next-cursor"))
                .andExpect(jsonPath("$.meta.hasNext").value(true))
                .andDo(document(
                        "project-comment-find-all",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Project Comment")
                                .summary("프로젝트 댓글 목록 조회")
                                .description("비로그인 또는 로그인 사용자가 공개 프로젝트의 댓글 목록을 조회한다. "
                                        + "size는 루트 댓글 개수이며 각 루트 댓글 뒤에 대댓글을 반환한다.")
                                .pathParameters(
                                        parameterWithName("projectId").description("댓글을 조회할 프로젝트 ID")
                                )
                                .queryParameters(
                                        parameterWithName("cursor")
                                                .description("다음 페이지 조회에 사용하는 opaque cursor")
                                                .optional(),
                                        parameterWithName("size")
                                                .description("조회할 루트 댓글 개수. 기본값 5, 1~50")
                                                .optional(),
                                        parameterWithName("sort")
                                                .description("루트 댓글 정렬 기준(LATEST 또는 OLDEST). 기본값 LATEST")
                                                .optional()
                                )
                                .responseSchema(Schema.schema("ProjectCommentFindAllSuccessResponse"))
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data").type(ARRAY).description("프로젝트 댓글 목록"),
                                        fieldWithPath("data[].id").type(NUMBER).description("댓글 ID"),
                                        fieldWithPath("data[].content").type(STRING)
                                                .description("댓글 내용. 삭제된 댓글은 null")
                                                .optional(),
                                        fieldWithPath("data[].author").type(OBJECT).description("댓글 작성자"),
                                        fieldWithPath("data[].author.userId").type(NUMBER).description("작성자 ID"),
                                        fieldWithPath("data[].author.displayName").type(STRING).description("작성자 표시 이름"),
                                        fieldWithPath("data[].author.avatarUrl").type(STRING)
                                                .description("작성자 프로필 이미지 공개 URL")
                                                .optional(),
                                        fieldWithPath("data[].parentId").type(NUMBER)
                                                .description("부모 루트 댓글 ID")
                                                .optional(),
                                        fieldWithPath("data[].createdAt").type(STRING)
                                                .description("생성 시각 (UTC ISO-8601)"),
                                        fieldWithPath("data[].updatedAt").type(STRING)
                                                .description("수정 시각 (UTC ISO-8601)"),
                                        fieldWithPath("data[].editable").type(BOOLEAN)
                                                .description("현재 사용자가 수정할 수 있는지 여부"),
                                        fieldWithPath("data[].edited").type(BOOLEAN)
                                                .description("댓글 내용이 수정된 적이 있는지 여부"),
                                        fieldWithPath("data[].deleted").type(BOOLEAN)
                                                .description("댓글이 삭제되었는지 여부"),
                                        fieldWithPath("meta.nextCursor").type(STRING)
                                                .description("다음 페이지 cursor")
                                                .optional(),
                                        fieldWithPath("meta.hasNext").type(BOOLEAN)
                                                .description("다음 페이지 존재 여부")
                                )
                                .build())
                ));

        verify(projectCommentService).findAll(
                eq(100L),
                any(ProjectCommentFindRequest.class),
                org.mockito.ArgumentMatchers.isNull()
        );
    }

    @Test
    @DisplayName("로그인 세션이 있으면 댓글 작성자 본인의 댓글만 editable로 반환한다.")
    void passesOptionalLoginUserToService() throws Exception {
        given(projectCommentService.findAll(
                eq(100L),
                any(ProjectCommentFindRequest.class),
                eq(7L)
        )).willReturn(new ProjectCommentFindResponse(
                List.of(new Comment(
                        501L,
                        "내 댓글",
                        new ProjectCommentFindResponse.Author(
                                7L, "샤라웃 운영팀", "https://cdn.example.com/media/10/display"),
                        null,
                        Instant.parse("2026-09-14T00:00:00Z"),
                        Instant.parse("2026-09-14T00:00:00Z"),
                        true,
                        false,
                        false
                )),
                new ProjectCommentFindResponse.Meta(null, false)
        ));

        mockMvc.perform(get("/api/v1/projects/{projectId}/comments", 100L)
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE,
                                new AuthenticatedSession(7L, UserRole.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].editable").value(true));

        verify(projectCommentService).findAll(
                eq(100L),
                any(ProjectCommentFindRequest.class),
                eq(7L)
        );
    }

    @Test
    @DisplayName("삭제된 댓글은 원문 없이 삭제 상태로 반환한다.")
    void returnsDeletedCommentWithoutContent() throws Exception {
        given(projectCommentService.findAll(
                eq(100L),
                any(ProjectCommentFindRequest.class),
                org.mockito.ArgumentMatchers.isNull()
        )).willReturn(new ProjectCommentFindResponse(
                List.of(new Comment(
                        503L,
                        null,
                        new ProjectCommentFindResponse.Author(
                                7L, "샤라웃 운영팀", "https://cdn.example.com/media/10/display"),
                        null,
                        Instant.parse("2026-09-14T00:00:00Z"),
                        Instant.parse("2026-09-14T00:00:00Z"),
                        false,
                        false,
                        true
                )),
                new ProjectCommentFindResponse.Meta(null, false)
        ));

        mockMvc.perform(get("/api/v1/projects/{projectId}/comments", 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].content").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.data[0].deleted").value(true));
    }

    @Test
    @DisplayName("댓글 목록 조회 크기가 범위를 벗어나면 서비스를 호출하지 않고 400을 반환한다.")
    void rejectsInvalidFindSize() throws Exception {
                mockMvc.perform(get("/api/v1/projects/{projectId}/comments", 100L)
                        .queryParam("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CommentErrorCode.INVALID_COMMENT_SIZE.name()));

        verifyNoInteractions(projectCommentService);
    }

    @Test
    @DisplayName("공개 프로젝트에 댓글을 작성하면 201과 작성자 정보를 포함한 댓글을 반환한다.")
    void createsComment() throws Exception {
        given(projectCommentService.create(eq(100L), eq(7L), any(ProjectCommentCreateRequest.class)))
                .willReturn(new ProjectCommentCreateResponse(
                        501L,
                        "좋은 프로젝트네요.",
                        new ProjectCommentCreateResponse.Author(
                                7L, "샤라웃 운영팀", "https://cdn.example.com/media/10/display"),
                        null,
                        Instant.parse("2026-09-14T00:00:00Z"),
                        Instant.parse("2026-09-14T00:00:00Z"),
                        true
                ));

        mockMvc.perform(post("/api/v1/projects/{projectId}/comments", 100L)
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
                .andExpect(jsonPath("$.data.author.avatarUrl")
                        .value("https://cdn.example.com/media/10/display"))
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
                                .pathParameters(
                                        parameterWithName("projectId").description("댓글을 작성할 프로젝트 ID")
                                )
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
                                        fieldWithPath("data.author.avatarUrl").type(STRING)
                                                .description("작성자 프로필 이미지 공개 URL")
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
    @DisplayName("댓글 작성자가 댓글을 수정하면 200과 수정된 댓글을 반환한다.")
    void updatesComment() throws Exception {
        given(projectCommentService.update(
                eq(100L),
                eq(501L),
                eq(7L),
                any(ProjectCommentUpdateRequest.class)
        )).willReturn(new ProjectCommentUpdateResponse(
                501L,
                "수정된 댓글입니다.",
                new ProjectCommentUpdateResponse.Author(
                        7L, "샤라웃 운영팀", "https://cdn.example.com/media/10/display"),
                null,
                Instant.parse("2026-09-14T00:00:00Z"),
                Instant.parse("2026-09-14T00:30:00Z"),
                true,
                true
        ));

        mockMvc.perform(patch("/api/v1/projects/{projectId}/comments/{commentId}", 100L, 501L)
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE, new AuthenticatedSession(7L, UserRole.USER))
                        .header("X-CSRF-Token", "csrf-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "  수정된 댓글입니다.  "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(501))
                .andExpect(jsonPath("$.data.content").value("수정된 댓글입니다."))
                .andExpect(jsonPath("$.data.author.userId").value(7))
                .andExpect(jsonPath("$.data.author.displayName").value("샤라웃 운영팀"))
                .andExpect(jsonPath("$.data.author.avatarUrl")
                        .value("https://cdn.example.com/media/10/display"))
                .andExpect(jsonPath("$.data.parentId").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.data.createdAt").value("2026-09-14T00:00:00Z"))
                .andExpect(jsonPath("$.data.updatedAt").value("2026-09-14T00:30:00Z"))
                .andExpect(jsonPath("$.data.editable").value(true))
                .andExpect(jsonPath("$.data.edited").value(true))
                .andDo(document(
                        "project-comment-update",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Project Comment")
                                .summary("프로젝트 댓글 수정")
                                .description("로그인한 프로젝트 댓글 작성자가 공개 프로젝트의 댓글 내용을 수정한다. "
                                        + "트림 후 기존 내용과 같으면 저장하지 않고 수정 시각을 유지한다.")
                                .pathParameters(
                                        parameterWithName("projectId").description("댓글이 속한 프로젝트 ID"),
                                        parameterWithName("commentId").description("수정할 댓글 ID")
                                )
                                .requestHeaders(
                                        headerWithName("X-CSRF-Token").description("세션 조회로 발급받은 CSRF 토큰")
                                )
                                .requestSchema(Schema.schema("ProjectCommentUpdateRequest"))
                                .responseSchema(Schema.schema("ProjectCommentUpdateSuccessResponse"))
                                .requestFields(
                                        fieldWithPath("content")
                                                .type(STRING)
                                                .description("앞뒤 공백을 제거한 뒤 저장하는 댓글 내용 (1~500자, Unicode code point 기준)")
                                )
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data.id").type(NUMBER).description("댓글 ID"),
                                        fieldWithPath("data.content").type(STRING).description("저장된 댓글 내용"),
                                        fieldWithPath("data.author").type(OBJECT).description("댓글 작성자"),
                                        fieldWithPath("data.author.userId").type(NUMBER).description("작성자 ID"),
                                        fieldWithPath("data.author.displayName").type(STRING).description("작성자 표시 이름"),
                                        fieldWithPath("data.author.avatarUrl").type(STRING)
                                                .description("작성자 프로필 이미지 공개 URL")
                                                .optional(),
                                        fieldWithPath("data.parentId").type(NUMBER).description("부모 댓글 ID")
                                                .optional(),
                                        fieldWithPath("data.createdAt").type(STRING).description("생성 시각 (UTC ISO-8601)"),
                                        fieldWithPath("data.updatedAt").type(STRING)
                                                .description("수정 시각 (UTC ISO-8601, 변경 없으면 기존 값 유지)"),
                                        fieldWithPath("data.editable").type(BOOLEAN).description("현재 사용자가 수정할 수 있는지 여부"),
                                        fieldWithPath("data.edited").type(BOOLEAN).description("댓글 내용이 수정된 적이 있는지 여부")
                                )
                                .build())
                ));

        verify(projectCommentService).update(
                eq(100L),
                eq(501L),
                eq(7L),
                any(ProjectCommentUpdateRequest.class)
        );
    }

    @Test
    @DisplayName("댓글 작성자가 댓글을 삭제하면 200과 삭제 상태를 반환한다.")
    void deletesComment() throws Exception {
        given(projectCommentService.delete(100L, 501L, 7L))
                .willReturn(new ProjectCommentDeleteResponse(501L, true));

        mockMvc.perform(delete("/api/v1/projects/{projectId}/comments/{commentId}", 100L, 501L)
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE,
                                new AuthenticatedSession(7L, UserRole.USER))
                        .header("X-CSRF-Token", "csrf-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(501))
                .andExpect(jsonPath("$.data.deleted").value(true))
                .andDo(document(
                        "project-comment-delete",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Project Comment")
                                .summary("프로젝트 댓글 삭제")
                                .description("댓글 작성자 본인이 공개 프로젝트의 댓글을 soft delete한다.")
                                .pathParameters(
                                        parameterWithName("projectId").description("댓글이 속한 프로젝트 ID"),
                                        parameterWithName("commentId").description("삭제할 댓글 ID")
                                )
                                .requestHeaders(
                                        headerWithName("X-CSRF-Token").description("세션 조회로 발급받은 CSRF 토큰")
                                )
                                .responseSchema(Schema.schema("ProjectCommentDeleteSuccessResponse"))
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data.id").type(NUMBER).description("삭제된 댓글 ID"),
                                        fieldWithPath("data.deleted").type(BOOLEAN).description("댓글 삭제 여부")
                                )
                                .build())
                ));

        verify(projectCommentService).delete(100L, 501L, 7L);
    }

    @Test
    @DisplayName("댓글 내용이 500자를 초과하면 400과 필드 오류를 반환하고 서비스를 호출하지 않는다.")
    void rejectsContentOver500CodePoints() throws Exception {
        mockMvc.perform(post("/api/v1/projects/{projectId}/comments", 100L)
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
                                .pathParameters(
                                        parameterWithName("projectId").description("댓글을 작성할 프로젝트 ID")
                                )
                                .requestSchema(Schema.schema("ProjectCommentCreateRequest"))
                                .responseSchema(Schema.schema("ErrorResponse"))
                                .responseFields(RestDocsFields.errorResponse())
                                .build())
                ));

        verifyNoInteractions(projectCommentService);
    }

    @Test
    @DisplayName("댓글 수정 내용이 500자를 초과하면 400과 필드 오류를 반환하고 서비스를 호출하지 않는다.")
    void rejectsUpdateContentOver500CodePoints() throws Exception {
        mockMvc.perform(patch("/api/v1/projects/{projectId}/comments/{commentId}", 100L, 501L)
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE, new AuthenticatedSession(7L, UserRole.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"" + "😀".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details[0].field").value("content"));

        verifyNoInteractions(projectCommentService);
    }

    @Test
    @DisplayName("로그인하지 않고 댓글을 작성하면 401을 반환한다.")
    void rejectsUnauthenticatedUser() throws Exception {
        mockMvc.perform(post("/api/v1/projects/{projectId}/comments", 100L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"댓글\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        verifyNoInteractions(projectCommentService);
    }

    @Test
    @DisplayName("로그인하지 않고 댓글을 수정하면 401을 반환한다.")
    void rejectsUnauthenticatedUpdate() throws Exception {
        mockMvc.perform(patch("/api/v1/projects/{projectId}/comments/{commentId}", 100L, 501L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"수정된 댓글\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        verifyNoInteractions(projectCommentService);
    }

    @Test
    @DisplayName("로그인하지 않고 댓글을 삭제하면 401을 반환한다.")
    void rejectsUnauthenticatedDelete() throws Exception {
        mockMvc.perform(delete("/api/v1/projects/{projectId}/comments/{commentId}", 100L, 501L))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        verifyNoInteractions(projectCommentService);
    }

    @Test
    @DisplayName("공개 프로젝트가 아니면 프로젝트를 찾을 수 없다는 404를 반환한다.")
    void rejectsNonPublicProject() throws Exception {
        given(projectCommentService.create(eq(100L), eq(7L), any(ProjectCommentCreateRequest.class)))
                .willThrow(new EntityNotFoundException(ProjectErrorCode.PROJECT_NOT_FOUND));

        mockMvc.perform(post("/api/v1/projects/{projectId}/comments", 100L)
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE, new AuthenticatedSession(7L, UserRole.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"댓글\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
    }

    @Test
    @DisplayName("공개 프로젝트가 아니면 댓글 수정 요청에 프로젝트를 찾을 수 없다는 404를 반환한다.")
    void rejectsUpdateForNonPublicProject() throws Exception {
        given(projectCommentService.update(
                eq(100L),
                eq(501L),
                eq(7L),
                any(ProjectCommentUpdateRequest.class)
        )).willThrow(new EntityNotFoundException(ProjectErrorCode.PROJECT_NOT_FOUND));

        mockMvc.perform(patch("/api/v1/projects/{projectId}/comments/{commentId}", 100L, 501L)
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE, new AuthenticatedSession(7L, UserRole.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"수정된 댓글\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
    }

    @Test
    @DisplayName("공개 프로젝트가 아니면 댓글 삭제 요청에 프로젝트를 찾을 수 없다는 404를 반환한다.")
    void rejectsDeleteForNonPublicProject() throws Exception {
        given(projectCommentService.delete(100L, 501L, 7L))
                .willThrow(new EntityNotFoundException(ProjectErrorCode.PROJECT_NOT_FOUND));

        mockMvc.perform(delete("/api/v1/projects/{projectId}/comments/{commentId}", 100L, 501L)
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE,
                                new AuthenticatedSession(7L, UserRole.USER))
                        .header("X-CSRF-Token", "csrf-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
    }

    @Test
    @DisplayName("댓글 작성자가 아니면 댓글 수정 요청에 403을 반환한다.")
    void rejectsUpdateFromAnotherAuthor() throws Exception {
        given(projectCommentService.update(
                eq(100L),
                eq(501L),
                eq(7L),
                any(ProjectCommentUpdateRequest.class)
        )).willThrow(new ForbiddenException(CommonErrorCode.FORBIDDEN));

        mockMvc.perform(patch("/api/v1/projects/{projectId}/comments/{commentId}", 100L, 501L)
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE, new AuthenticatedSession(7L, UserRole.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"수정된 댓글\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("댓글 작성자가 아니면 댓글 삭제 요청에 403을 반환한다.")
    void rejectsDeleteFromAnotherAuthor() throws Exception {
        given(projectCommentService.delete(100L, 501L, 7L))
                .willThrow(new ForbiddenException(CommonErrorCode.FORBIDDEN));

        mockMvc.perform(delete("/api/v1/projects/{projectId}/comments/{commentId}", 100L, 501L)
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE,
                                new AuthenticatedSession(7L, UserRole.USER))
                        .header("X-CSRF-Token", "csrf-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("삭제되었거나 다른 프로젝트의 댓글이면 댓글 수정 요청에 404를 반환한다.")
    void rejectsUnavailableComment() throws Exception {
        given(projectCommentService.update(
                eq(100L),
                eq(501L),
                eq(7L),
                any(ProjectCommentUpdateRequest.class)
        )).willThrow(new EntityNotFoundException(CommentErrorCode.COMMENT_NOT_FOUND));

        mockMvc.perform(patch("/api/v1/projects/{projectId}/comments/{commentId}", 100L, 501L)
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE, new AuthenticatedSession(7L, UserRole.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"수정된 댓글\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"));
    }

    @Test
    @DisplayName("삭제되었거나 다른 프로젝트의 댓글이면 댓글 삭제 요청에 404를 반환한다.")
    void rejectsUnavailableCommentForDelete() throws Exception {
        given(projectCommentService.delete(100L, 501L, 7L))
                .willThrow(new EntityNotFoundException(CommentErrorCode.COMMENT_NOT_FOUND));

        mockMvc.perform(delete("/api/v1/projects/{projectId}/comments/{commentId}", 100L, 501L)
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE,
                                new AuthenticatedSession(7L, UserRole.USER))
                        .header("X-CSRF-Token", "csrf-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"));
    }

    @Test
    @DisplayName("대댓글 깊이를 초과하면 400을 반환한다.")
    void rejectsExceededCommentDepth() throws Exception {
        given(projectCommentService.create(eq(100L), eq(7L), any(ProjectCommentCreateRequest.class)))
                .willThrow(new BadRequestException(CommentErrorCode.COMMENT_DEPTH_EXCEEDED));

        mockMvc.perform(post("/api/v1/projects/{projectId}/comments", 100L)
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE, new AuthenticatedSession(7L, UserRole.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"댓글\",\"parentId\":301}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMENT_DEPTH_EXCEEDED"));
    }
}
