package com.shoutoutz.api.news.presentation;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.BDDMockito.given;
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
import com.shoutoutz.api.common.exception.code.CommonErrorCode;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.InvalidInputException;
import com.shoutoutz.api.common.restdocs.RestDocsFields;
import com.shoutoutz.api.news.application.NewsReactionService;
import com.shoutoutz.api.news.domain.NewsErrorCode;
import com.shoutoutz.api.news.domain.NewsReactionType;
import com.shoutoutz.api.news.presentation.dto.response.NewsReactionResponse;
import com.shoutoutz.api.user.domain.account.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = NewsReactionHttpApi.class)
@AutoConfigureRestDocs
class NewsReactionHttpApiTest {

    private static final long USER_ID = 1L;
    private static final long NEWS_ID = 100L;
    private static final String AUTHENTICATED_SESSION_ATTRIBUTE = AuthenticatedSession.class.getName();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NewsReactionService newsReactionService;

    @Test
    void 소식_좋아요를_추가한다() throws Exception {
        given(newsReactionService.add(NEWS_ID, USER_ID, "LIKE"))
                .willReturn(new NewsReactionResponse(NEWS_ID, NewsReactionType.LIKE, true, 21L));

        mockMvc.perform(put("/api/v1/news/{newsId}/reactions/{type}", NEWS_ID, "LIKE")
                        .with(authenticated())
                        .header("X-CSRF-Token", "csrf-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.newsId").value(NEWS_ID))
                .andExpect(jsonPath("$.data.type").value("LIKE"))
                .andExpect(jsonPath("$.data.active").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(21))
                .andDo(document(
                        "news-reaction-add",
                        resource(ResourceSnippetParameters.builder()
                                .tag("News Reaction")
                                .summary("소식 반응 추가")
                                .description("현재 로그인 사용자가 삭제되지 않은 소식에 type 반응을 추가한다. 현재는 LIKE만 지원하며, 이미 존재하는 반응이면 현재 상태를 반환한다.")
                                .pathParameters(
                                        parameterWithName("newsId").description("소식 ID"),
                                        parameterWithName("type").description("반응 타입. 현재 LIKE만 지원한다.")
                                )
                                .requestHeaders(
                                        headerWithName("X-CSRF-Token").description("세션 조회로 발급받은 CSRF 토큰")
                                )
                                .responseSchema(Schema.schema("NewsReactionSuccessResponse"))
                                .responseFields(successResponseFields())
                                .build())
                ));

        verify(newsReactionService).add(NEWS_ID, USER_ID, "LIKE");
    }

    @Test
    void 소식_좋아요를_제거한다() throws Exception {
        given(newsReactionService.remove(NEWS_ID, USER_ID, "LIKE"))
                .willReturn(new NewsReactionResponse(NEWS_ID, NewsReactionType.LIKE, false, 20L));

        mockMvc.perform(delete("/api/v1/news/{newsId}/reactions/{type}", NEWS_ID, "LIKE")
                        .with(authenticated())
                        .header("X-CSRF-Token", "csrf-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.newsId").value(NEWS_ID))
                .andExpect(jsonPath("$.data.type").value("LIKE"))
                .andExpect(jsonPath("$.data.active").value(false))
                .andExpect(jsonPath("$.data.likeCount").value(20))
                .andDo(document(
                        "news-reaction-remove",
                        resource(ResourceSnippetParameters.builder()
                                .tag("News Reaction")
                                .summary("소식 반응 제거")
                                .description("현재 로그인 사용자가 삭제되지 않은 소식의 type 반응을 제거한다. 현재는 LIKE만 지원하며, 반응이 없어도 현재 상태를 반환한다.")
                                .pathParameters(
                                        parameterWithName("newsId").description("소식 ID"),
                                        parameterWithName("type").description("반응 타입. 현재 LIKE만 지원한다.")
                                )
                                .requestHeaders(
                                        headerWithName("X-CSRF-Token").description("세션 조회로 발급받은 CSRF 토큰")
                                )
                                .responseSchema(Schema.schema("NewsReactionSuccessResponse"))
                                .responseFields(successResponseFields())
                                .build())
                ));

        verify(newsReactionService).remove(NEWS_ID, USER_ID, "LIKE");
    }

    @Test
    void 지원하지_않는_소식_반응_타입은_400을_반환한다() throws Exception {
        given(newsReactionService.add(NEWS_ID, USER_ID, "BOOKMARK"))
                .willThrow(new InvalidInputException(NewsErrorCode.REACTION_TYPE_INVALID));

        mockMvc.perform(put("/api/v1/news/{newsId}/reactions/{type}", NEWS_ID, "BOOKMARK")
                        .with(authenticated())
                        .header("X-CSRF-Token", "csrf-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(NewsErrorCode.REACTION_TYPE_INVALID.name()))
                .andDo(document(
                        "news-reaction-add-invalid-type",
                        resource(ResourceSnippetParameters.builder()
                                .tag("News Reaction")
                                .summary("소식 반응 추가")
                                .description("현재 로그인 사용자가 삭제되지 않은 소식에 type 반응을 추가한다. 현재는 LIKE만 지원하며, 이미 존재하는 반응이면 현재 상태를 반환한다.")
                                .pathParameters(
                                        parameterWithName("newsId").description("소식 ID"),
                                        parameterWithName("type").description("반응 타입. 현재 LIKE만 지원한다.")
                                )
                                .requestHeaders(
                                        headerWithName("X-CSRF-Token").description("세션 조회로 발급받은 CSRF 토큰")
                                )
                                .responseSchema(Schema.schema("ErrorResponse"))
                                .responseFields(RestDocsFields.errorResponse())
                                .build())
                ));

        verify(newsReactionService).add(NEWS_ID, USER_ID, "BOOKMARK");
    }

    @Test
    void 로그인하지_않으면_소식_좋아요를_변경할_수_없다() throws Exception {
        mockMvc.perform(put("/api/v1/news/{newsId}/reactions/{type}", NEWS_ID, "LIKE"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.UNAUTHORIZED.name()))
                .andDo(document(
                        "news-reaction-add-unauthorized",
                        resource(ResourceSnippetParameters.builder()
                                .tag("News Reaction")
                                .summary("소식 반응 추가")
                                .description("현재 로그인 사용자가 삭제되지 않은 소식에 type 반응을 추가한다. 현재는 LIKE만 지원하며, 이미 존재하는 반응이면 현재 상태를 반환한다.")
                                .pathParameters(
                                        parameterWithName("newsId").description("소식 ID"),
                                        parameterWithName("type").description("반응 타입. 현재 LIKE만 지원한다.")
                                )
                                .responseSchema(Schema.schema("ErrorResponse"))
                                .responseFields(RestDocsFields.errorResponse())
                                .build())
                ));

        mockMvc.perform(delete("/api/v1/news/{newsId}/reactions/{type}", NEWS_ID, "LIKE"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.UNAUTHORIZED.name()))
                .andDo(document(
                        "news-reaction-remove-unauthorized",
                        resource(ResourceSnippetParameters.builder()
                                .tag("News Reaction")
                                .summary("소식 반응 제거")
                                .description("현재 로그인 사용자가 삭제되지 않은 소식의 type 반응을 제거한다. 현재는 LIKE만 지원하며, 반응이 없어도 현재 상태를 반환한다.")
                                .pathParameters(
                                        parameterWithName("newsId").description("소식 ID"),
                                        parameterWithName("type").description("반응 타입. 현재 LIKE만 지원한다.")
                                )
                                .responseSchema(Schema.schema("ErrorResponse"))
                                .responseFields(RestDocsFields.errorResponse())
                                .build())
                ));

        verifyNoInteractions(newsReactionService);
    }

    @Test
    void 삭제된_소식에_좋아요를_추가하면_404를_반환한다() throws Exception {
        given(newsReactionService.add(NEWS_ID, USER_ID, "LIKE"))
                .willThrow(new EntityNotFoundException(NewsErrorCode.NEWS_NOT_FOUND));

        mockMvc.perform(put("/api/v1/news/{newsId}/reactions/{type}", NEWS_ID, "LIKE")
                        .with(authenticated())
                        .header("X-CSRF-Token", "csrf-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(NewsErrorCode.NEWS_NOT_FOUND.name()))
                .andDo(document(
                        "news-reaction-add-not-found",
                        resource(ResourceSnippetParameters.builder()
                                .tag("News Reaction")
                                .summary("소식 반응 추가")
                                .description("현재 로그인 사용자가 삭제되지 않은 소식에 type 반응을 추가한다. 현재는 LIKE만 지원하며, 이미 존재하는 반응이면 현재 상태를 반환한다.")
                                .pathParameters(
                                        parameterWithName("newsId").description("소식 ID"),
                                        parameterWithName("type").description("반응 타입. 현재 LIKE만 지원한다.")
                                )
                                .requestHeaders(
                                        headerWithName("X-CSRF-Token").description("세션 조회로 발급받은 CSRF 토큰")
                                )
                                .responseSchema(Schema.schema("ErrorResponse"))
                                .responseFields(RestDocsFields.errorResponse())
                                .build())
                ));

        verify(newsReactionService).add(NEWS_ID, USER_ID, "LIKE");
    }

    @Test
    void 삭제된_소식에서_좋아요를_제거하면_404를_반환한다() throws Exception {
        given(newsReactionService.remove(NEWS_ID, USER_ID, "LIKE"))
                .willThrow(new EntityNotFoundException(NewsErrorCode.NEWS_NOT_FOUND));

        mockMvc.perform(delete("/api/v1/news/{newsId}/reactions/{type}", NEWS_ID, "LIKE")
                        .with(authenticated())
                        .header("X-CSRF-Token", "csrf-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(NewsErrorCode.NEWS_NOT_FOUND.name()))
                .andDo(document(
                        "news-reaction-remove-not-found",
                        resource(ResourceSnippetParameters.builder()
                                .tag("News Reaction")
                                .summary("소식 반응 제거")
                                .description("현재 로그인 사용자가 삭제되지 않은 소식의 type 반응을 제거한다. 현재는 LIKE만 지원하며, 반응이 없어도 현재 상태를 반환한다.")
                                .pathParameters(
                                        parameterWithName("newsId").description("소식 ID"),
                                        parameterWithName("type").description("반응 타입. 현재 LIKE만 지원한다.")
                                )
                                .requestHeaders(
                                        headerWithName("X-CSRF-Token").description("세션 조회로 발급받은 CSRF 토큰")
                                )
                                .responseSchema(Schema.schema("ErrorResponse"))
                                .responseFields(RestDocsFields.errorResponse())
                                .build())
                ));

        verify(newsReactionService).remove(NEWS_ID, USER_ID, "LIKE");
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor authenticated() {
        return request -> {
            request.setAttribute(
                    AUTHENTICATED_SESSION_ATTRIBUTE,
                    new AuthenticatedSession(USER_ID, UserRole.USER)
            );
            return request;
        };
    }

    private org.springframework.restdocs.payload.FieldDescriptor[] successResponseFields() {
        return new org.springframework.restdocs.payload.FieldDescriptor[]{
                fieldWithPath("status").type(STRING).description("응답 상태"),
                fieldWithPath("data").type(OBJECT).description("소식 반응 변경 결과"),
                fieldWithPath("data.newsId").type(NUMBER).description("소식 ID"),
                fieldWithPath("data.type").type(STRING).description("반응 타입. 현재 LIKE만 지원하며 향후 확장할 수 있다."),
                fieldWithPath("data.active").type(BOOLEAN).description("요청한 반응의 활성 상태"),
                fieldWithPath("data.likeCount").type(NUMBER).description("소식 좋아요 수")
        };
    }
}
