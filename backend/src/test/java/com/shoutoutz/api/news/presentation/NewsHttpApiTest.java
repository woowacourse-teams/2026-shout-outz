package com.shoutoutz.api.news.presentation;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import com.shoutoutz.api.news.application.NewsService;
import com.shoutoutz.api.news.domain.NewsType;
import com.shoutoutz.api.news.presentation.dto.request.NoticeCreateRequest;
import com.shoutoutz.api.news.presentation.dto.response.NoticeCreateResponse;
import java.time.Instant;
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

    @Test
    @DisplayName("공지 등록 성공 테스트. 공통 성공 응답과 201을 반환한다")
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
                                .responseSchema(Schema.schema("SuccessResponseNoticeCreateResponse"))
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
    @DisplayName("CTA가 없으면 null로 응답한다")
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

    @Test
    @DisplayName("필수 요청값이 없으면 400을 반환하고 서비스를 호출하지 않는다")
    void returnsBadRequestWithoutCallingServiceWhenRequiredFieldIsMissing() throws Exception {
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
                                .summary("공지 생성 실패")
                                .description("필수 요청값이 없으면 400 Bad Request를 반환한다.")
                                .build())
                ));

        verifyNoInteractions(newsService);
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
}
