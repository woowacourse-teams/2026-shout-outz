package com.shoutoutz.api.project.presentation;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.BOOLEAN;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.OBJECT;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.shoutoutz.api.auth.presentation.session.AuthenticatedSession;
import com.shoutoutz.api.cohort.domain.InvalidCohortException;
import com.shoutoutz.api.common.exception.custom.DuplicateEntityException;
import com.shoutoutz.api.common.restdocs.RestDocsFields;
import com.shoutoutz.api.project.application.ProjectService;
import com.shoutoutz.api.project.domain.ProjectErrorCode;
import com.shoutoutz.api.project.domain.exception.InvalidDescriptionMediaException;
import com.shoutoutz.api.project.domain.exception.InvalidProjectCursorException;
import com.shoutoutz.api.project.domain.exception.InvalidProjectMemberException;
import com.shoutoutz.api.project.domain.exception.InvalidTechTagException;
import com.shoutoutz.api.project.domain.exception.InvalidThumbnailException;
import com.shoutoutz.api.project.domain.exception.ProjectRegistrationForbiddenException;
import com.shoutoutz.api.project.presentation.dto.request.ProjectCreateRequest;
import com.shoutoutz.api.project.presentation.dto.request.ProjectFindAllRequest;
import com.shoutoutz.api.project.presentation.dto.response.ProjectCreateResponse;
import com.shoutoutz.api.project.presentation.dto.response.ProjectFindAllResponse;
import com.shoutoutz.api.user.domain.account.UserRole;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
    private static final String DESCRIPTION = "로그인한 우아한테크코스 크루 또는 코치를 등록자로 프로젝트를 등록한다. "
            + "slug는 GitHub 리포지토리 이름에서 앞 연도를 떼고 소문자로 만든다. "
            + "운영 상태는 deploymentUrl이 있으면 OPERATING, 없으면 CLOSED로 저장한다. "
            + "팀원은 등록자를 첫 번째로 두고 memberHandles 순서대로 저장한다. "
            + "본문 이미지는 descriptionMd에 ![설명](media://{mediaId}) 형식으로 넣는다. "
            + "요청값, 기술 스택, 썸네일, 본문 이미지, 팀원이 유효하지 않으면 400, 로그인하지 않았으면 401, "
            + "크루나 코치가 아니면 403, 이미 등록된 리포지토리면 409를 반환한다.";
    private static final String FIND_ALL_SUMMARY = "프로젝트 목록 조회";
    private static final String FIND_ALL_DESCRIPTION = "승인된 프로젝트 목록을 검색어, 기수, 기술 스택으로 걸러 정렬 기준대로 조회한다. "
            + "로그인하지 않아도 조회할 수 있다. 커서 기반으로, 첫 요청은 cursor를 생략하고 "
            + "다음 요청부터 응답의 meta.nextCursor를 그대로 전달한다. 정렬을 바꾸면 cursor 없이 처음부터 다시 요청한다. "
            + "POPULAR에서 좋아요 수가 같으면 최근 등록된 프로젝트가 앞에 온다. "
            + "요청 형식이 올바르지 않거나, 정의되지 않은 기수이거나, 커서가 올바르지 않으면 400을 반환한다.";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectService projectService;

    @Test
    @DisplayName("로그인 사용자를 등록자로 프로젝트를 등록하고 201을 반환한다.")
    void createsProject() throws Exception {
        given(projectService.create(anyLong(), any(ProjectCreateRequest.class)))
                .willReturn(new ProjectCreateResponse(100L, "loop"));

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
                                                .description("서비스 배포 URL (http/https). 빈 문자열은 입력하지 않은 것으로 본다.")
                                                .optional(),
                                        fieldWithPath("descriptionMd").type(STRING)
                                                .description("프로젝트 설명 마크다운 (100,000자 이하). "
                                                        + "이미지는 ![설명](media://{mediaId}) 형식으로 넣으며, "
                                                        + "본인이 업로드한 PROJECT_DESCRIPTION 용도의 처리 완료 이미지만 쓸 수 있다.")
                                                .optional(),
                                        fieldWithPath("techTagIds").type(ARRAY)
                                                .description("선택 가능한 기술 스택 ID 목록. 중복할 수 없으며, 배열 순서가 표시 순서가 된다.")
                                                .attributes(key("itemsType").value("number")),
                                        fieldWithPath("memberHandles").type(ARRAY)
                                                .description("등록자를 제외한 팀원 handle 목록 (1명 이상). "
                                                        + "활동 중인 우아한테크코스 크루 또는 코치여야 하며, "
                                                        + "대소문자만 다른 handle도 같은 사용자로 본다. 배열 순서가 표시 순서가 된다.")
                                                .attributes(key("itemsType").value("string"))
                                )
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data.projectId").type(NUMBER).description("등록된 프로젝트 ID"),
                                        fieldWithPath("data.slug").type(STRING).description("프로젝트 주소로 쓰이는 slug")
                                )
                                .build())
                ));

        verify(projectService).create(eq(7L), any(ProjectCreateRequest.class));
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
        given(projectService.create(anyLong(), any(ProjectCreateRequest.class)))
                .willThrow(new DuplicateEntityException(ProjectErrorCode.PROJECT_DUPLICATE_SLUG));

        mockMvc.perform(post("/api/v1/projects")
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE, new AuthenticatedSession(7L, UserRole.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROJECT_DUPLICATE_SLUG"))
                .andDo(document("project-create-duplicate", resource(errorResource())));
    }

    @Test
    @DisplayName("우아한테크코스 크루나 코치가 아닌 경우, 403을 반환한다.")
    void rejectsForbiddenRegistrant() throws Exception {
        given(projectService.create(anyLong(), any(ProjectCreateRequest.class)))
                .willThrow(new ProjectRegistrationForbiddenException());

        mockMvc.perform(post("/api/v1/projects")
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE, new AuthenticatedSession(7L, UserRole.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PROJECT_REGISTRATION_FORBIDDEN"))
                .andDo(document("project-create-forbidden", resource(errorResource())));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidRegistrationCases")
    @DisplayName("기술 스택, 썸네일, 본문 이미지, 팀원이 유효하지 않은 경우, 400과 원인 에러 코드를 반환한다.")
    void rejectsInvalidRegistration(
            String documentName,
            RuntimeException exception,
            ProjectErrorCode errorCode
    ) throws Exception {
        given(projectService.create(anyLong(), any(ProjectCreateRequest.class))).willThrow(exception);

        mockMvc.perform(post("/api/v1/projects")
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE, new AuthenticatedSession(7L, UserRole.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(errorCode.name()))
                .andDo(document(documentName, resource(errorResource())));
    }

    /**
     * 원인별로 대표 에러 코드 하나씩 문서화한다. 전체 에러 코드는 ProjectErrorCode 를 따른다.
     */
    private static Stream<Arguments> invalidRegistrationCases() {
        return Stream.of(
                Arguments.of("project-create-invalid-tech-tag",
                        new InvalidTechTagException(ProjectErrorCode.PROJECT_INVALID_TECH_TAG),
                        ProjectErrorCode.PROJECT_INVALID_TECH_TAG),
                Arguments.of("project-create-invalid-thumbnail",
                        new InvalidThumbnailException(ProjectErrorCode.PROJECT_INVALID_THUMBNAIL),
                        ProjectErrorCode.PROJECT_INVALID_THUMBNAIL),
                Arguments.of("project-create-invalid-description-media",
                        new InvalidDescriptionMediaException(ProjectErrorCode.PROJECT_INVALID_DESCRIPTION_MEDIA),
                        ProjectErrorCode.PROJECT_INVALID_DESCRIPTION_MEDIA),
                Arguments.of("project-create-invalid-member",
                        new InvalidProjectMemberException(ProjectErrorCode.PROJECT_INVALID_MEMBER),
                        ProjectErrorCode.PROJECT_INVALID_MEMBER)
        );
    }

    @Test
    @DisplayName("로그인하지 않아도 검색어, 기수, 기술 스택, 정렬 조건으로 프로젝트 목록을 조회하고 200을 반환한다.")
    void findsProjects() throws Exception {
        given(projectService.findAll(any(ProjectFindAllRequest.class))).willReturn(new ProjectFindAllResponse(
                List.of(new ProjectFindAllResponse.Item(
                        100L, "loop", "루프 (Loop)", "스프린트 회고와 액션 아이템을 하나로 엮은 실시간 협업 도구",
                        6, 12L, 184L, 14L)),
                new ProjectFindAllResponse.Meta("UE9QVUxBUnwxODR8MjAyNi0wOC0wOVQwMjozMDowMFp8MTAw", true, 48L)
        ));

        mockMvc.perform(get("/api/v1/projects")
                        .queryParam("keyword", "루프")
                        .queryParam("cohorts", "6,7")
                        .queryParam("techTagIds", "1,2")
                        .queryParam("sort", "POPULAR")
                        .queryParam("size", "8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data[0].id").value(100))
                .andExpect(jsonPath("$.data[0].likeCount").value(184))
                .andExpect(jsonPath("$.meta.hasNext").value(true))
                .andExpect(jsonPath("$.meta.totalCount").value(48))
                .andDo(document(
                        "project-find-all",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Project")
                                .summary(FIND_ALL_SUMMARY)
                                .description(FIND_ALL_DESCRIPTION)
                                .queryParameters(
                                        parameterWithName("keyword")
                                                .description("프로젝트 이름, 한 줄 소개, 참여 크루 이름, 기술 스택 이름 검색어. "
                                                        + "대소문자를 무시한 부분 일치이며 100자 이하")
                                                .optional(),
                                        parameterWithName("cohorts")
                                                .description("기수 필터. 쉼표로 구분 (ex. 6,7). 선택한 기수 중 하나에 해당하면 조회")
                                                .optional(),
                                        parameterWithName("techTagIds")
                                                .description("기술 스택 필터. 쉼표로 구분 (ex. 1,2,3). 선택한 기술 스택을 모두 사용한 프로젝트만 조회")
                                                .optional(),
                                        parameterWithName("sort")
                                                .description("정렬 기준 (LATEST, POPULAR). 기본값 LATEST")
                                                .optional(),
                                        parameterWithName("size")
                                                .description("한 번에 가져올 프로젝트 수. 기본값 8, 1~50")
                                                .optional(),
                                        parameterWithName("cursor")
                                                .description("다음 페이지 조회용 커서. 첫 요청은 생략하고, 이후 응답의 meta.nextCursor를 그대로 전달")
                                                .optional()
                                )
                                .responseSchema(Schema.schema("ProjectFindAllSuccessResponse"))
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data").type(ARRAY).description("프로젝트 목록"),
                                        fieldWithPath("data[].id").type(NUMBER).description("프로젝트 ID"),
                                        fieldWithPath("data[].slug").type(STRING).description("프로젝트 주소로 쓰이는 slug"),
                                        fieldWithPath("data[].title").type(STRING).description("프로젝트 이름"),
                                        fieldWithPath("data[].tagline").type(STRING).description("한 줄 소개"),
                                        fieldWithPath("data[].cohort").type(NUMBER).description("우아한테크코스 기수"),
                                        fieldWithPath("data[].thumbnailMediaId").type(NUMBER)
                                                .description("썸네일 미디어 ID. 이미지 URL은 GET /api/v1/media/{mediaId}로 받는다.")
                                                .optional(),
                                        fieldWithPath("data[].likeCount").type(NUMBER).description("좋아요 수"),
                                        fieldWithPath("data[].commentCount").type(NUMBER)
                                                .description("삭제되지 않은 댓글 수 (대댓글 포함)"),
                                        fieldWithPath("meta").type(OBJECT).description("페이지네이션 정보"),
                                        fieldWithPath("meta.nextCursor").type(STRING)
                                                .description("다음 페이지 조회에 쓸 커서. 다음 페이지가 없으면 null")
                                                .optional(),
                                        fieldWithPath("meta.hasNext").type(BOOLEAN).description("다음 페이지 존재 여부"),
                                        fieldWithPath("meta.totalCount").type(NUMBER)
                                                .description("검색어와 필터가 적용된 프로젝트 수")
                                )
                                .build())
                ));

        verify(projectService).findAll(new ProjectFindAllRequest("루프", List.of(6, 7), List.of(1L, 2L), "POPULAR", 8, null));
    }

    @Test
    @DisplayName("파라미터를 보내지 않으면 모두 입력하지 않은 값으로 조회한다.")
    void findsProjectsWithoutParameters() throws Exception {
        given(projectService.findAll(any(ProjectFindAllRequest.class))).willReturn(
                new ProjectFindAllResponse(List.of(), new ProjectFindAllResponse.Meta(null, false, 0L)));

        mockMvc.perform(get("/api/v1/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.meta.totalCount").value(0));

        verify(projectService).findAll(new ProjectFindAllRequest(null, null, null, null, null, null));
    }

    @Test
    @DisplayName("조회 개수가 범위를 벗어나면 400과 필드 오류를 반환하고, 서비스를 호출하지 않는다.")
    void rejectsSizeOutOfRange() throws Exception {
        mockMvc.perform(get("/api/v1/projects").queryParam("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details[0].field").value("size"))
                .andDo(document("project-find-all-invalid", resource(findAllErrorResource())));

        verifyNoInteractions(projectService);
    }

    @Test
    @DisplayName("정의되지 않은 정렬 기준이면 400과 필드 오류를 반환하고, 서비스를 호출하지 않는다.")
    void rejectsUnknownSort() throws Exception {
        mockMvc.perform(get("/api/v1/projects").queryParam("sort", "OLDEST"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details[0].field").value("sort"));

        verifyNoInteractions(projectService);
    }

    @Test
    @DisplayName("커서가 올바르지 않으면 400을 반환한다.")
    void rejectsInvalidCursor() throws Exception {
        given(projectService.findAll(any(ProjectFindAllRequest.class))).willThrow(new InvalidProjectCursorException());

        mockMvc.perform(get("/api/v1/projects").queryParam("cursor", "broken"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PROJECT_INVALID_CURSOR"))
                .andDo(document("project-find-all-invalid-cursor", resource(findAllErrorResource())));
    }

    @Test
    @DisplayName("정의되지 않은 기수로 필터링하면 400을 반환한다.")
    void rejectsUndefinedCohort() throws Exception {
        given(projectService.findAll(any(ProjectFindAllRequest.class))).willThrow(new InvalidCohortException());

        mockMvc.perform(get("/api/v1/projects").queryParam("cohorts", "99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_COHORT"))
                .andDo(document("project-find-all-invalid-cohort", resource(findAllErrorResource())));
    }

    private static ResourceSnippetParameters findAllErrorResource() {
        return ResourceSnippetParameters.builder()
                .tag("Project")
                .summary(FIND_ALL_SUMMARY)
                .description(FIND_ALL_DESCRIPTION)
                .responseSchema(Schema.schema("ErrorResponse"))
                .responseFields(RestDocsFields.errorResponse())
                .build();
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
                  "descriptionMd": "## 문제\\n회고 도구와 액션 아이템 관리가 흩어져 있습니다.\\n\\n![회고 화면](media://21)",
                  "techTagIds": [1, 2, 3],
                  "memberHandles": ["zzaekkii", "sangjun121"]
                }
                """;
    }
}
