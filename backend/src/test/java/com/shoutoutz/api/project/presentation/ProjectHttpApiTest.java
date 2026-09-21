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
import static org.springframework.restdocs.payload.JsonFieldType.OBJECT;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.EnumFields;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.shoutoutz.api.auth.presentation.session.AuthenticatedSession;
import com.shoutoutz.api.cohort.domain.InvalidCohortException;
import com.shoutoutz.api.common.exception.custom.ConflictException;
import com.shoutoutz.api.common.exception.custom.DuplicateEntityException;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.restdocs.RestDocsFields;
import com.shoutoutz.api.project.application.ProjectService;
import com.shoutoutz.api.project.domain.ApprovalStatus;
import com.shoutoutz.api.project.domain.DeletedProject;
import com.shoutoutz.api.project.domain.ProjectDeletion;
import com.shoutoutz.api.project.domain.ProjectErrorCode;
import com.shoutoutz.api.project.domain.RestoredProject;
import com.shoutoutz.api.project.domain.ServiceStatus;
import com.shoutoutz.api.project.domain.exception.InvalidDescriptionMediaException;
import com.shoutoutz.api.project.domain.exception.InvalidProjectCursorException;
import com.shoutoutz.api.project.domain.exception.InvalidProjectMemberException;
import com.shoutoutz.api.project.domain.exception.InvalidTechTagException;
import com.shoutoutz.api.project.domain.exception.InvalidThumbnailException;
import com.shoutoutz.api.project.domain.exception.ProjectRegistrationForbiddenException;
import com.shoutoutz.api.project.presentation.dto.request.ProjectCreateRequest;
import com.shoutoutz.api.project.presentation.dto.request.ProjectFilterOptionsRequest;
import com.shoutoutz.api.project.presentation.dto.request.ProjectFindAllRequest;
import com.shoutoutz.api.project.presentation.dto.request.ProjectUpdateRequest;
import com.shoutoutz.api.project.presentation.dto.response.ProjectCreateResponse;
import com.shoutoutz.api.project.presentation.dto.response.ProjectDetailResponse;
import com.shoutoutz.api.project.presentation.dto.response.ProjectFilterOptionsResponse;
import com.shoutoutz.api.project.presentation.dto.response.ProjectFindAllResponse;
import com.shoutoutz.api.project.presentation.dto.response.ProjectMemberProfileResponse;
import com.shoutoutz.api.project.presentation.dto.response.ProjectTechTagResponse;
import com.shoutoutz.api.project.presentation.dto.response.ProjectUpdateResponse;
import com.shoutoutz.api.user.domain.account.UserRole;
import com.shoutoutz.api.user.domain.profile.Track;
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
            + "크루나 코치가 아니면 403, 이미 등록된 리포지토리이거나 리포지토리 이름이 같아 slug가 겹치면 409를 반환한다.";
    private static final String UPDATE_SUMMARY = "프로젝트 수정";
    private static final String UPDATE_DESCRIPTION = "작성자가 프로젝트 정보를 수정한다. 대부분의 필드는 전체 교체 방식이며, "
            + "비우는 값은 null로 보낸다. thumbnailImageId는 생략하면 기존 이미지를 유지하고, "
            + "새 ID를 보내면 교체하며, null을 명시하면 제거한다. "
            + "descriptionMd는 저장 시 media://{mediaId} 형식으로 정규화하며, 상세 조회 응답의 CDN URL은 기존 프로젝트 미디어와 "
            + "매칭되는 경우에만 본문 이미지 참조로 복원한다. 매칭되지 않는 외부 이미지 URL은 허용하지 않는다. "
            + "techTagIds와 memberHandles도 전체 목록을 순서대로 보낸다. "
            + "반려된 프로젝트를 수정하면 재심사 요청으로 처리되어 approvalStatus가 PENDING으로 바뀌고, "
            + "그 밖의 상태는 그대로 유지된다. slug는 등록 시점 값으로 고정이라 바뀌지 않는다. "
            + "요청값과 기술 스택, 썸네일, 본문 이미지, 팀원이 유효하지 않으면 400, 로그인하지 않았으면 401, "
            + "없거나 삭제됐거나 다른 사람의 프로젝트면 404, 이미 등록된 리포지토리로 바꾸면 409를 반환한다.";
    private static final String FIND_ALL_SUMMARY = "프로젝트 목록 조회";
    private static final String FIND_ALL_DESCRIPTION = "승인된 프로젝트 목록을 검색어, 기수, 기술 스택으로 걸러 정렬 기준대로 조회한다. "
            + "로그인하지 않아도 조회할 수 있다. 커서 기반으로, 첫 요청은 cursor를 생략하고 "
            + "다음 요청부터 응답의 meta.nextCursor를 그대로 전달한다. 정렬을 바꾸면 cursor 없이 처음부터 다시 요청한다. "
            + "POPULAR에서 좋아요 수가 같으면 최근 등록된 프로젝트가 앞에 온다. "
            + "요청 형식이 올바르지 않거나, 정의되지 않은 기수이거나, 커서가 올바르지 않으면 400을 반환한다.";
    private static final String DELETE_SUMMARY = "프로젝트 삭제";
    private static final String DELETE_DESCRIPTION = "등록자 본인이 자신의 프로젝트를 삭제한다. 심사 중인 프로젝트도 삭제할 수 있다. "
            + "삭제된 프로젝트는 목록과 상세에서 보이지 않으며, 응답의 restoreDeadlineAt 까지 복구할 수 있다. "
            + "로그인하지 않았으면 401, 없는 프로젝트이거나 등록자가 아니거나 이미 삭제된 프로젝트이면 404를 반환한다.";
    private static final String RESTORE_SUMMARY = "프로젝트 복구";
    private static final String RESTORE_DESCRIPTION = "등록자 본인이 삭제한 자신의 프로젝트를 복구 기한 안에 되살린다. "
            + "승인 상태는 삭제 이전 값을 그대로 유지한다. "
            + "로그인하지 않았으면 401, 없는 프로젝트이거나 등록자가 아니거나 삭제되지 않은 프로젝트이면 404, "
            + "복구 기한이 지났으면 409를 반환한다. 복구 기한이 지난 프로젝트는 이후 영구 삭제되어 복구할 수 없다.";
    private static final String DETAIL_SUMMARY = "프로젝트 상세 조회";
    private static final String DETAIL_DESCRIPTION = "프로젝트 상세 화면에 필요한 기본 정보, 상세 설명, 팀원, 기술 스택, 외부 링크, "
            + "리액션 및 댓글 수를 조회한다. 로그인하지 않아도 조회할 수 있다. "
            + "승인된 프로젝트는 누구나, 승인되지 않은 프로젝트는 등록자만 조회할 수 있으며, "
            + "볼 수 없는 프로젝트는 존재 여부를 숨기기 위해 없는 프로젝트와 같은 404를 반환한다. "
            + "editable은 요청자가 등록자 본인인지를 나타내며, 수정·삭제 버튼 노출에 쓴다. "
            + "프로젝트 ID가 숫자가 아니면 400을 반환한다.";
    private static final String FILTER_OPTIONS_SUMMARY = "프로젝트 필터 옵션 조회";
    private static final String FILTER_OPTIONS_DESCRIPTION = "필터 모달에 보여줄 기수 및 기술 스택 목록과, "
            + "각 항목을 골랐을 때 나오는 프로젝트 수를 조회한다. 로그인하지 않아도 조회할 수 있다. "
            + "목록 조회에 적용 중인 검색어와 모달에서 고른 기수, 기술 스택을 전달하며, 선택이 바뀔 때마다 다시 요청한다. "
            + "기수 숫자는 다른 기수를 골라도 0이 되지 않도록 기수 선택을 빼고 세고, "
            + "기술 스택 숫자는 현재 조건에 그 기술 스택을 추가로 골랐을 때의 수다. 프로젝트 수가 0인 항목도 포함한다. "
            + "모달 안의 기술 스택 검색은 응답의 techTags를 이름으로 걸러 처리한다. "
            + "요청 형식이 올바르지 않거나 정의되지 않은 기수이면 400을 반환한다.";

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
                                        fieldWithPath("data").type(OBJECT).description("등록된 프로젝트"),
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
                .willThrow(new DuplicateEntityException(ProjectErrorCode.PROJECT_DUPLICATE_GITHUB_REPOSITORY));

        mockMvc.perform(post("/api/v1/projects")
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE, new AuthenticatedSession(7L, UserRole.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROJECT_DUPLICATE_GITHUB_REPOSITORY"))
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
                        6, 12L, "https://cdn.example.com/thumbnail", 128, 184L, 14L,
                        List.of(new ProjectTechTagResponse(1L, "React"), new ProjectTechTagResponse(2L, "Spring")),
                        List.of(
                                new ProjectMemberProfileResponse("dhyepark", "박다혜", 6, "BACKEND", 101L, "https://cdn.example.com/avatar-101", null, null),
                                new ProjectMemberProfileResponse("zzaekkii", "김도현", 6, "FRONTEND", null, null, null, null)
                        ))),
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
                .andExpect(jsonPath("$.data[0].thumbnailMediaId").value(12L))
                .andExpect(jsonPath("$.data[0].starCount").value(128))
                .andExpect(jsonPath("$.data[0].likeCount").value(184))
                .andExpect(jsonPath("$.data[0].techTags[0].displayName").value("React"))
                .andExpect(jsonPath("$.data[0].members[0].handle").value("dhyepark"))
                .andExpect(jsonPath("$.data[0].members[0].avatarImageId").value(101L))
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
                                                .description("프로젝트 썸네일 미디어 ID")
                                                .optional(),
                                        fieldWithPath("data[].thumbnailUrl").type(STRING)
                                                .description("CloudFront에서 제공하는 공개 썸네일 URL")
                                                .optional(),
                                        fieldWithPath("data[].starCount").type(NUMBER)
                                                .description("GitHub star 수. 동기화 전이면 null이다.")
                                                .optional(),
                                        fieldWithPath("data[].likeCount").type(NUMBER).description("좋아요 수"),
                                        fieldWithPath("data[].commentCount").type(NUMBER)
                                                .description("삭제되지 않은 댓글 수 (대댓글 포함)"),
                                        fieldWithPath("data[].techTags").type(ARRAY)
                                                .description("기술 스택 전체 목록. 등록 순서대로 정렬하며, 카드에 몇 개까지 보여줄지는 화면에서 정한다."),
                                        fieldWithPath("data[].techTags[].id").type(NUMBER).description("기술 스택 ID"),
                                        fieldWithPath("data[].techTags[].displayName").type(STRING).description("기술 스택 이름"),
                                        fieldWithPath("data[].members").type(ARRAY)
                                                .description("팀원 전체 목록. 상세 조회의 members와 같은 규칙이며, 등록 순서대로 정렬한다."),
                                        fieldWithPath("data[].members[].handle").type(STRING)
                                                .description("프로필 페이지 이동용 handle. 가입하지 않은 이관 팀원은 null이다.")
                                                .optional(),
                                        fieldWithPath("data[].members[].displayName").type(STRING)
                                                .description("표시 이름. 탈퇴한 팀원은 '탈퇴한 사용자', "
                                                        + "가입하지 않은 이관 팀원은 GitHub 이름(없으면 GitHub 아이디)이다."),
                                        fieldWithPath("data[].members[].cohort").type(NUMBER)
                                                .description("기수. 가입하지 않은 이관 팀원은 프로젝트 기수다.")
                                                .optional(),
                                        new EnumFields(Track.class).withPath("data[].members[].track")
                                                .description("트랙")
                                                .optional(),
                                        fieldWithPath("data[].members[].avatarImageId").type(NUMBER)
                                                .description("프로필 이미지 미디어 ID")
                                                .optional(),
                                        fieldWithPath("data[].members[].avatarUrl").type(STRING)
                                                .description("CloudFront에서 제공하는 공개 프로필 이미지 URL")
                                                .optional(),
                                        fieldWithPath("data[].members[].githubAvatarUrl").type(STRING)
                                                .description("GitHub 프로필 이미지 URL. 가입하지 않은 이관 팀원만 값이 있다.")
                                                .optional(),
                                        fieldWithPath("data[].members[].githubProfileUrl").type(STRING)
                                                .description("GitHub 프로필 URL. 가입하지 않은 이관 팀원만 값이 있다.")
                                                .optional(),
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
    @DisplayName("로그인하지 않아도 검색어, 기수, 기술 스택 조건으로 필터 옵션을 조회하고 200을 반환한다.")
    void findsFilterOptions() throws Exception {
        given(projectService.findFilterOptions(any(ProjectFilterOptionsRequest.class)))
                .willReturn(new ProjectFilterOptionsResponse(
                        List.of(
                                new ProjectFilterOptionsResponse.CohortItem(7, 2025, 3L),
                                new ProjectFilterOptionsResponse.CohortItem(6, 2024, 28L)
                        ),
                        List.of(
                                new ProjectFilterOptionsResponse.TechTagItem(2L, "React", 48L),
                                new ProjectFilterOptionsResponse.TechTagItem(1L, "Spring Boot", 51L)
                        ),
                        8L
                ));

        mockMvc.perform(get("/api/v1/projects/filters")
                        .queryParam("keyword", "루프")
                        .queryParam("cohorts", "6,7")
                        .queryParam("techTagIds", "1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.cohorts[1].cohort").value(6))
                .andExpect(jsonPath("$.data.cohorts[1].year").value(2024))
                .andExpect(jsonPath("$.data.cohorts[1].projectCount").value(28))
                .andExpect(jsonPath("$.data.techTags[1].displayName").value("Spring Boot"))
                .andExpect(jsonPath("$.data.techTags[1].projectCount").value(51))
                .andExpect(jsonPath("$.data.matchedProjectCount").value(8))
                .andDo(document(
                        "project-find-filter-options",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Project")
                                .summary(FILTER_OPTIONS_SUMMARY)
                                .description(FILTER_OPTIONS_DESCRIPTION)
                                .queryParameters(
                                        parameterWithName("keyword")
                                                .description("목록 조회에 적용 중인 검색어. 목록 조회의 keyword와 같은 조건으로 검색하며 100자 이하")
                                                .optional(),
                                        parameterWithName("cohorts")
                                                .description("모달에서 고른 기수. 쉼표로 구분 (ex. 6,7)")
                                                .optional(),
                                        parameterWithName("techTagIds")
                                                .description("모달에서 고른 기술 스택. 쉼표로 구분 (ex. 1,2)")
                                                .optional()
                                )
                                .responseSchema(Schema.schema("ProjectFilterOptionsSuccessResponse"))
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data").type(OBJECT).description("필터 옵션"),
                                        fieldWithPath("data.cohorts").type(ARRAY)
                                                .description("전체 기수. 최신 기수부터 정렬하며, 프로젝트 수가 0인 기수도 포함한다."),
                                        fieldWithPath("data.cohorts[].cohort").type(NUMBER).description("우아한테크코스 기수"),
                                        fieldWithPath("data.cohorts[].year").type(NUMBER).description("기수 연도"),
                                        fieldWithPath("data.cohorts[].projectCount").type(NUMBER)
                                                .description("검색어와 기술 스택 조건에 맞는 그 기수의 프로젝트 수. 다른 기수를 골라도 0이 되지 않는다."),
                                        fieldWithPath("data.techTags").type(ARRAY)
                                                .description("활성 기술 스택 전체. 이름순으로 정렬하며, 프로젝트 수가 0인 기술 스택도 포함한다."),
                                        fieldWithPath("data.techTags[].id").type(NUMBER).description("기술 스택 ID"),
                                        fieldWithPath("data.techTags[].displayName").type(STRING).description("기술 스택 이름"),
                                        fieldWithPath("data.techTags[].projectCount").type(NUMBER)
                                                .description("현재 조건에 이 기술 스택을 추가로 골랐을 때의 프로젝트 수. "
                                                        + "이미 고른 기술 스택은 matchedProjectCount와 같다."),
                                        fieldWithPath("data.matchedProjectCount").type(NUMBER)
                                                .description("검색어와 고른 필터를 모두 적용한 프로젝트 수. "
                                                        + "같은 조건의 목록 조회 meta.totalCount와 같다.")
                                )
                                .build())
                ));

        verify(projectService).findFilterOptions(new ProjectFilterOptionsRequest("루프", List.of(6, 7), List.of(1L, 2L)));
    }

    @Test
    @DisplayName("필터 옵션 조회 검색어가 100자를 넘으면 400과 필드 오류를 반환하고, 서비스를 호출하지 않는다.")
    void rejectsTooLongFilterOptionsKeyword() throws Exception {
        mockMvc.perform(get("/api/v1/projects/filters").queryParam("keyword", "가".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details[0].field").value("keyword"))
                .andDo(document("project-find-filter-options-invalid", resource(filterOptionsErrorResource())));

        verifyNoInteractions(projectService);
    }

    @Test
    @DisplayName("필터 옵션 조회에서 정의되지 않은 기수를 고르면 400을 반환한다.")
    void rejectsUndefinedCohortForFilterOptions() throws Exception {
        given(projectService.findFilterOptions(any(ProjectFilterOptionsRequest.class)))
                .willThrow(new InvalidCohortException());

        mockMvc.perform(get("/api/v1/projects/filters").queryParam("cohorts", "99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_COHORT"))
                .andDo(document("project-find-filter-options-invalid-cohort", resource(filterOptionsErrorResource())));
    }

    @Test
    @DisplayName("비로그인 사용자도 프로젝트를 상세 조회할 수 있고, 200과 상세 정보를 반환한다.")
    void findsProjectDetail() throws Exception {
        given(projectService.findDetail(100L, null)).willReturn(projectDetailResponse());

        mockMvc.perform(get("/api/v1/projects/{projectId}", 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(100))
                .andExpect(jsonPath("$.data.thumbnailMediaId").value(12L))
                .andExpect(jsonPath("$.data.descriptionMedia[0].mediaId").value(21L))
                .andExpect(jsonPath("$.data.editable").value(false))
                .andExpect(jsonPath("$.data.rejectReason").value(nullValue()))
                .andExpect(jsonPath("$.data.likedByMe").value(false))
                .andExpect(jsonPath("$.data.techTags[0].displayName").value("React"))
                .andExpect(jsonPath("$.data.members[0].handle").value("dhyepark"))
                .andExpect(jsonPath("$.data.members[0].avatarImageId").value(101L))
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
                                        fieldWithPath("data").type(OBJECT).description("프로젝트 상세"),
                                        fieldWithPath("data.id").type(NUMBER).description("프로젝트 ID"),
                                        fieldWithPath("data.slug").type(STRING).description("프로젝트 주소로 쓰이는 slug"),
                                        fieldWithPath("data.title").type(STRING).description("프로젝트 이름"),
                                        fieldWithPath("data.teamName").type(STRING).description("팀 이름"),
                                        fieldWithPath("data.tagline").type(STRING).description("한 줄 소개"),
                                        fieldWithPath("data.cohort").type(NUMBER).description("우아한테크코스 기수"),
                                        fieldWithPath("data.thumbnailMediaId").type(NUMBER)
                                                .description("프로젝트 썸네일 미디어 ID")
                                                .optional(),
                                        fieldWithPath("data.imageUrl").type(STRING)
                                                .description("CloudFront에서 제공하는 공개 이미지 URL")
                                                .optional(),
                                        fieldWithPath("data.descriptionMd").type(STRING)
                                                .description("프로젝트 설명 마크다운. 본문 이미지 참조는 공개 URL로 변환되어 있다.")
                                                .optional(),
                                        fieldWithPath("data.descriptionMedia").type(ARRAY)
                                                .description("본문 이미지의 미디어 ID와 공개 URL 매핑"),
                                        fieldWithPath("data.descriptionMedia[].mediaId").type(NUMBER)
                                                .description("본문 이미지 미디어 ID"),
                                        fieldWithPath("data.descriptionMedia[].url").type(STRING)
                                                .description("본문 이미지 공개 URL"),
                                        fieldWithPath("data.githubRepositoryUrl").type(STRING).description("GitHub 리포지토리 URL"),
                                        fieldWithPath("data.deploymentUrl").type(STRING).description("서비스 배포 URL").optional(),
                                        new EnumFields(ServiceStatus.class).withPath("data.serviceStatus")
                                                .description("운영 상태"),
                                        new EnumFields(ApprovalStatus.class).withPath("data.approvalStatus")
                                                .description("승인 상태"),
                                        fieldWithPath("data.rejectReason").type(STRING)
                                                .description("반려 사유. REJECTED일 때만 값이 있고 그 외에는 null이다.")
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
                                        fieldWithPath("data.editable").type(BOOLEAN)
                                                .description("요청자가 등록자 본인인지 여부. 수정·삭제할 수 있는 사용자에게만 true다. "
                                                        + "비로그인이거나 이전 기수에서 이관된 프로젝트면 false다."),
                                        fieldWithPath("data.commentCount").type(NUMBER)
                                                .description("삭제되지 않은 댓글 수 (대댓글 포함)"),
                                        fieldWithPath("data.techTags").type(ARRAY).description("기술 스택 목록. 등록 순서대로 정렬한다."),
                                        fieldWithPath("data.techTags[].id").type(NUMBER).description("기술 스택 ID"),
                                        fieldWithPath("data.techTags[].displayName").type(STRING).description("기술 스택 이름"),
                                        fieldWithPath("data.members").type(ARRAY)
                                                .description("팀원 목록. 신규 프로젝트는 등록 순서대로이며 등록자가 첫 번째다."),
                                        fieldWithPath("data.members[].handle").type(STRING)
                                                .description("프로필 페이지 이동용 handle. 가입하지 않은 이관 팀원은 null이다.")
                                                .optional(),
                                        fieldWithPath("data.members[].displayName").type(STRING)
                                                .description("표시 이름. 탈퇴한 팀원은 '탈퇴한 사용자', "
                                                        + "가입하지 않은 이관 팀원은 GitHub 이름(없으면 GitHub 아이디)이다."),
                                        fieldWithPath("data.members[].cohort").type(NUMBER)
                                                .description("기수. 가입하지 않은 이관 팀원은 프로젝트 기수다.")
                                                .optional(),
                                        new EnumFields(Track.class).withPath("data.members[].track")
                                                .description("트랙")
                                                .optional(),
                                        fieldWithPath("data.members[].avatarImageId").type(NUMBER)
                                                .description("프로필 이미지 미디어 ID")
                                                .optional(),
                                        fieldWithPath("data.members[].avatarUrl").type(STRING)
                                                .description("CloudFront에서 제공하는 공개 프로필 이미지 URL")
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

    @Test
    @DisplayName("등록자가 자신의 프로젝트를 삭제하면 200과 삭제 시각, 복구 기한을 반환한다.")
    void deletesProject() throws Exception {
        given(projectService.delete(100L, 7L)).willReturn(projectDeletion());

        mockMvc.perform(delete("/api/v1/projects/{projectId}", 100L)
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE, new AuthenticatedSession(7L, UserRole.USER))
                        .header("X-CSRF-Token", "csrf-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(100))
                .andExpect(jsonPath("$.data.deletedAt").value("2026-09-06T13:30:00Z"))
                .andExpect(jsonPath("$.data.restoreDeadlineAt").value("2026-10-06T13:30:00Z"))
                .andDo(document(
                        "project-delete",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Project")
                                .summary(DELETE_SUMMARY)
                                .description(DELETE_DESCRIPTION)
                                .pathParameters(
                                        parameterWithName("projectId").description("삭제할 프로젝트 ID")
                                )
                                .requestHeaders(
                                        headerWithName("X-CSRF-Token").description("세션 조회로 발급받은 CSRF 토큰")
                                )
                                .responseSchema(Schema.schema("ProjectDeleteSuccessResponse"))
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data").type(OBJECT).description("삭제 결과"),
                                        fieldWithPath("data.id").type(NUMBER).description("삭제한 프로젝트 ID"),
                                        fieldWithPath("data.deletedAt").type(STRING).description("삭제 시각 (UTC)"),
                                        fieldWithPath("data.restoreDeadlineAt").type(STRING)
                                                .description("복구 기한 (UTC). 이 시각까지 복구할 수 있다."),
                                        fieldWithPath("meta").type(OBJECT).description("메타 정보").optional()
                                )
                                .build())
                ));
    }

    @Test
    @DisplayName("없는 프로젝트이거나 등록자가 아니거나 이미 삭제된 프로젝트이면, 404를 반환한다.")
    void rejectsDeletingNotOwnedOrAlreadyDeletedProject() throws Exception {
        given(projectService.delete(100L, 7L))
                .willThrow(new EntityNotFoundException(ProjectErrorCode.PROJECT_NOT_FOUND));

        mockMvc.perform(delete("/api/v1/projects/{projectId}", 100L)
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE, new AuthenticatedSession(7L, UserRole.USER))
                        .header("X-CSRF-Token", "csrf-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"))
                .andDo(document("project-delete-not-found", resource(deleteErrorResource())));
    }

    @Test
    @DisplayName("로그인하지 않고 프로젝트 삭제를 요청하는 경우, 401을 반환하고 서비스를 호출하지 않는다.")
    void rejectsUnauthenticatedDeletion() throws Exception {
        mockMvc.perform(delete("/api/v1/projects/{projectId}", 100L)
                        .header("X-CSRF-Token", "csrf-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andDo(document("project-delete-unauthorized", resource(deleteErrorResource())));

        verifyNoInteractions(projectService);
    }

    private static ResourceSnippetParameters deleteErrorResource() {
        return ResourceSnippetParameters.builder()
                .tag("Project")
                .summary(DELETE_SUMMARY)
                .description(DELETE_DESCRIPTION)
                .pathParameters(
                        parameterWithName("projectId").description("삭제할 프로젝트 ID")
                )
                .responseSchema(Schema.schema("ErrorResponse"))
                .responseFields(RestDocsFields.errorResponse())
                .build();
    }

    private static ProjectDeletion projectDeletion() {
        return ProjectDeletion.selfDelete(
                new DeletedProject(100L, "loop", "루프 (Loop)"),
                7L,
                Instant.parse("2026-09-06T13:30:00Z")
        );
    }

    @Test
    @DisplayName("등록자가 복구 기한 안에 자신의 프로젝트를 복구하면 200과 승인 상태, 복구 시각을 반환한다.")
    void restoresProject() throws Exception {
        given(projectService.restore(100L, 7L)).willReturn(
                new RestoredProject(100L, ApprovalStatus.APPROVED, Instant.parse("2026-09-10T05:20:00Z"))
        );

        mockMvc.perform(post("/api/v1/projects/{projectId}/restore", 100L)
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE, new AuthenticatedSession(7L, UserRole.USER))
                        .header("X-CSRF-Token", "csrf-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(100))
                .andExpect(jsonPath("$.data.approvalStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.restoredAt").value("2026-09-10T05:20:00Z"))
                .andDo(document(
                        "project-restore",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Project")
                                .summary(RESTORE_SUMMARY)
                                .description(RESTORE_DESCRIPTION)
                                .pathParameters(
                                        parameterWithName("projectId").description("복구할 프로젝트 ID")
                                )
                                .requestHeaders(
                                        headerWithName("X-CSRF-Token").description("세션 조회로 발급받은 CSRF 토큰")
                                )
                                .responseSchema(Schema.schema("ProjectRestoreSuccessResponse"))
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data").type(OBJECT).description("복구 결과"),
                                        fieldWithPath("data.id").type(NUMBER).description("복구한 프로젝트 ID"),
                                        new EnumFields(ApprovalStatus.class).withPath("data.approvalStatus")
                                                .description("승인 상태. 삭제 이전 값을 그대로 유지한다."),
                                        fieldWithPath("data.restoredAt").type(STRING).description("복구 시각 (UTC)"),
                                        fieldWithPath("meta").type(OBJECT).description("메타 정보").optional()
                                )
                                .build())
                ));
    }

    @Test
    @DisplayName("없는 프로젝트이거나 등록자가 아니거나 삭제되지 않은 프로젝트이면, 404를 반환한다.")
    void rejectsRestoringNotDeletedOrNotOwnedProject() throws Exception {
        given(projectService.restore(100L, 7L))
                .willThrow(new EntityNotFoundException(ProjectErrorCode.PROJECT_NOT_FOUND));

        mockMvc.perform(post("/api/v1/projects/{projectId}/restore", 100L)
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE, new AuthenticatedSession(7L, UserRole.USER))
                        .header("X-CSRF-Token", "csrf-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"))
                .andDo(document("project-restore-not-found", resource(restoreErrorResource())));
    }

    @Test
    @DisplayName("복구 기한이 지난 프로젝트를 복구하려 하면, 409를 반환한다.")
    void rejectsRestoringAfterDeadline() throws Exception {
        given(projectService.restore(100L, 7L))
                .willThrow(new ConflictException(ProjectErrorCode.PROJECT_RESTORE_DEADLINE_EXPIRED));

        mockMvc.perform(post("/api/v1/projects/{projectId}/restore", 100L)
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE, new AuthenticatedSession(7L, UserRole.USER))
                        .header("X-CSRF-Token", "csrf-token"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROJECT_RESTORE_DEADLINE_EXPIRED"))
                .andDo(document("project-restore-deadline-expired", resource(restoreErrorResource())));
    }

    @Test
    @DisplayName("로그인하지 않고 프로젝트 복구를 요청하는 경우, 401을 반환하고 서비스를 호출하지 않는다.")
    void rejectsUnauthenticatedRestore() throws Exception {
        mockMvc.perform(post("/api/v1/projects/{projectId}/restore", 100L)
                        .header("X-CSRF-Token", "csrf-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andDo(document("project-restore-unauthorized", resource(restoreErrorResource())));

        verifyNoInteractions(projectService);
    }

    private static ResourceSnippetParameters restoreErrorResource() {
        return ResourceSnippetParameters.builder()
                .tag("Project")
                .summary(RESTORE_SUMMARY)
                .description(RESTORE_DESCRIPTION)
                .pathParameters(
                        parameterWithName("projectId").description("복구할 프로젝트 ID")
                )
                .responseSchema(Schema.schema("ErrorResponse"))
                .responseFields(RestDocsFields.errorResponse())
                .build();
    }

    private static ResourceSnippetParameters filterOptionsErrorResource() {
        return ResourceSnippetParameters.builder()
                .tag("Project")
                .summary(FILTER_OPTIONS_SUMMARY)
                .description(FILTER_OPTIONS_DESCRIPTION)
                .responseSchema(Schema.schema("ErrorResponse"))
                .responseFields(RestDocsFields.errorResponse())
                .build();
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
                "https://cdn.example.com/thumbnail",
                "## 문제\n![회고 화면](https://cdn.example.com/description-21)",
                List.of(new ProjectDetailResponse.DescriptionMedia(
                        21L,
                        "https://cdn.example.com/description-21"
                )),
                "https://github.com/woowacourse-teams/2026-loop",
                "https://loop.team",
                ServiceStatus.OPERATING,
                ApprovalStatus.APPROVED,
                null,
                831,
                128,
                84,
                28,
                false,
                false,
                false,
                18,
                List.of(
                        new ProjectTechTagResponse(1L, "React"),
                        new ProjectTechTagResponse(2L, "TypeScript")
                ),
                List.of(
                        new ProjectMemberProfileResponse("dhyepark", "박다혜", 6, "BACKEND", 101L, "https://cdn.example.com/avatar-101", null, null),
                        new ProjectMemberProfileResponse("zzaekkii", "김도현", 6, "FRONTEND", null, null, null, null)
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

    /**
     * 같은 경로와 메서드의 스니펫은 하나의 문서로 합쳐지므로, 수정 오류도 수정 API 의 설명을 달아야 한다.
     * 등록용 errorResource 를 쓰면 문서의 PUT 설명이 등록 API 설명으로 덮인다.
     */
    private static ResourceSnippetParameters updateErrorResource() {
        return ResourceSnippetParameters.builder()
                .tag("Project")
                .summary(UPDATE_SUMMARY)
                .description(UPDATE_DESCRIPTION)
                .pathParameters(parameterWithName("projectId").description("수정할 프로젝트 ID"))
                .requestSchema(Schema.schema("ProjectUpdateRequest"))
                .responseSchema(Schema.schema("ErrorResponse"))
                .responseFields(RestDocsFields.errorResponse())
                .build();
    }


    @Test
    @DisplayName("작성자가 프로젝트를 수정하면 바뀐 승인 상태와 함께 200을 반환한다.")
    void updatesProject() throws Exception {
        given(projectService.update(anyLong(), anyLong(), any(ProjectUpdateRequest.class)))
                .willReturn(new ProjectUpdateResponse(100L, ApprovalStatus.PENDING));

        mockMvc.perform(put("/api/v1/projects/{projectId}", 100L)
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE, new AuthenticatedSession(7L, UserRole.USER))
                        .header("X-CSRF-Token", "csrf-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateRequestJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.projectId").value(100))
                .andExpect(jsonPath("$.data.approvalStatus").value("PENDING"))
                .andDo(document(
                        "project-update",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Project")
                                .summary(UPDATE_SUMMARY)
                                .description(UPDATE_DESCRIPTION)
                                .pathParameters(parameterWithName("projectId").description("수정할 프로젝트 ID"))
                                .requestHeaders(
                                        headerWithName("X-CSRF-Token").description("세션 조회로 발급받은 CSRF 토큰")
                                )
                                .requestSchema(Schema.schema("ProjectUpdateRequest"))
                                .responseSchema(Schema.schema("ProjectUpdateSuccessResponse"))
                                .requestFields(
                                        fieldWithPath("title").type(STRING).description("프로젝트 이름 (100자 이하)"),
                                        fieldWithPath("teamName").type(STRING).description("팀 이름 (50자 이하)"),
                                        fieldWithPath("tagline").type(STRING).description("한 줄 소개 (200자 이하)"),
                                        fieldWithPath("cohort").type(NUMBER).description("우아한테크코스 기수 (1~8)"),
                                        fieldWithPath("thumbnailImageId").type(NUMBER)
                                                .description("본인이 업로드한 PROJECT_THUMBNAIL 용도의 처리 완료 이미지 ID. "
                                                        + "필드를 생략하면 기존 썸네일을 유지하고, null을 보내면 제거한다.")
                                                .optional(),
                                        fieldWithPath("githubRepositoryUrl").type(STRING)
                                                .description("https://github.com/{owner}/{repo} 형식. 바꿀 수 있지만 "
                                                        + "다른 프로젝트가 등록한 리포지토리로는 바꿀 수 없다. "
                                                        + "slug는 등록 시점 값으로 고정이라 따라 바뀌지 않는다."),
                                        fieldWithPath("deploymentUrl").type(STRING)
                                                .description("서비스 배포 URL (http/https). 비우려면 null로 보낸다.")
                                                .optional(),
                                        fieldWithPath("descriptionMd").type(STRING)
                                                .description("프로젝트 설명 마크다운 (100,000자 이하). "
                                                        + "이미지는 ![설명](media://{mediaId}) 형식으로 넣으며, "
                                                        + "상세 조회 응답의 CDN URL을 그대로 보내도 기존 본문 이미지 참조를 유지한다.")
                                                .optional(),
                                        new EnumFields(ServiceStatus.class).withPath("serviceStatus")
                                                .description("서비스 운영 상태. deploymentUrl이 없으면 CLOSED만 보낼 수 있다."),
                                        fieldWithPath("techTagIds").type(ARRAY)
                                                .description("기술 스택 ID 전체 목록. 통째로 교체하며 배열 순서가 표시 순서가 된다. "
                                                        + "이미 달려 있던 태그는 비활성화됐어도 그대로 둘 수 있다.")
                                                .attributes(key("itemsType").value("number")),
                                        fieldWithPath("memberHandles").type(ARRAY)
                                                .description("작성자를 제외한 팀원 handle 전체 목록 (1명 이상). "
                                                        + "통째로 교체하며 배열 순서가 표시 순서가 된다. "
                                                        + "이미 팀원인 사용자는 탈퇴했어도 그대로 둘 수 있다.")
                                                .attributes(key("itemsType").value("string"))
                                )
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data").type(OBJECT).description("수정 결과"),
                                        fieldWithPath("data.projectId").type(NUMBER).description("수정한 프로젝트 ID"),
                                        new EnumFields(ApprovalStatus.class).withPath("data.approvalStatus")
                                                .description("수정 후 승인 상태. PENDING 또는 APPROVED이며 REJECTED는 오지 않는다.")
                                )
                                .build())
                ));

        verify(projectService).update(eq(100L), eq(7L), any(ProjectUpdateRequest.class));
    }

    @Test
    @DisplayName("프로젝트 수정 요청에 필수값이 없는 경우, 400과 필드 오류를 반환하고, 서비스를 호출하지 않는다.")
    void rejectsInvalidUpdateRequest() throws Exception {
        mockMvc.perform(put("/api/v1/projects/{projectId}", 100L)
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE, new AuthenticatedSession(7L, UserRole.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateRequestJson().replace("\"serviceStatus\": \"OPERATING\",", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details[0].field").value("serviceStatus"))
                .andDo(document("project-update-invalid", resource(updateErrorResource())));

        verifyNoInteractions(projectService);
    }

    @Test
    @DisplayName("로그인하지 않고 프로젝트 수정을 요청하는 경우, 요청값 검증보다 먼저 401을 반환한다.")
    void rejectsUnauthenticatedUpdate() throws Exception {
        mockMvc.perform(put("/api/v1/projects/{projectId}", 100L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateRequestJson()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andDo(document("project-update-unauthorized", resource(updateErrorResource())));

        verifyNoInteractions(projectService);
    }

    @Test
    @DisplayName("없거나 삭제됐거나 다른 사람의 프로젝트를 수정하면, 존재 여부를 숨기고 404를 반환한다.")
    void rejectsUpdateOfInaccessibleProject() throws Exception {
        given(projectService.update(anyLong(), anyLong(), any(ProjectUpdateRequest.class)))
                .willThrow(new EntityNotFoundException(ProjectErrorCode.PROJECT_NOT_FOUND));

        mockMvc.perform(put("/api/v1/projects/{projectId}", 100L)
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE, new AuthenticatedSession(7L, UserRole.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateRequestJson()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"))
                .andDo(document("project-update-not-found", resource(updateErrorResource())));
    }

    @Test
    @DisplayName("다른 프로젝트가 등록한 리포지토리로 바꾸면, 409를 반환한다.")
    void rejectsUpdateToDuplicateRepository() throws Exception {
        given(projectService.update(anyLong(), anyLong(), any(ProjectUpdateRequest.class)))
                .willThrow(new DuplicateEntityException(ProjectErrorCode.PROJECT_DUPLICATE_GITHUB_REPOSITORY));

        mockMvc.perform(put("/api/v1/projects/{projectId}", 100L)
                        .requestAttr(AUTHENTICATED_SESSION_ATTRIBUTE, new AuthenticatedSession(7L, UserRole.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateRequestJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROJECT_DUPLICATE_GITHUB_REPOSITORY"))
                .andDo(document("project-update-duplicate", resource(updateErrorResource())));
    }

    private static String validUpdateRequestJson() {
        return """
                {
                  "title": "루프 (Loop)",
                  "teamName": "루프팀",
                  "tagline": "스프린트 회고와 액션 아이템을 하나로 엮은 실시간 협업 도구",
                  "cohort": 6,
                  "thumbnailImageId": 12,
                  "githubRepositoryUrl": "https://github.com/woowacourse-teams/2026-loop",
                  "deploymentUrl": "https://loop.team",
                  "descriptionMd": "## 문제\\n회고 도구와 액션 아이템 관리가 흩어져 있습니다.",
                  "serviceStatus": "OPERATING",
                  "techTagIds": [1, 2, 3],
                  "memberHandles": ["zzaekkii", "sangjun121"]
                }
                """;
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
