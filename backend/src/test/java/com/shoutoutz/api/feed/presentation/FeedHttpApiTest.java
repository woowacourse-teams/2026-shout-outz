package com.shoutoutz.api.feed.presentation;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.SimpleType.INTEGER;
import static com.shoutoutz.api.feed.presentation.FeedRestDocsFields.feedListResponseFields;
import static com.shoutoutz.api.feed.presentation.FeedRestDocsFields.commandSuccessResponseFields;
import static com.shoutoutz.api.feed.presentation.FeedRestDocsFields.successResponseFields;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
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
import com.shoutoutz.api.cohort.domain.Cohort;
import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.common.exception.custom.NotFoundException;
import com.shoutoutz.api.common.restdocs.RestDocsFields;
import com.shoutoutz.api.feed.application.FeedService;
import com.shoutoutz.api.feed.application.dto.FeedFindAllResult;
import com.shoutoutz.api.feed.application.dto.FeedItem;
import com.shoutoutz.api.feed.domain.FeedErrorCode;
import com.shoutoutz.api.feed.presentation.dto.request.FeedFindAllRequest;
import com.shoutoutz.api.feed.presentation.dto.request.FeedSaveRequest;
import com.shoutoutz.api.feed.presentation.dto.request.FeedSuggestionRequest;
import com.shoutoutz.api.feed.presentation.dto.request.FeedUpdateRequest;
import com.shoutoutz.api.feed.presentation.dto.response.FeedCommandResponse;
import com.shoutoutz.api.feed.presentation.dto.response.FeedResponse;
import com.shoutoutz.api.user.domain.account.UserRole;
import com.shoutoutz.api.user.domain.profile.UserType;
import com.shoutoutz.api.user.domain.profile.Track;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(controllers = FeedHttpApi.class)
@AutoConfigureRestDocs
class FeedHttpApiTest {

    private static final long USER_ID = 1L;
    private static final long FEED_ID = 10L;
    private static final Instant CREATED_AT = Instant.parse("2026-09-11T00:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FeedService feedService;

    @Test
    @DisplayName("최신순 피드 목록을 Slice 메타와 함께 조회한다")
    void findAllFeed() throws Exception {
        FeedItem feed = feedItem();
        given(feedService.findAllFeed(any(FeedFindAllRequest.class)))
                .willReturn(new FeedFindAllResult(
                        List.of(feed),
                        "next-cursor",
                        true,
                        mediaUrls()
                ));

        mockMvc.perform(get("/api/v1/feeds")
                        .queryParam("categoryId", "3")
                        .queryParam("sort", "LATEST")
                        .queryParam("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].feedId").value(FEED_ID))
                .andExpect(jsonPath("$.data[0].title").value("피드 제목"))
                .andExpect(jsonPath("$.data[0].author.avatarImageId").value(21L))
                .andExpect(jsonPath("$.data[0].media[0].mediaId").value(21L))
                .andExpect(jsonPath("$.meta.nextCursor").value("next-cursor"))
                .andExpect(jsonPath("$.meta.hasNext").value(true))
                .andDo(document(
                        "feed-find-all",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Feed")
                                .summary("피드 목록 조회")
                                .description("전체 공개 피드를 최신순, 인기순 또는 키워드 정확도순 Slice로 조회한다.")
                                .queryParameters(
                                        parameterWithName("sort")
                                                .description("LATEST, POPULAR 또는 RELEVANCE. 검색어가 없으면 기본 LATEST, 있으면 기본 RELEVANCE")
                                                .optional(),
                                        parameterWithName("categoryId")
                                                .type(INTEGER)
                                                .description("카테고리 ID")
                                                .optional(),
                                        parameterWithName("keyword")
                                                .description("제목과 본문 검색어(Unicode 최대 100자)")
                                                .optional(),
                                        parameterWithName("cursor")
                                                .description("같은 검색어, 카테고리, 정렬 조건의 다음 Slice 커서")
                                                .optional(),
                                        parameterWithName("size")
                                                .type(INTEGER)
                                                .description("조회 크기(기본 20, 최대 100)")
                                                .optional()
                                )
                                .responseSchema(Schema.schema("FeedFindAllSuccessResponse"))
                                .responseFields(feedListResponseFields("피드 목록"))
                                .build())
                ));
    }

    @Test
    @DisplayName("존재하지 않는 카테고리 ID로 조회하면 빈 목록을 반환한다")
    void findAllFeedByMissingCategoryId() throws Exception {
        given(feedService.findAllFeed(any(FeedFindAllRequest.class)))
                .willReturn(new FeedFindAllResult(List.of(), null, false));

        mockMvc.perform(get("/api/v1/feeds").queryParam("categoryId", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());

        verify(feedService).findAllFeed(any(FeedFindAllRequest.class));
    }

    @Test
    @DisplayName("피드 제목 자동완성 후보를 최대 10개 조회한다")
    void findTitleSuggestions() throws Exception {
        given(feedService.findTitleSuggestions(any(FeedSuggestionRequest.class)))
                .willReturn(List.of("우테코", "우테코 회고"));

        mockMvc.perform(get("/api/v1/feeds/search/suggestions")
                        .queryParam("keyword", "우테코")
                        .queryParam("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value("우테코"))
                .andExpect(jsonPath("$.data[1]").value("우테코 회고"))
                .andDo(document(
                        "feed-title-suggestions",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Feed")
                                .summary("피드 제목 자동완성")
                                .description("입력 중인 검색어와 일치하는 피드 제목을 최대 10개 반환한다.")
                                .queryParameters(
                                        parameterWithName("keyword")
                                                .description("자동완성 검색어(Unicode 2자 이상 100자 이하)"),
                                        parameterWithName("size")
                                                .type(INTEGER)
                                                .description("조회 크기(기본 10, 최대 10)")
                                                .optional()
                                )
                                .responseSchema(Schema.schema("FeedTitleSuggestionsSuccessResponse"))
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data").type(ARRAY)
                                                .description("피드 제목 자동완성 후보 문자열 목록")
                                )
                                .build())
                ));

        verify(feedService).findTitleSuggestions(any(FeedSuggestionRequest.class));
    }

    @Test
    @DisplayName("자동완성 검색어와 조회 크기가 범위를 벗어나면 400을 반환한다")
    void rejectInvalidTitleSuggestionRequest() throws Exception {
        mockMvc.perform(get("/api/v1/feeds/search/suggestions")
                        .queryParam("keyword", "한")
                        .queryParam("size", "11"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(feedService);
    }

    @Test
    @DisplayName("전체 공개 피드 상세를 조회한다")
    void findFeed() throws Exception {
        given(feedService.findFeed(FEED_ID)).willReturn(response());

        mockMvc.perform(get("/api/v1/feeds/{feedId}", FEED_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("피드 제목"))
                .andExpect(jsonPath("$.data.content").value("본문입니다."))
                .andDo(document(
                        "feed-find",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Feed")
                                .summary("피드 상세 조회")
                                .description("삭제되지 않은 전체 공개 피드를 조회한다.")
                                .pathParameters(parameterWithName("feedId")
                                        .type(INTEGER)
                                        .description("피드 ID"))
                                .responseSchema(Schema.schema("FeedFindSuccessResponse"))
                                .responseFields(successResponseFields("data."))
                                .build())
                ));
    }

    @Test
    @DisplayName("크루 또는 코치가 피드를 작성한다")
    void saveFeed() throws Exception {
        given(feedService.saveFeed(eq(USER_ID), any(FeedSaveRequest.class)))
                .willReturn(commandResponse());

        mockMvc.perform(post("/api/v1/feeds")
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "피드 제목",
                                  "content": "본문입니다.",
                                  "categoryIds": [3],
                                  "mediaIds": [21]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.feedId").value(FEED_ID))
                .andExpect(jsonPath("$.data.author.avatarImageId").doesNotExist())
                .andExpect(jsonPath("$.data.media[0].mediaId").doesNotExist())
                .andDo(document(
                        "feed-save",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Feed")
                                .summary("피드 작성")
                                .description("크루 또는 코치가 카테고리와 업로드 완료된 본문 이미지를 연결해 피드를 작성한다.")
                                .requestSchema(Schema.schema("FeedSaveRequest"))
                                .responseSchema(Schema.schema("FeedSaveSuccessResponse"))
                                .requestFields(
                                        fieldWithPath("title").type(STRING)
                                                .description("피드 제목(공백 제외 1자 이상, Unicode 최대 100자)"),
                                        fieldWithPath("content").type(STRING)
                                                .description("Markdown 본문(공백 제외 1자 이상, Unicode 최대 500자)"),
                                        fieldWithPath("categoryIds").type(ARRAY)
                                                .description("활성 카테고리 ID 목록(일반 1개, 이벤트 개수 제한 없음, 중복 불가)"),
                                        fieldWithPath("mediaIds").type(ARRAY)
                                                .description("작성자가 업로드한 READY FEED_CONTENT 미디어 ID 목록")
                                )
                                .responseFields(commandSuccessResponseFields("data."))
                                .build())
                ));

        verify(feedService).saveFeed(eq(USER_ID), any(FeedSaveRequest.class));
    }

    @Test
    @DisplayName("작성자가 피드 전체 내용을 수정한다")
    void updateFeed() throws Exception {
        given(feedService.updateFeed(eq(FEED_ID), eq(USER_ID), any(FeedUpdateRequest.class)))
                .willReturn(commandResponse());

        mockMvc.perform(put("/api/v1/feeds/{feedId}", FEED_ID)
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feedId").value(FEED_ID))
                .andExpect(jsonPath("$.data.author.avatarImageId").doesNotExist())
                .andExpect(jsonPath("$.data.media[0].mediaId").doesNotExist())
                .andDo(document(
                        "feed-update",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Feed")
                                .summary("피드 수정")
                                .description("작성자가 본문, 카테고리, 본문 미디어를 전체 교체한다.")
                                .pathParameters(parameterWithName("feedId")
                                        .type(INTEGER)
                                        .description("피드 ID"))
                                .requestSchema(Schema.schema("FeedUpdateRequest"))
                                .responseSchema(Schema.schema("FeedUpdateSuccessResponse"))
                                .requestFields(
                                        fieldWithPath("title").type(STRING).description("변경할 피드 제목"),
                                        fieldWithPath("content").type(STRING).description("변경할 Markdown 본문"),
                                        fieldWithPath("categoryIds").type(ARRAY)
                                                .description("변경할 카테고리 ID 목록(일반 1개, 이벤트 개수 제한 없음)"),
                                        fieldWithPath("mediaIds").type(ARRAY).description("변경할 본문 미디어 ID 목록")
                                )
                                .responseFields(commandSuccessResponseFields("data."))
                                .build())
                ));
    }

    @Test
    @DisplayName("작성자가 피드를 soft delete한다")
    void deleteFeed() throws Exception {
        mockMvc.perform(delete("/api/v1/feeds/{feedId}", FEED_ID)
                        .with(authenticated()))
                .andExpect(status().isNoContent())
                .andDo(document(
                        "feed-delete",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Feed")
                                .summary("피드 삭제")
                                .description("작성자가 피드를 soft delete한다.")
                                .pathParameters(parameterWithName("feedId")
                                        .type(INTEGER)
                                        .description("피드 ID"))
                                .build())
                ));

        verify(feedService).deleteFeed(FEED_ID, USER_ID);
    }

    @Test
    @DisplayName("인증 없이 피드 작성 요청을 하면 401을 반환한다")
    void rejectAnonymousCreate() throws Exception {
        mockMvc.perform(post("/api/v1/feeds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequest()))
                .andExpect(status().isUnauthorized())
                .andDo(document(
                        "feed-save-unauthorized",
                        resource(createErrorResponse(
                                "피드 작성",
                                "크루 또는 코치가 카테고리와 업로드 완료된 본문 이미지를 연결해 피드를 작성한다."
                        ))
                ));

        verifyNoInteractions(feedService);
    }

    @Test
    @DisplayName("본문이 공백이거나 Unicode 500자를 초과하면 400을 반환한다")
    void rejectInvalidContent() throws Exception {
        String overLimit = "😀".repeat(501);

        mockMvc.perform(post("/api/v1/feeds")
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"제목","content":"   ","categoryIds":[1],"mediaIds":[]}
                                """))
                .andExpect(status().isBadRequest())
                .andDo(document(
                        "feed-save-invalid",
                        resource(createErrorResponse(
                                "피드 작성",
                                "크루 또는 코치가 카테고리와 업로드 완료된 본문 이미지를 연결해 피드를 작성한다."
                        ))
                ));
        mockMvc.perform(post("/api/v1/feeds")
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"제목","content":"%s","categoryIds":[1],"mediaIds":[]}
                                """.formatted(overLimit)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(feedService);
    }

    @Test
    @DisplayName("제목이 공백이거나 Unicode 100자를 초과하면 400을 반환한다")
    void rejectInvalidTitle() throws Exception {
        String overLimit = "😀".repeat(101);

        mockMvc.perform(post("/api/v1/feeds")
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"   ","content":"본문","categoryIds":[1],"mediaIds":[]}
                                """))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/feeds")
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"%s","content":"본문","categoryIds":[1],"mediaIds":[]}
                                """.formatted(overLimit)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(feedService);
    }

    @Test
    @DisplayName("수정 본문이 공백이거나 Unicode 500자를 초과하면 400을 반환한다")
    void rejectInvalidUpdateContent() throws Exception {
        String overLimit = "😀".repeat(501);

        mockMvc.perform(put("/api/v1/feeds/{feedId}", FEED_ID)
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"제목\",\"content\":\"   \",\"categoryIds\":[1],\"mediaIds\":[]}"))
                .andExpect(status().isBadRequest())
                .andDo(document(
                        "feed-update-invalid",
                        resource(updateErrorResponse(
                                "피드 수정",
                                "작성자가 본문, 카테고리, 본문 미디어를 전체 교체한다."
                        ))
                ));
        mockMvc.perform(put("/api/v1/feeds/{feedId}", FEED_ID)
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"제목\",\"content\":\"%s\",\"categoryIds\":[1],\"mediaIds\":[]}".formatted(overLimit)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(feedService);
    }

    @Test
    @DisplayName("카테고리나 미디어 ID 목록이 비어 있거나 중복 또는 null이면 400을 반환한다")
    void rejectInvalidAssociations() throws Exception {
        mockMvc.perform(post("/api/v1/feeds")
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"제목","content":"본문","categoryIds":[1,1],"mediaIds":[]}
                                """))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/feeds")
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"제목","content":"본문","categoryIds":[null],"mediaIds":[]}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(feedService);
    }

    @Test
    @DisplayName("수정 필드가 누락되면 400을 반환한다")
    void rejectIncompleteUpdate() throws Exception {
        mockMvc.perform(put("/api/v1/feeds/{feedId}", FEED_ID)
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(feedService);
    }

    @Test
    @DisplayName("잘못된 목록 조건은 400을 반환한다")
    void rejectInvalidListRequest() throws Exception {
        mockMvc.perform(get("/api/v1/feeds")
                        .queryParam("sort", "INVALID")
                        .queryParam("size", "101"))
                .andExpect(status().isBadRequest())
                .andDo(document(
                        "feed-find-all-invalid",
                        resource(errorResponse(
                                "피드 목록 조회",
                                "전체 공개 피드를 최신순 또는 전체 기간 인기순 Slice로 조회한다."
                        ))
                ));

        verifyNoInteractions(feedService);
    }

    @Test
    @DisplayName("검색어가 Unicode 100자를 초과하면 400을 반환한다")
    void rejectOverlongKeyword() throws Exception {
        mockMvc.perform(get("/api/v1/feeds")
                        .queryParam("keyword", "가".repeat(101)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(feedService);
    }

    @Test
    @DisplayName("해석할 수 없는 목록 커서는 400을 반환한다")
    void rejectInvalidCursor() throws Exception {
        given(feedService.findAllFeed(any(FeedFindAllRequest.class)))
                .willThrow(new BadRequestException(FeedErrorCode.FEED_CURSOR_INVALID));

        mockMvc.perform(get("/api/v1/feeds").queryParam("cursor", "invalid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("조회 결과가 없는 피드 ID는 값의 범위와 관계없이 404를 반환한다")
    void rejectMissingFeedId() throws Exception {
        given(feedService.findFeed(0))
                .willThrow(new NotFoundException(FeedErrorCode.FEED_NOT_FOUND));

        mockMvc.perform(get("/api/v1/feeds/{feedId}", 0))
                .andExpect(status().isNotFound());

        verify(feedService).findFeed(0);
    }

    @Test
    @DisplayName("크루나 코치가 아니면 피드 작성 요청에 403을 반환한다")
    void rejectForbiddenWriter() throws Exception {
        given(feedService.saveFeed(eq(USER_ID), any(FeedSaveRequest.class)))
                .willThrow(new ForbiddenException(FeedErrorCode.FEED_WRITER_TYPE_FORBIDDEN));

        mockMvc.perform(post("/api/v1/feeds")
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                .content(validCreateRequest()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FEED_WRITER_TYPE_FORBIDDEN"))
                .andDo(document(
                        "feed-save-forbidden",
                        resource(createErrorResponse(
                                "피드 작성",
                                "크루 또는 코치가 카테고리와 업로드 완료된 본문 이미지를 연결해 피드를 작성한다."
                        ))
                ));
    }

    @Test
    @DisplayName("작성자가 아니면 수정과 삭제 요청에 403을 반환한다")
    void rejectNonAuthorMutation() throws Exception {
        given(feedService.updateFeed(eq(FEED_ID), eq(USER_ID), any(FeedUpdateRequest.class)))
                .willThrow(new ForbiddenException(FeedErrorCode.FEED_AUTHOR_FORBIDDEN));
        willThrow(new ForbiddenException(FeedErrorCode.FEED_AUTHOR_FORBIDDEN))
                .given(feedService).deleteFeed(FEED_ID, USER_ID);

        mockMvc.perform(put("/api/v1/feeds/{feedId}", FEED_ID)
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateRequest()))
                .andExpect(status().isForbidden())
                .andDo(document(
                        "feed-update-forbidden",
                        resource(updateErrorResponse(
                                "피드 수정",
                                "작성자가 본문, 카테고리, 본문 미디어를 전체 교체한다."
                        ))
                ));
        mockMvc.perform(delete("/api/v1/feeds/{feedId}", FEED_ID)
                        .with(authenticated()))
                .andExpect(status().isForbidden())
                .andDo(document(
                        "feed-delete-forbidden",
                        resource(pathErrorResponse(
                                "피드 삭제",
                                "작성자가 피드를 soft delete한다."
                        ))
                ));
    }

    @Test
    @DisplayName("인증 없이 피드 수정과 삭제를 요청하면 401을 반환한다")
    void rejectAnonymousMutation() throws Exception {
        mockMvc.perform(put("/api/v1/feeds/{feedId}", FEED_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateRequest()))
                .andExpect(status().isUnauthorized())
                .andDo(document(
                        "feed-update-unauthorized",
                        resource(updateErrorResponse(
                                "피드 수정",
                                "작성자가 본문, 카테고리, 본문 미디어를 전체 교체한다."
                        ))
                ));
        mockMvc.perform(delete("/api/v1/feeds/{feedId}", FEED_ID))
                .andExpect(status().isUnauthorized())
                .andDo(document(
                        "feed-delete-unauthorized",
                        resource(pathErrorResponse(
                                "피드 삭제",
                                "작성자가 피드를 soft delete한다."
                        ))
                ));

        verifyNoInteractions(feedService);
    }

    @Test
    @DisplayName("없는 피드 수정과 삭제 요청은 404를 반환한다")
    void rejectMissingFeedMutation() throws Exception {
        given(feedService.updateFeed(eq(FEED_ID), eq(USER_ID), any(FeedUpdateRequest.class)))
                .willThrow(new NotFoundException(FeedErrorCode.FEED_NOT_FOUND));
        willThrow(new NotFoundException(FeedErrorCode.FEED_NOT_FOUND))
                .given(feedService).deleteFeed(FEED_ID, USER_ID);

        mockMvc.perform(put("/api/v1/feeds/{feedId}", FEED_ID)
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateRequest()))
                .andExpect(status().isNotFound())
                .andDo(document(
                        "feed-update-not-found",
                        resource(updateErrorResponse(
                                "피드 수정",
                                "작성자가 본문, 카테고리, 본문 미디어를 전체 교체한다."
                        ))
                ));
        mockMvc.perform(delete("/api/v1/feeds/{feedId}", FEED_ID)
                        .with(authenticated()))
                .andExpect(status().isNotFound())
                .andDo(document(
                        "feed-delete-not-found",
                        resource(pathErrorResponse(
                                "피드 삭제",
                                "작성자가 피드를 soft delete한다."
                        ))
                ));
    }

    @Test
    @DisplayName("없는 범위의 피드 ID도 조회 결과에 따라 404를 반환한다")
    void rejectInvalidFeedIdMutation() throws Exception {
        given(feedService.updateFeed(eq(0L), eq(USER_ID), any(FeedUpdateRequest.class)))
                .willThrow(new NotFoundException(FeedErrorCode.FEED_NOT_FOUND));
        willThrow(new NotFoundException(FeedErrorCode.FEED_NOT_FOUND))
                .given(feedService).deleteFeed(0L, USER_ID);

        mockMvc.perform(put("/api/v1/feeds/{feedId}", 0)
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateRequest()))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/feeds/{feedId}", 0)
                        .with(authenticated()))
                .andExpect(status().isNotFound())
                .andDo(document(
                        "feed-delete-invalid",
                        resource(pathErrorResponse(
                                "피드 삭제",
                                "작성자가 피드를 soft delete한다."
                        ))
                ));

    }

    @Test
    @DisplayName("없는 피드 상세 조회는 404를 반환한다")
    void rejectMissingFeed() throws Exception {
        given(feedService.findFeed(FEED_ID))
                .willThrow(new NotFoundException(FeedErrorCode.FEED_NOT_FOUND));

        mockMvc.perform(get("/api/v1/feeds/{feedId}", FEED_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FEED_NOT_FOUND"))
                .andDo(document(
                        "feed-find-not-found",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Feed")
                                .summary("피드 상세 조회")
                                .description("삭제되지 않은 전체 공개 피드를 조회한다.")
                                .pathParameters(parameterWithName("feedId")
                                        .type(INTEGER)
                                        .description("피드 ID"))
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
                {"title":"피드 제목","content":"본문","categoryIds":[1],"mediaIds":[]}
                """;
    }

    private String validUpdateRequest() {
        return """
                {"title":"수정 제목","content":"수정 본문","categoryIds":[1],"mediaIds":[]}
                """;
    }

    private ResourceSnippetParameters errorResponse(String summary, String description) {
        return ResourceSnippetParameters.builder()
                .tag("Feed")
                .summary(summary)
                .description(description)
                .responseSchema(Schema.schema("ErrorResponse"))
                .responseFields(RestDocsFields.errorResponse())
                .build();
    }

    private ResourceSnippetParameters createErrorResponse(String summary, String description) {
        return ResourceSnippetParameters.builder()
                .tag("Feed")
                .summary(summary)
                .description(description)
                .requestSchema(Schema.schema("FeedSaveRequest"))
                .responseSchema(Schema.schema("ErrorResponse"))
                .responseFields(RestDocsFields.errorResponse())
                .build();
    }

    private ResourceSnippetParameters updateErrorResponse(String summary, String description) {
        return ResourceSnippetParameters.builder()
                .tag("Feed")
                .summary(summary)
                .description(description)
                .pathParameters(parameterWithName("feedId")
                        .type(INTEGER)
                        .description("피드 ID"))
                .requestSchema(Schema.schema("FeedUpdateRequest"))
                .responseSchema(Schema.schema("ErrorResponse"))
                .responseFields(RestDocsFields.errorResponse())
                .build();
    }

    private ResourceSnippetParameters pathErrorResponse(String summary, String description) {
        return ResourceSnippetParameters.builder()
                .tag("Feed")
                .summary(summary)
                .description(description)
                .pathParameters(parameterWithName("feedId")
                        .type(INTEGER)
                        .description("피드 ID"))
                .responseSchema(Schema.schema("ErrorResponse"))
                .responseFields(RestDocsFields.errorResponse())
                .build();
    }

    private FeedItem feedItem() {
        return new FeedItem(
                FEED_ID,
                "피드 제목",
                "본문입니다.",
                new FeedItem.Author(
                        "zzaekkii",
                        "재키",
                        UserType.WOOWACOURSE_CREW,
                        Track.BACKEND,
                        Cohort.COHORT_8,
                        21L
                ),
                List.of(new FeedItem.Category(
                        3L,
                        "backend",
                        "백엔드",
                        CategoryType.GENERAL
                )),
                List.of(new FeedItem.Media(21L, 0)),
                0L,
                0L,
                0,
                CREATED_AT,
                CREATED_AT
        );
    }

    private FeedResponse response() {
        return FeedResponse.from(feedItem(), mediaUrls());
    }

    private FeedCommandResponse commandResponse() {
        return FeedCommandResponse.from(feedItem(), mediaUrls());
    }

    private Map<Long, URI> mediaUrls() {
        return Map.of(21L, URI.create("https://cdn.example.com/media/21/display"));
    }

}
