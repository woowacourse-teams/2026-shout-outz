package com.shoutoutz.api.feed.presentation;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.SimpleType.INTEGER;
import static com.shoutoutz.api.feed.presentation.FeedRestDocsFields.userFeedListResponseFields;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.shoutoutz.api.category.domain.CategoryType;
import com.shoutoutz.api.cohort.domain.Cohort;
import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.restdocs.RestDocsFields;
import com.shoutoutz.api.feed.application.FeedService;
import com.shoutoutz.api.feed.application.dto.FeedFindAllResult;
import com.shoutoutz.api.feed.application.dto.FeedItem;
import com.shoutoutz.api.feed.domain.FeedErrorCode;
import com.shoutoutz.api.feed.presentation.dto.request.UserFeedFindRequest;
import com.shoutoutz.api.user.domain.account.UserErrorCode;
import com.shoutoutz.api.user.domain.profile.UserType;
import com.shoutoutz.api.user.domain.profile.Track;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@DisplayName("사용자 피드 목록 API")
@WebMvcTest(controllers = UserFeedHttpApi.class)
@AutoConfigureRestDocs
class UserFeedHttpApiTest {

    private static final String SUMMARY = "사용자 피드 목록 조회";
    private static final String DESCRIPTION = "handle로 사용자가 작성한 피드를 최신순으로 조회한다. "
            + "탈퇴한 사용자는 빈 목록을 반환하고, 정지된 사용자는 기존 피드를 공개한다. "
            + "로그인하지 않아도 조회할 수 있으며, 응답의 meta.nextCursor를 다음 요청에 그대로 전달한다.";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FeedService feedService;

    @Test
    @DisplayName("로그인하지 않아도 사용자가 작성한 피드를 조회한다")
    void findsUserFeeds() throws Exception {
        given(feedService.findAllByUser("zzaekkii", new UserFeedFindRequest("current-cursor", 20)))
                .willReturn(new FeedFindAllResult(List.of(feed()), "next-cursor", true));

        mockMvc.perform(get("/api/v1/users/{handle}/feeds", "zzaekkii")
                        .queryParam("cursor", "current-cursor")
                        .queryParam("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data[0].feedId").value(10))
                .andExpect(jsonPath("$.data[0].author.handle").value("zzaekkii"))
                .andExpect(jsonPath("$.data[0].categories[0].type").value("GENERAL"))
                .andExpect(jsonPath("$.data[0].media[0].mediaId").value(30))
                .andExpect(jsonPath("$.data[0].likeCount").value(5))
                .andExpect(jsonPath("$.data[0].commentCount").value(3))
                .andExpect(jsonPath("$.meta.nextCursor").value("next-cursor"))
                .andExpect(jsonPath("$.meta.hasNext").value(true))
                .andDo(document(
                        "user-feed-find-all",
                        resource(ResourceSnippetParameters.builder()
                                .tag("User")
                                .summary(SUMMARY)
                                .description(DESCRIPTION)
                                .pathParameters(
                                        parameterWithName("handle").description("조회할 사용자의 handle")
                                )
                                .queryParameters(
                                        parameterWithName("cursor")
                                                .description("다음 페이지 조회용 커서. 첫 요청은 생략")
                                                .optional(),
                                        parameterWithName("size")
                                                .type(INTEGER)
                                                .description("한 번에 가져올 피드 수. 기본값 20, 1~50")
                                                .optional()
                                )
                                .responseSchema(Schema.schema("UserFeedFindAllSuccessResponse"))
                                .responseFields(userFeedListResponseFields("사용자가 작성한 피드 목록"))
                                .build())
                ));

        verify(feedService).findAllByUser("zzaekkii", new UserFeedFindRequest("current-cursor", 20));
    }

    @Test
    @DisplayName("파라미터를 생략하면 기본 조회 조건을 사용한다")
    void usesDefaultParameters() throws Exception {
        given(feedService.findAllByUser("zzaekkii", new UserFeedFindRequest(null, null)))
                .willReturn(new FeedFindAllResult(List.of(), null, false));

        mockMvc.perform(get("/api/v1/users/{handle}/feeds", "zzaekkii"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.meta.nextCursor").doesNotExist())
                .andExpect(jsonPath("$.meta.hasNext").value(false));

        verify(feedService).findAllByUser("zzaekkii", new UserFeedFindRequest(null, null));
    }

    @Test
    @DisplayName("handle 형식이 올바르지 않으면 조회할 수 없다")
    void rejectsInvalidHandle() throws Exception {
        mockMvc.perform(get("/api/v1/users/{handle}/feeds", "잘못된-핸들"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(feedService);
    }

    @Test
    @DisplayName("조회 개수가 범위를 벗어나면 조회할 수 없다")
    void rejectsInvalidSize() throws Exception {
        mockMvc.perform(get("/api/v1/users/{handle}/feeds", "zzaekkii")
                        .queryParam("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(feedService);
    }

    @Test
    @DisplayName("커서가 올바르지 않으면 조회할 수 없다")
    void rejectsInvalidCursor() throws Exception {
        given(feedService.findAllByUser("zzaekkii", new UserFeedFindRequest("broken", null)))
                .willThrow(new BadRequestException(FeedErrorCode.FEED_CURSOR_INVALID));

        mockMvc.perform(get("/api/v1/users/{handle}/feeds", "zzaekkii")
                        .queryParam("cursor", "broken"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FEED_CURSOR_INVALID"))
                .andDo(document("user-feed-find-all-invalid-cursor", resource(errorResource())));
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 피드는 조회할 수 없다")
    void rejectsUnknownUser() throws Exception {
        given(feedService.findAllByUser("missing-user", new UserFeedFindRequest(null, null)))
                .willThrow(new EntityNotFoundException(UserErrorCode.USER_NOT_FOUND));

        mockMvc.perform(get("/api/v1/users/{handle}/feeds", "missing-user"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
                .andDo(document("user-feed-find-all-not-found", resource(errorResource())));
    }

    private static FeedItem feed() {
        Instant now = Instant.parse("2026-09-16T00:00:00Z");
        return new FeedItem(
                10L,
                "사용자 피드 본문",
                new FeedItem.Author(
                        "zzaekkii",
                        "재키",
                        UserType.WOOWACOURSE_CREW,
                        Track.BACKEND,
                        Cohort.COHORT_8,
                        20L
                ),
                List.of(new FeedItem.Category(1L, "backend", "백엔드", CategoryType.GENERAL)),
                List.of(new FeedItem.Media(30L, 0)),
                5L,
                3L,
                now,
                now
        );
    }

    private static ResourceSnippetParameters errorResource() {
        return ResourceSnippetParameters.builder()
                .tag("User")
                .summary(SUMMARY)
                .description(DESCRIPTION)
                .pathParameters(
                        parameterWithName("handle").description("조회할 사용자의 handle")
                )
                .responseSchema(Schema.schema("ErrorResponse"))
                .responseFields(RestDocsFields.errorResponse())
                .build();
    }
}
