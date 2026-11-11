package com.shoutoutz.api.projectresource.presentation;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.OBJECT;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.EnumFields;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.shoutoutz.api.projectresource.application.ProjectTopicService;
import com.shoutoutz.api.projectresource.domain.ProjectTopicGroup;
import com.shoutoutz.api.projectresource.presentation.dto.response.ProjectTopicsResponse;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ProjectTopicHttpApi.class)
@AutoConfigureRestDocs
@DisplayName("프로젝트 기술 주제 목록 API 문서")
class ProjectTopicApiDocumentationTest {

    private static final String DESCRIPTION = "탐색 화면 프로젝트 탭 왼쪽의 기술 주제 목록과 주제별 프로젝트 수를 직군 묶음으로 조회한다. 로그인 없이 부를 수 있다. 프로젝트 수는 공개된 프로젝트 중 그 주제의 팀 자료(삭제되지 않은 블로그, PR, 팀 문서이며 관리자가 받아들인 필요 없어요 제보가 없는 자료)가 1건 이상인 프로젝트를 센다. 프로젝트 상세 기술 이야기 탭과 같은 규칙이다. 수는 검색어, 기수, 기술 스택과 상관없는 전체 기준이다. 묶음은 COMMON, BE, FE, AN 순이고 주제가 없는 묶음은 빠진다. 묶음 안 주제는 프로젝트가 많은 순, 같으면 이름순이다. 주제를 고르면 프로젝트 목록 조회에 topicId를 붙인다.";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectTopicService projectTopicService;

    @Test
    @DisplayName("기술 주제 목록과 주제별 프로젝트 수를 직군 묶음으로 조회한다")
    void findTopics() throws Exception {
        given(projectTopicService.findTopics()).willReturn(new ProjectTopicsResponse(120, List.of(
                new ProjectTopicsResponse.Group(ProjectTopicGroup.COMMON, "공통", "공통", 85, List.of(
                        new ProjectTopicsResponse.Topic("auth", "인증/인가", 41),
                        new ProjectTopicsResponse.Topic("zero_downtime", "무중단 배포", 18)
                )),
                new ProjectTopicsResponse.Group(ProjectTopicGroup.BE, "백엔드", "BE", 70, List.of(
                        new ProjectTopicsResponse.Topic("concurrency", "동시성 제어/락", 23)
                ))
        )));

        mockMvc.perform(get("/api/v1/projects/topics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groups[0].code").value("COMMON"))
                .andExpect(jsonPath("$.data.groups[1].topics[0].topicId").value("concurrency"))
                .andDo(document(
                        "project-topics",
                        resource(ResourceSnippetParameters.builder()
                                .tag("ProjectResource")
                                .summary("프로젝트 기술 주제 목록 조회")
                                .description(DESCRIPTION)
                                .responseSchema(Schema.schema("ProjectTopicsSuccessResponse"))
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data").type(OBJECT).description("기술 주제 목록"),
                                        fieldWithPath("data.totalCount").type(NUMBER)
                                                .description("주제가 하나라도 있는 공개 프로젝트 수"),
                                        fieldWithPath("data.groups").type(ARRAY)
                                                .description("직군 묶음. COMMON, BE, FE, AN 순이고 주제가 없는 묶음은 빠진다"),
                                        new EnumFields(ProjectTopicGroup.class).withPath("data.groups[].code")
                                                .description("묶음 코드"),
                                        fieldWithPath("data.groups[].name").type(STRING)
                                                .description("묶음 이름. 공통, 백엔드, 프론트엔드, 안드로이드"),
                                        fieldWithPath("data.groups[].shortName").type(STRING)
                                                .description("짧은 이름. 공통, BE, FE, AN"),
                                        fieldWithPath("data.groups[].projectCount").type(NUMBER)
                                                .description("그 묶음 주제를 하나라도 다룬 프로젝트 수"),
                                        fieldWithPath("data.groups[].topics").type(ARRAY)
                                                .description("묶음 안 주제. 프로젝트가 많은 순, 같으면 이름순"),
                                        fieldWithPath("data.groups[].topics[].topicId").type(STRING)
                                                .description("주제 id. 프로젝트 목록 조회의 topicId 조건으로 쓴다"),
                                        fieldWithPath("data.groups[].topics[].name").type(STRING).description("주제 이름"),
                                        fieldWithPath("data.groups[].topics[].projectCount").type(NUMBER)
                                                .description("그 주제를 다룬 프로젝트 수. 1 이상")
                                )
                                .build())
                ));
    }
}
