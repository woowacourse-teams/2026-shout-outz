package com.shoutoutz.api.project.presentation;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.shoutoutz.api.auth.presentation.session.AuthenticatedSession;
import com.shoutoutz.api.common.exception.custom.DuplicateEntityException;
import com.shoutoutz.api.common.restdocs.RestDocsFields;
import com.shoutoutz.api.project.application.ProjectService;
import com.shoutoutz.api.project.application.dto.command.ProjectCreateCommand;
import com.shoutoutz.api.project.application.dto.result.ProjectCreateResult;
import com.shoutoutz.api.project.domain.ProjectErrorCode;
import com.shoutoutz.api.user.domain.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ProjectHttpApi.class)
@AutoConfigureRestDocs
class ProjectHttpApiTest {

    /**
     * SessionAuthenticationFilter 가 인증 세션을 담는 요청 속성 이름이다.
     * 슬라이스 테스트에는 필터가 없어 로그인 사용자를 직접 넣는다.
     */
    private static final String AUTHENTICATED_SESSION_ATTRIBUTE = AuthenticatedSession.class.getName();
    private static final String SUMMARY = "프로젝트 등록";
    private static final String DESCRIPTION = "로그인 사용자를 등록자로 프로젝트를 등록한다. "
            + "slug는 GitHub 리포지토리 이름에서 앞 연도를 떼고 소문자로 만든다. "
            + "운영 상태는 deploymentUrl이 있으면 OPERATING, 없으면 CLOSED로 저장한다. "
            + "요청값이 유효하지 않으면 400, 로그인하지 않았으면 401, 이미 등록된 리포지토리면 409를 반환한다.";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectService projectService;

    @Test
    @DisplayName("로그인 사용자를 등록자로 프로젝트를 등록하고 201을 반환한다.")
    void createsProject() throws Exception {
        given(projectService.create(any(ProjectCreateCommand.class)))
                .willReturn(new ProjectCreateResult(100L, "loop"));

        mockMvc.perform(post("/api/v1/projects")
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE, new AuthenticatedSession(7L, UserRole.USER))
                        .header("X-CSRF-Token", "csrf-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.projectId").value(100))
                .andExpect(jsonPath("$.data.slug").value("loop"))
                .andDo(document(
                        "project-create",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Project")
                                .summary(SUMMARY)
                                .description(DESCRIPTION)
                                .requestHeaders(
                                        headerWithName("X-CSRF-Token").description("세션 조회로 발급받은 CSRF 토큰")
                                )
                                .requestSchema(Schema.schema("ProjectCreateRequest"))
                                .responseSchema(Schema.schema("ProjectCreateSuccessResponse"))
                                .requestFields(
                                        fieldWithPath("title").type(STRING).description("프로젝트 이름 (100자 이하)"),
                                        fieldWithPath("teamName").type(STRING).description("팀 이름 (50자 이하)"),
                                        fieldWithPath("tagline").type(STRING).description("한 줄 소개 (200자 이하)"),
                                        fieldWithPath("cohort").type(NUMBER).description("우아한테크코스 기수 (1~8)"),
                                        fieldWithPath("thumbnailMediaId").type(NUMBER)
                                                .description("본인이 업로드한 PROJECT_THUMBNAIL 용도의 처리 완료 이미지 ID")
                                                .optional(),
                                        fieldWithPath("githubRepositoryUrl").type(STRING)
                                                .description("https://github.com/{owner}/{repo} 형식. 리포지토리 이름으로 slug를 만든다."),
                                        fieldWithPath("deploymentUrl").type(STRING)
                                                .description("서비스 배포 URL (http/https)").optional(),
                                        fieldWithPath("descriptionMd").type(STRING)
                                                .description("프로젝트 설명 마크다운 (100,000자 이하)").optional(),
                                        fieldWithPath("techTagIds").type(ARRAY)
                                                .description("선택 가능한 기술 스택 ID 목록. 중복할 수 없으며, 배열 순서가 표시 순서가 된다.")
                                                .attributes(key("itemsType").value("number")),
                                        fieldWithPath("memberHandles").type(ARRAY)
                                                .description("등록자를 제외한 크루 팀원 handle 목록. 배열 순서가 표시 순서가 된다.")
                                                .attributes(key("itemsType").value("string"))
                                )
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data.projectId").type(NUMBER).description("등록된 프로젝트 ID"),
                                        fieldWithPath("data.slug").type(STRING).description("프로젝트 주소로 쓰이는 slug")
                                )
                                .build())
                ));

        ArgumentCaptor<ProjectCreateCommand> captor = ArgumentCaptor.forClass(ProjectCreateCommand.class);
        verify(projectService).create(captor.capture());
        assertThat(captor.getValue().registeredBy()).isEqualTo(7L);
    }

    @Test
    @DisplayName("프로젝트 등록 요청에 필수값이 없는 경우, 400과 필드 오류를 반환하고, 서비스를 호출하지 않는다.")
    void rejectsInvalidRegistrationRequest() throws Exception {
        mockMvc.perform(post("/api/v1/projects")
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE, new AuthenticatedSession(7L, UserRole.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson().replace("\"title\": \"루프 (Loop)\",", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.length()").value(1))
                .andExpect(jsonPath("$.details[0].field").value("title"))
                .andDo(document("project-create-invalid", resource(errorResource())));

        verifyNoInteractions(projectService);
    }

    @Test
    @DisplayName("로그인하지 않고 프로젝트 등록을 요청하는 경우, 요청값 검증보다 먼저 401을 반환한다.")
    void rejectsUnauthenticatedRegistration() throws Exception {
        mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andDo(document("project-create-unauthorized", resource(errorResource())));

        verifyNoInteractions(projectService);
    }

    @Test
    @DisplayName("이미 등록된 리포지토리인 경우, 409를 반환한다.")
    void rejectsDuplicateRepository() throws Exception {
        given(projectService.create(any(ProjectCreateCommand.class)))
                .willThrow(new DuplicateEntityException(ProjectErrorCode.PROJECT_DUPLICATE_SLUG));

        mockMvc.perform(post("/api/v1/projects")
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE, new AuthenticatedSession(7L, UserRole.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROJECT_DUPLICATE_SLUG"))
                .andDo(document("project-create-duplicate", resource(errorResource())));
    }

    private static ResourceSnippetParameters errorResource() {
        return ResourceSnippetParameters.builder()
                .tag("Project")
                .summary(SUMMARY)
                .description(DESCRIPTION)
                .requestSchema(Schema.schema("ProjectCreateRequest"))
                .responseSchema(Schema.schema("ErrorResponse"))
                .responseFields(RestDocsFields.errorResponse())
                .build();
    }

    private static String validRequestJson() {
        return """
                {
                  "title": "루프 (Loop)",
                  "teamName": "루프팀",
                  "tagline": "스프린트 회고와 액션 아이템을 하나로 엮은 실시간 협업 도구",
                  "cohort": 6,
                  "thumbnailMediaId": 12,
                  "githubRepositoryUrl": "https://github.com/woowacourse-teams/2026-loop",
                  "deploymentUrl": "https://loop.team",
                  "descriptionMd": "## 문제\\n회고 도구와 액션 아이템 관리가 흩어져 있습니다.",
                  "techTagIds": [1, 2, 3],
                  "memberHandles": ["dhyepark", "zzaekkii"]
                }
                """;
    }
}
