package com.shoutoutz.api.homebanner.presentation;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.EnumFields;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.shoutoutz.api.homebanner.application.HomeBannerService;
import com.shoutoutz.api.homebanner.domain.BannerDestinationType;
import com.shoutoutz.api.homebanner.domain.BannerLinkType;
import com.shoutoutz.api.homebanner.domain.BannerTargetType;
import com.shoutoutz.api.homebanner.presentation.dto.response.HomeBannerResponse;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = HomeBannerHttpApi.class)
@AutoConfigureRestDocs
class HomeBannerHttpApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HomeBannerService homeBannerService;

    @Test
    void 활성_홈_배너를_인증_없이_조회한다() throws Exception {
        given(homeBannerService.findAll()).willReturn(List.of(
                new HomeBannerResponse(
                        100L,
                        10L,
                        URI.create("https://cdn.example.com/banner"),
                        "URL",
                        null,
                        null,
                        "EXTERNAL_URL",
                        "https://example.com/promotion"
                )
        ));

        mockMvc.perform(get("/api/v1/home/banners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].bannerId").value(100L))
                .andExpect(jsonPath("$.data[0].mediaId").value(10L))
                .andExpect(jsonPath("$.data[0].destinationType").value("URL"))
                .andDo(document(
                        "home-banner-find-all",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Home Banner")
                                .summary("홈 배너 목록 조회")
                                .description("활성 홈 배너를 표시 순서대로 전체 조회한다.")
                                .responseSchema(Schema.schema("HomeBannerFindAllSuccessResponse"))
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data").type(ARRAY).description("활성 홈 배너 목록"),
                                        fieldWithPath("data[].bannerId").type(NUMBER).description("배너 ID"),
                                        fieldWithPath("data[].mediaId").type(NUMBER).description("배너 이미지 미디어 ID"),
                                        fieldWithPath("data[].imageUrl").type(STRING).description("표시용 이미지 URL"),
                                        new EnumFields(BannerDestinationType.class)
                                                .withPath("data[].destinationType")
                                                .description("이동 방식"),
                                        new EnumFields(BannerTargetType.class).withPath("data[].targetType")
                                                .description("대상 리소스 유형").optional(),
                                        fieldWithPath("data[].targetId").type(NUMBER)
                                                .description("대상 리소스 ID").optional(),
                                        new EnumFields(BannerLinkType.class).withPath("data[].linkType")
                                                .description("URL 유형").optional(),
                                        fieldWithPath("data[].linkUrl").type(STRING)
                                                .description("내부 경로 또는 외부 HTTPS URL").optional()
                                )
                                .build())
                ));
    }
}
