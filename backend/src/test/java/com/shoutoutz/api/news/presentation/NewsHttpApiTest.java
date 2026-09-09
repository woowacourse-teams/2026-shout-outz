package com.shoutoutz.api.news.presentation;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.BOOLEAN;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.OBJECT;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.shoutoutz.api.common.exception.custom.DomainValidationException;
import com.shoutoutz.api.common.restdocs.RestDocsFields;
import com.shoutoutz.api.news.application.NewsFindAllQuery;
import com.shoutoutz.api.news.application.NewsService;
import com.shoutoutz.api.news.domain.EventStatus;
import com.shoutoutz.api.news.domain.NewsErrorCode;
import com.shoutoutz.api.news.domain.NewsType;
import com.shoutoutz.api.news.presentation.dto.request.EventCreateRequest;
import com.shoutoutz.api.news.presentation.dto.request.NoticeCreateRequest;
import com.shoutoutz.api.news.presentation.dto.response.EventCreateResponse;
import com.shoutoutz.api.news.presentation.dto.response.NewsFindAllResponse;
import com.shoutoutz.api.news.presentation.dto.response.NoticeCreateResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 컨트롤러 슬라이스 테스트
 */
@WebMvcTest(controllers = NewsHttpApi.class)
@AutoConfigureRestDocs
class NewsHttpApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NewsService newsService;

    /**
     * DOCS: /api/v1/news/notices
     */
    @Test
    @DisplayName("공지 등록 성공 테스트. 201과 생성된 공지 정보를 반환한다.")
    void returnsSuccessResponseAndCreatedStatusWhenNoticeIsCreated() throws Exception {
        NoticeCreateResponse response = new NoticeCreateResponse(
                106L,
                NewsType.NOTICE,
                "데모데이 안내",
                "데모데이 일정을 안내합니다.",
                "2026년 9월 12일에 데모데이를 진행합니다.",
                new NoticeCreateResponse.Author(1L, "샤라웃 운영팀"),
                Instant.parse("2026-09-05T00:00:00Z"),
                false,
                null,
                new NoticeCreateResponse.Cta("일정 확인", "example.com")
        );
        given(newsService.createNotice(any(NoticeCreateRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/v1/news/notices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJsonWithCta()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(106))
                .andExpect(jsonPath("$.data.type").value("NOTICE"))
                .andExpect(jsonPath("$.data.author.userId").value(1))
                .andExpect(jsonPath("$.data.author.name").value("샤라웃 운영팀"))
                .andExpect(jsonPath("$.data.isPinned").value(false))
                .andExpect(jsonPath("$.data.pinOrder").value(nullValue()))
                .andExpect(jsonPath("$.data.cta.label").value("일정 확인"))
                .andExpect(jsonPath("$.data.cta.url").value("example.com"))
                .andDo(document(
                        "news-notice-create",
                        resource(ResourceSnippetParameters.builder()
                                .tag("News")
                                .summary("공지 생성")
                                .description("공지와 선택적인 CTA를 생성한다.")
                                .requestSchema(Schema.schema("NoticeCreateRequest"))
                                .responseSchema(Schema.schema("NoticeCreateSuccessResponse"))
                                .requestFields(
                                        fieldWithPath("title")
                                                .type(STRING)
                                                .description("공지 제목"),
                                        fieldWithPath("summary")
                                                .type(STRING)
                                                .description("공지 요약"),
                                        fieldWithPath("body")
                                                .type(STRING)
                                                .description("공지 본문"),
                                        fieldWithPath("authorName")
                                                .type(STRING)
                                                .description("공지 작성자 이름"),
                                        fieldWithPath("cta")
                                                .type(OBJECT)
                                                .description("공지 CTA")
                                                .optional(),
                                        fieldWithPath("cta.label")
                                                .type(STRING)
                                                .description("CTA 라벨"),
                                        fieldWithPath("cta.url")
                                                .type(STRING)
                                                .description("CTA URL")
                                )
                                .responseFields(
                                        fieldWithPath("status")
                                                .type(STRING)
                                                .description("응답 상태"),
                                        fieldWithPath("data.id")
                                                .type(NUMBER)
                                                .description("공지 ID"),
                                        fieldWithPath("data.type")
                                                .type(STRING)
                                                .description("공지 유형"),
                                        fieldWithPath("data.title")
                                                .type(STRING)
                                                .description("공지 제목"),
                                        fieldWithPath("data.summary")
                                                .type(STRING)
                                                .description("공지 요약"),
                                        fieldWithPath("data.body")
                                                .type(STRING)
                                                .description("공지 본문"),
                                        fieldWithPath("data.author")
                                                .type(OBJECT)
                                                .description("공지 작성자"),
                                        fieldWithPath("data.author.userId")
                                                .type(NUMBER)
                                                .description("작성자 ID"),
                                        fieldWithPath("data.author.name")
                                                .type(STRING)
                                                .description("작성자 이름"),
                                        fieldWithPath("data.publishedAt")
                                                .type(STRING)
                                                .description("게시 시각"),
                                        fieldWithPath("data.isPinned")
                                                .type(BOOLEAN)
                                                .description("고정 여부"),
                                        fieldWithPath("data.pinOrder")
                                                .type(NUMBER)
                                                .description("고정 순서")
                                                .optional(),
                                        fieldWithPath("data.cta")
                                                .type(OBJECT)
                                                .description("공지 CTA")
                                                .optional(),
                                        fieldWithPath("data.cta.label")
                                                .type(STRING)
                                                .description("CTA 라벨"),
                                        fieldWithPath("data.cta.url")
                                                .type(STRING)
                                                .description("CTA URL")
                                )
                                .build())
                ));

        verify(newsService).createNotice(any(NoticeCreateRequest.class));
    }

    @Test
    @DisplayName("공지 등록 성공 테스트. CTA가 없는 경우 cta 항목을 null로 반환한다.")
    void returnsNullCtaWhenCtaIsAbsent() throws Exception {
        NoticeCreateResponse response = new NoticeCreateResponse(
                107L,
                NewsType.NOTICE,
                "서비스 점검 안내",
                "점검 일정을 안내합니다.",
                "2026년 9월 10일에 점검을 진행합니다.",
                new NoticeCreateResponse.Author(1L, "샤라웃 운영팀"),
                Instant.parse("2026-09-05T00:00:00Z"),
                false,
                null,
                null
        );
        given(newsService.createNotice(any(NoticeCreateRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/v1/news/notices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJsonWithoutCta()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.cta").value(nullValue()))
                .andExpect(jsonPath("$.data.pinOrder").value(nullValue()));
    }

    /**
     * DOCS: /api/v1/news/events
     */
    @Test
    @DisplayName("이벤트 등록 성공 테스트. 201과 생성된 이벤트 정보를 반환한다.")
    void returnsEventResponseAndCreatedStatusWhenEventIsCreated() throws Exception {
        EventCreateResponse response = new EventCreateResponse(
                107L,
                NewsType.EVENT,
                "프로젝트 아카이빙 챌린지",
                "팀 프로젝트를 등록하고 피드백을 받아보세요.",
                "프로젝트를 등록하면 동료 크루들의 피드백을 받을 수 있습니다.",
                new EventCreateResponse.Author(1L, "샤라웃 운영팀"),
                Instant.parse("2026-09-05T00:00:00Z"),
                EventStatus.ONGOING,
                Instant.parse("2026-09-01T00:00:00Z"),
                Instant.parse("2026-09-30T23:59:59Z"),
                false,
                null,
                new EventCreateResponse.Cta("프로젝트 등록하기", "/projects/3001")
        );
        given(newsService.createEvent(any(EventCreateRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/v1/news/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventRequestJsonWithCta()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(107))
                .andExpect(jsonPath("$.data.type").value("EVENT"))
                .andExpect(jsonPath("$.data.eventStatus").value("ONGOING"))
                .andExpect(jsonPath("$.data.eventStartAt").value("2026-09-01T00:00:00Z"))
                .andExpect(jsonPath("$.data.eventEndAt").value("2026-09-30T23:59:59Z"))
                .andExpect(jsonPath("$.data.isPinned").value(false))
                .andExpect(jsonPath("$.data.pinOrder").value(nullValue()))
                .andExpect(jsonPath("$.data.cta.label").value("프로젝트 등록하기"))
                .andExpect(jsonPath("$.data.cta.url").value("/projects/3001"))
                .andDo(document(
                        "news-event-create",
                        resource(ResourceSnippetParameters.builder()
                                .tag("News")
                                .summary("이벤트 생성")
                                .description("이벤트와 선택적인 CTA를 생성한다.")
                                .requestSchema(Schema.schema("EventCreateRequest"))
                                .responseSchema(Schema.schema("EventCreateSuccessResponse"))
                                .requestFields(
                                        fieldWithPath("title").type(STRING).description("이벤트 제목"),
                                        fieldWithPath("summary").type(STRING).description("이벤트 요약"),
                                        fieldWithPath("body").type(STRING).description("이벤트 본문"),
                                        fieldWithPath("authorName").type(STRING).description("이벤트 작성자 이름"),
                                        fieldWithPath("eventStartAt").type(STRING).description("이벤트 시작 시각"),
                                        fieldWithPath("eventEndAt").type(STRING).description("이벤트 종료 시각"),
                                        fieldWithPath("cta").type(OBJECT).description("이벤트 CTA").optional(),
                                        fieldWithPath("cta.label").type(STRING).description("CTA 라벨"),
                                        fieldWithPath("cta.url").type(STRING).description("CTA URL")
                                )
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data.id").type(NUMBER).description("이벤트 ID"),
                                        fieldWithPath("data.type").type(STRING).description("소식 유형"),
                                        fieldWithPath("data.title").type(STRING).description("이벤트 제목"),
                                        fieldWithPath("data.summary").type(STRING).description("이벤트 요약"),
                                        fieldWithPath("data.body").type(STRING).description("이벤트 본문"),
                                        fieldWithPath("data.author").type(OBJECT).description("이벤트 작성자"),
                                        fieldWithPath("data.author.userId").type(NUMBER).description("작성자 ID"),
                                        fieldWithPath("data.author.name").type(STRING).description("작성자 이름"),
                                        fieldWithPath("data.publishedAt").type(STRING).description("게시 시각"),
                                        fieldWithPath("data.eventStatus").type(STRING).description("이벤트 상태"),
                                        fieldWithPath("data.eventStartAt").type(STRING).description("이벤트 시작 시각"),
                                        fieldWithPath("data.eventEndAt").type(STRING).description("이벤트 종료 시각"),
                                        fieldWithPath("data.isPinned").type(BOOLEAN).description("고정 여부"),
                                        fieldWithPath("data.pinOrder").type(NUMBER).description("고정 순서").optional(),
                                        fieldWithPath("data.cta").type(OBJECT).description("이벤트 CTA").optional(),
                                        fieldWithPath("data.cta.label").type(STRING).description("CTA 라벨"),
                                        fieldWithPath("data.cta.url").type(STRING).description("CTA URL")
                                )
                                .build())
                ));

        verify(newsService).createEvent(any(EventCreateRequest.class));
    }

    @Test
    @DisplayName("이벤트 등록 성공 테스트. CTA가 없는 경우 cta 항목을 null로 반환한다.")
    void createsEventWithoutCta() throws Exception {
        EventCreateResponse response = new EventCreateResponse(
                108L,
                NewsType.EVENT,
                "서비스 이벤트",
                "이벤트 요약",
                "이벤트 본문",
                new EventCreateResponse.Author(1L, "샤라웃 운영팀"),
                Instant.parse("2026-09-05T00:00:00Z"),
                EventStatus.UPCOMING,
                Instant.parse("2026-10-01T00:00:00Z"),
                Instant.parse("2026-10-31T23:59:59Z"),
                false,
                null,
                null
        );
        given(newsService.createEvent(any(EventCreateRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/v1/news/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventRequestJsonWithoutCta()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.type").value("EVENT"))
                .andExpect(jsonPath("$.data.cta").value(nullValue()))
                .andExpect(jsonPath("$.data.isPinned").value(false))
                .andExpect(jsonPath("$.data.pinOrder").value(nullValue()));
    }

    /**
     * DOCS: /api/v1/news
     */
    @Test
    @DisplayName("소식 목록 조회 요청이 성공하면 최신순 목록과 페이지네이션 정보를 반환한다")
    void returnsNewsListWithPaginationMetadata() throws Exception {
        NewsFindAllResponse response = new NewsFindAllResponse(
                List.of(new NewsFindAllResponse.Item(
                        102L,
                        NewsType.EVENT,
                        "프로젝트 아카이빙 챌린지",
                        "팀 프로젝트를 등록하고 피드백을 받아보세요.",
                        Instant.parse("2026-08-25T00:00:00Z"),
                        EventStatus.ONGOING,
                        Instant.parse("2026-08-20T00:00:00Z"),
                        Instant.parse("2026-09-20T14:59:59Z"),
                        false,
                        null
                )),
                new NewsFindAllResponse.Meta(null, false)
        );
        given(newsService.findAll(
                new NewsFindAllQuery(NewsType.EVENT, EventStatus.ONGOING, 20, null)
        )).willReturn(response);

        mockMvc.perform(get("/api/v1/news")
                        .queryParam("type", "EVENT")
                        .queryParam("eventStatus", "ONGOING")
                        .queryParam("sort", "LATEST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data[0].id").value(102))
                .andExpect(jsonPath("$.data[0].type").value("EVENT"))
                .andExpect(jsonPath("$.data[0].eventStatus").value("ONGOING"))
                .andExpect(jsonPath("$.data[0].isPinned").value(false))
                .andExpect(jsonPath("$.meta.nextCursor").value(nullValue()))
                .andExpect(jsonPath("$.meta.hasNext").value(false))
                .andDo(document(
                        "news-find-all",
                        resource(ResourceSnippetParameters.builder()
                                .tag("News")
                                .summary("소식 목록 조회")
                                .description("소식 목록을 유형과 이벤트 상태로 필터링하고 최신순으로 조회한다.")
                                .queryParameters(
                                        parameterWithName("type")
                                                .description("소식 유형(ALL, NOTICE, EVENT). 기본값은 ALL")
                                                .optional(),
                                        parameterWithName("eventStatus")
                                                .description("이벤트 상태(UPCOMING, ONGOING, ENDED)")
                                                .optional(),
                                        parameterWithName("sort")
                                                .description("정렬 기준. 현재 LATEST만 지원하며 기본값은 LATEST")
                                                .optional(),
                                        parameterWithName("size")
                                                .description("조회 개수. 기본값 20, 최댓값 50")
                                                .optional(),
                                        parameterWithName("cursor")
                                                .description("다음 페이지 조회에 사용하는 opaque cursor")
                                                .optional()
                                )
                                .responseSchema(Schema.schema("NewsFindAllSuccessResponse"))
                                .responseFields(
                                        fieldWithPath("status")
                                                .type(STRING)
                                                .description("응답 상태"),
                                        fieldWithPath("data")
                                                .type(ARRAY)
                                                .description("소식 목록"),
                                        fieldWithPath("data[].id")
                                                .type(NUMBER)
                                                .description("소식 ID"),
                                        fieldWithPath("data[].type")
                                                .type(STRING)
                                                .description("소식 유형"),
                                        fieldWithPath("data[].title")
                                                .type(STRING)
                                                .description("소식 제목"),
                                        fieldWithPath("data[].summary")
                                                .type(STRING)
                                                .description("소식 요약"),
                                        fieldWithPath("data[].publishedAt")
                                                .type(STRING)
                                                .description("게시 시각"),
                                        fieldWithPath("data[].eventStatus")
                                                .type(STRING)
                                                .description("이벤트 상태. 공지인 경우 null")
                                                .optional(),
                                        fieldWithPath("data[].eventStartAt")
                                                .type(STRING)
                                                .description("이벤트 시작 시각. 공지인 경우 null")
                                                .optional(),
                                        fieldWithPath("data[].eventEndAt")
                                                .type(STRING)
                                                .description("이벤트 종료 시각. 공지인 경우 null")
                                                .optional(),
                                        fieldWithPath("data[].isPinned")
                                                .type(BOOLEAN)
                                                .description("고정 여부"),
                                        fieldWithPath("data[].pinOrder")
                                                .type(NUMBER)
                                                .description("고정 순서. 고정되지 않은 경우 null")
                                                .optional(),
                                        fieldWithPath("meta")
                                                .type(OBJECT)
                                                .description("페이지네이션 정보"),
                                        fieldWithPath("meta.nextCursor")
                                                .type(STRING)
                                                .description("다음 페이지 커서. 다음 페이지가 없으면 null")
                                                .optional(),
                                        fieldWithPath("meta.hasNext")
                                                .type(BOOLEAN)
                                                .description("다음 페이지 존재 여부")
                                )
                                .build())
                ));

        verify(newsService).findAll(
                new NewsFindAllQuery(NewsType.EVENT, EventStatus.ONGOING, 20, null)
        );
    }

    @Test
    @DisplayName("소식 목록 조회 기본값을 서비스에 전달한다")
    void usesDefaultNewsListQueryValues() throws Exception {
        NewsFindAllResponse response = new NewsFindAllResponse(
                List.of(),
                new NewsFindAllResponse.Meta(null, false)
        );
        given(newsService.findAll(
                new NewsFindAllQuery(null, null, 20, null)
        )).willReturn(response);

        mockMvc.perform(get("/api/v1/news"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.meta.hasNext").value(false));

        verify(newsService).findAll(
                new NewsFindAllQuery(null, null, 20, null)
        );
    }

    @Test
    @DisplayName("소식 목록 조회 개수가 50을 초과하면 400을 반환하고 서비스를 호출하지 않는다")
    void returnsBadRequestWhenNewsListSizeExceedsMaximum() throws Exception {
        mockMvc.perform(get("/api/v1/news")
                        .queryParam("size", "51"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(newsService);
    }

    @Test
    @DisplayName("소식 목록 조회 유형이 올바르지 않으면 400을 반환하고 서비스를 호출하지 않는다")
    void returnsBadRequestWhenNewsTypeIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/news")
                        .queryParam("type", "INVALID"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(newsService);
    }

    @Test
    @DisplayName("소식 목록 조회 이벤트 상태가 올바르지 않으면 400을 반환하고 서비스를 호출하지 않는다")
    void returnsBadRequestWhenEventStatusIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/news")
                        .queryParam("eventStatus", "INVALID"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(newsService);
    }

    @Test
    @DisplayName("소식 목록 조회 정렬 기준이 올바르지 않으면 400을 반환하고 서비스를 호출하지 않는다")
    void returnsBadRequestWhenNewsSortIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/news")
                        .queryParam("sort", "INVALID"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(newsService);
    }

    /**
     * 공지 실패 테스트
     * DOCS: /api/v1/news/notices
     */
    @Test
    @DisplayName("공지 생성 실패 테스트. 필수 요청값이 없으면 400을 반환하고 서비스를 호출하지 않는다.")
    void returnsBadRequestWithoutCallingServiceWhenNoticeRequiredFieldIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/news/notices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "summary": "요약",
                                  "body": "본문",
                                  "authorName": "샤라웃 운영팀"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andDo(document(
                        "news-notice-create-invalid",
                        resource(ResourceSnippetParameters.builder()
                                .tag("News")
                                .summary("공지 생성")
                                .description("공지와 선택적인 CTA를 생성한다.")
                                .responseSchema(Schema.schema("ErrorResponse"))
                                .responseFields(RestDocsFields.errorResponse())
                                .build())
                ));

        verifyNoInteractions(newsService);
    }

    /**
     * 이벤트 생성 실패 테스트
     * DOCS: /api/v1/news/events
     */
    @Test
    @DisplayName("이벤트 생성 실패 테스트. 필수 요청값이 없으면 400을 반환하고 서비스를 호출하지 않는다.")
    void returnsBadRequestWithoutCallingServiceWhenEventRequiredFieldIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/news/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "summary": "요약",
                                  "body": "본문",
                                  "authorName": "샤라웃 운영팀"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andDo(document(
                        "news-event-create-required-invalid",
                        resource(ResourceSnippetParameters.builder()
                                .tag("News")
                                .summary("이벤트 생성")
                                .description("이벤트와 선택적인 CTA를 생성한다.")
                                .responseSchema(Schema.schema("ErrorResponse"))
                                .responseFields(RestDocsFields.errorResponse())
                                .build())
                ));

        verifyNoInteractions(newsService);
    }

    @Test
    @DisplayName("이벤트 시작 시각이 없으면 400을 반환하고 서비스를 호출하지 않는다")
    void returnsBadRequestWithoutCallingServiceWhenEventStartAtIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/news/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "이벤트",
                                  "summary": "요약",
                                  "body": "본문",
                                  "authorName": "샤라웃 운영팀",
                                  "eventEndAt": "2026-09-30T23:59:59Z"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andDo(document(
                        "news-event-create-period-invalid",
                        resource(ResourceSnippetParameters.builder()
                                .tag("News")
                                .summary("이벤트 생성")
                                .description("이벤트와 선택적인 CTA를 생성한다.")
                                .responseSchema(Schema.schema("ErrorResponse"))
                                .responseFields(RestDocsFields.errorResponse())
                                .build())
                ));

        verifyNoInteractions(newsService);
    }

    @Test
    @DisplayName("이벤트 시작 시각이 종료 시각보다 늦으면 400을 반환하고 서비스를 호출하지 않는다")
    void returnsBadRequestWhenEventStartAtIsAfterEndAt() throws Exception {
        mockMvc.perform(post("/api/v1/news/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "이벤트",
                                  "summary": "요약",
                                  "body": "본문",
                                  "authorName": "샤라웃 운영팀",
                                  "eventStartAt": "2026-10-01T00:00:00Z",
                                  "eventEndAt": "2026-09-30T23:59:59Z"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details[0].field").value("eventStartAt"))
                .andExpect(jsonPath("$.details[0].message")
                        .value("eventStartAt은 eventEndAt보다 늦을 수 없습니다."))
                .andDo(document(
                        "news-event-create-period-order-invalid",
                        resource(ResourceSnippetParameters.builder()
                                .tag("News")
                                .summary("이벤트 생성 실패")
                                .description("이벤트 시작 시각이 종료 시각보다 늦으면 400 Bad Request를 반환한다.")
                                .responseSchema(Schema.schema("ErrorResponse"))
                                .responseFields(RestDocsFields.errorResponse())
                                .build())
                ));

        verifyNoInteractions(newsService);
    }

    @Test
    @DisplayName("공지 CTA 검증에 실패하면 400을 반환하고 서비스를 호출하지 않는다")
    void returnsBadRequestWhenNoticeCtaIsInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/news/notices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "공지",
                                  "summary": "요약",
                                  "body": "본문",
                                  "authorName": "샤라웃 운영팀",
                                  "cta": {
                                    "label": "",
                                    "url": "example.com"
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details[0].field").value("cta"))
                .andDo(document(
                        "news-notice-create-cta-invalid",
                        resource(errorResponseResource(
                                "공지 생성",
                                "공지와 선택적인 CTA를 생성한다."))
                ));

        verifyNoInteractions(newsService);
    }

    @Test
    @DisplayName("잘못된 JSON 요청 본문은 400 오류 응답으로 반환한다")
    void returnsBadRequestWhenNoticeRequestBodyIsMalformed() throws Exception {
        mockMvc.perform(post("/api/v1/news/notices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andDo(document(
                        "news-notice-create-malformed-json",
                        resource(errorResponseResource(
                                "공지 생성",
                                "공지와 선택적인 CTA를 생성한다."))
                ));

        verifyNoInteractions(newsService);
    }

    @Test
    @DisplayName("공지 생성 중 도메인 예외가 발생하면 500 오류 응답을 반환한다")
    void returnsInternalServerErrorWhenNoticeDomainValidationFails() throws Exception {
        given(newsService.createNotice(any(NoticeCreateRequest.class)))
                .willThrow(new DomainValidationException(NewsErrorCode.NEWS_INVALID_AUTHOR_ID_SIZE));

        mockMvc.perform(post("/api/v1/news/notices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJsonWithCta()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(NewsErrorCode.NEWS_INVALID_AUTHOR_ID_SIZE.name()))
                .andDo(document(
                        "news-notice-create-domain-invalid",
                        resource(errorResponseResource(
                                "공지 생성",
                                "공지와 선택적인 CTA를 생성한다."))
                ));

        verify(newsService).createNotice(any(NoticeCreateRequest.class));
    }

    @Test
    @DisplayName("이벤트 생성 중 도메인 예외가 발생하면 500 오류 응답을 반환한다")
    void returnsInternalServerErrorWhenEventDomainValidationFails() throws Exception {
        given(newsService.createEvent(any(EventCreateRequest.class)))
                .willThrow(new DomainValidationException(NewsErrorCode.NEWS_EVENT_PERIOD_INVALID));

        mockMvc.perform(post("/api/v1/news/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventRequestJsonWithCta()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(NewsErrorCode.NEWS_EVENT_PERIOD_INVALID.name()))
                .andDo(document(
                        "news-event-create-domain-invalid",
                        resource(errorResponseResource(
                                "이벤트 생성",
                                "이벤트와 선택적인 CTA를 생성한다."))
                ));

        verify(newsService).createEvent(any(EventCreateRequest.class));
    }

    // TODO: 인증 방식 확정 후 미인증 요청의 401 응답과 서비스 미호출을 검증한다.
    // TODO: 권한 정책 적용 후 관리자 외 사용자의 403 응답을 검증한다.

    /**
     * 헬퍼 메서드
     */
    private String requestJsonWithCta() {
        return """
                {
                  "title": "우아한테크코스 6기 최종 프로젝트 데모데이 안내",
                  "summary": "최종 프로젝트 데모데이 일정을 안내합니다.",
                  "body": "우아한테크코스 6기 최종 프로젝트 데모데이는 2026년 9월 12일에 진행됩니다.",
                  "authorName": "샤라웃 운영팀",
                  "cta": {
                    "label": "데모데이 일정 확인",
                    "url": "example.com"
                  }
                }
                """;
    }

    private String requestJsonWithoutCta() {
        return """
                {
                  "title": "서비스 점검 안내",
                  "summary": "안정적인 서비스 제공을 위해 점검을 진행합니다.",
                  "body": "2026년 9월 10일에 점검을 진행합니다.",
                  "authorName": "샤라웃 운영팀",
                  "cta": null
                }
                """;
    }

    private String eventRequestJsonWithCta() {
        return """
                {
                  "title": "프로젝트 아카이빙 챌린지",
                  "summary": "팀 프로젝트를 등록하고 피드백을 받아보세요.",
                  "body": "프로젝트를 등록하면 동료 크루들의 피드백을 받을 수 있습니다.",
                  "authorName": "샤라웃 운영팀",
                  "eventStartAt": "2026-09-01T00:00:00Z",
                  "eventEndAt": "2026-09-30T23:59:59Z",
                  "cta": {
                    "label": "프로젝트 등록하기",
                    "url": "/projects/3001"
                  }
                }
                """;
    }

    private String eventRequestJsonWithoutCta() {
        return """
                {
                  "title": "서비스 이벤트",
                  "summary": "이벤트 요약",
                  "body": "이벤트 본문",
                  "authorName": "샤라웃 운영팀",
                  "eventStartAt": "2026-10-01T00:00:00Z",
                  "eventEndAt": "2026-10-31T23:59:59Z"
                }
                """;
    }

    private ResourceSnippetParameters errorResponseResource(String summary, String description) {
        return ResourceSnippetParameters.builder()
                .tag("News")
                .summary(summary)
                .description(description)
                .responseSchema(Schema.schema("ErrorResponse"))
                .responseFields(RestDocsFields.errorResponse())
                .build();
    }
}
