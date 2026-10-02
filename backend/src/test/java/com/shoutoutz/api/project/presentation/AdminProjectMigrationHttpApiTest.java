package com.shoutoutz.api.project.presentation;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.OBJECT;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.EnumFields;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.epages.restdocs.apispec.SimpleType;
import com.shoutoutz.api.auth.presentation.session.AuthenticatedSession;
import com.shoutoutz.api.project.application.AdminProjectMigrationService;
import com.shoutoutz.api.project.domain.ApprovalStatus;
import com.shoutoutz.api.project.domain.ServiceStatus;
import com.shoutoutz.api.project.presentation.dto.response.AdminProjectMigrationUpdateResponse;
import com.shoutoutz.api.user.domain.account.UserRole;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminProjectMigrationHttpApi.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs
class AdminProjectMigrationHttpApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminProjectMigrationService service;

    @Test
    void updatesProjectDirectly() throws Exception {
        given(service.update(eq(100L), eq(7L), eq(UserRole.ADMIN), any()))
                .willReturn(new AdminProjectMigrationUpdateResponse(
                        100L, List.of("title", "descriptionMd", "techTagIds"),
                        Instant.parse("2026-09-30T00:00:00Z")
                ));

        mockMvc.perform(patch("/api/v1/admin/projects/{projectId}/migration", 100L)
                        .requestAttr(AuthenticatedSession.class.getName(),
                                new AuthenticatedSession(7L, UserRole.ADMIN))
                        .header(HttpHeaders.COOKIE, "JSESSIONID=admin-session")
                        .header("X-CSRF-Token", "csrf-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "수정된 프로젝트",
                                  "descriptionMd": "![화면](https://github.com/user-attachments/assets/example)",
                                  "techTagIds": [3, 1]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.projectId").value(100))
                .andExpect(jsonPath("$.data.updatedFields[2]").value("techTagIds"))
                .andDo(document("admin-project-migration-update",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Admin Project Migration")
                                .summary("관리자 프로젝트 데이터 직접 수정")
                                .description("관리자가 등록자 여부와 관계없이 삭제되지 않은 프로젝트의 projects 컬럼과 기술 태그를 "
                                        + "한 요청으로 직접 수정한다. 생략한 필드는 유지하며, techTagIds는 보내면 전체 교체한다. "
                                        + "팀원 테이블은 수정하지 않는다. 일반 프로젝트 수정 정책은 적용하지 않는다.")
                                .pathParameters(parameterWithName("projectId")
                                        .type(SimpleType.INTEGER).description("프로젝트 ID"))
                                .requestHeaders(
                                        org.springframework.restdocs.headers.HeaderDocumentation.headerWithName(
                                                HttpHeaders.COOKIE).description("관리자 세션의 JSESSIONID"),
                                        org.springframework.restdocs.headers.HeaderDocumentation.headerWithName(
                                                "X-CSRF-Token").description("세션 CSRF 토큰")
                                )
                                .requestSchema(Schema.schema("AdminProjectMigrationUpdateRequest"))
                                .requestFields(requestFields())
                                .responseSchema(Schema.schema("AdminProjectMigrationUpdateSuccessResponse"))
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data").type(OBJECT).description("수정 결과"),
                                        fieldWithPath("data.projectId").type(NUMBER).description("프로젝트 ID"),
                                        fieldWithPath("data.updatedFields").type(ARRAY)
                                                .description("수정한 필드명 목록")
                                                .attributes(key("itemsType").value("string")),
                                        fieldWithPath("data.updatedAt").type(STRING).description("수정 시각")
                                )
                                .build())
                ));

        verify(service).update(eq(100L), eq(7L), eq(UserRole.ADMIN), any());
    }

    @Test
    void rejectsUnauthenticatedRequest() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/projects/{projectId}/migration", 100L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"수정\"}"))
                .andExpect(status().isUnauthorized());
    }

    private static FieldDescriptor[] requestFields() {
        return new FieldDescriptor[]{
                fieldWithPath("cohort").type(NUMBER).description("기수(1~8)").optional(),
                fieldWithPath("teamName").type(STRING).description("팀 이름").optional(),
                fieldWithPath("slug").type(STRING).description("프로젝트 slug").optional(),
                fieldWithPath("title").type(STRING).description("프로젝트 이름").optional(),
                fieldWithPath("tagline").type(STRING).description("한 줄 소개").optional(),
                fieldWithPath("starCount").type(NUMBER).description("GitHub 스타 수. null 허용").optional(),
                fieldWithPath("starSyncedAt").type(STRING)
                        .description("스타 동기화 시각. ISO 날짜·시각 또는 YYYY-MM-DD. null 허용").optional(),
                fieldWithPath("viewCount").type(NUMBER).description("조회수").optional(),
                new EnumFields(ServiceStatus.class).withPath("serviceStatus").description("서비스 상태").optional(),
                new EnumFields(ApprovalStatus.class).withPath("approvalStatus").description("승인 상태").optional(),
                fieldWithPath("descriptionMd").type(STRING).description("마크다운 본문. null 허용").optional(),
                fieldWithPath("githubRepositoryUrl").type(STRING).description("GitHub 저장소 URL").optional(),
                fieldWithPath("deploymentUrl").type(STRING).description("배포 URL. null 허용").optional(),
                fieldWithPath("thumbnailImageId").type(NUMBER).description("썸네일 미디어 ID. null 허용").optional(),
                fieldWithPath("createdAt").type(STRING).description("생성 시각. ISO 날짜·시각 또는 YYYY-MM-DD").optional(),
                fieldWithPath("updatedAt").type(STRING).description("수정 시각. ISO 날짜·시각 또는 YYYY-MM-DD").optional(),
                fieldWithPath("techTagIds").type(ARRAY)
                        .description("기술 태그 ID 전체 목록. 빈 배열이면 모두 제거. 생략하면 유지")
                        .attributes(key("itemsType").value("number")).optional()
        };
    }
}
