package com.shoutoutz.api.resourcesearch.presentation;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.epages.restdocs.apispec.SimpleType;
import com.shoutoutz.api.resourcesearch.application.ResourceTopicQueryService;
import com.shoutoutz.api.resourcesearch.domain.ResourceTopicSide;
import com.shoutoutz.api.resourcesearch.presentation.dto.request.ResourceTopicPopularRequest;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.PopularTopicResponse;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.ResourceTopicResponse;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.ResourceTopicSideResponse;
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

@WebMvcTest(controllers = ResourceTopicHttpApi.class)
@AutoConfigureRestDocs
class ResourceTopicHttpApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResourceTopicQueryService resourceTopicQueryService;

    @Test
    @DisplayName("기술 주제 전체를 분야별로 묶어 준다")
    void findsTopicsBySide() throws Exception {
        given(resourceTopicQueryService.findAllBySide()).willReturn(List.of(
                new ResourceTopicSideResponse(ResourceTopicSide.BACKEND, List.of(
                        new ResourceTopicResponse("jpa", "JPA/Hibernate"),
                        new ResourceTopicResponse("concurrency", "동시성 제어/락"))),
                new ResourceTopicSideResponse(ResourceTopicSide.FRONTEND, List.of(
                        new ResourceTopicResponse("react_rendering", "React 렌더링/훅"))),
                new ResourceTopicSideResponse(ResourceTopicSide.COMMON, List.of(
                        new ResourceTopicResponse("cicd", "CI/CD"))),
                new ResourceTopicSideResponse(ResourceTopicSide.MOBILE, List.of(
                        new ResourceTopicResponse("android", "Android")))));

        mockMvc.perform(get("/api/v1/resources/topics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].side").value("BACKEND"))
                .andExpect(jsonPath("$.data[0].topics[1].name").value("동시성 제어/락"))
                .andDo(document(
                        "resource-topics",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Resource")
                                .summary("기술 주제 분야별 조회")
                                .description("""
                                        검색 첫 화면의 '분야별 키워드'에 보여줄 기술 주제 전체를 분야별로 묶어 준다. 로그인 없이 볼 수 있다.
                                        분야는 BACKEND, FRONTEND, COMMON, MOBILE 순서로 항상 4개를 준다. 주제가 없는 분야는 빈 배열이다.
                                        분야 안의 주제는 이름의 가나다순이다. 영문은 대소문자를 가리지 않고 한글보다 앞에 온다.
                                        자료가 없는 주제도 포함한다.""")
                                .responseSchema(Schema.schema("ResourceTopicsSuccessResponse"))
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data").type(ARRAY).description("분야 목록"),
                                        fieldWithPath("data[].side").type(STRING)
                                                .description("분야. BACKEND, FRONTEND, COMMON, MOBILE 중 하나"),
                                        fieldWithPath("data[].topics").type(ARRAY).description("분야의 주제 목록"),
                                        fieldWithPath("data[].topics[].topicId").type(STRING).description("기술 주제 ID"),
                                        fieldWithPath("data[].topics[].name").type(STRING).description("기술 주제 이름")
                                )
                                .build())
                ));
    }

    @Test
    @DisplayName("많이 다룬 기술 주제를 자료 수와 함께 준다")
    void findsPopularTopics() throws Exception {
        given(resourceTopicQueryService.findPopular(new ResourceTopicPopularRequest(10))).willReturn(List.of(
                new PopularTopicResponse("cicd", "CI/CD", 128),
                new PopularTopicResponse("monitoring", "모니터링/관측성", 97)));

        mockMvc.perform(get("/api/v1/resources/topics/popular").queryParam("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].topicId").value("cicd"))
                .andExpect(jsonPath("$.data[0].resourceCount").value(128))
                .andDo(document(
                        "resource-topics-popular",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Resource")
                                .summary("많이 다룬 기술 주제 조회")
                                .description("""
                                        검색 첫 화면의 '많이 다룬 키워드'에 보여줄 기술 주제를 준다. 로그인 없이 볼 수 있다.
                                        삭제되지 않은 자료(블로그, PR과 팀 문서, 테코톡)가 많은 순, 같으면 이름의 가나다순이다.
                                        자료가 없는 주제는 넣지 않는다. size가 1~20이 아니면 400(VALIDATION_FAILED)으로 응답한다.""")
                                .queryParameters(
                                        parameterWithName("size").type(SimpleType.INTEGER).defaultValue(10)
                                                .description("조회 개수(1~20), 기본값 10").optional()
                                )
                                .responseSchema(Schema.schema("ResourceTopicsPopularSuccessResponse"))
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data").type(ARRAY).description("많이 다룬 주제 목록"),
                                        fieldWithPath("data[].topicId").type(STRING).description("기술 주제 ID"),
                                        fieldWithPath("data[].name").type(STRING).description("기술 주제 이름"),
                                        fieldWithPath("data[].resourceCount").type(NUMBER).description("이 주제의 자료 수")
                                )
                                .build())
                ));
    }

    @Test
    @DisplayName("size를 생략하면 10개를 조회한다")
    void usesDefaultPopularSize() throws Exception {
        given(resourceTopicQueryService.findPopular(new ResourceTopicPopularRequest(null))).willReturn(List.of());

        mockMvc.perform(get("/api/v1/resources/topics/popular"))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "21", "-1", "ten"})
    @DisplayName("size가 1~20의 정수가 아니면 400을 준다")
    void rejectsInvalidPopularSize(String size) throws Exception {
        mockMvc.perform(get("/api/v1/resources/topics/popular").queryParam("size", size))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(resourceTopicQueryService);
    }
}
