package com.shoutoutz.api.project.presentation;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.BOOLEAN;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
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
import com.shoutoutz.api.common.exception.custom.DuplicateEntityException;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.restdocs.RestDocsFields;
import com.shoutoutz.api.project.application.ProjectService;
import com.shoutoutz.api.project.domain.ApprovalStatus;
import com.shoutoutz.api.project.domain.ProjectErrorCode;
import com.shoutoutz.api.project.domain.ServiceStatus;
import com.shoutoutz.api.project.domain.exception.InvalidDescriptionMediaException;
import com.shoutoutz.api.project.domain.exception.InvalidProjectMemberException;
import com.shoutoutz.api.project.domain.exception.InvalidTechTagException;
import com.shoutoutz.api.project.domain.exception.InvalidThumbnailException;
import com.shoutoutz.api.project.domain.exception.ProjectRegistrationForbiddenException;
import com.shoutoutz.api.project.presentation.dto.request.ProjectCreateRequest;
import com.shoutoutz.api.project.presentation.dto.response.ProjectCreateResponse;
import com.shoutoutz.api.project.presentation.dto.response.ProjectDetailResponse;
import com.shoutoutz.api.user.domain.account.UserRole;
import java.time.Instant;
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
    private static final String DETAIL_SUMMARY = "프로젝트 상세 조회";
    private static final String DETAIL_DESCRIPTION = "프로젝트 상세 화면에 필요한 기본 정보, 상세 설명, 팀원, 기술 스택, 외부 링크, "
            + "리액션 및 댓글 수를 조회한다. 로그인하지 않아도 조회할 수 있다. "
            + "승인된 프로젝트는 누구나, 승인되지 않은 프로젝트는 등록자만 조회할 수 있으며, "
            + "볼 수 없는 프로젝트는 존재 여부를 숨기기 위해 없는 프로젝트와 같은 404를 반환한다. "
            + "registeredBy가 null이면 이전 기수에서 이관된 프로젝트다. "
            + "프로젝트 ID가 숫자가 아니면 400을 반환한다.";

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
    @DisplayName("비로그인 사용자도 프로젝트를 상세 조회할 수 있고, 200과 상세 정보를 반환한다.")
    void findsProjectDetail() throws Exception {
        given(projectService.findDetail(100L, null)).willReturn(projectDetailResponse());

        mockMvc.perform(get("/api/v1/projects/{projectId}", 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(100))
                .andExpect(jsonPath("$.data.registeredBy").value(7))
                .andExpect(jsonPath("$.data.rejectReason").value(nullValue()))
                .andExpect(jsonPath("$.data.likedByMe").value(false))
                .andExpect(jsonPath("$.data.techTags[0].displayName").value("React"))
                .andExpect(jsonPath("$.data.members[0].handle").value("dhyepark"))
                .andExpect(jsonPath("$.data.members[0].githubAvatarUrl").value(nullValue()))
                .andDo(document(
                        "project-find-detail",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Project")
                                .summary(DETAIL_SUMMARY)
                                .description(DETAIL_DESCRIPTION)
                                .pathParameters(
                                        parameterWithName("projectId").description("조회할 프로젝트 ID")
                                )
                                .responseSchema(Schema.schema("ProjectFindDetailSuccessResponse"))
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data.id").type(NUMBER).description("프로젝트 ID"),
                                        fieldWithPath("data.slug").type(STRING).description("프로젝트 주소로 쓰이는 slug"),
                                        fieldWithPath("data.title").type(STRING).description("프로젝트 이름"),
                                        fieldWithPath("data.teamName").type(STRING).description("팀 이름"),
                                        fieldWithPath("data.tagline").type(STRING).description("한 줄 소개"),
                                        fieldWithPath("data.cohort").type(NUMBER).description("우아한테크코스 기수"),
                                        fieldWithPath("data.thumbnailMediaId").type(NUMBER)
                                                .description("썸네일 미디어 ID. 이미지 URL은 GET /api/v1/media/{mediaId}로 받는다.")
                                                .optional(),
                                        fieldWithPath("data.descriptionMd").type(STRING)
                                                .description("프로젝트 설명 마크다운. 본문 이미지는 media://{mediaId} 형식으로 들어 있다.")
                                                .optional(),
                                        fieldWithPath("data.githubRepositoryUrl").type(STRING).description("GitHub 리포지토리 URL"),
                                        fieldWithPath("data.deploymentUrl").type(STRING).description("서비스 배포 URL").optional(),
                                        fieldWithPath("data.serviceStatus").type(STRING).description("운영 상태 (OPERATING, CLOSED)"),
                                        fieldWithPath("data.approvalStatus").type(STRING)
                                                .description("승인 상태 (PENDING, APPROVED, REJECTED)"),
                                        fieldWithPath("data.rejectReason").type(STRING)
                                                .description("반려 사유. REJECTED일 때만 값이 있고 그 외에는 null이다.")
                                                .optional(),
                                        fieldWithPath("data.registeredBy").type(NUMBER)
                                                .description("등록자 사용자 ID. null이면 이전 기수에서 이관된 프로젝트다.")
                                                .optional(),
                                        fieldWithPath("data.viewCount").type(NUMBER).description("조회수"),
                                        fieldWithPath("data.starCount").type(NUMBER)
                                                .description("GitHub star 수. 동기화 전이면 null이다.")
                                                .optional(),
                                        fieldWithPath("data.likeCount").type(NUMBER).description("좋아요 수"),
                                        fieldWithPath("data.bookmarkCount").type(NUMBER).description("북마크 수"),
                                        fieldWithPath("data.likedByMe").type(BOOLEAN)
                                                .description("요청자의 좋아요 여부. 비로그인이면 false다."),
                                        fieldWithPath("data.bookmarkedByMe").type(BOOLEAN)
                                                .description("요청자의 북마크 여부. 비로그인이면 false다."),
                                        fieldWithPath("data.commentCount").type(NUMBER)
                                                .description("삭제되지 않은 댓글 수 (대댓글 포함)"),
                                        fieldWithPath("data.techTags").type(ARRAY).description("기술 스택 목록. 등록 순서대로 정렬한다."),
                                        fieldWithPath("data.techTags[].id").type(NUMBER).description("기술 스택 ID"),
                                        fieldWithPath("data.techTags[].displayName").type(STRING).description("기술 스택 이름"),
                                        fieldWithPath("data.members").type(ARRAY)
                                                .description("팀원 목록. 신규 프로젝트는 등록 순서대로이며 등록자가 첫 번째다. "
                                                        + "userId가 registeredBy와 같은 팀원이 작성자다."),
                                        fieldWithPath("data.members[].userId").type(NUMBER)
                                                .description("사용자 ID. 가입하지 않은 이관 팀원은 null이다.")
                                                .optional(),
                                        fieldWithPath("data.members[].handle").type(STRING)
                                                .description("프로필 페이지 이동용 handle. 가입하지 않은 이관 팀원은 null이다.")
                                                .optional(),
                                        fieldWithPath("data.members[].displayName").type(STRING)
                                                .description("표시 이름. 탈퇴한 팀원은 '탈퇴한 사용자', "
                                                        + "가입하지 않은 이관 팀원은 GitHub 이름(없으면 GitHub 아이디)이다."),
                                        fieldWithPath("data.members[].cohort").type(NUMBER)
                                                .description("기수. 가입하지 않은 이관 팀원은 프로젝트 기수다.")
                                                .optional(),
                                        fieldWithPath("data.members[].track").type(STRING).description("트랙").optional(),
                                        fieldWithPath("data.members[].avatarImageId").type(NUMBER)
                                                .description("프로필 이미지 미디어 ID")
                                                .optional(),
                                        fieldWithPath("data.members[].githubAvatarUrl").type(STRING)
                                                .description("GitHub 프로필 이미지 URL. 가입하지 않은 이관 팀원만 값이 있다.")
                                                .optional(),
                                        fieldWithPath("data.members[].githubProfileUrl").type(STRING)
                                                .description("GitHub 프로필 URL. 가입하지 않은 이관 팀원만 값이 있다.")
                                                .optional(),
                                        fieldWithPath("data.createdAt").type(STRING).description("등록 시각"),
                                        fieldWithPath("data.updatedAt").type(STRING).description("수정 시각")
                                )
                                .build())
                ));

        verify(projectService).findDetail(100L, null);
    }

    @Test
    @DisplayName("로그인한 경우, 로그인 사용자 ID로 상세 조회한다.")
    void findsProjectDetailWithLoginUser() throws Exception {
        given(projectService.findDetail(100L, 7L)).willReturn(projectDetailResponse());

        mockMvc.perform(get("/api/v1/projects/{projectId}", 100L)
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE, new AuthenticatedSession(7L, UserRole.USER)))
                .andExpect(status().isOk());

        verify(projectService).findDetail(100L, 7L);
    }

    @Test
    @DisplayName("없거나 볼 수 없는 프로젝트인 경우, 404를 반환한다.")
    void rejectsMissingProjectDetail() throws Exception {
        given(projectService.findDetail(100L, null))
                .willThrow(new EntityNotFoundException(ProjectErrorCode.PROJECT_NOT_FOUND));

        mockMvc.perform(get("/api/v1/projects/{projectId}", 100L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"))
                .andDo(document("project-find-detail-not-found", resource(detailErrorResource())));
    }

    @Test
    @DisplayName("프로젝트 ID가 숫자가 아닌 경우, 400을 반환하고 서비스를 호출하지 않는다.")
    void rejectsNonNumericProjectId() throws Exception {
        mockMvc.perform(get("/api/v1/projects/{projectId}", "moamoa"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andDo(document("project-find-detail-invalid-id", resource(detailErrorResource())));

        verifyNoInteractions(projectService);
    }

    private static ResourceSnippetParameters detailErrorResource() {
        return ResourceSnippetParameters.builder()
                .tag("Project")
                .summary(DETAIL_SUMMARY)
                .description(DETAIL_DESCRIPTION)
                .pathParameters(
                        parameterWithName("projectId").description("조회할 프로젝트 ID")
                )
                .responseSchema(Schema.schema("ErrorResponse"))
                .responseFields(RestDocsFields.errorResponse())
                .build();
    }

    private static ProjectDetailResponse projectDetailResponse() {
        return new ProjectDetailResponse(
                100L,
                "loop",
                "루프 (Loop)",
                "루프팀",
                "스프린트 회고와 액션 아이템을 하나로 엮은 실시간 협업 도구",
                6,
                12L,
                "## 문제\n회고 도구와 액션 아이템 관리가 흩어져 있습니다.",
                "https://github.com/woowacourse-teams/2026-loop",
                "https://loop.team",
                ServiceStatus.OPERATING,
                ApprovalStatus.APPROVED,
                null,
                7L,
                831,
                128,
                84,
                28,
                false,
                false,
                18,
                List.of(
                        new ProjectDetailResponse.TechTag(1L, "React"),
                        new ProjectDetailResponse.TechTag(2L, "TypeScript")
                ),
                List.of(
                        new ProjectDetailResponse.Member(7L, "dhyepark", "박다혜", 6, "BE", 101L, null, null),
                        new ProjectDetailResponse.Member(8L, "zzaekkii", "김도현", 6, "FE", null, null, null)
                ),
                Instant.parse("2026-08-09T02:30:00Z"),
                Instant.parse("2026-08-09T03:00:00Z")
        );
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
