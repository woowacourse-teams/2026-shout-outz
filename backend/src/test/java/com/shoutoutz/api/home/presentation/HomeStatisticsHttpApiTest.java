package com.shoutoutz.api.home.presentation;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.OBJECT;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.shoutoutz.api.home.application.HomeStatisticsService;
import com.shoutoutz.api.home.presentation.dto.response.HomeStatisticsResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = HomeStatisticsHttpApi.class)
@AutoConfigureRestDocs
class HomeStatisticsHttpApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HomeStatisticsService homeStatisticsService;

    @Test
    @DisplayName("로그인 없이 홈 통계를 조회한다")
    void findsHomeStatistics() throws Exception {
        given(homeStatisticsService.find()).willReturn(new HomeStatisticsResponse(128L, 341L, 8, 2L));

        mockMvc.perform(get("/api/v1/home/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.projectCount").value(128))
                .andExpect(jsonPath("$.data.feedCount").value(341))
                .andExpect(jsonPath("$.data.currentCohort").value(8))
                .andExpect(jsonPath("$.data.ongoingEventCount").value(2))
                .andExpect(jsonPath("$.meta").doesNotExist())
                .andDo(document(
                        "home-statistics-find",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Home")
                                .summary("홈 통계 조회")
                                .description("프로젝트, 피드, 전체 기수 개수, 진행 중인 이벤트 통계를 조회한다.")
                                .responseSchema(Schema.schema("HomeStatisticsSuccessResponse"))
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data").type(OBJECT).description("홈 통계"),
                                        fieldWithPath("data.projectCount").type(NUMBER)
                                                .description("승인되었고 삭제되지 않은 프로젝트 수"),
                                        fieldWithPath("data.feedCount").type(NUMBER)
                                                .description("삭제되지 않은 피드 수"),
                                        fieldWithPath("data.currentCohort").type(NUMBER)
                                                .description("전체 기수 개수"),
                                        fieldWithPath("data.ongoingEventCount").type(NUMBER)
                                                .description("조회 시각에 진행 중인 삭제되지 않은 이벤트 수")
                                )
                                .build())
                ));
    }
}
