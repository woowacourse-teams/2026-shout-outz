package com.shoutoutz.api.visitor.presentation;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.shoutoutz.api.visitor.application.VisitorService;
import com.shoutoutz.api.visitor.presentation.dto.request.VisitorSaveRequest;
import com.shoutoutz.api.visitor.presentation.dto.response.VisitorSaveResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * REST DOCS 문서화를 위한 테스트코드 작성 대표 예시
 * TODO: 현재 해당 코드도 예시코드입니다. 방문자(Visitor) 도메인 담당자는 해당 코드도 함께 수정 바랍니다.
 *
 * @author josangjun
 */

/**
 * 컨트롤러 계층 슬라이스 테스트
 */
@DisplayName("방문자 API")
@WebMvcTest(controllers = VisitorHttpApi.class)
@AutoConfigureRestDocs
class VisitorHttpApiTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VisitorService visitorService;

    @Test
    @DisplayName("방문자 생성 요청이 성공하면 201 Created와 생성된 방문자 정보를 반환한다")
    void createVisitor() throws Exception {
        given(visitorService.createVisitor(any(VisitorSaveRequest.class)))
                .willReturn(new VisitorSaveResponse(1L, "example"));

        mockMvc.perform(post("/api/v1/visitors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "example": "example"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.example").value("example"))
                /**
                 * @author josangjun
                 * 여기서부터 API 문서화 도구 코드이다.
                 *  - Spring REST Docs
                 *  - resource.json 생성 및 OpenAPI(OAS)로 변환해주는 ePages의 restdocs-api-spec
                 *
                 * 따라서, API 명세에 포함할 컨트롤러 테스터에만 작성하면 된다.
                 * 주의. document, resource, ResourceSnippetParameters, req/resSchema는 springframework.restdocs이 아닌, epages.restdocs.apispec를 사용
                 * 이외는 REST DOCS의 테스트코드 작성법과 동일하다.
                 */
                .andDo(document(
                        "visitor-create",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Visitor")
                                .summary("방문자 생성")
                                .description("방문자를 생성한다.")
                                .requestSchema(Schema.schema("VisitorSaveRequest"))
                                .responseSchema(Schema.schema("VisitorSaveResponse"))
                                .requestFields(
                                        fieldWithPath("example")
                                                .type(STRING)
                                                .description("방문자 예시 값")
                                )
                                .responseFields(
                                        fieldWithPath("id")
                                                .type(NUMBER)
                                                .description("방문자 ID"),
                                        fieldWithPath("example")
                                                .type(STRING)
                                                .description("방문자 예시 값")
                                )
                                .build())
                ));
    }
}
