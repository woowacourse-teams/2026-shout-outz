package com.shoutoutz.api.cohort.presentation;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CohortHttpApi.class)
@AutoConfigureRestDocs
class CohortHttpApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("기수 선택지를 최신 기수부터 진행 연도와 함께 반환한다.")
    void findsAllCohorts() throws Exception {
        mockMvc.perform(get("/api/v1/cohorts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.items.length()").value(8))
                .andExpect(jsonPath("$.data.items[0].cohort").value(8))
                .andExpect(jsonPath("$.data.items[0].year").value(2026))
                .andExpect(jsonPath("$.data.items[7].cohort").value(1))
                .andExpect(jsonPath("$.meta").doesNotExist())
                .andDo(document(
                        "cohort-find-all",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Cohort")
                                .summary("기수 선택지 조회")
                                .description("우아한테크코스 기수와 진행 연도를 최신 기수부터 반환한다.")
                                .responseSchema(Schema.schema("CohortFindAllSuccessResponse"))
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data.items").type(ARRAY).description("기수 목록 (최신 기수부터)"),
                                        fieldWithPath("data.items[].cohort").type(NUMBER).description("기수"),
                                        fieldWithPath("data.items[].year").type(NUMBER).description("진행 연도")
                                )
                                .build())
                ));
    }
}
