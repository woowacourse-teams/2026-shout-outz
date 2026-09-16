package com.shoutoutz.api.project.presentation;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.SimpleType.INTEGER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.BOOLEAN;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.OBJECT;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.response.SliceMetaResponse;
import com.shoutoutz.api.common.restdocs.RestDocsFields;
import com.shoutoutz.api.project.application.ProjectService;
import com.shoutoutz.api.project.domain.exception.InvalidProjectCursorException;
import com.shoutoutz.api.project.presentation.dto.request.UserProjectFindRequest;
import com.shoutoutz.api.project.presentation.dto.response.ProjectFindAllResponse;
import com.shoutoutz.api.project.presentation.dto.response.ProjectMemberProfileResponse;
import com.shoutoutz.api.project.presentation.dto.response.ProjectTechTagResponse;
import com.shoutoutz.api.project.presentation.dto.response.UserProjectFindResponse;
import com.shoutoutz.api.user.domain.account.UserErrorCode;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@DisplayName("사용자 프로젝트 목록 API")
@WebMvcTest(controllers = UserProjectHttpApi.class)
@AutoConfigureRestDocs
class UserProjectHttpApiTest {

    private static final String SUMMARY = "사용자 프로젝트 목록 조회";
    private static final String DESCRIPTION = "handle로 사용자가 참여한 승인 프로젝트를 최신순으로 조회한다. "
            + "현재 프로젝트 팀원과 가입 계정에 매칭된 이관 프로젝트 팀원을 모두 포함한다. "
            + "탈퇴한 사용자는 빈 목록을 반환하고, 정지된 사용자는 기존 프로젝트를 공개한다. "
            + "로그인하지 않아도 조회할 수 있으며, 응답의 meta.nextCursor를 다음 요청에 그대로 전달한다.";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectService projectService;

    @Test
    @DisplayName("로그인하지 않아도 사용자가 참여한 프로젝트를 조회한다.")
    void findsUserProjects() throws Exception {
        given(projectService.findAllByUser("zzaekkii", new UserProjectFindRequest(20, null)))
                .willReturn(new UserProjectFindResponse(
                        List.of(project()),
                        new SliceMetaResponse("next-cursor", true)
                ));

        mockMvc.perform(get("/api/v1/users/{handle}/projects", "zzaekkii")
                        .queryParam("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data[0].id").value(100))
                .andExpect(jsonPath("$.data[0].title").value("루프"))
                .andExpect(jsonPath("$.data[0].techTags[0].displayName").value("Spring"))
                .andExpect(jsonPath("$.data[0].members[0].handle").value("zzaekkii"))
                .andExpect(jsonPath("$.meta.nextCursor").value("next-cursor"))
                .andExpect(jsonPath("$.meta.hasNext").value(true))
                .andExpect(jsonPath("$.meta.totalCount").doesNotExist())
                .andDo(document(
                        "user-project-find-all",
                        resource(ResourceSnippetParameters.builder()
                                .tag("User")
                                .summary(SUMMARY)
                                .description(DESCRIPTION)
                                .pathParameters(
                                        parameterWithName("handle").description("조회할 사용자의 handle")
                                )
                                .queryParameters(
                                        parameterWithName("size")
                                                .type(INTEGER)
                                                .description("한 번에 가져올 프로젝트 수. 기본값 20, 1~50")
                                                .optional(),
                                        parameterWithName("cursor")
                                                .description("다음 페이지 조회용 커서. 첫 요청은 생략")
                                                .optional()
                                )
                                .responseSchema(Schema.schema("UserProjectFindAllSuccessResponse"))
                                .responseFields(responseFields())
                                .build())
                ));

        verify(projectService).findAllByUser("zzaekkii", new UserProjectFindRequest(20, null));
    }

    @Test
    @DisplayName("파라미터를 생략하면 기본 조회 조건을 사용한다.")
    void usesDefaultParameters() throws Exception {
        given(projectService.findAllByUser("zzaekkii", new UserProjectFindRequest(null, null)))
                .willReturn(new UserProjectFindResponse(List.of(), new SliceMetaResponse(null, false)));

        mockMvc.perform(get("/api/v1/users/{handle}/projects", "zzaekkii"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.meta.hasNext").value(false));

        verify(projectService).findAllByUser("zzaekkii", new UserProjectFindRequest(null, null));
    }

    @Test
    @DisplayName("handle 형식이 올바르지 않으면 조회할 수 없다.")
    void rejectsInvalidHandle() throws Exception {
        mockMvc.perform(get("/api/v1/users/{handle}/projects", "잘못된-핸들"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(projectService);
    }

    @Test
    @DisplayName("조회 개수가 범위를 벗어나면 조회할 수 없다.")
    void rejectsInvalidSize() throws Exception {
        mockMvc.perform(get("/api/v1/users/{handle}/projects", "zzaekkii")
                        .queryParam("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(projectService);
    }

    @Test
    @DisplayName("커서가 올바르지 않으면 조회할 수 없다.")
    void rejectsInvalidCursor() throws Exception {
        given(projectService.findAllByUser("zzaekkii", new UserProjectFindRequest(null, "broken")))
                .willThrow(new InvalidProjectCursorException());

        mockMvc.perform(get("/api/v1/users/{handle}/projects", "zzaekkii")
                        .queryParam("cursor", "broken"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PROJECT_INVALID_CURSOR"))
                .andDo(document("user-project-find-all-invalid-cursor", resource(errorResource())));
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 프로젝트는 조회할 수 없다.")
    void rejectsUnknownUser() throws Exception {
        given(projectService.findAllByUser("missing-user", new UserProjectFindRequest(null, null)))
                .willThrow(new EntityNotFoundException(UserErrorCode.USER_NOT_FOUND));

        mockMvc.perform(get("/api/v1/users/{handle}/projects", "missing-user"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
                .andDo(document("user-project-find-all-not-found", resource(errorResource())));
    }

    private static ProjectFindAllResponse.Item project() {
        return new ProjectFindAllResponse.Item(
                100L,
                "loop",
                "루프",
                "스프린트 회고와 액션 아이템을 관리하는 협업 도구",
                6,
                12L,
                128,
                184L,
                14L,
                List.of(new ProjectTechTagResponse(1L, "Spring")),
                List.of(new ProjectMemberProfileResponse(
                        7L, "zzaekkii", "재키", 6, "BACKEND", 21L, null, null
                ))
        );
    }

    private static List<org.springframework.restdocs.payload.FieldDescriptor> responseFields() {
        return List.of(
                fieldWithPath("status").type(STRING).description("응답 상태"),
                fieldWithPath("data").type(ARRAY).description("사용자가 참여한 프로젝트 목록"),
                fieldWithPath("data[].id").type(NUMBER).description("프로젝트 ID"),
                fieldWithPath("data[].slug").type(STRING).description("프로젝트 slug"),
                fieldWithPath("data[].title").type(STRING).description("프로젝트 이름"),
                fieldWithPath("data[].tagline").type(STRING).description("한 줄 소개"),
                fieldWithPath("data[].cohort").type(NUMBER).description("우아한테크코스 기수"),
                fieldWithPath("data[].thumbnailMediaId").type(NUMBER).description("썸네일 미디어 ID").optional(),
                fieldWithPath("data[].starCount").type(NUMBER)
                        .description("GitHub star 수. 동기화 전이면 null이다.").optional(),
                fieldWithPath("data[].likeCount").type(NUMBER).description("좋아요 수"),
                fieldWithPath("data[].commentCount").type(NUMBER).description("삭제되지 않은 댓글 수"),
                fieldWithPath("data[].techTags").type(ARRAY).description("기술 스택"),
                fieldWithPath("data[].techTags[].id").type(NUMBER).description("기술 스택 ID"),
                fieldWithPath("data[].techTags[].displayName").type(STRING).description("기술 스택 이름"),
                fieldWithPath("data[].members").type(ARRAY).description("프로젝트 팀원"),
                fieldWithPath("data[].members[].userId").type(NUMBER).description("사용자 ID").optional(),
                fieldWithPath("data[].members[].handle").type(STRING).description("사용자 handle").optional(),
                fieldWithPath("data[].members[].displayName").type(STRING).description("표시 이름"),
                fieldWithPath("data[].members[].cohort").type(NUMBER).description("기수").optional(),
                fieldWithPath("data[].members[].track").type(STRING).description("트랙").optional(),
                fieldWithPath("data[].members[].avatarImageId").type(NUMBER).description("프로필 이미지 ID").optional(),
                fieldWithPath("data[].members[].githubAvatarUrl").type(STRING)
                        .description("이관 팀원의 GitHub 프로필 이미지 URL").optional(),
                fieldWithPath("data[].members[].githubProfileUrl").type(STRING)
                        .description("이관 팀원의 GitHub 프로필 URL").optional(),
                fieldWithPath("meta").type(OBJECT).description("페이지네이션 정보"),
                fieldWithPath("meta.nextCursor").type(STRING).description("다음 페이지 커서").optional(),
                fieldWithPath("meta.hasNext").type(BOOLEAN).description("다음 페이지 존재 여부")
        );
    }

    private static ResourceSnippetParameters errorResource() {
        return ResourceSnippetParameters.builder()
                .tag("User")
                .summary(SUMMARY)
                .description(DESCRIPTION)
                .pathParameters(
                        parameterWithName("handle").description("조회할 사용자의 handle")
                )
                .responseSchema(Schema.schema("ErrorResponse"))
                .responseFields(RestDocsFields.errorResponse())
                .build();
    }
}
