package com.shoutoutz.api.media.presentation;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.SimpleType.INTEGER;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.OBJECT;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.EnumFields;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.shoutoutz.api.auth.presentation.session.AuthenticatedSession;
import com.shoutoutz.api.common.exception.code.CommonErrorCode;
import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.common.restdocs.RestDocsFields;
import com.shoutoutz.api.media.application.MediaStatusQueryService;
import com.shoutoutz.api.media.domain.MediaStatus;
import com.shoutoutz.api.media.presentation.dto.response.MediaStatusResponse;
import com.shoutoutz.api.user.domain.account.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.stream.Stream;

@WebMvcTest(controllers = MediaStatusHttpApi.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs
class MediaStatusHttpApiTest {

    private static final long USER_ID = 7L;
    private static final long MEDIA_ID = 10L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MediaStatusQueryService mediaStatusQueryService;

    @Test
    void 업로더에게_처리_상태를_봉투에_담아_반환한다() throws Exception {
        given(mediaStatusQueryService.getStatus(USER_ID, MEDIA_ID))
                .willReturn(new MediaStatusResponse(MEDIA_ID, MediaStatus.PROCESSING));

        mockMvc.perform(get("/api/v1/media/{mediaId}/status", MEDIA_ID)
                        .header(HttpHeaders.COOKIE, "JSESSIONID=session-id")
                        .requestAttr(AuthenticatedSession.class.getName(), authenticatedSession()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.mediaId").value(MEDIA_ID))
                .andExpect(jsonPath("$.data.status").value("PROCESSING"))
                .andExpect(jsonPath("$.meta").doesNotExist())
                .andDo(document(
                        "media-status-get",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Media")
                                .summary("미디어 처리 상태 조회")
                                .description("업로더가 미디어의 현재 처리 상태를 조회한다. FAILED도 조회 성공으로 응답한다.")
                                .pathParameters(parameterWithName("mediaId")
                                        .type(INTEGER).description("조회할 미디어 ID"))
                                .requestHeaders(headerWithName(HttpHeaders.COOKIE)
                                        .description("인증된 사용자의 JSESSIONID"))
                                .responseHeaders(headerWithName(HttpHeaders.CACHE_CONTROL)
                                        .description("상태 응답 캐시 방지 (no-store)"))
                                .responseSchema(Schema.schema("MediaStatusSuccessResponse"))
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태 (success)"),
                                        fieldWithPath("data").type(OBJECT).description("미디어 처리 상태"),
                                        fieldWithPath("data.mediaId").type(NUMBER).description("미디어 ID"),
                                        new EnumFields(MediaStatus.class).withPath("data.status")
                                                .description("미디어 상태")
                                )
                                .build())
                ));
    }

    @Test
    void 처리_실패도_조회_요청은_성공한다() throws Exception {
        given(mediaStatusQueryService.getStatus(USER_ID, MEDIA_ID))
                .willReturn(new MediaStatusResponse(MEDIA_ID, MediaStatus.FAILED));

        mockMvc.perform(get("/api/v1/media/{mediaId}/status", MEDIA_ID)
                        .requestAttr(AuthenticatedSession.class.getName(), authenticatedSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.status").value("FAILED"));
    }

    @Test
    void 로그인하지_않으면_401_오류를_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/media/{mediaId}/status", MEDIA_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andDo(document(
                        "media-status-get-unauthorized",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Media")
                                .summary("미디어 처리 상태 조회")
                                .description("업로더가 미디어의 현재 처리 상태를 조회한다. FAILED도 조회 성공으로 응답한다.")
                                .pathParameters(parameterWithName("mediaId")
                                        .type(INTEGER).description("조회할 미디어 ID"))
                                .responseSchema(Schema.schema("ErrorResponse"))
                                .responseFields(RestDocsFields.errorResponse())
                                .build())
                ));

        verifyNoInteractions(mediaStatusQueryService);
    }

    @ParameterizedTest
    @MethodSource("requestFailures")
    void 잘못된_요청은_공통_오류_응답을_반환한다(
            long mediaId,
            RuntimeException exception,
            int httpStatus,
            String errorCode
    ) throws Exception {
        given(mediaStatusQueryService.getStatus(USER_ID, mediaId)).willThrow(exception);

        mockMvc.perform(get("/api/v1/media/{mediaId}/status", mediaId)
                        .header(HttpHeaders.COOKIE, "JSESSIONID=session-id")
                        .requestAttr(AuthenticatedSession.class.getName(), authenticatedSession()))
                .andExpect(status().is(httpStatus))
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value(errorCode))
                .andDo(document(
                        "media-status-get-" + errorCode.toLowerCase(),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Media")
                                .summary("미디어 처리 상태 조회")
                                .description("업로더가 미디어의 현재 처리 상태를 조회한다. FAILED도 조회 성공으로 응답한다.")
                                .pathParameters(parameterWithName("mediaId")
                                        .type(INTEGER).description("조회할 미디어 ID"))
                                .requestHeaders(headerWithName(HttpHeaders.COOKIE)
                                        .description("인증된 사용자의 JSESSIONID"))
                                .responseSchema(Schema.schema("ErrorResponse"))
                                .responseFields(RestDocsFields.errorResponse())
                                .build())
                ));
    }

    @Test
    void 숫자가_아닌_미디어_ID는_400_오류를_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/media/{mediaId}/status", "not-a-number")
                        .requestAttr(AuthenticatedSession.class.getName(), authenticatedSession()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(mediaStatusQueryService);
    }

    private static Stream<Arguments> requestFailures() {
        return Stream.of(
                Arguments.of(0L, new BadRequestException(CommonErrorCode.VALIDATION_FAILED),
                        400, "VALIDATION_FAILED"),
                Arguments.of(MEDIA_ID, new ForbiddenException(CommonErrorCode.FORBIDDEN),
                        403, "FORBIDDEN"),
                Arguments.of(MEDIA_ID, new EntityNotFoundException(CommonErrorCode.RESOURCE_NOT_FOUND),
                        404, "RESOURCE_NOT_FOUND")
        );
    }

    private static AuthenticatedSession authenticatedSession() {
        return new AuthenticatedSession(USER_ID, UserRole.USER);
    }
}
