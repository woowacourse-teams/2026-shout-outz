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
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.epages.restdocs.apispec.SimpleType;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.resourcesearch.application.ResourceQueryService;
import com.shoutoutz.api.resourcesearch.domain.ResourceSearchErrorCode;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.ResourceCardResponse;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.ResourceDetailResponse;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.ResourceLinkPreviewResponse;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.ResourceExperienceResponse;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.ResourceTopicResponse;
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
class ResourceHttpApiDetailTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResourceQueryService resourceQueryService;

    @Test
    @DisplayName("자료 상세를 준다")
    void findsDetail() throws Exception {
        given(resourceQueryService.findDetail(101L)).willReturn(new ResourceDetailResponse(
                101L, "blog-post", "[Database] MySQL(InnoDB) 락(Lock) (2)", "https://soeun2537.tistory.com/127",
                "잠금 범위와 대기 동작을 살펴봐요.", Instant.parse("2025-10-06T03:00:00Z"), "tistory", "https://t1.daumcdn.net/tistory_admin/top_v2/bi-tistory-favicon.svg", null,
                new ResourceCardResponse.Team("페스타북", "festabook", 31L, "https://cdn.example.com/media/project/31/thumbnail"),
                new ResourceTopicResponse("concurrency", "동시성 제어/락"),
                List.of("레코드 락, 갭 락, 넥스트 키 락의 잠금 범위를 알아봐요."),
                List.of("MySQL", "InnoDB"),
                List.of(new ResourceExperienceResponse("대기가 길어졌어요.", "넥스트 키 락이 넓게 걸렸어요.",
                        "기본 키 단건 조회로 바꿨어요.", null, List.of("MySQL", "InnoDB"))),
                new ResourceLinkPreviewResponse("[Database] MySQL(InnoDB) 락(Lock) (2)", "MySQL의 락 종류를 정리합니다.",
                        "https://img1.daumcdn.net/thumb/og.png", "Tistory")));

        mockMvc.perform(get("/api/v1/resources/{resourceId}", 101L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resourceId").value(101L))
                .andExpect(jsonPath("$.data.topic.topicId").value("concurrency"))
                .andExpect(jsonPath("$.data.experiences[0].techs[0]").value("MySQL"))
                .andExpect(jsonPath("$.data.linkPreview.siteName").value("Tistory"))
                .andDo(document(
                        "resource-detail",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Resource")
                                .summary("기술 자료 상세 조회")
                                .description("""
                                        검색 결과나 최근 자료 카드를 눌렀을 때 이동하는 자료 상세 화면의 정리 내용을 준다. 로그인 없이 볼 수 있다.
                                        없거나 삭제된 자료, 제보로 삭제가 승인된 자료면 404(RESOURCE_NOT_FOUND), resourceId가 1 이상의 정수가 아니면 400(VALIDATION_FAILED)으로 응답한다.""")
                                .pathParameters(parameterWithName("resourceId").type(SimpleType.INTEGER).description("자료 ID"))
                                .responseSchema(Schema.schema("ResourceDetailSuccessResponse"))
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data").type(OBJECT).description("자료 상세"),
                                        fieldWithPath("data.resourceId").type(NUMBER).description("자료 ID"),
                                        fieldWithPath("data.source").type(STRING).description("자료 원천"),
                                        fieldWithPath("data.title").type(STRING).description("자료 제목"),
                                        fieldWithPath("data.url").type(STRING).description("원문 주소"),
                                        fieldWithPath("data.summary").type(STRING).description("한줄 소개").optional(),
                                        fieldWithPath("data.publishedAt").type(STRING).description("원문 게시 시각(UTC)").optional(),
                                        fieldWithPath("data.platform").type(STRING).description("블로그 플랫폼. 블로그가 아니면 null").optional(),
                                        fieldWithPath("data.faviconUrl").type(STRING).description("블로그 출처 아이콘 주소. tistory, velog, rilog 는 플랫폼 공식 아이콘, 그 밖의 블로그(개인 도메인 등)는 원문 페이지의 파비콘. 블로그가 아니거나 아직 수집 전이면 null").optional(),
                                        fieldWithPath("data.contentType").type(STRING)
                                                .description("PR과 팀 문서의 종류(problem_solving, tech_decision, implementation, chore, design, "
                                                        + "operation, experiment, convention, plan, other). 블로그와 테코톡은 null").optional(),
                                        fieldWithPath("data.team").type(OBJECT).description("자료를 만든 팀. 없으면 null").optional(),
                                        fieldWithPath("data.team.name").type(STRING).description("팀 이름").optional(),
                                        fieldWithPath("data.team.projectSlug").type(STRING)
                                                .description("등록된 프로젝트 slug. 등록 전 팀이면 null").optional(),
                                        fieldWithPath("data.team.logoMediaId").type(NUMBER)
                                                .description("프로젝트 로고 미디어 ID. 없으면 null").optional(),
                                        fieldWithPath("data.team.logoUrl").type(STRING)
                                                .description("팀 로고 이미지 주소(작은 크기). logoMediaId가 없거나 이미지가 준비되지 않았으면 null").optional(),
                                        fieldWithPath("data.topic").type(OBJECT).description("자료의 기술 주제. 없으면 null").optional(),
                                        fieldWithPath("data.topic.topicId").type(STRING).description("기술 주제 ID").optional(),
                                        fieldWithPath("data.topic.name").type(STRING).description("기술 주제 이름").optional(),
                                        fieldWithPath("data.keyPoints").type(ARRAY).description("이 자료에서 배울 점. 없으면 빈 배열")
                                                .attributes(key("itemsType").value("string")),
                                        fieldWithPath("data.techTags").type(ARRAY).description("자료에 붙은 기술 태그. 원래 표기 그대로. 없으면 빈 배열")
                                                .attributes(key("itemsType").value("string")),
                                        fieldWithPath("data.experiences").type(ARRAY).description("어떤 문제를 어떻게 풀었나. 없으면 빈 배열"),
                                        fieldWithPath("data.experiences[].problem").type(STRING).description("문제"),
                                        fieldWithPath("data.experiences[].cause").type(STRING).description("원인").optional(),
                                        fieldWithPath("data.experiences[].solution").type(STRING).description("해결"),
                                        fieldWithPath("data.experiences[].result").type(STRING).description("결과").optional(),
                                        fieldWithPath("data.experiences[].techs").type(ARRAY).description("쓴 기술. 없으면 빈 배열")
                                                .attributes(key("itemsType").value("string")),
                                        fieldWithPath("data.linkPreview").type(OBJECT)
                                                .description("원문 링크 미리보기. 아직 모으지 못했거나 실패했으면 null").optional(),
                                        fieldWithPath("data.linkPreview.title").type(STRING).description("원문 제목(og:title)").optional(),
                                        fieldWithPath("data.linkPreview.description").type(STRING).description("원문 설명(og:description)").optional(),
                                        fieldWithPath("data.linkPreview.imageUrl").type(STRING).description("썸네일 주소(og:image)").optional(),
                                        fieldWithPath("data.linkPreview.siteName").type(STRING).description("사이트 이름(og:site_name)").optional()
                                )
                                .build())
                ));
    }

    @Test
    @DisplayName("없거나 삭제된 자료면 404를 준다")
    void respondsNotFound() throws Exception {
        given(resourceQueryService.findDetail(404L))
                .willThrow(new EntityNotFoundException(ResourceSearchErrorCode.RESOURCE_NOT_FOUND));

        mockMvc.perform(get("/api/v1/resources/{resourceId}", 404L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "abc"})
    @DisplayName("자료 ID가 1 이상의 정수가 아니면 400을 준다")
    void rejectsInvalidResourceId(String resourceId) throws Exception {
        mockMvc.perform(get("/api/v1/resources/{resourceId}", resourceId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(resourceQueryService);
    }
}
