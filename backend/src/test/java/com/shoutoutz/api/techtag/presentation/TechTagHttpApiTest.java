package com.shoutoutz.api.techtag.presentation;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.shoutoutz.api.techtag.domain.TechTag;
import com.shoutoutz.api.techtag.domain.TechTagRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = TechTagHttpApi.class)
@AutoConfigureRestDocs
class TechTagHttpApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TechTagRepository techTagRepository;

    @Test
    @DisplayName("keyword로 선택 가능한 기술 스택을 검색한다.")
    void findsTechTagsByKeyword() throws Exception {
        given(techTagRepository.findAllActiveByKeyword("boot"))
                .willReturn(List.of(tag(8L, "spring-boot", "Spring Boot")));

        mockMvc.perform(get("/api/v1/tech-tags").param("keyword", "boot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.items[0].id").value(8))
                .andExpect(jsonPath("$.data.items[0].displayName").value("Spring Boot"))
                .andExpect(jsonPath("$.data.items[0].slug").doesNotExist())
                .andDo(document(
                        "tech-tag-find-all",
                        resource(ResourceSnippetParameters.builder()
                                .tag("TechTag")
                                .summary("기술 스택 조회")
                                .description("선택 가능한 기술 스택을 displayName 오름차순으로 반환한다. "
                                        + "keyword가 없거나 비어 있으면 전체를 반환한다.")
                                .queryParameters(
                                        parameterWithName("keyword")
                                                .description("displayName 또는 slug 부분 일치 검색어 (대소문자 무시)")
                                                .optional()
                                )
                                .responseSchema(Schema.schema("TechTagFindAllSuccessResponse"))
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data.items").type(ARRAY).description("기술 스택 목록"),
                                        fieldWithPath("data.items[].id").type(NUMBER).description("기술 스택 ID"),
                                        fieldWithPath("data.items[].displayName").type(STRING).description("화면 표시 이름")
                                )
                                .build())
                ));
    }

    @Test
    @DisplayName("keyword가 없는 경우, 전체 기술 스택을 반환한다.")
    void findsAllTechTagsWithoutKeyword() throws Exception {
        given(techTagRepository.findAllActive())
                .willReturn(List.of(tag(1L, "android", "Android"), tag(8L, "spring-boot", "Spring Boot")));

        mockMvc.perform(get("/api/v1/tech-tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2));

        verify(techTagRepository, never()).findAllActiveByKeyword(anyString());
    }

    @Test
    @DisplayName("keyword가 공백뿐인 경우, 검색하지 않고 전체를 반환한다.")
    void findsAllTechTagsWithBlankKeyword() throws Exception {
        given(techTagRepository.findAllActive()).willReturn(List.of());

        mockMvc.perform(get("/api/v1/tech-tags").param("keyword", "   "))
                .andExpect(status().isOk());

        verify(techTagRepository).findAllActive();
        verify(techTagRepository, never()).findAllActiveByKeyword(anyString());
    }

    @Test
    @DisplayName("keyword 앞뒤 공백은 제거하고 검색한다.")
    void stripsKeyword() throws Exception {
        given(techTagRepository.findAllActiveByKeyword("boot")).willReturn(List.of());

        mockMvc.perform(get("/api/v1/tech-tags").param("keyword", "  boot  "))
                .andExpect(status().isOk());

        verify(techTagRepository).findAllActiveByKeyword("boot");
    }

    private static TechTag tag(Long id, String slug, String displayName) {
        return TechTag.builder().id(id).slug(slug).displayName(displayName).active(true).build();
    }
}
