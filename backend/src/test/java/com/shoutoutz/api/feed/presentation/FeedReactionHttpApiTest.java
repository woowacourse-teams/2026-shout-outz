package com.shoutoutz.api.feed.presentation;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.SimpleType.INTEGER;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import com.shoutoutz.api.common.exception.custom.InvalidInputException;
import com.shoutoutz.api.common.exception.code.CommonErrorCode;
import com.shoutoutz.api.feed.application.FeedReactionService;
import com.shoutoutz.api.feed.domain.FeedErrorCode;
import com.shoutoutz.api.feed.domain.FeedReactionType;
import com.shoutoutz.api.feed.presentation.dto.response.FeedReactionResponse;
import com.shoutoutz.api.user.domain.account.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(controllers = FeedReactionHttpApi.class)
@AutoConfigureRestDocs
class FeedReactionHttpApiTest {

    private static final long USER_ID = 1L;
    private static final long FEED_ID = 10L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FeedReactionService feedReactionService;

    @Test
    void 피드_좋아요를_추가한다() throws Exception {
        given(feedReactionService.add(FEED_ID, USER_ID, "LIKE"))
                .willReturn(new FeedReactionResponse(FEED_ID, FeedReactionType.LIKE, true, 12L, 3L));

        mockMvc.perform(put("/api/v1/feeds/{feedId}/reactions/{type}", FEED_ID, "LIKE")
                        .with(authenticated()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.feedId").value(FEED_ID))
                .andExpect(jsonPath("$.data.type").value("LIKE"))
                .andExpect(jsonPath("$.data.active").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(12))
                .andExpect(jsonPath("$.data.bookmarkCount").value(3))
                .andDo(document(
                        "feed-reaction-add",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Feed Reaction")
                                .summary("피드 반응 추가")
                                .description("현재 로그인 사용자가 피드에 좋아요 또는 북마크를 추가한다. 이미 존재하는 반응이면 현재 상태를 반환한다.")
                                .pathParameters(
                                        parameterWithName("feedId").type(INTEGER).description("피드 ID"),
                                        parameterWithName("type").description("반응 타입(LIKE 또는 BOOKMARK)")
                                )
                                .responseSchema(Schema.schema("FeedReactionSuccessResponse"))
                                .responseFields(successResponseFields())
                                .build())
                ));

        verify(feedReactionService).add(FEED_ID, USER_ID, "LIKE");
    }

    @Test
    void 피드_북마크를_제거한다() throws Exception {
        given(feedReactionService.remove(FEED_ID, USER_ID, "BOOKMARK"))
                .willReturn(new FeedReactionResponse(FEED_ID, FeedReactionType.BOOKMARK, false, 11L, 3L));

        mockMvc.perform(delete("/api/v1/feeds/{feedId}/reactions/{type}", FEED_ID, "BOOKMARK")
                        .with(authenticated()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("BOOKMARK"))
                .andExpect(jsonPath("$.data.active").value(false))
                .andExpect(jsonPath("$.data.likeCount").value(11))
                .andExpect(jsonPath("$.data.bookmarkCount").value(3))
                .andDo(document(
                        "feed-reaction-remove",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Feed Reaction")
                                .summary("피드 반응 제거")
                                .description("현재 로그인 사용자가 피드의 좋아요 또는 북마크를 제거한다. 반응이 없어도 현재 상태를 반환한다.")
                                .pathParameters(
                                        parameterWithName("feedId").type(INTEGER).description("피드 ID"),
                                        parameterWithName("type").description("반응 타입(LIKE 또는 BOOKMARK)")
                                )
                                .responseSchema(Schema.schema("FeedReactionSuccessResponse"))
                                .responseFields(successResponseFields())
                                .build())
                ));

        verify(feedReactionService).remove(FEED_ID, USER_ID, "BOOKMARK");
    }

    @Test
    void 지원하지_않는_반응_타입은_400을_반환한다() throws Exception {
        willThrow(new InvalidInputException(FeedErrorCode.REACTION_TYPE_INVALID))
                .given(feedReactionService).add(FEED_ID, USER_ID, "AGREE");

        mockMvc.perform(put("/api/v1/feeds/{feedId}/reactions/{type}", FEED_ID, "AGREE")
                        .with(authenticated()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REACTION_TYPE_INVALID"));
    }

    @Test
    void 로그인하지_않으면_반응을_변경할_수_없다() throws Exception {
        mockMvc.perform(put("/api/v1/feeds/{feedId}/reactions/{type}", FEED_ID, "LIKE"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.UNAUTHORIZED.name()));

        verifyNoInteractions(feedReactionService);
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

    private org.springframework.restdocs.payload.FieldDescriptor[] successResponseFields() {
        return new org.springframework.restdocs.payload.FieldDescriptor[]{
                fieldWithPath("status").type(STRING).description("응답 상태"),
                fieldWithPath("data").type(OBJECT).description("피드 반응 변경 결과"),
                fieldWithPath("data.feedId").type(NUMBER).description("피드 ID"),
                fieldWithPath("data.type").type(STRING).description("반응 타입(LIKE 또는 BOOKMARK)"),
                fieldWithPath("data.active").type(BOOLEAN).description("요청한 반응의 활성 상태"),
                fieldWithPath("data.likeCount").type(NUMBER).description("피드 좋아요 수"),
                fieldWithPath("data.bookmarkCount").type(NUMBER).description("피드 북마크 수")
        };
    }
}
