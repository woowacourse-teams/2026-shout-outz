package com.shoutoutz.api.resourcesearch.presentation;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.OBJECT;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.applyPathPrefix;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.epages.restdocs.apispec.SimpleType;
import com.shoutoutz.api.resourcesearch.application.ResourceQueryService;
import com.shoutoutz.api.resourcesearch.presentation.dto.request.ResourceSearchRequest;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.ResourceCardResponse;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.ResourceSearchResponse;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.ResourceSectionResponse;
import com.shoutoutz.api.visitor.domain.VisitorKey;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ResourceHttpApi.class)
@AutoConfigureRestDocs
class ResourceHttpApiSearchTest {

    private static final String VISITOR_KEY_ATTRIBUTE = VisitorKey.class.getName();
    private static final VisitorKey VISITOR_KEY = new VisitorKey("a".repeat(64));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResourceQueryService resourceQueryService;

    @Test
    @DisplayName("검색어로 자료를 찾으면 블로그, 문서, 테코톡 섹션별 카드를 준다")
    void searchesResources() throws Exception {
        given(resourceQueryService.search(any(ResourceSearchRequest.class), any(VisitorKey.class)))
                .willReturn(new ResourceSearchResponse(
                        new ResourceSectionResponse(List.of(new ResourceCardResponse(
                                101L, "blog-post", "[Database] MySQL(InnoDB) 락(Lock) (2)",
                                "https://soeun2537.tistory.com/127", "잠금 범위와 대기 동작을 살펴봐요.",
                                Instant.parse("2025-10-06T03:00:00Z"), "tistory",
                                "https://t1.daumcdn.net/tistory_admin/top_v2/bi-tistory-favicon.svg",
                                new ResourceCardResponse.Team("페스타북", "festabook", 31L, "https://cdn.example.com/media/project/31/thumbnail")))),
                        new ResourceSectionResponse(List.of(new ResourceCardResponse(
                                3412L, "github-teamdoc", "포트폴리오 원장 변경 전에 포트폴리오 잠금 획득",
                                "https://github.com/woowacourse-teams/2026-DAYNOMY/blob/main/docs/adr.md",
                                "비관적 잠금으로 원장 정합성을 지켜요.", Instant.parse("2026-10-20T05:12:00Z"),
                                null, null, null))),
                        new ResourceSectionResponse(List.of(new ResourceCardResponse(
                                2301L, "youtube-tekotalk", "[10분 테코톡] 벨로의 Race Condition 이해하기",
                                "https://www.youtube.com/watch?v=At4Gyezw8WA", "동시 요청에서 데이터 오류가 생기는 원리를 이해해요.",
                                Instant.parse("2025-09-05T09:00:00Z"), null, null, null)))
                ));

        mockMvc.perform(get("/api/v1/resources/search")
                        .queryParam("keyword", "동시성")
                        .queryParam("track", "BE")
                        .requestAttr(VISITOR_KEY_ATTRIBUTE, VISITOR_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.blogs.items[0].resourceId").value(101L))
                .andExpect(jsonPath("$.data.blogs.items[0].team.projectSlug").value("festabook"))
                .andExpect(jsonPath("$.data.documents.items[0].team").isEmpty())
                .andExpect(jsonPath("$.data.talks.items[0].team").isEmpty())
                .andDo(document(
                        "resource-search",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Resource")
                                .summary("기술 자료 검색")
                                .description("""
                                        검색어로 블로그, PR과 팀 문서, 테코톡을 찾아 섹션별로 연관도가 높은 순으로 준다. 로그인 없이 볼 수 있다.
                                        blogs, documents는 최대 20건, talks는 최대 6건이며, 관련성이 약한 자료로 억지로 채우지 않아 더 적거나 비어 있을 수 있다.
                                        검색어는 인기 검색어 집계에 방문자별로 하루 한 번 기록한다(VISITOR_ID 쿠키를 함께 보낸다).
                                        keyword가 없거나 50자를 넘거나, track이 BE, FE, AN이 아니면 400(VALIDATION_FAILED)으로 응답한다.""")
                                .queryParameters(
                                        parameterWithName("keyword").type(SimpleType.STRING)
                                                .description("검색어(앞뒤 공백 제거 후 1~50자)"),
                                        parameterWithName("track").type(SimpleType.STRING)
                                                .description("직군(BE, FE, AN). 없으면 직군 구분 없이 찾고, 있으면 그 직군과 공통, 미분류 자료를 찾는다")
                                                .optional()
                                )
                                .responseSchema(Schema.schema("ResourceSearchSuccessResponse"))
                                .responseFields(searchResponseFields())
                                .build())
                ));
    }

    @Test
    @DisplayName("검색어가 없으면 400을 준다")
    void rejectsMissingKeyword() throws Exception {
        mockMvc.perform(get("/api/v1/resources/search").requestAttr(VISITOR_KEY_ATTRIBUTE, VISITOR_KEY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(resourceQueryService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("검색어가 비어 있거나 공백뿐이면 400을 준다")
    void rejectsBlankKeyword(String keyword) throws Exception {
        mockMvc.perform(get("/api/v1/resources/search")
                        .queryParam("keyword", keyword)
                        .requestAttr(VISITOR_KEY_ATTRIBUTE, VISITOR_KEY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(resourceQueryService);
    }

    @Test
    @DisplayName("검색어가 50자를 넘으면 400을 준다")
    void rejectsTooLongKeyword() throws Exception {
        mockMvc.perform(get("/api/v1/resources/search")
                        .queryParam("keyword", "가".repeat(51))
                        .requestAttr(VISITOR_KEY_ATTRIBUTE, VISITOR_KEY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(resourceQueryService);
    }

    @Test
    @DisplayName("검색어 길이는 앞뒤 공백을 지운 뒤 센다")
    void countsKeywordLengthAfterTrimming() throws Exception {
        given(resourceQueryService.search(any(ResourceSearchRequest.class), any(VisitorKey.class)))
                .willReturn(new ResourceSearchResponse(
                        new ResourceSectionResponse(List.of()),
                        new ResourceSectionResponse(List.of()),
                        new ResourceSectionResponse(List.of())
                ));

        mockMvc.perform(get("/api/v1/resources/search")
                        .queryParam("keyword", "  " + "가".repeat(50) + "  ")
                        .requestAttr(VISITOR_KEY_ATTRIBUTE, VISITOR_KEY))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ALL", "BACKEND", "be"})
    @DisplayName("직군이 BE, FE, AN이 아니면 400을 준다")
    void rejectsUnsupportedTrack(String track) throws Exception {
        mockMvc.perform(get("/api/v1/resources/search")
                        .queryParam("keyword", "동시성")
                        .queryParam("track", track)
                        .requestAttr(VISITOR_KEY_ATTRIBUTE, VISITOR_KEY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(resourceQueryService);
    }

    private List<FieldDescriptor> searchResponseFields() {
        List<FieldDescriptor> fields = new ArrayList<>(List.of(
                fieldWithPath("status").type(STRING).description("응답 상태"),
                fieldWithPath("data").type(OBJECT).description("섹션별 검색 결과"),
                fieldWithPath("data.blogs.items").type(ARRAY).description("블로그(blog-post), 최대 20건"),
                fieldWithPath("data.documents.items").type(ARRAY)
                        .description("PR과 팀 문서(github-pr, github-teamdoc, github-wiki, github-discussion), 최대 20건"),
                fieldWithPath("data.talks.items").type(ARRAY).description("테코톡(youtube-tekotalk), 최대 6건")
        ));
        fields.addAll(applyPathPrefix("data.blogs.items[].", cardFields()));
        fields.addAll(applyPathPrefix("data.documents.items[].", cardFields()));
        fields.addAll(applyPathPrefix("data.talks.items[].", cardFields()));
        return List.copyOf(fields);
    }

    private List<FieldDescriptor> cardFields() {
        return List.of(
                fieldWithPath("resourceId").type(NUMBER).description("자료 ID. 자료 상세 조회에 쓴다"),
                fieldWithPath("source").type(STRING).description("자료 원천"),
                fieldWithPath("title").type(STRING).description("자료 제목"),
                fieldWithPath("url").type(STRING).description("원문 주소"),
                fieldWithPath("summary").type(STRING).description("한줄 소개").optional(),
                fieldWithPath("publishedAt").type(STRING).description("원문 게시 시각(UTC)").optional(),
                fieldWithPath("platform").type(STRING).description("블로그 플랫폼. 블로그가 아니면 null").optional(),
                fieldWithPath("faviconUrl").type(STRING).description("블로그 출처 아이콘 주소. tistory, velog, rilog 는 플랫폼 공식 아이콘, 그 밖의 블로그(개인 도메인 등)는 원문 페이지의 파비콘. 블로그가 아니거나 아직 수집 전이면 null").optional(),
                fieldWithPath("team").type(OBJECT).description("자료를 만든 팀. 팀이 없거나 알 수 없으면 null").optional(),
                fieldWithPath("team.name").type(STRING).description("팀 이름").optional(),
                fieldWithPath("team.projectSlug").type(STRING).description("등록된 프로젝트 slug. 등록 전 팀이면 null").optional(),
                fieldWithPath("team.logoMediaId").type(NUMBER).description("프로젝트 로고 미디어 ID. 없으면 null").optional(),
                fieldWithPath("team.logoUrl").type(STRING).description("팀 로고 이미지 주소(작은 크기). logoMediaId가 없거나 이미지가 준비되지 않았으면 null").optional()
        );
    }
}
