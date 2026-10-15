package com.shoutoutz.api.interview.presentation;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.payload.JsonFieldType.BOOLEAN;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.OBJECT;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.shoutoutz.api.auth.presentation.session.AuthenticatedSession;
import com.shoutoutz.api.common.exception.code.CommonErrorCode;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.InvalidInputException;
import com.shoutoutz.api.common.restdocs.RestDocsFields;
import com.shoutoutz.api.interview.application.InterviewReactionService;
import com.shoutoutz.api.interview.domain.InterviewErrorCode;
import com.shoutoutz.api.interview.domain.InterviewReactionType;
import com.shoutoutz.api.interview.presentation.dto.response.InterviewReactionResponse;
import com.shoutoutz.api.user.domain.account.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(controllers = InterviewReactionHttpApi.class)
@AutoConfigureRestDocs
class InterviewReactionHttpApiTest {

    private static final long USER_ID = 1L;
    private static final long INTERVIEW_ID = 100L;
    private static final String AUTHENTICATED_SESSION_ATTRIBUTE = AuthenticatedSession.class.getName();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InterviewReactionService interviewReactionService;

    @Test
    void 인터뷰_좋아요를_추가한다() throws Exception {
        given(interviewReactionService.add(INTERVIEW_ID, USER_ID, "LIKE"))
                .willReturn(new InterviewReactionResponse(INTERVIEW_ID, InterviewReactionType.LIKE, true, 21L));

        mockMvc.perform(put("/api/v1/interviews/{interviewId}/reactions/{type}", INTERVIEW_ID, "LIKE")
                        .with(authenticated())
                        .header("X-CSRF-Token", "csrf-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.interviewId").value(INTERVIEW_ID))
                .andExpect(jsonPath("$.data.type").value("LIKE"))
                .andExpect(jsonPath("$.data.active").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(21))
                .andDo(document(
                        "interview-reaction-add",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Interview Reaction")
                                .summary("인터뷰 반응 추가")
                                .description("현재 로그인한 활성 사용자가 발행된 인터뷰에 type 반응을 추가한다. 현재는 LIKE만 지원하며, 이미 존재하는 반응이면 현재 상태를 반환한다.")
                                .pathParameters(
                                        parameterWithName("interviewId").description("인터뷰 ID"),
                                        parameterWithName("type").description("반응 타입. 현재 LIKE만 지원한다.")
                                )
                                .requestHeaders(
                                        headerWithName("X-CSRF-Token").description("세션 조회로 발급받은 CSRF 토큰")
                                )
                                .responseSchema(Schema.schema("InterviewReactionSuccessResponse"))
                                .responseFields(successResponseFields())
                                .build())
                ));

        verify(interviewReactionService).add(INTERVIEW_ID, USER_ID, "LIKE");
    }

    @Test
    void 인터뷰_좋아요를_제거한다() throws Exception {
        given(interviewReactionService.remove(INTERVIEW_ID, USER_ID, "LIKE"))
                .willReturn(new InterviewReactionResponse(INTERVIEW_ID, InterviewReactionType.LIKE, false, 20L));

        mockMvc.perform(delete("/api/v1/interviews/{interviewId}/reactions/{type}", INTERVIEW_ID, "LIKE")
                        .with(authenticated())
                        .header("X-CSRF-Token", "csrf-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.interviewId").value(INTERVIEW_ID))
                .andExpect(jsonPath("$.data.type").value("LIKE"))
                .andExpect(jsonPath("$.data.active").value(false))
                .andExpect(jsonPath("$.data.likeCount").value(20))
                .andDo(document(
                        "interview-reaction-remove",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Interview Reaction")
                                .summary("인터뷰 반응 제거")
                                .description("현재 로그인한 활성 사용자가 발행된 인터뷰의 type 반응을 제거한다. 현재는 LIKE만 지원하며, 반응이 존재하면 삭제 후 현재 상태를 반환하고, 반응이 없으면 REACTION_NOT_FOUND를 반환한다.")
                                .pathParameters(
                                        parameterWithName("interviewId").description("인터뷰 ID"),
                                        parameterWithName("type").description("반응 타입. 현재 LIKE만 지원한다.")
                                )
                                .requestHeaders(
                                        headerWithName("X-CSRF-Token").description("세션 조회로 발급받은 CSRF 토큰")
                                )
                                .responseSchema(Schema.schema("InterviewReactionSuccessResponse"))
                                .responseFields(successResponseFields())
                                .build())
                ));

        verify(interviewReactionService).remove(INTERVIEW_ID, USER_ID, "LIKE");
    }

    @Test
    void 인터뷰에_반응이_없으면_삭제할_때_404를_반환한다() throws Exception {
        given(interviewReactionService.remove(INTERVIEW_ID, USER_ID, "LIKE"))
                .willThrow(new EntityNotFoundException(InterviewErrorCode.REACTION_NOT_FOUND));

        mockMvc.perform(delete("/api/v1/interviews/{interviewId}/reactions/{type}", INTERVIEW_ID, "LIKE")
                        .with(authenticated())
                        .header("X-CSRF-Token", "csrf-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value(InterviewErrorCode.REACTION_NOT_FOUND.name()))
                .andExpect(jsonPath("$.message").value("요청한 반응을 찾을 수 없습니다."))
                .andDo(document(
                        "interview-reaction-remove-reaction-not-found",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Interview Reaction")
                                .summary("인터뷰 반응 제거")
                                .description("현재 로그인 사용자의 해당 인터뷰 반응이 없으면 REACTION_NOT_FOUND를 반환한다.")
                                .pathParameters(
                                        parameterWithName("interviewId").description("인터뷰 ID"),
                                        parameterWithName("type").description("반응 타입. 현재 LIKE만 지원한다.")
                                )
                                .requestHeaders(
                                        headerWithName("X-CSRF-Token").description("세션 조회로 발급받은 CSRF 토큰")
                                )
                                .responseSchema(Schema.schema("ErrorResponse"))
                                .responseFields(RestDocsFields.errorResponse())
                                .build())
                ));

        verify(interviewReactionService).remove(INTERVIEW_ID, USER_ID, "LIKE");
    }

    @Test
    void 지원하지_않는_인터뷰_반응_타입은_400을_반환한다() throws Exception {
        given(interviewReactionService.add(INTERVIEW_ID, USER_ID, "BOOKMARK"))
                .willThrow(new InvalidInputException(InterviewErrorCode.REACTION_TYPE_INVALID));

        mockMvc.perform(put("/api/v1/interviews/{interviewId}/reactions/{type}", INTERVIEW_ID, "BOOKMARK")
                        .with(authenticated())
                        .header("X-CSRF-Token", "csrf-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(InterviewErrorCode.REACTION_TYPE_INVALID.name()))
                .andDo(document(
                        "interview-reaction-add-invalid-type",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Interview Reaction")
                                .summary("인터뷰 반응 추가")
                                .description("현재 로그인한 활성 사용자가 발행된 인터뷰에 type 반응을 추가한다. 현재는 LIKE만 지원하며, 이미 존재하는 반응이면 현재 상태를 반환한다.")
                                .pathParameters(
                                        parameterWithName("interviewId").description("인터뷰 ID"),
                                        parameterWithName("type").description("반응 타입. 현재 LIKE만 지원한다.")
                                )
                                .requestHeaders(
                                        headerWithName("X-CSRF-Token").description("세션 조회로 발급받은 CSRF 토큰")
                                )
                                .responseSchema(Schema.schema("ErrorResponse"))
                                .responseFields(RestDocsFields.errorResponse())
                                .build())
                ));

        verify(interviewReactionService).add(INTERVIEW_ID, USER_ID, "BOOKMARK");
    }

    @Test
    void 로그인하지_않으면_인터뷰_좋아요를_변경할_수_없다() throws Exception {
        mockMvc.perform(put("/api/v1/interviews/{interviewId}/reactions/{type}", INTERVIEW_ID, "LIKE"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.UNAUTHORIZED.name()))
                .andDo(document(
                        "interview-reaction-add-unauthorized",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Interview Reaction")
                                .summary("인터뷰 반응 추가")
                                .description("현재 로그인한 활성 사용자가 발행된 인터뷰에 type 반응을 추가한다. 현재는 LIKE만 지원하며, 이미 존재하는 반응이면 현재 상태를 반환한다.")
                                .pathParameters(
                                        parameterWithName("interviewId").description("인터뷰 ID"),
                                        parameterWithName("type").description("반응 타입. 현재 LIKE만 지원한다.")
                                )
                                .responseSchema(Schema.schema("ErrorResponse"))
                                .responseFields(RestDocsFields.errorResponse())
                                .build())
                ));

        mockMvc.perform(delete("/api/v1/interviews/{interviewId}/reactions/{type}", INTERVIEW_ID, "LIKE"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.UNAUTHORIZED.name()))
                .andDo(document(
                        "interview-reaction-remove-unauthorized",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Interview Reaction")
                                .summary("인터뷰 반응 제거")
                                .description("현재 로그인한 활성 사용자가 발행된 인터뷰의 type 반응을 제거한다. 현재는 LIKE만 지원하며, 반응이 존재하면 삭제 후 현재 상태를 반환하고, 반응이 없으면 REACTION_NOT_FOUND를 반환한다.")
                                .pathParameters(
                                        parameterWithName("interviewId").description("인터뷰 ID"),
                                        parameterWithName("type").description("반응 타입. 현재 LIKE만 지원한다.")
                                )
                                .responseSchema(Schema.schema("ErrorResponse"))
                                .responseFields(RestDocsFields.errorResponse())
                                .build())
                ));

        verifyNoInteractions(interviewReactionService);
    }

    @Test
    void 삭제된_인터뷰에_좋아요를_추가하면_404를_반환한다() throws Exception {
        given(interviewReactionService.add(INTERVIEW_ID, USER_ID, "LIKE"))
                .willThrow(new EntityNotFoundException(InterviewErrorCode.INTERVIEW_NOT_FOUND));

        mockMvc.perform(put("/api/v1/interviews/{interviewId}/reactions/{type}", INTERVIEW_ID, "LIKE")
                        .with(authenticated())
                        .header("X-CSRF-Token", "csrf-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(InterviewErrorCode.INTERVIEW_NOT_FOUND.name()))
                .andDo(document(
                        "interview-reaction-add-not-found",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Interview Reaction")
                                .summary("인터뷰 반응 추가")
                                .description("현재 로그인한 활성 사용자가 발행된 인터뷰에 type 반응을 추가한다. 현재는 LIKE만 지원하며, 이미 존재하는 반응이면 현재 상태를 반환한다.")
                                .pathParameters(
                                        parameterWithName("interviewId").description("인터뷰 ID"),
                                        parameterWithName("type").description("반응 타입. 현재 LIKE만 지원한다.")
                                )
                                .requestHeaders(
                                        headerWithName("X-CSRF-Token").description("세션 조회로 발급받은 CSRF 토큰")
                                )
                                .responseSchema(Schema.schema("ErrorResponse"))
                                .responseFields(RestDocsFields.errorResponse())
                                .build())
                ));

        verify(interviewReactionService).add(INTERVIEW_ID, USER_ID, "LIKE");
    }

    @Test
    void 삭제된_인터뷰에서_좋아요를_제거하면_404를_반환한다() throws Exception {
        given(interviewReactionService.remove(INTERVIEW_ID, USER_ID, "LIKE"))
                .willThrow(new EntityNotFoundException(InterviewErrorCode.INTERVIEW_NOT_FOUND));

        mockMvc.perform(delete("/api/v1/interviews/{interviewId}/reactions/{type}", INTERVIEW_ID, "LIKE")
                        .with(authenticated())
                        .header("X-CSRF-Token", "csrf-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(InterviewErrorCode.INTERVIEW_NOT_FOUND.name()))
                .andDo(document(
                        "interview-reaction-remove-not-found",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Interview Reaction")
                                .summary("인터뷰 반응 제거")
                                .description("현재 로그인한 활성 사용자가 발행된 인터뷰의 type 반응을 제거한다. 현재는 LIKE만 지원하며, 반응이 존재하면 삭제 후 현재 상태를 반환하고, 반응이 없으면 REACTION_NOT_FOUND를 반환한다.")
                                .pathParameters(
                                        parameterWithName("interviewId").description("인터뷰 ID"),
                                        parameterWithName("type").description("반응 타입. 현재 LIKE만 지원한다.")
                                )
                                .requestHeaders(
                                        headerWithName("X-CSRF-Token").description("세션 조회로 발급받은 CSRF 토큰")
                                )
                                .responseSchema(Schema.schema("ErrorResponse"))
                                .responseFields(RestDocsFields.errorResponse())
                                .build())
                ));

        verify(interviewReactionService).remove(INTERVIEW_ID, USER_ID, "LIKE");
    }

    private RequestPostProcessor authenticated() {
        return request -> {
            request.setAttribute(
                    AUTHENTICATED_SESSION_ATTRIBUTE,
                    new AuthenticatedSession(USER_ID, UserRole.USER)
            );
            return request;
        };
    }

    private FieldDescriptor[] successResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("status").type(STRING).description("응답 상태"),
                fieldWithPath("data").type(OBJECT).description("인터뷰 반응 변경 결과"),
                fieldWithPath("data.interviewId").type(NUMBER).description("인터뷰 ID"),
                fieldWithPath("data.type").type(STRING).description("반응 타입. 현재 LIKE만 지원하며 향후 확장할 수 있다."),
                fieldWithPath("data.active").type(BOOLEAN).description("요청한 반응의 활성 상태"),
                fieldWithPath("data.likeCount").type(NUMBER).description("인터뷰 좋아요 수")
        };
    }
}
