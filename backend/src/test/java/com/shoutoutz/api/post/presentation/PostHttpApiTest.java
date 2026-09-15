package com.shoutoutz.api.post.presentation;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.SimpleType.INTEGER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.BOOLEAN;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.OBJECT;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.applyPathPrefix;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.shoutoutz.api.auth.presentation.session.AuthenticatedSession;
import com.shoutoutz.api.category.domain.CategoryType;
import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.common.exception.custom.NotFoundException;
import com.shoutoutz.api.common.restdocs.RestDocsFields;
import com.shoutoutz.api.post.application.PostService;
import com.shoutoutz.api.post.application.dto.PostFindAllResult;
import com.shoutoutz.api.post.application.dto.PostItem;
import com.shoutoutz.api.post.domain.PostErrorCode;
import com.shoutoutz.api.post.presentation.dto.request.PostFindAllRequest;
import com.shoutoutz.api.post.presentation.dto.request.PostSaveRequest;
import com.shoutoutz.api.post.presentation.dto.request.PostUpdateRequest;
import com.shoutoutz.api.post.presentation.dto.response.PostResponse;
import com.shoutoutz.api.user.domain.account.UserRole;
import com.shoutoutz.api.user.domain.profile.UserType;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(controllers = PostHttpApi.class)
@AutoConfigureRestDocs
class PostHttpApiTest {

    private static final long USER_ID = 1L;
    private static final long POST_ID = 10L;
    private static final Instant CREATED_AT = Instant.parse("2026-09-11T00:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PostService postService;

    @Test
    @DisplayName("최신순 포스트 목록을 Slice 메타와 함께 조회한다")
    void findAllPost() throws Exception {
        PostItem post = postItem();
        given(postService.findAllPost(any(PostFindAllRequest.class)))
                .willReturn(new PostFindAllResult(List.of(post), "next-cursor", true));

        mockMvc.perform(get("/api/v1/posts")
                        .queryParam("categoryId", "3")
                        .queryParam("sort", "LATEST")
                        .queryParam("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].postId").value(POST_ID))
                .andExpect(jsonPath("$.meta.nextCursor").value("next-cursor"))
                .andExpect(jsonPath("$.meta.hasNext").value(true))
                .andDo(document(
                        "post-find-all",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Post")
                                .summary("포스트 목록 조회")
                                .description("전체 공개 포스트를 최신순 또는 전체 기간 인기순 Slice로 조회한다.")
                                .queryParameters(
                                        parameterWithName("sort").description("LATEST 또는 POPULAR, 기본 LATEST").optional(),
                                        parameterWithName("categoryId")
                                                .type(INTEGER)
                                                .description("카테고리 ID")
                                                .optional(),
                                        parameterWithName("cursor").description("다음 Slice 커서").optional(),
                                        parameterWithName("size")
                                                .type(INTEGER)
                                                .description("조회 크기(기본 20, 최대 100)")
                                                .optional()
                                )
                                .responseSchema(Schema.schema("PostFindAllSuccessResponse"))
                                .responseFields(postListResponseFields())
                                .build())
                ));
    }

    @Test
    @DisplayName("존재하지 않는 카테고리 ID로 조회하면 빈 목록을 반환한다")
    void findAllPostByMissingCategoryId() throws Exception {
        given(postService.findAllPost(any(PostFindAllRequest.class)))
                .willReturn(new PostFindAllResult(List.of(), null, false));

        mockMvc.perform(get("/api/v1/posts").queryParam("categoryId", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());

        verify(postService).findAllPost(any(PostFindAllRequest.class));
    }

    @Test
    @DisplayName("전체 공개 포스트 상세를 조회한다")
    void findPost() throws Exception {
        given(postService.findPost(POST_ID)).willReturn(PostResponse.from(postItem()));

        mockMvc.perform(get("/api/v1/posts/{postId}", POST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("본문입니다."))
                .andDo(document(
                        "post-find",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Post")
                                .summary("포스트 상세 조회")
                                .description("삭제되지 않은 전체 공개 포스트를 조회한다.")
                                .pathParameters(parameterWithName("postId")
                                        .type(INTEGER)
                                        .description("포스트 ID"))
                                .responseSchema(Schema.schema("PostFindSuccessResponse"))
                                .responseFields(successResponseFields("data."))
                                .build())
                ));
    }

    @Test
    @DisplayName("크루 또는 코치가 포스트를 작성한다")
    void savePost() throws Exception {
        given(postService.savePost(eq(USER_ID), any(PostSaveRequest.class)))
                .willReturn(PostResponse.from(postItem()));

        mockMvc.perform(post("/api/v1/posts")
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "본문입니다.",
                                  "categoryIds": [3],
                                  "mediaIds": [21]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.postId").value(POST_ID))
                .andDo(document(
                        "post-save",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Post")
                                .summary("포스트 작성")
                                .description("크루 또는 코치가 카테고리와 업로드 완료된 본문 이미지를 연결해 포스트를 작성한다.")
                                .requestSchema(Schema.schema("PostSaveRequest"))
                                .responseSchema(Schema.schema("PostSaveSuccessResponse"))
                                .requestFields(
                                        fieldWithPath("content").type(STRING)
                                                .description("Markdown 본문(공백 제외 1자 이상, Unicode 최대 500자)"),
                                        fieldWithPath("categoryIds").type(ARRAY)
                                                .description("활성 카테고리 ID 목록(일반 1개, 이벤트 개수 제한 없음, 중복 불가)"),
                                        fieldWithPath("mediaIds").type(ARRAY)
                                                .description("작성자가 업로드한 READY POST_CONTENT 미디어 ID 목록")
                                )
                                .responseFields(successResponseFields("data."))
                                .build())
                ));

        verify(postService).savePost(eq(USER_ID), any(PostSaveRequest.class));
    }

    @Test
    @DisplayName("작성자가 포스트 전체 내용을 수정한다")
    void updatePost() throws Exception {
        given(postService.updatePost(eq(POST_ID), eq(USER_ID), any(PostUpdateRequest.class)))
                .willReturn(PostResponse.from(postItem()));

        mockMvc.perform(put("/api/v1/posts/{postId}", POST_ID)
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.postId").value(POST_ID))
                .andDo(document(
                        "post-update",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Post")
                                .summary("포스트 수정")
                                .description("작성자가 본문, 카테고리, 본문 미디어를 전체 교체한다.")
                                .pathParameters(parameterWithName("postId")
                                        .type(INTEGER)
                                        .description("포스트 ID"))
                                .requestSchema(Schema.schema("PostUpdateRequest"))
                                .responseSchema(Schema.schema("PostUpdateSuccessResponse"))
                                .requestFields(
                                        fieldWithPath("content").type(STRING).description("변경할 Markdown 본문"),
                                        fieldWithPath("categoryIds").type(ARRAY)
                                                .description("변경할 카테고리 ID 목록(일반 1개, 이벤트 개수 제한 없음)"),
                                        fieldWithPath("mediaIds").type(ARRAY).description("변경할 본문 미디어 ID 목록")
                                )
                                .responseFields(successResponseFields("data."))
                                .build())
                ));
    }

    @Test
    @DisplayName("작성자가 포스트를 soft delete한다")
    void deletePost() throws Exception {
        mockMvc.perform(delete("/api/v1/posts/{postId}", POST_ID)
                        .with(authenticated()))
                .andExpect(status().isNoContent())
                .andDo(document(
                        "post-delete",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Post")
                                .summary("포스트 삭제")
                                .description("작성자가 포스트를 soft delete한다.")
                                .pathParameters(parameterWithName("postId")
                                        .type(INTEGER)
                                        .description("포스트 ID"))
                                .build())
                ));

        verify(postService).deletePost(POST_ID, USER_ID);
    }

    @Test
    @DisplayName("인증 없이 포스트 작성 요청을 하면 401을 반환한다")
    void rejectAnonymousCreate() throws Exception {
        mockMvc.perform(post("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequest()))
                .andExpect(status().isUnauthorized())
                .andDo(document(
                        "post-save-unauthorized",
                        resource(errorResponse(
                                "포스트 작성",
                                "크루 또는 코치가 카테고리와 업로드 완료된 본문 이미지를 연결해 포스트를 작성한다."
                        ))
                ));

        verifyNoInteractions(postService);
    }

    @Test
    @DisplayName("본문이 공백이거나 Unicode 500자를 초과하면 400을 반환한다")
    void rejectInvalidContent() throws Exception {
        String overLimit = "😀".repeat(501);

        mockMvc.perform(post("/api/v1/posts")
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"   ","categoryIds":[1],"mediaIds":[]}
                                """))
                .andExpect(status().isBadRequest())
                .andDo(document(
                        "post-save-invalid",
                        resource(errorResponse(
                                "포스트 작성",
                                "크루 또는 코치가 카테고리와 업로드 완료된 본문 이미지를 연결해 포스트를 작성한다."
                        ))
                ));
        mockMvc.perform(post("/api/v1/posts")
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"%s","categoryIds":[1],"mediaIds":[]}
                                """.formatted(overLimit)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(postService);
    }

    @Test
    @DisplayName("수정 본문이 공백이거나 Unicode 500자를 초과하면 400을 반환한다")
    void rejectInvalidUpdateContent() throws Exception {
        String overLimit = "😀".repeat(501);

        mockMvc.perform(put("/api/v1/posts/{postId}", POST_ID)
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"   \",\"categoryIds\":[1],\"mediaIds\":[]}"))
                .andExpect(status().isBadRequest())
                .andDo(document(
                        "post-update-invalid",
                        resource(pathErrorResponse(
                                "포스트 수정",
                                "작성자가 본문, 카테고리, 본문 미디어를 전체 교체한다."
                        ))
                ));
        mockMvc.perform(put("/api/v1/posts/{postId}", POST_ID)
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"%s\",\"categoryIds\":[1],\"mediaIds\":[]}".formatted(overLimit)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(postService);
    }

    @Test
    @DisplayName("카테고리나 미디어 ID 목록이 비어 있거나 중복 또는 null이면 400을 반환한다")
    void rejectInvalidAssociations() throws Exception {
        mockMvc.perform(post("/api/v1/posts")
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"본문","categoryIds":[1,1],"mediaIds":[]}
                                """))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/posts")
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"본문","categoryIds":[null],"mediaIds":[]}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(postService);
    }

    @Test
    @DisplayName("수정 필드가 누락되면 400을 반환한다")
    void rejectIncompleteUpdate() throws Exception {
        mockMvc.perform(put("/api/v1/posts/{postId}", POST_ID)
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(postService);
    }

    @Test
    @DisplayName("잘못된 목록 조건은 400을 반환한다")
    void rejectInvalidListRequest() throws Exception {
        mockMvc.perform(get("/api/v1/posts")
                        .queryParam("sort", "INVALID")
                        .queryParam("size", "101"))
                .andExpect(status().isBadRequest())
                .andDo(document(
                        "post-find-all-invalid",
                        resource(errorResponse(
                                "포스트 목록 조회",
                                "전체 공개 포스트를 최신순 또는 전체 기간 인기순 Slice로 조회한다."
                        ))
                ));

        verifyNoInteractions(postService);
    }

    @Test
    @DisplayName("해석할 수 없는 목록 커서는 400을 반환한다")
    void rejectInvalidCursor() throws Exception {
        given(postService.findAllPost(any(PostFindAllRequest.class)))
                .willThrow(new BadRequestException(PostErrorCode.POST_CURSOR_INVALID));

        mockMvc.perform(get("/api/v1/posts").queryParam("cursor", "invalid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("조회 결과가 없는 포스트 ID는 값의 범위와 관계없이 404를 반환한다")
    void rejectMissingPostId() throws Exception {
        given(postService.findPost(0))
                .willThrow(new NotFoundException(PostErrorCode.POST_NOT_FOUND));

        mockMvc.perform(get("/api/v1/posts/{postId}", 0))
                .andExpect(status().isNotFound());

        verify(postService).findPost(0);
    }

    @Test
    @DisplayName("크루나 코치가 아니면 포스트 작성 요청에 403을 반환한다")
    void rejectForbiddenWriter() throws Exception {
        given(postService.savePost(eq(USER_ID), any(PostSaveRequest.class)))
                .willThrow(new ForbiddenException(PostErrorCode.POST_WRITER_TYPE_FORBIDDEN));

        mockMvc.perform(post("/api/v1/posts")
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                .content(validCreateRequest()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("POST_WRITER_TYPE_FORBIDDEN"))
                .andDo(document(
                        "post-save-forbidden",
                        resource(errorResponse(
                                "포스트 작성",
                                "크루 또는 코치가 카테고리와 업로드 완료된 본문 이미지를 연결해 포스트를 작성한다."
                        ))
                ));
    }

    @Test
    @DisplayName("작성자가 아니면 수정과 삭제 요청에 403을 반환한다")
    void rejectNonAuthorMutation() throws Exception {
        given(postService.updatePost(eq(POST_ID), eq(USER_ID), any(PostUpdateRequest.class)))
                .willThrow(new ForbiddenException(PostErrorCode.POST_AUTHOR_FORBIDDEN));
        willThrow(new ForbiddenException(PostErrorCode.POST_AUTHOR_FORBIDDEN))
                .given(postService).deletePost(POST_ID, USER_ID);

        mockMvc.perform(put("/api/v1/posts/{postId}", POST_ID)
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateRequest()))
                .andExpect(status().isForbidden())
                .andDo(document(
                        "post-update-forbidden",
                        resource(pathErrorResponse(
                                "포스트 수정",
                                "작성자가 본문, 카테고리, 본문 미디어를 전체 교체한다."
                        ))
                ));
        mockMvc.perform(delete("/api/v1/posts/{postId}", POST_ID)
                        .with(authenticated()))
                .andExpect(status().isForbidden())
                .andDo(document(
                        "post-delete-forbidden",
                        resource(pathErrorResponse(
                                "포스트 삭제",
                                "작성자가 포스트를 soft delete한다."
                        ))
                ));
    }

    @Test
    @DisplayName("인증 없이 포스트 수정과 삭제를 요청하면 401을 반환한다")
    void rejectAnonymousMutation() throws Exception {
        mockMvc.perform(put("/api/v1/posts/{postId}", POST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateRequest()))
                .andExpect(status().isUnauthorized())
                .andDo(document(
                        "post-update-unauthorized",
                        resource(pathErrorResponse(
                                "포스트 수정",
                                "작성자가 본문, 카테고리, 본문 미디어를 전체 교체한다."
                        ))
                ));
        mockMvc.perform(delete("/api/v1/posts/{postId}", POST_ID))
                .andExpect(status().isUnauthorized())
                .andDo(document(
                        "post-delete-unauthorized",
                        resource(pathErrorResponse(
                                "포스트 삭제",
                                "작성자가 포스트를 soft delete한다."
                        ))
                ));

        verifyNoInteractions(postService);
    }

    @Test
    @DisplayName("없는 포스트 수정과 삭제 요청은 404를 반환한다")
    void rejectMissingPostMutation() throws Exception {
        given(postService.updatePost(eq(POST_ID), eq(USER_ID), any(PostUpdateRequest.class)))
                .willThrow(new NotFoundException(PostErrorCode.POST_NOT_FOUND));
        willThrow(new NotFoundException(PostErrorCode.POST_NOT_FOUND))
                .given(postService).deletePost(POST_ID, USER_ID);

        mockMvc.perform(put("/api/v1/posts/{postId}", POST_ID)
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateRequest()))
                .andExpect(status().isNotFound())
                .andDo(document(
                        "post-update-not-found",
                        resource(pathErrorResponse(
                                "포스트 수정",
                                "작성자가 본문, 카테고리, 본문 미디어를 전체 교체한다."
                        ))
                ));
        mockMvc.perform(delete("/api/v1/posts/{postId}", POST_ID)
                        .with(authenticated()))
                .andExpect(status().isNotFound())
                .andDo(document(
                        "post-delete-not-found",
                        resource(pathErrorResponse(
                                "포스트 삭제",
                                "작성자가 포스트를 soft delete한다."
                        ))
                ));
    }

    @Test
    @DisplayName("없는 범위의 포스트 ID도 조회 결과에 따라 404를 반환한다")
    void rejectInvalidPostIdMutation() throws Exception {
        given(postService.updatePost(eq(0L), eq(USER_ID), any(PostUpdateRequest.class)))
                .willThrow(new NotFoundException(PostErrorCode.POST_NOT_FOUND));
        willThrow(new NotFoundException(PostErrorCode.POST_NOT_FOUND))
                .given(postService).deletePost(0L, USER_ID);

        mockMvc.perform(put("/api/v1/posts/{postId}", 0)
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateRequest()))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/posts/{postId}", 0)
                        .with(authenticated()))
                .andExpect(status().isNotFound())
                .andDo(document(
                        "post-delete-invalid",
                        resource(pathErrorResponse(
                                "포스트 삭제",
                                "작성자가 포스트를 soft delete한다."
                        ))
                ));

    }

    @Test
    @DisplayName("없는 포스트 상세 조회는 404를 반환한다")
    void rejectMissingPost() throws Exception {
        given(postService.findPost(POST_ID))
                .willThrow(new NotFoundException(PostErrorCode.POST_NOT_FOUND));

        mockMvc.perform(get("/api/v1/posts/{postId}", POST_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"))
                .andDo(document(
                        "post-find-not-found",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Post")
                                .summary("포스트 상세 조회")
                                .description("삭제되지 않은 전체 공개 포스트를 조회한다.")
                                .pathParameters(parameterWithName("postId")
                                        .type(INTEGER)
                                        .description("포스트 ID"))
                                .responseSchema(Schema.schema("ErrorResponse"))
                                .responseFields(RestDocsFields.errorResponse())
                                .build())
                ));
    }

    private RequestPostProcessor authenticated() {
        return request -> {
            request.setAttribute(
                    AuthenticatedSession.class.getName(),
                    new AuthenticatedSession(USER_ID, UserRole.USER)
            );
            return request;
        };
    }

    private String validCreateRequest() {
        return """
                {"content":"본문","categoryIds":[1],"mediaIds":[]}
                """;
    }

    private String validUpdateRequest() {
        return """
                {"content":"수정 본문","categoryIds":[1],"mediaIds":[]}
                """;
    }

    private ResourceSnippetParameters errorResponse(String summary, String description) {
        return ResourceSnippetParameters.builder()
                .tag("Post")
                .summary(summary)
                .description(description)
                .responseSchema(Schema.schema("ErrorResponse"))
                .responseFields(RestDocsFields.errorResponse())
                .build();
    }

    private ResourceSnippetParameters pathErrorResponse(String summary, String description) {
        return ResourceSnippetParameters.builder()
                .tag("Post")
                .summary(summary)
                .description(description)
                .pathParameters(parameterWithName("postId")
                        .type(INTEGER)
                        .description("포스트 ID"))
                .responseSchema(Schema.schema("ErrorResponse"))
                .responseFields(RestDocsFields.errorResponse())
                .build();
    }

    private PostItem postItem() {
        return new PostItem(
                POST_ID,
                "본문입니다.",
                new PostItem.Author(
                        "zzaekkii",
                        "재키",
                        UserType.WOOWACOURSE_CREW,
                        "BACKEND",
                        (short) 8,
                        21L
                ),
                List.of(new PostItem.Category(
                        3L,
                        "backend",
                        "백엔드",
                        CategoryType.GENERAL
                )),
                List.of(new PostItem.Media(21L, 0)),
                0L,
                CREATED_AT,
                CREATED_AT
        );
    }

    private List<FieldDescriptor> postFields(String prefix) {
        return applyPathPrefix(prefix, List.of(
                fieldWithPath("postId").type(NUMBER).description("포스트 ID"),
                fieldWithPath("content").type(STRING).description("Markdown 본문"),
                fieldWithPath("author").type(OBJECT).description("현재 작성자 프로필"),
                fieldWithPath("author.handle").type(STRING).description("작성자 핸들"),
                fieldWithPath("author.displayName").type(STRING).description("작성자 이름"),
                fieldWithPath("author.userType").type(STRING).description("작성자 유형"),
                fieldWithPath("author.track").type(STRING).description("작성자 트랙").optional(),
                fieldWithPath("author.cohort").type(NUMBER).description("작성자 기수").optional(),
                fieldWithPath("author.avatarImageId").type(NUMBER).description("현재 프로필 이미지 미디어 ID").optional(),
                fieldWithPath("categories").type(ARRAY).description("카테고리 목록"),
                fieldWithPath("categories[].categoryId").type(NUMBER).description("카테고리 ID"),
                fieldWithPath("categories[].slug").type(STRING).description("카테고리 slug"),
                fieldWithPath("categories[].displayName").type(STRING).description("카테고리 표시 이름"),
                fieldWithPath("categories[].type").type(STRING).description("GENERAL 또는 EVENT"),
                fieldWithPath("media").type(ARRAY).description("본문 미디어 목록"),
                fieldWithPath("media[].mediaId").type(NUMBER).description("미디어 ID"),
                fieldWithPath("media[].displayOrder").type(NUMBER).description("미디어 표시 순서"),
                fieldWithPath("createdAt").type(STRING).description("ISO-8601 생성 시각"),
                fieldWithPath("updatedAt").type(STRING).description("ISO-8601 수정 시각")
        ));
    }

    private List<FieldDescriptor> successResponseFields(String postPrefix) {
        List<FieldDescriptor> fields = new java.util.ArrayList<>();
        fields.add(fieldWithPath("status").type(STRING).description("응답 상태"));
        fields.add(fieldWithPath("data").type(OBJECT).description("포스트"));
        fields.addAll(postFields(postPrefix));
        return fields;
    }

    private List<FieldDescriptor> postListResponseFields() {
        List<FieldDescriptor> fields = new java.util.ArrayList<>();
        fields.add(fieldWithPath("status").type(STRING).description("응답 상태"));
        fields.add(fieldWithPath("data").type(ARRAY).description("포스트 목록"));
        fields.addAll(postFields("data[]."));
        fields.add(fieldWithPath("meta").type(OBJECT).description("Slice 메타데이터"));
        fields.add(fieldWithPath("meta.nextCursor").type(STRING).description("다음 Slice 커서").optional());
        fields.add(fieldWithPath("meta.hasNext").type(BOOLEAN).description("다음 Slice 존재 여부"));
        return fields;
    }
}
