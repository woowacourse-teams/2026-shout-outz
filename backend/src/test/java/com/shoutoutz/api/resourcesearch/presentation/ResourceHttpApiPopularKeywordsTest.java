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
                new PopularKeywordResponse("Redis"), new PopularKeywordResponse("JPA"),
                new PopularKeywordResponse("Kotlin"), new PopularKeywordResponse("LLM"),
                new PopularKeywordResponse("동시성"), new PopularKeywordResponse("부하 테스트"),
                new PopularKeywordResponse("무중단 배포"), new PopularKeywordResponse("쿼리 최적화")));

        mockMvc.perform(get("/api/v1/resources/popular-keywords"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(8))
                .andExpect(jsonPath("$.data[0].keyword").value("Redis"))
                .andDo(document(
                        "resource-popular-keywords",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Resource")
                                .summary("인기 검색어 조회")
                                .description("""
                                        검색창 아래 칩에 보여줄 인기 검색어 8개를 준다. 로그인 없이 볼 수 있다.
                                        최근 7일(한국 날짜, 오늘 포함) 동안 5번 이상 검색된 검색어 중 자료의 기술 태그나 기술 주제 이름에 있는 것만 후보로 둔다.
                                        같은 방문자가 같은 날 여러 번 검색해도 한 번으로 센다. 많이 검색된 순, 같으면 최근에 검색된 순, 그다음 이름순이다.
                                        8개가 안 되면 기본 칩(LLM, 동시성, 부하 테스트, 무중단 배포, 쿼리 최적화, 모니터링, 웹 성능, 인증/인가)으로 채운다.""")
                                .responseSchema(Schema.schema("PopularKeywordsSuccessResponse"))
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data").type(ARRAY).description("인기 검색어. 항상 8개"),
                                        fieldWithPath("data[].keyword").type(STRING)
                                                .description("칩에 보여줄 검색어. 기술 태그나 주제 이름의 원래 표기")
                                )
                                .build())
                ));
    }
}
