package com.shoutoutz.api.project.presentation;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.OBJECT;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.restdocs.RestDocsFields;
import com.shoutoutz.api.project.application.ProjectViewService;
import com.shoutoutz.api.project.domain.ProjectErrorCode;
import com.shoutoutz.api.project.presentation.dto.response.ProjectViewRecordResponse;
import com.shoutoutz.api.visitor.domain.VisitorKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ProjectViewHttpApi.class)
@AutoConfigureRestDocs
class ProjectViewHttpApiTest {

    /**
     * VisitorCookieFilter 가 방문자 키를 담는 요청 속성 이름이다.
     * 슬라이스 테스트에는 필터가 없어 방문자 키를 직접 넣는다.
     */
    private static final String VISITOR_KEY_ATTRIBUTE = VisitorKey.class.getName();
    private static final VisitorKey VISITOR_KEY = new VisitorKey("a".repeat(64));
    private static final String SUMMARY = "프로젝트 조회 기록";
    private static final String DESCRIPTION = "프로젝트 상세 페이지에 들어갈 때 조회를 기록하고, 이번 조회를 반영한 조회수를 반환한다. "
            + "상세 조회 API와 함께(병렬로) 호출하고, 화면의 조회수는 상세 조회 응답으로 먼저 그린 뒤 이 응답의 viewCount로 바꾼다. "
            + "같은 방문자가 같은 날(한국 시간 기준) 같은 프로젝트를 여러 번 조회해도 조회수는 한 번만 오르며, "
            + "다시 조회하면 200과 현재 조회수를 반환한다. "
            + "방문자는 서버가 발급한 VISITOR_ID 쿠키로 구분하므로 쿠키를 함께 보내도록 호출한다(credentials: include). "
            + "쿠키가 없거나 형식이 잘못되면 이 응답에서 새로 발급한다. "
            + "로그인하지 않아도 기록하며, CSRF 토큰은 로그인 상태에서만 필요하다. "
            + "없거나 삭제됐거나 승인되지 않은 프로젝트면 404를 반환한다. 이 API가 실패해도 상세 페이지 표시에는 영향이 없도록 무시한다.";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectViewService projectViewService;

    @Test
    @DisplayName("프로젝트 조회를 기록하면 200과 이번 조회를 반영한 조회수를 반환한다.")
    void recordsView() throws Exception {
        given(projectViewService.record(100L, VISITOR_KEY)).willReturn(new ProjectViewRecordResponse(129));

        mockMvc.perform(post("/api/v1/projects/{projectId}/views", 100L)
                        .requestAttr(VISITOR_KEY_ATTRIBUTE, VISITOR_KEY)
                        .header(HttpHeaders.COOKIE, "VISITOR_ID=3f2a1b4c-5d6e-4f70-8a9b-0c1d2e3f4a5b"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.viewCount").value(129))
                .andDo(document(
                        "project-view-record",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Project")
                                .summary(SUMMARY)
                                .description(DESCRIPTION)
                                .pathParameters(
                                        parameterWithName("projectId").description("조회한 프로젝트 ID")
                                )
                                .requestHeaders(
                                        headerWithName(HttpHeaders.COOKIE)
                                                .description("서버가 발급한 VISITOR_ID. 로그인 상태면 JSESSIONID도 함께 보낸다. "
                                                        + "처음 방문해 쿠키가 없으면 생략된다.")
                                                .optional()
                                )
                                .responseSchema(Schema.schema("ProjectViewRecordSuccessResponse"))
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data").type(OBJECT).description("조회 기록 결과"),
                                        fieldWithPath("data.viewCount").type(NUMBER)
                                                .description("이번 조회를 반영한 조회수. 같은 날 다시 조회해 집계되지 않았으면 현재 조회수다."),
                                        fieldWithPath("meta").type(OBJECT).description("메타 정보").optional()
                                )
                                .build())
                ));
    }

    @Test
    @DisplayName("없거나 삭제됐거나 승인되지 않은 프로젝트이면, 404를 반환한다.")
    void rejectsNotViewableProject() throws Exception {
        willThrow(new EntityNotFoundException(ProjectErrorCode.PROJECT_NOT_FOUND))
                .given(projectViewService).record(eq(100L), any());

        mockMvc.perform(post("/api/v1/projects/{projectId}/views", 100L)
                        .requestAttr(VISITOR_KEY_ATTRIBUTE, VISITOR_KEY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"))
                .andDo(document(
                        "project-view-record-not-found",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Project")
                                .summary(SUMMARY)
                                .description(DESCRIPTION)
                                .pathParameters(
                                        parameterWithName("projectId").description("조회한 프로젝트 ID")
                                )
                                .responseSchema(Schema.schema("ErrorResponse"))
                                .responseFields(RestDocsFields.errorResponse())
                                .build())
                ));
    }
}
