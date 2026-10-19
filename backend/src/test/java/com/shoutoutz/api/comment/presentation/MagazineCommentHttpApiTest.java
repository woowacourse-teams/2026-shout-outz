package com.shoutoutz.api.comment.presentation;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.epages.restdocs.apispec.SimpleType;
import com.shoutoutz.api.auth.presentation.session.AuthenticatedSession;
import com.shoutoutz.api.comment.application.MagazineCommentService;
import com.shoutoutz.api.comment.domain.CommentErrorCode;
import com.shoutoutz.api.comment.presentation.dto.request.MagazineCommentCreateRequest;
import com.shoutoutz.api.comment.presentation.dto.request.MagazineCommentFindRequest;
import com.shoutoutz.api.comment.presentation.dto.request.MagazineCommentUpdateRequest;
import com.shoutoutz.api.comment.presentation.dto.response.MagazineCommentAuthorResponse;
import com.shoutoutz.api.comment.presentation.dto.response.MagazineCommentCreateResponse;
import com.shoutoutz.api.comment.presentation.dto.response.MagazineCommentDeleteResponse;
import com.shoutoutz.api.comment.presentation.dto.response.MagazineCommentFindResponse.Comment;
import com.shoutoutz.api.comment.presentation.dto.response.MagazineCommentFindResponse;
import com.shoutoutz.api.comment.presentation.dto.response.MagazineCommentUpdateResponse;
import com.shoutoutz.api.common.exception.code.CommonErrorCode;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.common.response.SliceMetaResponse;
import com.shoutoutz.api.common.restdocs.RestDocsFields;
import com.shoutoutz.api.magazine.domain.MagazineErrorCode;
import com.shoutoutz.api.user.domain.account.UserRole;
import java.time.Instant;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultHandler;

@WebMvcTest(controllers = MagazineCommentHttpApi.class)
@AutoConfigureRestDocs
class MagazineCommentHttpApiTest {

    private static final String AUTHENTICATED_SESSION_ATTRIBUTE = AuthenticatedSession.class.getName();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MagazineCommentService magazineCommentService;

    @Test
    @DisplayName("비로그인 사용자가 댓글 목록을 조회하면 댓글 목록과 페이지 정보를 반환한다.")
    void findsCommentsForAnonymousUser() throws Exception {
        given(magazineCommentService.findAll(
                eq(100L),
                any(MagazineCommentFindRequest.class),
                org.mockito.ArgumentMatchers.isNull()
        )).willReturn(new MagazineCommentFindResponse(
                List.of(
                        new Comment(
                                501L,
                                "좋은 매거진네요.",
                                new MagazineCommentFindResponse.Author(
                                        7L,
                                        "@author7",
                                        "샤라웃 운영팀",
                                        null,
                                        null,
                                        null,
                                        null,
                                        10L,
                                        "https://cdn.example.com/media/10/display"
                                ),
                                null,
                                Instant.parse("2026-09-14T00:00:00Z"),
                                Instant.parse("2026-09-14T00:00:00Z"),
                                false,
                                false,
                                false,
                                0L,
                                false,
                                false,
                                null
                        ),
                        new Comment(
                                502L,
                                "저도 그렇게 생각합니다.",
                                new MagazineCommentFindResponse.Author(
                                        8L,
                                        "@author8",
                                        "재키",
                                        null,
                                        null,
                                        null,
                                        null,
                                        11L,
                                        "https://cdn.example.com/media/11/display"
                                ),
                                501L,
                                Instant.parse("2026-09-14T00:05:00Z"),
                                Instant.parse("2026-09-14T00:05:00Z"),
                                false,
                                false,
                                false,
                                0L,
                                false,
                                false,
                                null
                        ),
                        new Comment(
                                503L,
                                null,
                                new MagazineCommentFindResponse.Author(
                                        9L,
                                        "@author9",
                                        "이전 작성자",
                                        null,
                                        null,
                                        null,
                                        null,
                                        12L,
                                        "https://cdn.example.com/media/12/display"
                                ),
                                null,
                                Instant.parse("2026-09-14T00:10:00Z"),
                                Instant.parse("2026-09-14T00:10:00Z"),
                                false,
                                false,
                                true,
                                0L,
                                false,
                                false,
                                null
                        )
                ),
                new SliceMetaResponse("next-cursor", true, 2L)
        ));

        mockMvc.perform(get("/api/v1/magazines/{magazineId}/comments", 100L)
                        .queryParam("size", "5")
                        .queryParam("sort", "LATEST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(501))
                .andExpect(jsonPath("$.data[0].author.userId").value(7))
                .andExpect(jsonPath("$.data[0].author.handle").value("@author7"))
                .andExpect(jsonPath("$.data[0].author.avatarImageId").value(10L))
                .andExpect(jsonPath("$.data[0].editable").value(false))
                .andExpect(jsonPath("$.data[0].edited").value(false))
                .andExpect(jsonPath("$.data[0].deleted").value(false))
                .andExpect(jsonPath("$.data[1].parentId").value(501))
                .andExpect(jsonPath("$.data[2].content").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.data[2].deleted").value(true))
                .andExpect(jsonPath("$.meta.nextCursor").value("next-cursor"))
                .andExpect(jsonPath("$.meta.hasNext").value(true))
                .andExpect(jsonPath("$.meta.totalCount").value(2))
                .andDo(document(
                        "magazine-comment-find-all",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Magazine Comment")
                                .summary("매거진 댓글 목록 조회")
                                .description("비로그인 또는 로그인 사용자가 발행된 매거진의 댓글 목록을 조회한다. "
                                        + "size는 루트 댓글 개수이며 각 루트 댓글 뒤에 대댓글을 반환한다.")
                                .pathParameters(
                                        parameterWithName("magazineId").type(SimpleType.INTEGER).description("댓글을 조회할 매거진 ID")
                                )
                                .queryParameters(
                                        parameterWithName("cursor")
                                                .description("다음 페이지 조회에 사용하는 opaque cursor")
                                                .optional(),
                                        parameterWithName("size").type(SimpleType.INTEGER).defaultValue(5)
                                                .description("조회할 루트 댓글 개수. 기본값 5, 1~50")
                                                .optional(),
                                        parameterWithName("sort").defaultValue("LATEST")
                                                .description("루트 댓글 정렬 기준(LATEST 또는 OLDEST). 기본값 LATEST")
                                                .optional()
                                )
                                .responseSchema(Schema.schema("MagazineCommentFindAllSuccessResponse"))
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data").type(ARRAY).description("매거진 댓글 목록"),
                                        fieldWithPath("data[].id").type(NUMBER).description("댓글 ID"),
                                        fieldWithPath("data[].content").type(STRING)
                                                .description("댓글 내용. 삭제된 댓글은 null")
                                                .optional(),
                                        fieldWithPath("data[].linkPreview").type(OBJECT)
                                                .description("댓글 본문에서 추출한 첫 URL의 링크 미리보기. URL이 없으면 null")
                                                .optional(),
                                        fieldWithPath("data[].linkPreview.url").type(STRING)
                                                .description("댓글 본문에서 추출한 첫 URL").optional(),
                                        fieldWithPath("data[].linkPreview.title").type(STRING)
                                                .description("외부 페이지 제목. 수집 전·실패 시 null").optional(),
                                        fieldWithPath("data[].linkPreview.description").type(STRING)
                                                .description("외부 페이지 설명").optional(),
                                        fieldWithPath("data[].linkPreview.imageUrl").type(STRING)
                                                .description("외부 페이지 이미지 URL").optional(),
                                        fieldWithPath("data[].linkPreview.siteName").type(STRING)
                                                .description("외부 사이트 이름").optional(),
                                        fieldWithPath("data[].isAnonymous").type(BOOLEAN)
                                                .description("작성자 정보를 익명으로 공개할지 여부"),
                                        fieldWithPath("data[].author").type(OBJECT).description("댓글 작성자"),
                                        fieldWithPath("data[].author.userId").type(NUMBER)
                                                .description("작성자 ID. 타인의 익명 댓글이면 null").optional(),
                                        fieldWithPath("data[].author.handle").type(STRING)
                                                .description("작성자 handle. 타인의 익명 댓글이면 null").optional(),
                                        fieldWithPath("data[].author.displayName").type(STRING)
                                                .description("작성자 표시 이름. 타인의 익명 댓글이면 null").optional(),
                                        fieldWithPath("data[].author.userType").type(STRING)
                                                .description("작성자 유형. 크루 배지 구분을 위해 타인의 익명 댓글에도 포함").optional(),
                                        fieldWithPath("data[].author.track").type(STRING)
                                                .description("작성자 트랙. 크루가 아니거나 타인의 익명 댓글이면 null").optional(),
                                        fieldWithPath("data[].author.cohort").type(NUMBER)
                                                .description("작성자 정보가 공개되는 크루의 기수. 타인의 익명 댓글이면 제외").optional(),
                                        fieldWithPath("data[].author.isCurrent").type(BOOLEAN)
                                                .description("작성자가 현재 기수 크루인지 여부. 크루가 아니면 null").optional(),
                                        fieldWithPath("data[].author.avatarImageId").type(NUMBER)
                                                .description("작성자 프로필 이미지 미디어 ID. 타인의 익명 댓글이면 null")
                                                .optional(),
                                        fieldWithPath("data[].author.avatarUrl").type(STRING)
                                                .description("직접 업로드한 이미지가 없으면 GitHub 아바타를 사용하는 작성자 프로필 이미지 공개 URL. 타인의 익명 댓글이면 null")
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
                                                .description("댓글 내용이 수정된 적이 있는지 여부. 삭제된 댓글은 false"),
                                        fieldWithPath("data[].deleted").type(BOOLEAN)
                                                .description("댓글이 삭제되었는지 여부"),
                                        fieldWithPath("data[].agreeCount").type(NUMBER)
                                                .description("댓글 공감 수"),
                                        fieldWithPath("data[].agreedByMe").type(BOOLEAN)
                                                .description("현재 사용자의 공감 여부. 비로그인이면 false"),
                                        fieldWithPath("meta").type(OBJECT)
                                                .description("페이지네이션 정보"),
                                        fieldWithPath("meta.nextCursor").type(STRING)
                                                .description("다음 페이지 cursor")
                                                .optional(),
                                        fieldWithPath("meta.hasNext").type(BOOLEAN)
                                                .description("다음 페이지 존재 여부"),
                                        fieldWithPath("meta.totalCount").type(NUMBER)
                                                .description("커서와 size에 무관한 삭제되지 않은 루트 댓글과 답글의 전체 수")
                                )
                                .build())
                ));

        verify(magazineCommentService).findAll(
                eq(100L),
                any(MagazineCommentFindRequest.class),
                org.mockito.ArgumentMatchers.isNull()
        );
    }

    @Test
    @DisplayName("로그인 세션이 있으면 댓글 작성자 본인의 댓글만 editable로 반환한다.")
    void passesOptionalLoginUserToService() throws Exception {
        given(magazineCommentService.findAll(
                eq(100L),
                any(MagazineCommentFindRequest.class),
                eq(7L)
        )).willReturn(new MagazineCommentFindResponse(
                List.of(new Comment(
                                501L,
                                "내 댓글",
                                new MagazineCommentFindResponse.Author(
                                        7L,
                                        "@author7",
                                        "샤라웃 운영팀",
                                        null,
                                        null,
                                        null,
                                        null,
                                        10L,
                                        "https://cdn.example.com/media/10/display"
                                ),
                                null,
                                Instant.parse("2026-09-14T00:00:00Z"),
                                Instant.parse("2026-09-14T00:00:00Z"),
                                true,
                                false,
                                false,
                                0L,
                                false,
                                false,
                                null
                        )),
                new SliceMetaResponse(null, false, 1L)
        ));

        mockMvc.perform(get("/api/v1/magazines/{magazineId}/comments", 100L)
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE,
                                new AuthenticatedSession(7L, UserRole.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].editable").value(true));

        verify(magazineCommentService).findAll(
                eq(100L),
                any(MagazineCommentFindRequest.class),
                eq(7L)
        );
    }

    @Test
    @DisplayName("댓글 목록 조회 크기가 범위를 벗어나면 서비스를 호출하지 않고 400을 반환한다.")
    void rejectsInvalidFindSize() throws Exception {
        mockMvc.perform(get("/api/v1/magazines/{magazineId}/comments", 100L)
                        .queryParam("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CommentErrorCode.INVALID_COMMENT_SIZE.name()))
                .andDo(errorDocument("magazine-comment-find-all-invalid-size", "매거진 댓글 목록 조회", false));

        verifyNoInteractions(magazineCommentService);
    }

    @Test
    void rejectsInvalidFindSort() throws Exception {
        mockMvc.perform(get("/api/v1/magazines/{magazineId}/comments", 100L)
                        .queryParam("sort", "POPULAR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_COMMENT_SORT"))
                .andDo(errorDocument("magazine-comment-find-all-invalid-sort", "매거진 댓글 목록 조회", false));

        verifyNoInteractions(magazineCommentService);
    }

    @Test
    void rejectsNonPositiveParentId() throws Exception {
        mockMvc.perform(post("/api/v1/magazines/{magazineId}/comments", 100L)
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE, new AuthenticatedSession(7L, UserRole.USER))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"댓글\",\"parentId\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details[0].field").value("parentId"))
                .andDo(errorDocument("magazine-comment-create-invalid-parent", "매거진 댓글 작성", false));

        verifyNoInteractions(magazineCommentService);
    }

    @Test
    void rejectsFindingUnpublishedMagazineComments() throws Exception {
        given(magazineCommentService.findAll(eq(100L), any(MagazineCommentFindRequest.class), any()))
                .willThrow(new EntityNotFoundException(MagazineErrorCode.MAGAZINE_NOT_FOUND));

        mockMvc.perform(get("/api/v1/magazines/{magazineId}/comments", 100L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MAGAZINE_NOT_FOUND"))
                .andDo(errorDocument("magazine-comment-find-all-not-found", "매거진 댓글 목록 조회", false));
    }

    @Test
    void rejectsCreatingCommentForInactiveUser() throws Exception {
        given(magazineCommentService.create(eq(100L), eq(7L), any(MagazineCommentCreateRequest.class)))
                .willThrow(new ForbiddenException(MagazineErrorCode.MAGAZINE_USER_FORBIDDEN));

        mockMvc.perform(post("/api/v1/magazines/{magazineId}/comments", 100L)
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE, new AuthenticatedSession(7L, UserRole.USER))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"댓글\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MAGAZINE_USER_FORBIDDEN"))
                .andDo(errorDocument("magazine-comment-create-forbidden", "매거진 댓글 작성", false));
    }

    @Test
    @DisplayName("발행된 매거진에 댓글을 작성하면 201과 작성자 정보를 포함한 댓글을 반환한다.")
    void createsComment() throws Exception {
        given(magazineCommentService.create(eq(100L), eq(7L), any(MagazineCommentCreateRequest.class)))
                .willReturn(new MagazineCommentCreateResponse(
                                501L,
                                "좋은 매거진네요.",
                                new MagazineCommentAuthorResponse(
                                        7L,
                                        "@author7",
                                        "샤라웃 운영팀",
                                        null,
                                        null,
                                        null,
                                        null,
                                        "https://cdn.example.com/media/10/display"
                                ),
                                null,
                                Instant.parse("2026-09-14T00:00:00Z"),
                                Instant.parse("2026-09-14T00:00:00Z"),
                                true,
                                false,
                                null
                        ));

        mockMvc.perform(post("/api/v1/magazines/{magazineId}/comments", 100L)
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE,
                                new AuthenticatedSession(7L, UserRole.USER))
                        .header(HttpHeaders.COOKIE, "JSESSIONID=user-session")
                        .header("X-CSRF-Token", "csrf-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "  좋은 매거진네요.  "
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(501))
                .andExpect(jsonPath("$.data.content").value("좋은 매거진네요."))
                .andExpect(jsonPath("$.data.author.userId").value(7))
                .andExpect(jsonPath("$.data.author.handle").value("@author7"))
                .andExpect(jsonPath("$.data.author.displayName").value("샤라웃 운영팀"))
                .andExpect(jsonPath("$.data.author.avatarImageId").doesNotExist())
                .andExpect(jsonPath("$.data.author.avatarUrl")
                        .value("https://cdn.example.com/media/10/display"))
                .andExpect(jsonPath("$.data.parentId").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.data.createdAt").value("2026-09-14T00:00:00Z"))
                .andExpect(jsonPath("$.data.updatedAt").value("2026-09-14T00:00:00Z"))
                .andExpect(jsonPath("$.data.editable").value(true))
                .andDo(document(
                        "magazine-comment-create",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Magazine Comment")
                                .summary("매거진 댓글 작성")
                                .description("활성 회원이 발행된 매거진에 댓글 또는 1단계 대댓글을 작성한다.")
                                .pathParameters(
                                        parameterWithName("magazineId").type(SimpleType.INTEGER).description("댓글을 작성할 매거진 ID")
                                )
                                .requestHeaders(
                                        headerWithName(HttpHeaders.COOKIE).description("활성 회원 세션의 JSESSIONID"),
                                        headerWithName("X-CSRF-Token").description("세션 조회로 발급받은 CSRF 토큰")
                                )
                                .requestSchema(Schema.schema("MagazineCommentCreateRequest"))
                                .responseSchema(Schema.schema("MagazineCommentCreateSuccessResponse"))
                                .requestFields(
                                        fieldWithPath("content")
                                                .type(STRING)
                                                .description("앞뒤 공백을 제거한 뒤 저장하는 댓글 내용 (1~500자, Unicode code point 기준)"),
                                        fieldWithPath("isAnonymous").type(BOOLEAN)
                                                .description("작성자 정보를 익명으로 공개할지 여부")
                                                .optional(),
                                        fieldWithPath("parentId")
                                                .type(NUMBER)
                                                .description("같은 매거진의 삭제되지 않은 루트 댓글 ID. 없으면 루트 댓글")
                                                .optional()
                                )
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data").type(OBJECT).description("생성된 댓글"),
                                        fieldWithPath("data.id").type(NUMBER).description("댓글 ID"),
                                        fieldWithPath("data.content").type(STRING).description("저장된 댓글 내용"),
                                        fieldWithPath("data.linkPreview").type(OBJECT)
                                                .description("댓글 본문에서 추출한 첫 URL의 링크 미리보기. URL이 없으면 null")
                                                .optional(),
                                        fieldWithPath("data.linkPreview.url").type(STRING)
                                                .description("댓글 본문에서 추출한 첫 URL").optional(),
                                        fieldWithPath("data.linkPreview.title").type(STRING)
                                                .description("외부 페이지 제목. 수집 전·실패 시 null").optional(),
                                        fieldWithPath("data.linkPreview.description").type(STRING)
                                                .description("외부 페이지 설명").optional(),
                                        fieldWithPath("data.linkPreview.imageUrl").type(STRING)
                                                .description("외부 페이지 이미지 URL").optional(),
                                        fieldWithPath("data.linkPreview.siteName").type(STRING)
                                                .description("외부 사이트 이름").optional(),
                                        fieldWithPath("data.isAnonymous").type(BOOLEAN)
                                                .description("작성자 정보를 익명으로 공개할지 여부"),
                                        fieldWithPath("data.author").type(OBJECT).description("댓글 작성자"),
                                        fieldWithPath("data.author.userId").type(NUMBER).description("작성자 ID"),
                                        fieldWithPath("data.author.handle").type(STRING).description("작성자 handle").optional(),
                                        fieldWithPath("data.author.displayName").type(STRING).description("작성자 표시 이름"),
                                        fieldWithPath("data.author.userType").type(STRING).description("작성자 유형").optional(),
                                        fieldWithPath("data.author.track").type(STRING).description("작성자 트랙").optional(),
                                        fieldWithPath("data.author.cohort").type(NUMBER)
                                                .description("작성자 정보가 공개되는 크루의 기수. 타인의 익명 댓글이면 제외").optional(),
                                        fieldWithPath("data.author.isCurrent").type(BOOLEAN)
                                                .description("작성자가 현재 기수 크루인지 여부. 크루가 아니면 null").optional(),
                                        fieldWithPath("data.author.avatarUrl").type(STRING)
                                                .description("직접 업로드한 이미지가 없으면 GitHub 아바타를 사용하는 작성자 프로필 이미지 공개 URL")
                                                .optional(),
                                        fieldWithPath("data.parentId").type(NUMBER).description("부모 댓글 ID")
                                                .optional(),
                                        fieldWithPath("data.createdAt").type(STRING).description("생성 시각 (UTC ISO-8601)"),
                                        fieldWithPath("data.updatedAt").type(STRING).description("수정 시각 (UTC ISO-8601)"),
                                        fieldWithPath("data.editable").type(BOOLEAN).description("현재 사용자가 수정할 수 있는지 여부")
                                )
                                .build())
                ));

        verify(magazineCommentService).create(eq(100L), eq(7L), any(MagazineCommentCreateRequest.class));
    }

    @Test
    @DisplayName("댓글 작성자가 댓글을 수정하면 200과 수정된 댓글을 반환한다.")
    void updatesComment() throws Exception {
        given(magazineCommentService.update(
                eq(100L),
                eq(501L),
                eq(7L),
                any(MagazineCommentUpdateRequest.class)
        )).willReturn(new MagazineCommentUpdateResponse(
                        501L,
                        "수정된 댓글입니다.",
                        new MagazineCommentAuthorResponse(
                                7L,
                                "@author7",
                                "샤라웃 운영팀",
                                null,
                                null,
                                null,
                                null,
                                "https://cdn.example.com/media/10/display"
                        ),
                        null,
                        Instant.parse("2026-09-14T00:00:00Z"),
                        Instant.parse("2026-09-14T00:30:00Z"),
                        true,
                        true,
                        false,
                        null
                ));

        mockMvc.perform(patch("/api/v1/magazines/{magazineId}/comments/{commentId}", 100L, 501L)
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE,
                                new AuthenticatedSession(7L, UserRole.USER))
                        .header(HttpHeaders.COOKIE, "JSESSIONID=user-session")
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
                .andExpect(jsonPath("$.data.author.avatarImageId").doesNotExist())
                .andExpect(jsonPath("$.data.author.avatarUrl")
                        .value("https://cdn.example.com/media/10/display"))
                .andExpect(jsonPath("$.data.parentId").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.data.createdAt").value("2026-09-14T00:00:00Z"))
                .andExpect(jsonPath("$.data.updatedAt").value("2026-09-14T00:30:00Z"))
                .andExpect(jsonPath("$.data.editable").value(true))
                .andExpect(jsonPath("$.data.edited").value(true))
                .andDo(document(
                        "magazine-comment-update",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Magazine Comment")
                                .summary("매거진 댓글 수정")
                                .description("활성 회원인 매거진 댓글 작성자가 발행된 매거진의 댓글 내용을 수정한다. "
                                        + "트림 후 기존 내용과 같으면 저장하지 않고 수정 시각을 유지한다.")
                                .pathParameters(
                                        parameterWithName("magazineId").type(SimpleType.INTEGER).description("댓글이 속한 매거진 ID"),
                                        parameterWithName("commentId").type(SimpleType.INTEGER).description("수정할 댓글 ID")
                                )
                                .requestHeaders(
                                        headerWithName(HttpHeaders.COOKIE).description("활성 회원 세션의 JSESSIONID"),
                                        headerWithName("X-CSRF-Token").description("세션 조회로 발급받은 CSRF 토큰")
                                )
                                .requestSchema(Schema.schema("MagazineCommentUpdateRequest"))
                                .responseSchema(Schema.schema("MagazineCommentUpdateSuccessResponse"))
                                .requestFields(
                                        fieldWithPath("content")
                                                .type(STRING)
                                                .description("앞뒤 공백을 제거한 뒤 저장하는 댓글 내용 (1~500자, Unicode code point 기준)")
                                )
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data").type(OBJECT).description("수정된 댓글"),
                                        fieldWithPath("data.id").type(NUMBER).description("댓글 ID"),
                                        fieldWithPath("data.content").type(STRING).description("저장된 댓글 내용"),
                                        fieldWithPath("data.linkPreview").type(OBJECT)
                                                .description("댓글 본문에서 추출한 첫 URL의 링크 미리보기. URL이 없으면 null")
                                                .optional(),
                                        fieldWithPath("data.linkPreview.url").type(STRING)
                                                .description("댓글 본문에서 추출한 첫 URL").optional(),
                                        fieldWithPath("data.linkPreview.title").type(STRING)
                                                .description("외부 페이지 제목. 수집 전·실패 시 null").optional(),
                                        fieldWithPath("data.linkPreview.description").type(STRING)
                                                .description("외부 페이지 설명").optional(),
                                        fieldWithPath("data.linkPreview.imageUrl").type(STRING)
                                                .description("외부 페이지 이미지 URL").optional(),
                                        fieldWithPath("data.linkPreview.siteName").type(STRING)
                                                .description("외부 사이트 이름").optional(),
                                        fieldWithPath("data.isAnonymous").type(BOOLEAN)
                                                .description("작성자 정보를 익명으로 공개할지 여부"),
                                        fieldWithPath("data.author").type(OBJECT).description("댓글 작성자"),
                                        fieldWithPath("data.author.userId").type(NUMBER).description("작성자 ID"),
                                        fieldWithPath("data.author.handle").type(STRING).description("작성자 handle").optional(),
                                        fieldWithPath("data.author.displayName").type(STRING).description("작성자 표시 이름"),
                                        fieldWithPath("data.author.userType").type(STRING).description("작성자 유형").optional(),
                                        fieldWithPath("data.author.track").type(STRING).description("작성자 트랙").optional(),
                                        fieldWithPath("data.author.cohort").type(NUMBER)
                                                .description("작성자 정보가 공개되는 크루의 기수. 타인의 익명 댓글이면 제외").optional(),
                                        fieldWithPath("data.author.isCurrent").type(BOOLEAN)
                                                .description("작성자가 현재 기수 크루인지 여부. 크루가 아니면 null").optional(),
                                        fieldWithPath("data.author.avatarUrl").type(STRING)
                                                .description("직접 업로드한 이미지가 없으면 GitHub 아바타를 사용하는 작성자 프로필 이미지 공개 URL")
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

        verify(magazineCommentService).update(
                eq(100L),
                eq(501L),
                eq(7L),
                any(MagazineCommentUpdateRequest.class)
        );
    }

    @Test
    @DisplayName("댓글 작성자가 댓글을 삭제하면 200과 삭제 상태를 반환한다.")
    void deletesComment() throws Exception {
        given(magazineCommentService.delete(100L, 501L, 7L))
                .willReturn(new MagazineCommentDeleteResponse(501L, true));

        mockMvc.perform(delete("/api/v1/magazines/{magazineId}/comments/{commentId}", 100L, 501L)
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE,
                                new AuthenticatedSession(7L, UserRole.USER))
                        .header(HttpHeaders.COOKIE, "JSESSIONID=user-session")
                        .header("X-CSRF-Token", "csrf-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(501))
                .andExpect(jsonPath("$.data.deleted").value(true))
                .andDo(document(
                        "magazine-comment-delete",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Magazine Comment")
                                .summary("매거진 댓글 삭제")
                                .description("활성 회원인 댓글 작성자 본인이 발행된 매거진의 댓글을 soft delete한다.")
                                .pathParameters(
                                        parameterWithName("magazineId").type(SimpleType.INTEGER).description("댓글이 속한 매거진 ID"),
                                        parameterWithName("commentId").type(SimpleType.INTEGER).description("삭제할 댓글 ID")
                                )
                                .requestHeaders(
                                        headerWithName(HttpHeaders.COOKIE).description("활성 회원 세션의 JSESSIONID"),
                                        headerWithName("X-CSRF-Token").description("세션 조회로 발급받은 CSRF 토큰")
                                )
                                .responseSchema(Schema.schema("MagazineCommentDeleteSuccessResponse"))
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data").type(OBJECT).description("삭제된 댓글"),
                                        fieldWithPath("data.id").type(NUMBER).description("삭제된 댓글 ID"),
                                        fieldWithPath("data.deleted").type(BOOLEAN).description("댓글 삭제 여부")
                                )
                                .build())
                ));

        verify(magazineCommentService).delete(100L, 501L, 7L);
    }

    @Test
    @DisplayName("댓글 내용이 500자를 초과하면 400과 필드 오류를 반환하고 서비스를 호출하지 않는다.")
    void rejectsContentOver500CodePoints() throws Exception {
        mockMvc.perform(post("/api/v1/magazines/{magazineId}/comments", 100L)
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE,
                                new AuthenticatedSession(7L, UserRole.USER))
                        .header("X-CSRF-Token", "csrf-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"" + "😀".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details[0].field").value("content"))
                .andDo(errorDocument("magazine-comment-create-validation-failed", "매거진 댓글 작성", false));

        verifyNoInteractions(magazineCommentService);
    }

    @Test
    @DisplayName("댓글 수정 내용이 500자를 초과하면 400과 필드 오류를 반환하고 서비스를 호출하지 않는다.")
    void rejectsUpdateContentOver500CodePoints() throws Exception {
        mockMvc.perform(patch("/api/v1/magazines/{magazineId}/comments/{commentId}", 100L, 501L)
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE,
                                new AuthenticatedSession(7L, UserRole.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"" + "😀".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details[0].field").value("content"))
                .andDo(errorDocument("magazine-comment-update-validation-failed", "매거진 댓글 수정", true));

        verifyNoInteractions(magazineCommentService);
    }

    @Test
    @DisplayName("로그인하지 않고 댓글을 작성하면 401을 반환한다.")
    void rejectsUnauthenticatedUser() throws Exception {
        mockMvc.perform(post("/api/v1/magazines/{magazineId}/comments", 100L)
                        .header("X-CSRF-Token", "csrf-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"댓글\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andDo(errorDocument("magazine-comment-create-unauthorized", "매거진 댓글 작성", false));

        verifyNoInteractions(magazineCommentService);
    }

    @Test
    @DisplayName("로그인하지 않고 댓글을 수정하면 401을 반환한다.")
    void rejectsUnauthenticatedUpdate() throws Exception {
        mockMvc.perform(patch("/api/v1/magazines/{magazineId}/comments/{commentId}", 100L, 501L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"수정된 댓글\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andDo(errorDocument("magazine-comment-update-unauthorized", "매거진 댓글 수정", true));

        verifyNoInteractions(magazineCommentService);
    }

    @Test
    @DisplayName("로그인하지 않고 댓글을 삭제하면 401을 반환한다.")
    void rejectsUnauthenticatedDelete() throws Exception {
        mockMvc.perform(delete("/api/v1/magazines/{magazineId}/comments/{commentId}", 100L, 501L))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andDo(errorDocument("magazine-comment-delete-unauthorized", "매거진 댓글 삭제", true));

        verifyNoInteractions(magazineCommentService);
    }

    @Test
    @DisplayName("발행된 매거진이 아니면 매거진을 찾을 수 없다는 404를 반환한다.")
    void rejectsInactiveFeed() throws Exception {
        given(magazineCommentService.create(eq(100L), eq(7L), any(MagazineCommentCreateRequest.class)))
                .willThrow(new EntityNotFoundException(MagazineErrorCode.MAGAZINE_NOT_FOUND));

        mockMvc.perform(post("/api/v1/magazines/{magazineId}/comments", 100L)
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE,
                                new AuthenticatedSession(7L, UserRole.USER))
                        .header("X-CSRF-Token", "csrf-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"댓글\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(MagazineErrorCode.MAGAZINE_NOT_FOUND.name()))
                .andDo(errorDocument("magazine-comment-create-magazine-not-found", "매거진 댓글 작성", false));
    }

    @Test
    @DisplayName("발행된 매거진이 아니면 댓글 수정 요청에 매거진을 찾을 수 없다는 404를 반환한다.")
    void rejectsUpdateForInactiveFeed() throws Exception {
        given(magazineCommentService.update(
                eq(100L),
                eq(501L),
                eq(7L),
                any(MagazineCommentUpdateRequest.class)
        )).willThrow(new EntityNotFoundException(MagazineErrorCode.MAGAZINE_NOT_FOUND));

        mockMvc.perform(patch("/api/v1/magazines/{magazineId}/comments/{commentId}", 100L, 501L)
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE,
                                new AuthenticatedSession(7L, UserRole.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"수정된 댓글\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(MagazineErrorCode.MAGAZINE_NOT_FOUND.name()))
                .andDo(errorDocument("magazine-comment-update-magazine-not-found", "매거진 댓글 수정", true));
    }

    @Test
    @DisplayName("발행된 매거진이 아니면 댓글 삭제 요청에 매거진을 찾을 수 없다는 404를 반환한다.")
    void rejectsDeleteForInactiveFeed() throws Exception {
        given(magazineCommentService.delete(100L, 501L, 7L))
                .willThrow(new EntityNotFoundException(MagazineErrorCode.MAGAZINE_NOT_FOUND));

        mockMvc.perform(delete("/api/v1/magazines/{magazineId}/comments/{commentId}", 100L, 501L)
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE,
                                new AuthenticatedSession(7L, UserRole.USER))
                        .header("X-CSRF-Token", "csrf-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(MagazineErrorCode.MAGAZINE_NOT_FOUND.name()))
                .andDo(errorDocument("magazine-comment-delete-magazine-not-found", "매거진 댓글 삭제", true));
    }

    @Test
    @DisplayName("댓글 작성자가 아니면 댓글 수정 요청에 403을 반환한다.")
    void rejectsUpdateFromAnotherAuthor() throws Exception {
        given(magazineCommentService.update(
                eq(100L),
                eq(501L),
                eq(7L),
                any(MagazineCommentUpdateRequest.class)
        )).willThrow(new ForbiddenException(CommonErrorCode.FORBIDDEN));

        mockMvc.perform(patch("/api/v1/magazines/{magazineId}/comments/{commentId}", 100L, 501L)
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE,
                                new AuthenticatedSession(7L, UserRole.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"수정된 댓글\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andDo(errorDocument("magazine-comment-update-forbidden", "매거진 댓글 수정", true));
    }

    @Test
    @DisplayName("댓글 작성자가 아니면 댓글 삭제 요청에 403을 반환한다.")
    void rejectsDeleteFromAnotherAuthor() throws Exception {
        given(magazineCommentService.delete(100L, 501L, 7L))
                .willThrow(new ForbiddenException(CommonErrorCode.FORBIDDEN));

        mockMvc.perform(delete("/api/v1/magazines/{magazineId}/comments/{commentId}", 100L, 501L)
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE,
                                new AuthenticatedSession(7L, UserRole.USER))
                        .header("X-CSRF-Token", "csrf-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andDo(errorDocument("magazine-comment-delete-forbidden", "매거진 댓글 삭제", true));
    }

    @Test
    @DisplayName("삭제되었거나 다른 매거진의 댓글이면 댓글 수정 요청에 404를 반환한다.")
    void rejectsUnavailableComment() throws Exception {
        given(magazineCommentService.update(
                eq(100L),
                eq(501L),
                eq(7L),
                any(MagazineCommentUpdateRequest.class)
        )).willThrow(new EntityNotFoundException(CommentErrorCode.COMMENT_NOT_FOUND));

        mockMvc.perform(patch("/api/v1/magazines/{magazineId}/comments/{commentId}", 100L, 501L)
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE,
                                new AuthenticatedSession(7L, UserRole.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"수정된 댓글\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(CommentErrorCode.COMMENT_NOT_FOUND.name()))
                .andDo(errorDocument("magazine-comment-update-comment-not-found", "매거진 댓글 수정", true));
    }

    @Test
    @DisplayName("삭제되었거나 다른 매거진의 댓글이면 댓글 삭제 요청에 404를 반환한다.")
    void rejectsUnavailableCommentForDelete() throws Exception {
        given(magazineCommentService.delete(100L, 501L, 7L))
                .willThrow(new EntityNotFoundException(CommentErrorCode.COMMENT_NOT_FOUND));

        mockMvc.perform(delete("/api/v1/magazines/{magazineId}/comments/{commentId}", 100L, 501L)
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE,
                                new AuthenticatedSession(7L, UserRole.USER))
                        .header("X-CSRF-Token", "csrf-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(CommentErrorCode.COMMENT_NOT_FOUND.name()))
                .andDo(errorDocument("magazine-comment-delete-comment-not-found", "매거진 댓글 삭제", true));
    }

    @Test
    @DisplayName("대댓글 깊이를 초과하면 400을 반환한다.")
    void rejectsExceededCommentDepth() throws Exception {
        given(magazineCommentService.create(eq(100L), eq(7L), any(MagazineCommentCreateRequest.class)))
                .willThrow(new com.shoutoutz.api.common.exception.custom.BadRequestException(
                        CommentErrorCode.COMMENT_DEPTH_EXCEEDED
                ));

        mockMvc.perform(post("/api/v1/magazines/{magazineId}/comments", 100L)
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE,
                                new AuthenticatedSession(7L, UserRole.USER))
                        .header("X-CSRF-Token", "csrf-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"댓글\",\"parentId\":301}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CommentErrorCode.COMMENT_DEPTH_EXCEEDED.name()))
                .andDo(errorDocument("magazine-comment-create-depth-exceeded", "매거진 댓글 작성", false));
    }
    private ResultHandler errorDocument(String name, String summary, boolean commentPath) {
        var magazineId = parameterWithName("magazineId").type(SimpleType.INTEGER).description("매거진 ID");
        var builder = ResourceSnippetParameters.builder().tag("Magazine Comment").summary(summary)
                .pathParameters(magazineId)
                .responseSchema(Schema.schema("ErrorResponse"))
                .responseFields(RestDocsFields.errorResponse());
        if (commentPath) {
            builder.pathParameters(magazineId,
                    parameterWithName("commentId").type(SimpleType.INTEGER).description("댓글 ID"));
        }
        if (name.startsWith("magazine-comment-create-")) {
            builder.requestSchema(Schema.schema("MagazineCommentCreateRequest"));
        }
        if (name.startsWith("magazine-comment-update-")) {
            builder.requestSchema(Schema.schema("MagazineCommentUpdateRequest"));
        }
        return document(name, resource(builder.build()));
    }

}
