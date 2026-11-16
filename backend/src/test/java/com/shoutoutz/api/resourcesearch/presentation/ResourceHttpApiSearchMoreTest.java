package com.shoutoutz.api.resourcesearch.presentation;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.BOOLEAN;
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
import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.resourcesearch.application.ResourceQueryService;
import com.shoutoutz.api.resourcesearch.domain.ResourceSearchErrorCode;
import com.shoutoutz.api.resourcesearch.presentation.dto.request.ResourceSearchMoreRequest;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.ResourceCardResponse;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.ResourceSearchMoreResponse;
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
class ResourceHttpApiSearchMoreTest {

    private static final String CURSOR = "eyJwdWJsaXNoZWRBdCI6IjIwMjYtMTAtMTlUMDg6MTI6NDVaIiwicmVzb3VyY2VJZCI6MzQxMn0";
    private static final String NEXT_CURSOR = "eyJwdWJsaXNoZWRBdCI6IjIwMjYtMTAtMTJUMDM6MDA6MDBaIiwicmVzb3VyY2VJZCI6MzM4MH0";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResourceQueryService resourceQueryService;

    @Test
    @DisplayName("프로젝트 이름 검색에서 받은 커서로 그 프로젝트의 자료를 이어서 준다")
    void searchesMoreResourcesOfNamedProject() throws Exception {
        given(resourceQueryService.searchMore(any(ResourceSearchMoreRequest.class)))
                .willReturn(new ResourceSearchMoreResponse(
                        List.of(new ResourceCardResponse(
                                3380L, "github-pr", "[Feature] #686 The Rilog. API 연동",
                                "https://github.com/woowacourse-teams/2026-rilog/pull/686", "매일 오전 6시에 갱신해요.",
                                Instant.parse("2026-10-12T03:00:00Z"), null,
                                "https://github.githubassets.com/favicons/favicon.svg",
                                new ResourceCardResponse.Team("Rilog.", "rilog", 31L, "https://cdn.example.com/media/project/31/thumbnail"))),
                        NEXT_CURSOR,
                        84L
                ));

        mockMvc.perform(get("/api/v1/resources/search/more")
                        .queryParam("keyword", "Rilog")
                        .queryParam("section", "DOCUMENT")
                        .queryParam("track", "BE")
                        .queryParam("cursor", CURSOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].resourceId").value(3380L))
                .andExpect(jsonPath("$.meta.nextCursor").value(NEXT_CURSOR))
                .andExpect(jsonPath("$.meta.hasNext").value(true))
                .andExpect(jsonPath("$.meta.totalCount").value(84))
                .andDo(document(
                        "resource-search-more",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Resource")
                                .summary("프로젝트 이름 검색 결과 이어 읽기")
                                .description("""
                                        기술 자료 검색에서 검색어가 프로젝트 이름과 일치해 섹션에 nextCursor가 온 경우, 그 프로젝트의 나머지 자료를 최신순으로 20건씩 이어 준다. 로그인 없이 볼 수 있다.
                                        keyword, track은 검색 때와 같은 값을 보낸다. 검색어가 프로젝트 이름이 아니면 빈 목록을 준다.
                                        이어 읽기는 인기 검색어 집계에 기록하지 않는다.
                                        keyword가 없거나 50자를 넘거나, track이 BE, FE, AN이 아니거나, section이 BLOG, DOCUMENT가 아니면 400(VALIDATION_FAILED), 커서를 풀 수 없으면 400(RESOURCE_SEARCH_CURSOR_INVALID)으로 응답한다.""")
                                .queryParameters(
                                        parameterWithName("keyword").type(SimpleType.STRING)
                                                .description("검색 때 보낸 검색어(앞뒤 공백 제거 후 1~50자)"),
                                        parameterWithName("section").type(SimpleType.STRING)
                                                .description("이어 읽을 섹션(BLOG, DOCUMENT)"),
                                        parameterWithName("track").type(SimpleType.STRING)
                                                .description("검색 때 보낸 직군(BE, FE, AN)")
                                                .optional(),
                                        parameterWithName("cursor").type(SimpleType.STRING)
                                                .description("검색 응답의 섹션이나 앞 페이지의 meta에서 받은 nextCursor. 없으면 처음부터 준다")
                                                .optional()
                                )
                                .responseSchema(Schema.schema("ResourceSearchMoreSuccessResponse"))
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data").type(ARRAY).description("그 프로젝트의 자료 카드, 최신순 최대 20건"),
                                        fieldWithPath("data[].resourceId").type(NUMBER).description("자료 ID. 자료 상세 조회에 쓴다"),
                                        fieldWithPath("data[].source").type(STRING).description("자료 원천"),
                                        fieldWithPath("data[].title").type(STRING).description("자료 제목"),
                                        fieldWithPath("data[].url").type(STRING).description("원문 주소"),
                                        fieldWithPath("data[].summary").type(STRING).description("한줄 소개").optional(),
                                        fieldWithPath("data[].publishedAt").type(STRING).description("원문 게시 시각(UTC)").optional(),
                                        fieldWithPath("data[].platform").type(STRING).description("블로그 플랫폼. 블로그가 아니면 null").optional(),
                                        fieldWithPath("data[].faviconUrl").type(STRING).description("원문 사이트 아이콘 주소. 기술 자료 검색의 카드와 같다").optional(),
                                        fieldWithPath("data[].team").type(OBJECT).description("자료를 만든 팀. 검색어가 가리킨 프로젝트다").optional(),
                                        fieldWithPath("data[].team.name").type(STRING).description("프로젝트 이름").optional(),
                                        fieldWithPath("data[].team.projectSlug").type(STRING).description("프로젝트 slug").optional(),
                                        fieldWithPath("data[].team.logoMediaId").type(NUMBER).description("프로젝트 로고 미디어 ID. 없으면 null").optional(),
                                        fieldWithPath("data[].team.logoUrl").type(STRING).description("팀 로고 이미지 주소(작은 크기). 없으면 null").optional(),
                                        fieldWithPath("meta").type(OBJECT).description("이어 읽기 정보"),
                                        fieldWithPath("meta.nextCursor").type(STRING).description("다음 페이지 커서. 마지막 페이지면 없다").optional(),
                                        fieldWithPath("meta.hasNext").type(BOOLEAN).description("다음 페이지가 있는지"),
                                        fieldWithPath("meta.totalCount").type(NUMBER).description("이 섹션에서 그 프로젝트 자료의 전체 수")
                                )
                                .build())
                ));
    }

    @Test
    @DisplayName("마지막 페이지면 다음 커서 없이 hasNext가 false다")
    void marksLastPage() throws Exception {
        given(resourceQueryService.searchMore(any(ResourceSearchMoreRequest.class)))
                .willReturn(new ResourceSearchMoreResponse(List.of(), null, 20L));

        mockMvc.perform(get("/api/v1/resources/search/more")
                        .queryParam("keyword", "Rilog")
                        .queryParam("section", "BLOG")
                        .queryParam("cursor", CURSOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.meta.nextCursor").doesNotExist())
                .andExpect(jsonPath("$.meta.hasNext").value(false))
                .andExpect(jsonPath("$.meta.totalCount").value(20));
    }

    @ParameterizedTest
    @ValueSource(strings = {"TALK", "blog", "ALL"})
    @DisplayName("섹션이 BLOG, DOCUMENT가 아니면 400을 준다")
    void rejectsUnsupportedSection(String section) throws Exception {
        mockMvc.perform(get("/api/v1/resources/search/more")
                        .queryParam("keyword", "Rilog")
                        .queryParam("section", section))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(resourceQueryService);
    }

    @Test
    @DisplayName("섹션이나 검색어가 없으면 400을 준다")
    void rejectsMissingParameters() throws Exception {
        mockMvc.perform(get("/api/v1/resources/search/more").queryParam("keyword", "Rilog"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mockMvc.perform(get("/api/v1/resources/search/more").queryParam("section", "BLOG"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(resourceQueryService);
    }

    @Test
    @DisplayName("풀 수 없는 커서면 400을 준다")
    void rejectsInvalidCursor() throws Exception {
        willThrow(new BadRequestException(ResourceSearchErrorCode.RESOURCE_SEARCH_CURSOR_INVALID))
                .given(resourceQueryService).searchMore(any(ResourceSearchMoreRequest.class));

        mockMvc.perform(get("/api/v1/resources/search/more")
                        .queryParam("keyword", "Rilog")
                        .queryParam("section", "BLOG")
                        .queryParam("cursor", "!!!"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RESOURCE_SEARCH_CURSOR_INVALID"));
    }
}
