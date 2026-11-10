package com.shoutoutz.api.resourcesearch.presentation;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.OBJECT;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.epages.restdocs.apispec.SimpleType;
import com.shoutoutz.api.resourcesearch.application.ResourceQueryService;
import com.shoutoutz.api.resourcesearch.presentation.dto.request.ResourceRecentRequest;
import com.shoutoutz.api.resourcesearch.domain.ResourceSection;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.ResourceCardResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ResourceHttpApi.class)
@AutoConfigureRestDocs
class ResourceHttpApiRecentTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResourceQueryService resourceQueryService;

    @Test
    @DisplayName("최근 블로그를 팀마다 하나씩 최신순으로 준다")
    void findsRecentResources() throws Exception {
        given(resourceQueryService.findRecent(new ResourceRecentRequest(ResourceSection.BLOG, 4))).willReturn(List.of(
                new ResourceCardResponse(1201L, "blog-post", "CSR, SSR, SSG, ISR..? 난 ISR로 할래 #1",
                        "https://rilog.kr/@rilog/posts/219", "정기 콘텐츠를 재배포 없이 갱신하는 구조를 설계할 때 참고하기 좋아요.",
                        Instant.parse("2026-10-20T01:00:00Z"), "github",
                        new ResourceCardResponse.Team("Rilog.", "rilog", 44L))));

        mockMvc.perform(get("/api/v1/resources/recent").queryParam("kind", "BLOG").queryParam("size", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].resourceId").value(1201L))
                .andDo(document(
                        "resource-recent",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Resource")
                                .summary("최근 기술 자료 조회")
                                .description("""
                                        검색 첫 화면의 '이런 컨텐츠는 어때요?'에 보여줄 최근 크루 블로그와 테코톡을 원문 게시 시각 최신순으로 준다. 로그인 없이 볼 수 있다.
                                        한 팀이나 한 발표자의 글이 몰리지 않게 블로그는 팀마다, 테코톡은 발표자마다 최신 하나만 고른다. 팀이나 발표자를 모르면 자료마다 따로 본다.
                                        한줄 소개나 게시 시각이 없는 자료는 뺀다. size는 이렇게 고른 뒤의 개수다.
                                        kind가 BLOG, TALK가 아니거나 size가 1~20이 아니면 400(VALIDATION_FAILED)으로 응답한다.""")
                                .queryParameters(
                                        parameterWithName("kind").type(SimpleType.STRING)
                                                .description("자료 종류. BLOG(블로그) 또는 TALK(테코톡)"),
                                        parameterWithName("size").type(SimpleType.INTEGER).defaultValue(4)
                                                .description("조회 개수(1~20), 기본값 4").optional()
                                )
                                .responseSchema(Schema.schema("ResourceRecentSuccessResponse"))
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data").type(ARRAY).description("자료 카드 목록"),
                                        fieldWithPath("data[].resourceId").type(NUMBER).description("자료 ID. 자료 상세 조회에 쓴다"),
                                        fieldWithPath("data[].source").type(STRING).description("자료 원천"),
                                        fieldWithPath("data[].title").type(STRING).description("자료 제목"),
                                        fieldWithPath("data[].url").type(STRING).description("원문 주소"),
                                        fieldWithPath("data[].summary").type(STRING).description("한줄 소개"),
                                        fieldWithPath("data[].publishedAt").type(STRING).description("원문 게시 시각(UTC)"),
                                        fieldWithPath("data[].platform").type(STRING).description("블로그 플랫폼. 블로그가 아니면 null").optional(),
                                        fieldWithPath("data[].team").type(OBJECT).description("자료를 만든 팀. 없으면 null").optional(),
                                        fieldWithPath("data[].team.name").type(STRING).description("팀 이름").optional(),
                                        fieldWithPath("data[].team.projectSlug").type(STRING)
                                                .description("등록된 프로젝트 slug. 등록 전 팀이면 null").optional(),
                                        fieldWithPath("data[].team.logoMediaId").type(NUMBER)
                                                .description("프로젝트 썸네일 미디어 ID. 없으면 null").optional()
                                )
                                .build())
                ));
    }

    @Test
    @DisplayName("size를 생략하면 4개를 조회한다")
    void usesDefaultSize() throws Exception {
        given(resourceQueryService.findRecent(new ResourceRecentRequest(ResourceSection.TALK, null))).willReturn(List.of());

        mockMvc.perform(get("/api/v1/resources/recent").queryParam("kind", "TALK"))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"DOCUMENT", "NEWS", "blog"})
    @DisplayName("kind가 BLOG, TALK가 아니면 400을 준다")
    void rejectsUnsupportedKind(String kind) throws Exception {
        mockMvc.perform(get("/api/v1/resources/recent").queryParam("kind", kind))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(resourceQueryService);
    }

    @Test
    @DisplayName("kind가 없으면 400을 준다")
    void rejectsMissingKind() throws Exception {
        mockMvc.perform(get("/api/v1/resources/recent"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(resourceQueryService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "21", "-1"})
    @DisplayName("size가 1~20이 아니면 400을 준다")
    void rejectsSizeOutOfRange(String size) throws Exception {
        mockMvc.perform(get("/api/v1/resources/recent").queryParam("kind", "BLOG").queryParam("size", size))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(resourceQueryService);
    }
}
