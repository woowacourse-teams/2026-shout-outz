package com.shoutoutz.api.user.presentation;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.SimpleType.INTEGER;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.BOOLEAN;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.OBJECT;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.shoutoutz.api.auth.presentation.session.AuthenticatedSession;
import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.ConflictException;
import com.shoutoutz.api.common.exception.custom.CustomException;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.common.restdocs.RestDocsFields;
import com.shoutoutz.api.user.application.UserService;
import com.shoutoutz.api.user.application.dto.UserSearchItem;
import com.shoutoutz.api.user.application.dto.UserSearchResult;
import com.shoutoutz.api.user.domain.account.UserErrorCode;
import com.shoutoutz.api.user.domain.account.UserRole;
import com.shoutoutz.api.user.domain.profile.UserProfileErrorCode;
import com.shoutoutz.api.user.domain.profile.UserType;
import com.shoutoutz.api.user.presentation.dto.request.UserProfileUpdateRequest;
import com.shoutoutz.api.user.presentation.dto.response.UserProfileResponse;
import com.shoutoutz.api.user.presentation.dto.response.UserProfileSummaryResponse;
import com.shoutoutz.api.user.presentation.dto.response.UserProfileUpdateResponse;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@DisplayName("사용자 API")
@WebMvcTest(controllers = UserHttpApi.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs
class UserHttpApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("내 프로필 요약을 조회한다")
    void getMyProfileSummary() throws Exception {
        given(userService.getMyProfileSummary(1L))
                .willReturn(new UserProfileSummaryResponse(
                        "zzaekkii",
                        "재키",
                        21L
                ));

        mockMvc.perform(get("/api/v1/users/me/summary")
                        .header(HttpHeaders.COOKIE, "JSESSIONID=session-id")
                        .requestAttr(
                                AuthenticatedSession.class.getName(),
                                new AuthenticatedSession(1L, UserRole.USER)
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.userId").doesNotExist())
                .andExpect(jsonPath("$.data.handle").value("zzaekkii"))
                .andExpect(jsonPath("$.data.displayName").value("재키"))
                .andExpect(jsonPath("$.data.avatarImageId").value(21))
                .andDo(document(
                        "user-profile-summary-get",
                        resource(ResourceSnippetParameters.builder()
                                .tag("User")
                                .summary("내 프로필 요약 조회")
                                .description("로그인 후 공통 헤더에 표시할 최소 사용자 정보를 조회한다.")
                                .requestHeaders(
                                        headerWithName(HttpHeaders.COOKIE)
                                                .description("인증된 사용자의 JSESSIONID")
                                )
                                .responseSchema(Schema.schema("UserProfileSummarySuccessResponse"))
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data").type(OBJECT).description("프로필 요약 정보"),
                                        fieldWithPath("data.handle").type(STRING).description("사용자 handle"),
                                        fieldWithPath("data.displayName").type(STRING).description("표시 이름"),
                                        fieldWithPath("data.avatarImageId").type(NUMBER)
                                                .description("프로필 이미지 미디어 ID").optional()
                                )
                                .build())
                ));
    }

    @Test
    @DisplayName("인증 정보가 없으면 내 프로필 요약 조회에 실패한다")
    void rejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/summary"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andDo(document(
                        "user-profile-summary-get-unauthorized",
                        resource(ResourceSnippetParameters.builder()
                                .tag("User")
                                .summary("내 프로필 요약 조회")
                                .description("로그인 후 공통 헤더에 표시할 최소 사용자 정보를 조회한다.")
                                .responseSchema(Schema.schema("ErrorResponse"))
                                .responseFields(RestDocsFields.errorResponse())
                                .build())
                ));
    }

    @Test
    @DisplayName("마이페이지 프로필을 조회한다")
    void getMyProfile() throws Exception {
        given(userService.getMyProfile(1L))
                .willReturn(new UserProfileResponse(
                        "zzaekkii",
                        "재키",
                        UserType.WOOWACOURSE_CREW,
                        "BACKEND",
                        (short) 8,
                        "백엔드 개발자입니다.",
                        21L,
                        "https://github.com/zzaekkii",
                        "https://zzaekkii.dev",
                        new UserProfileResponse.Counts(2L, 18L)
                ));

        mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.COOKIE, "JSESSIONID=session-id")
                        .requestAttr(
                                AuthenticatedSession.class.getName(),
                                new AuthenticatedSession(1L, UserRole.USER)
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.userId").doesNotExist())
                .andExpect(jsonPath("$.data.handle").value("zzaekkii"))
                .andExpect(jsonPath("$.data.displayName").value("재키"))
                .andExpect(jsonPath("$.data.userType").value("WOOWACOURSE_CREW"))
                .andExpect(jsonPath("$.data.track").value("BACKEND"))
                .andExpect(jsonPath("$.data.cohort").value(8))
                .andExpect(jsonPath("$.data.bio").value("백엔드 개발자입니다."))
                .andExpect(jsonPath("$.data.avatarImageId").value(21))
                .andExpect(jsonPath("$.data.githubProfileUrl")
                        .value("https://github.com/zzaekkii"))
                .andExpect(jsonPath("$.data.blogUrl").value("https://zzaekkii.dev"))
                .andExpect(jsonPath("$.data.counts.projects").value(2))
                .andExpect(jsonPath("$.data.counts.posts").value(18))
                .andDo(document(
                        "user-profile-get",
                        resource(ResourceSnippetParameters.builder()
                                .tag("User")
                                .summary("마이페이지 조회")
                                .description("로그인한 사용자의 프로필과 프로젝트 및 피드 개수를 조회한다.")
                                .requestHeaders(
                                        headerWithName(HttpHeaders.COOKIE)
                                                .description("인증된 사용자의 JSESSIONID")
                                )
                                .responseSchema(Schema.schema("UserProfileSuccessResponse"))
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data").type(OBJECT).description("사용자 프로필"),
                                        fieldWithPath("data.handle").type(STRING).description("사용자 handle"),
                                        fieldWithPath("data.displayName").type(STRING).description("표시 이름"),
                                        fieldWithPath("data.userType").type(STRING).description("사용자 유형"),
                                        fieldWithPath("data.track").type(STRING).description("우테코 트랙").optional(),
                                        fieldWithPath("data.cohort").type(NUMBER).description("우테코 기수").optional(),
                                        fieldWithPath("data.bio").type(STRING).description("한 줄 소개").optional(),
                                        fieldWithPath("data.avatarImageId").type(NUMBER)
                                                .description("프로필 이미지 미디어 ID").optional(),
                                        fieldWithPath("data.githubProfileUrl").type(STRING)
                                                .description("GitHub 프로필 URL").optional(),
                                        fieldWithPath("data.blogUrl").type(STRING)
                                                .description("블로그 URL").optional(),
                                        fieldWithPath("data.counts").type(OBJECT).description("프로필 항목 개수"),
                                        fieldWithPath("data.counts.projects").type(NUMBER)
                                                .description("삭제되지 않은 참여 프로젝트 개수"),
                                        fieldWithPath("data.counts.posts").type(NUMBER)
                                                .description("삭제되지 않은 작성 피드 개수")
                                )
                                .build())
                ));
    }

    @Test
    @DisplayName("인증 정보가 없으면 마이페이지 조회에 실패한다")
    void rejectUnauthenticatedMyProfileRequest() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andDo(document(
                        "user-profile-get-unauthorized",
                        resource(ResourceSnippetParameters.builder()
                                .tag("User")
                                .summary("마이페이지 조회")
                                .description("로그인한 사용자의 프로필과 프로젝트 및 피드 개수를 조회한다.")
                                .responseSchema(Schema.schema("ErrorResponse"))
                                .responseFields(RestDocsFields.errorResponse())
                                .build())
                ));

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("내 프로필을 수정한다")
    void updateMyProfile() throws Exception {
        given(userService.updateMyProfile(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.any(UserProfileUpdateRequest.class)
        )).willReturn(new UserProfileUpdateResponse(
                        "zzaekkii",
                        "재키",
                        UserType.WOOWACOURSE_CREW,
                        "BACKEND",
                        (short) 8,
                        "백엔드 개발자입니다.",
                        21L,
                        "https://github.com/zzaekkii",
                        "https://zzaekkii.dev"
                ));

        mockMvc.perform(put("/api/v1/users/me")
                        .header(HttpHeaders.COOKIE, "JSESSIONID=session-id")
                        .header("X-CSRF-Token", "csrf-token")
                        .requestAttr(
                                AuthenticatedSession.class.getName(),
                                new AuthenticatedSession(1L, UserRole.USER)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "재키",
                                  "bio": "백엔드 개발자입니다.",
                                  "avatarImageId": 21,
                                  "githubProfileUrl": "https://github.com/zzaekkii",
                                  "blogUrl": "https://zzaekkii.dev"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.userId").doesNotExist())
                .andExpect(jsonPath("$.data.handle").value("zzaekkii"))
                .andExpect(jsonPath("$.data.displayName").value("재키"))
                .andExpect(jsonPath("$.data.userType").value("WOOWACOURSE_CREW"))
                .andExpect(jsonPath("$.data.track").value("BACKEND"))
                .andExpect(jsonPath("$.data.cohort").value(8))
                .andExpect(jsonPath("$.data.bio").value("백엔드 개발자입니다."))
                .andExpect(jsonPath("$.data.avatarImageId").value(21))
                .andExpect(jsonPath("$.data.githubProfileUrl").value("https://github.com/zzaekkii"))
                .andExpect(jsonPath("$.data.blogUrl").value("https://zzaekkii.dev"))
                .andDo(document(
                        "user-profile-update",
                        resource(ResourceSnippetParameters.builder()
                                .tag("User")
                                .summary("내 프로필 수정")
                                .description("로그인한 사용자의 수정 가능한 프로필 정보를 저장한다.")
                                .requestHeaders(
                                        headerWithName(HttpHeaders.COOKIE)
                                                .description("인증된 사용자의 JSESSIONID"),
                                        headerWithName("X-CSRF-Token")
                                                .description("CSRF 토큰")
                                )
                                .requestSchema(Schema.schema("UserProfileUpdateRequest"))
                                .requestFields(
                                        fieldWithPath("displayName").type(STRING)
                                                .description("표시 이름. 인증된 우테코 사용자는 변경 불가"),
                                        fieldWithPath("bio").type(STRING).description("한 줄 소개").optional(),
                                        fieldWithPath("avatarImageId").type(NUMBER)
                                                .description("READY 상태의 USER_AVATAR 미디어 ID").optional(),
                                        fieldWithPath("githubProfileUrl").type(STRING)
                                                .description("GitHub 프로필 URL").optional(),
                                        fieldWithPath("blogUrl").type(STRING).description("블로그 URL").optional()
                                )
                                .responseSchema(Schema.schema("UserProfileUpdateSuccessResponse"))
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data").type(OBJECT).description("수정된 사용자 프로필"),
                                        fieldWithPath("data.handle").type(STRING).description("사용자 handle"),
                                        fieldWithPath("data.displayName").type(STRING).description("표시 이름"),
                                        fieldWithPath("data.userType").type(STRING).description("사용자 유형"),
                                        fieldWithPath("data.track").type(STRING).description("우테코 트랙").optional(),
                                        fieldWithPath("data.cohort").type(NUMBER).description("우테코 기수").optional(),
                                        fieldWithPath("data.bio").type(STRING).description("한 줄 소개").optional(),
                                        fieldWithPath("data.avatarImageId").type(NUMBER)
                                                .description("프로필 이미지 미디어 ID").optional(),
                                        fieldWithPath("data.githubProfileUrl").type(STRING)
                                                .description("GitHub 프로필 URL").optional(),
                                        fieldWithPath("data.blogUrl").type(STRING)
                                                .description("블로그 URL").optional()
                                )
                                .build())
                ));
    }

    @Test
    @DisplayName("프로필 URL 형식이 잘못되면 수정 요청을 거절한다")
    void rejectInvalidProfileUrl() throws Exception {
        mockMvc.perform(put("/api/v1/users/me")
                        .requestAttr(
                                AuthenticatedSession.class.getName(),
                                new AuthenticatedSession(1L, UserRole.USER)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "재키",
                                  "githubProfileUrl": "http://github.com/zzaekkii",
                                  "blogUrl": "not-a-url"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.length()").value(2))
                .andDo(document(
                        "user-profile-update-invalid",
                        resource(ResourceSnippetParameters.builder()
                                .tag("User")
                                .summary("내 프로필 수정")
                                .description("로그인한 사용자의 수정 가능한 프로필 정보를 저장한다.")
                                .requestSchema(Schema.schema("UserProfileUpdateRequest"))
                                .responseSchema(Schema.schema("ErrorResponse"))
                                .responseFields(RestDocsFields.errorResponse())
                                .build())
                ));
    }

    @Test
    @DisplayName("인증 정보가 없으면 내 프로필 수정에 실패한다")
    void rejectUnauthenticatedProfileUpdateRequest() throws Exception {
        mockMvc.perform(put("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andDo(document(
                        "user-profile-update-unauthorized",
                        resource(ResourceSnippetParameters.builder()
                                .tag("User")
                                .summary("내 프로필 수정")
                                .description("로그인한 사용자의 수정 가능한 프로필 정보를 저장한다.")
                                .responseSchema(Schema.schema("ErrorResponse"))
                                .responseFields(RestDocsFields.errorResponse())
                                .build())
                ));

        verifyNoInteractions(userService);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("profileUpdateFailureCases")
    @DisplayName("프로필 수정 정책을 위반하면 해당 오류로 응답한다")
    void rejectInvalidProfileUpdate(
            String documentIdentifier,
            CustomException exception
    ) throws Exception {
        given(userService.updateMyProfile(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.any(UserProfileUpdateRequest.class)
        )).willThrow(exception);

        mockMvc.perform(put("/api/v1/users/me")
                        .header(HttpHeaders.COOKIE, "JSESSIONID=session-id")
                        .header("X-CSRF-Token", "csrf-token")
                        .requestAttr(
                                AuthenticatedSession.class.getName(),
                                new AuthenticatedSession(1L, UserRole.USER)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "재키",
                                  "avatarImageId": 21
                                }
                                """))
                .andExpect(status().is(exception.getHttpStatus().value()))
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value(exception.getErrorCode().name()))
                .andDo(document(
                        documentIdentifier,
                        resource(ResourceSnippetParameters.builder()
                                .tag("User")
                                .summary("내 프로필 수정")
                                .description("로그인한 사용자의 수정 가능한 프로필 정보를 저장한다.")
                                .requestSchema(Schema.schema("UserProfileUpdateRequest"))
                                .responseSchema(Schema.schema("ErrorResponse"))
                                .responseFields(RestDocsFields.errorResponse())
                                .build())
                ));
    }

    static Stream<Arguments> profileUpdateFailureCases() {
        return Stream.of(
                Arguments.of(
                        "user-profile-update-display-name-immutable",
                        new BadRequestException(UserProfileErrorCode.PROFILE_DISPLAY_NAME_IMMUTABLE)
                ),
                Arguments.of(
                        "user-profile-update-avatar-not-found",
                        new EntityNotFoundException(UserProfileErrorCode.AVATAR_IMAGE_NOT_FOUND)
                ),
                Arguments.of(
                        "user-profile-update-avatar-forbidden",
                        new ForbiddenException(UserProfileErrorCode.AVATAR_IMAGE_FORBIDDEN)
                ),
                Arguments.of(
                        "user-profile-update-avatar-invalid-purpose",
                        new BadRequestException(UserProfileErrorCode.AVATAR_IMAGE_INVALID_PURPOSE)
                ),
                Arguments.of(
                        "user-profile-update-avatar-not-ready",
                        new ConflictException(UserProfileErrorCode.AVATAR_IMAGE_NOT_READY)
                )
        );
    }

    @Test
    @DisplayName("handle로 사용자 공개 프로필을 조회한다")
    void getPublicProfile() throws Exception {
        given(userService.getPublicProfile("zzaekkii"))
                .willReturn(new UserProfileResponse(
                        "zzaekkii",
                        "재키",
                        UserType.WOOWACOURSE_CREW,
                        "BACKEND",
                        (short) 8,
                        "백엔드 개발자입니다.",
                        21L,
                        "https://github.com/zzaekkii",
                        "https://zzaekkii.dev",
                        new UserProfileResponse.Counts(2L, 18L)
                ));

        mockMvc.perform(get("/api/v1/users/{handle}", "zzaekkii"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.userId").doesNotExist())
                .andExpect(jsonPath("$.data.handle").value("zzaekkii"))
                .andExpect(jsonPath("$.data.displayName").value("재키"))
                .andExpect(jsonPath("$.data.userType").value("WOOWACOURSE_CREW"))
                .andExpect(jsonPath("$.data.track").value("BACKEND"))
                .andExpect(jsonPath("$.data.cohort").value(8))
                .andExpect(jsonPath("$.data.bio").value("백엔드 개발자입니다."))
                .andExpect(jsonPath("$.data.avatarImageId").value(21))
                .andExpect(jsonPath("$.data.githubProfileUrl")
                        .value("https://github.com/zzaekkii"))
                .andExpect(jsonPath("$.data.blogUrl").value("https://zzaekkii.dev"))
                .andExpect(jsonPath("$.data.counts.projects").value(2))
                .andExpect(jsonPath("$.data.counts.posts").value(18))
                .andDo(document(
                        "user-public-profile-get",
                        resource(ResourceSnippetParameters.builder()
                                .tag("User")
                                .summary("사용자 공개 프로필 조회")
                                .description("handle로 사용자의 공개 프로필과 프로젝트 및 피드 개수를 조회한다.")
                                .pathParameters(
                                        parameterWithName("handle").description("조회할 사용자의 handle")
                                )
                                .responseSchema(Schema.schema("UserProfileSuccessResponse"))
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data").type(OBJECT).description("사용자 공개 프로필"),
                                        fieldWithPath("data.handle").type(STRING).description("사용자 handle"),
                                        fieldWithPath("data.displayName").type(STRING).description("표시 이름"),
                                        fieldWithPath("data.userType").type(STRING).description("사용자 유형").optional(),
                                        fieldWithPath("data.track").type(STRING).description("우테코 트랙").optional(),
                                        fieldWithPath("data.cohort").type(NUMBER).description("우테코 기수").optional(),
                                        fieldWithPath("data.bio").type(STRING).description("한 줄 소개").optional(),
                                        fieldWithPath("data.avatarImageId").type(NUMBER)
                                                .description("프로필 이미지 미디어 ID").optional(),
                                        fieldWithPath("data.githubProfileUrl").type(STRING)
                                                .description("GitHub 프로필 URL").optional(),
                                        fieldWithPath("data.blogUrl").type(STRING)
                                                .description("블로그 URL").optional(),
                                        fieldWithPath("data.counts").type(OBJECT).description("프로필 항목 개수"),
                                        fieldWithPath("data.counts.projects").type(NUMBER)
                                                .description("삭제되지 않은 참여 프로젝트 개수"),
                                        fieldWithPath("data.counts.posts").type(NUMBER)
                                                .description("삭제되지 않은 작성 피드 개수")
                                )
                                .build())
                ));
    }

    @Test
    @DisplayName("인증 없이 ACTIVE 우테코 크루와 코치를 검색한다")
    void searchWoowaMember() throws Exception {
        given(userService.searchWoowaMember("재키", null, 20))
                .willReturn(new UserSearchResult(
                        List.of(
                                new UserSearchItem(
                                        "zzaekkii",
                                        "재키",
                                        UserType.WOOWACOURSE_CREW,
                                        "BACKEND",
                                        (short) 8,
                                        21L,
                                        0
                                ),
                                new UserSearchItem(
                                        "coach-jack",
                                        "재키 코치",
                                        UserType.WOOWACOURSE_COACH,
                                        null,
                                        null,
                                        null,
                                        1
                                )
                        ),
                        "eyJyZWxldmFuY2VSYW5rIjowLCJkaXNwbGF5TmFtZSI6IuyerO2CpCIsImhhbmRsZSI6Inp6YWVra2lpIn0",
                        true
                ));

        mockMvc.perform(get("/api/v1/users/search")
                        .queryParam("keyword", "재키"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.items[0].handle").value("zzaekkii"))
                .andExpect(jsonPath("$.data.items[0].displayName").value("재키"))
                .andExpect(jsonPath("$.data.items[0].userType").value("WOOWACOURSE_CREW"))
                .andExpect(jsonPath("$.data.items[0].track").value("BACKEND"))
                .andExpect(jsonPath("$.data.items[0].cohort").value(8))
                .andExpect(jsonPath("$.data.items[0].avatarImageId").value(21))
                .andExpect(jsonPath("$.data.items[1].handle").value("coach-jack"))
                .andExpect(jsonPath("$.data.items[1].userType").value("WOOWACOURSE_COACH"))
                .andExpect(jsonPath("$.data.items[1].track").isEmpty())
                .andExpect(jsonPath("$.data.items[1].cohort").isEmpty())
                .andExpect(jsonPath("$.meta.nextCursor").value(
                        "eyJyZWxldmFuY2VSYW5rIjowLCJkaXNwbGF5TmFtZSI6IuyerO2CpCIsImhhbmRsZSI6Inp6YWVra2lpIn0"
                ))
                .andExpect(jsonPath("$.meta.hasNext").value(true))
                .andDo(document(
                        "user-search-get",
                        resource(ResourceSnippetParameters.builder()
                                .tag("User")
                                .summary("우테코 사용자 검색")
                                .description("ACTIVE 상태의 우테코 크루와 코치를 이름 또는 handle로 검색한다.")
                                .queryParameters(
                                        parameterWithName("keyword").description("이름 또는 handle 검색어"),
                                        parameterWithName("cursor").description("다음 페이지 커서").optional(),
                                        parameterWithName("size")
                                                .type(INTEGER)
                                                .defaultValue(20)
                                                .description("조회 개수(1~100), 기본값 20")
                                                .optional()
                                )
                                .responseSchema(Schema.schema("UserSearchSuccessResponse"))
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data").type(OBJECT).description("검색 결과"),
                                        fieldWithPath("data.items").type(ARRAY).description("검색된 크루와 코치"),
                                        fieldWithPath("data.items[].handle").type(STRING).description("사용자 handle"),
                                        fieldWithPath("data.items[].displayName").type(STRING).description("표시 이름"),
                                        fieldWithPath("data.items[].userType").type(STRING).description("사용자 유형"),
                                        fieldWithPath("data.items[].track").type(STRING)
                                                .description("우테코 트랙").optional(),
                                        fieldWithPath("data.items[].cohort").type(NUMBER)
                                                .description("우테코 기수").optional(),
                                        fieldWithPath("data.items[].avatarImageId").type(NUMBER)
                                                .description("프로필 이미지 미디어 ID").optional(),
                                        fieldWithPath("meta").type(OBJECT).description("페이지 정보"),
                                        fieldWithPath("meta.nextCursor").type(STRING)
                                                .description("다음 페이지 커서").optional(),
                                        fieldWithPath("meta.hasNext").type(BOOLEAN)
                                                .description("다음 페이지 존재 여부")
                                )
                                .build())
                ));
    }

    @Test
    @DisplayName("빈 검색어로 우테코 사용자를 검색할 수 없다")
    void rejectBlankSearchKeyword() throws Exception {
        mockMvc.perform(get("/api/v1/users/search")
                        .queryParam("keyword", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andDo(document(
                        "user-search-get-invalid",
                        resource(ResourceSnippetParameters.builder()
                                .tag("User")
                                .summary("우테코 사용자 검색")
                                .description("ACTIVE 상태의 우테코 크루와 코치를 이름 또는 handle로 검색한다.")
                                .queryParameters(
                                        parameterWithName("keyword").description("이름 또는 handle 검색어")
                                )
                                .responseSchema(Schema.schema("ErrorResponse"))
                                .responseFields(RestDocsFields.errorResponse())
                                .build())
                ));

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("검색어가 50자를 초과하면 우테코 사용자를 검색할 수 없다")
    void rejectLongSearchKeyword() throws Exception {
        mockMvc.perform(get("/api/v1/users/search")
                        .queryParam("keyword", "😀".repeat(51)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("검색 결과 개수가 허용 범위를 벗어나면 우테코 사용자를 검색할 수 없다")
    void rejectInvalidSearchSize() throws Exception {
        mockMvc.perform(get("/api/v1/users/search")
                        .queryParam("keyword", "재키")
                        .queryParam("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("잘못된 커서로 우테코 사용자를 검색할 수 없다")
    void rejectInvalidSearchCursor() throws Exception {
        given(userService.searchWoowaMember("재키", "invalid", 20))
                .willThrow(new BadRequestException(UserErrorCode.USER_SEARCH_CURSOR_INVALID));

        mockMvc.perform(get("/api/v1/users/search")
                .queryParam("keyword", "재키")
                .queryParam("cursor", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USER_SEARCH_CURSOR_INVALID"))
                .andDo(document(
                        "user-search-get-cursor-invalid",
                        resource(ResourceSnippetParameters.builder()
                                .tag("User")
                                .summary("우테코 사용자 검색")
                                .description("ACTIVE 상태의 우테코 크루와 코치를 이름 또는 handle로 검색한다.")
                                .queryParameters(
                                        parameterWithName("keyword").description("이름 또는 handle 검색어"),
                                        parameterWithName("cursor").description("다음 페이지 커서")
                                )
                                .responseSchema(Schema.schema("ErrorResponse"))
                                .responseFields(RestDocsFields.errorResponse())
                                .build())
                ));
    }

    @Test
    @DisplayName("형식이 잘못된 handle로 공개 프로필을 조회할 수 없다")
    void rejectInvalidPublicProfileHandle() throws Exception {
        mockMvc.perform(get("/api/v1/users/{handle}", "잘못된-핸들"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andDo(document(
                        "user-public-profile-get-invalid",
                        resource(ResourceSnippetParameters.builder()
                                .tag("User")
                                .summary("사용자 공개 프로필 조회")
                                .description("handle로 사용자의 공개 프로필과 프로젝트 및 피드 개수를 조회한다.")
                                .pathParameters(
                                        parameterWithName("handle").description("조회할 사용자의 handle")
                                )
                                .responseSchema(Schema.schema("ErrorResponse"))
                                .responseFields(RestDocsFields.errorResponse())
                                .build())
                ));

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("존재하지 않는 handle로 공개 프로필을 조회할 수 없다")
    void rejectNotFoundPublicProfileHandle() throws Exception {
        given(userService.getPublicProfile("missing-user"))
                .willThrow(new EntityNotFoundException(UserErrorCode.USER_NOT_FOUND));

        mockMvc.perform(get("/api/v1/users/{handle}", "missing-user"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
                .andDo(document(
                        "user-public-profile-get-not-found",
                        resource(ResourceSnippetParameters.builder()
                                .tag("User")
                                .summary("사용자 공개 프로필 조회")
                                .description("handle로 사용자의 공개 프로필과 프로젝트 및 피드 개수를 조회한다.")
                                .pathParameters(
                                        parameterWithName("handle").description("조회할 사용자의 handle")
                                )
                                .responseSchema(Schema.schema("ErrorResponse"))
                                .responseFields(RestDocsFields.errorResponse())
                                .build())
                ));
    }
}
