package com.shoutoutz.api.resourcesearch.presentation;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.shoutoutz.api.resourcesearch.application.ResourceQueryService;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.PopularKeywordResponse;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ResourceHttpApi.class)
@AutoConfigureRestDocs
class ResourceHttpApiPopularKeywordsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResourceQueryService resourceQueryService;

    @Test
    @DisplayName("인기 검색어 8개를 준다")
    void findsPopularKeywords() throws Exception {
        given(resourceQueryService.findPopularKeywords()).willReturn(List.of(
                new PopularKeywordResponse("LLM"), new PopularKeywordResponse("동시성"),
                new PopularKeywordResponse("부하 테스트"), new PopularKeywordResponse("무중단 배포"),
                new PopularKeywordResponse("쿼리 최적화"), new PopularKeywordResponse("모니터링"),
                new PopularKeywordResponse("웹 성능"), new PopularKeywordResponse("인증/인가")));

        mockMvc.perform(get("/api/v1/resources/popular-keywords"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(8))
                .andExpect(jsonPath("$.data[0].keyword").value("LLM"))
                .andDo(document(
                        "resource-popular-keywords",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Resource")
                                .summary("인기 검색어 조회")
                                .description("""
                                        검색창 아래 칩에 보여줄 검색어 8개를 준다. 로그인 없이 볼 수 있다.
                                        지금은 정해 둔 목록(LLM, 동시성, 부하 테스트, 무중단 배포, 쿼리 최적화, 모니터링, 웹 성능, 인증/인가)을 순서대로 준다.
                                        검색어 기록은 계속 쌓고 있으며, 나중에 실제 인기순으로 바뀌어도 응답 형식은 같다.""")
                                .responseSchema(Schema.schema("PopularKeywordsSuccessResponse"))
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data").type(ARRAY).description("인기 검색어. 항상 8개"),
                                        fieldWithPath("data[].keyword").type(STRING)
                                                .description("칩에 보여줄 검색어")
                                )
                                .build())
                ));
    }
}
