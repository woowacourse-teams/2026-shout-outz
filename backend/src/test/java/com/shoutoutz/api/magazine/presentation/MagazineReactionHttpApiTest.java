package com.shoutoutz.api.magazine.presentation;

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
import com.epages.restdocs.apispec.SimpleType;
import com.shoutoutz.api.auth.presentation.session.AuthenticatedSession;
import com.shoutoutz.api.common.exception.code.CommonErrorCode;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.InvalidInputException;
import com.shoutoutz.api.common.restdocs.RestDocsFields;
import com.shoutoutz.api.magazine.application.MagazineReactionService;
import com.shoutoutz.api.magazine.domain.MagazineErrorCode;
import com.shoutoutz.api.magazine.domain.MagazineReactionType;
import com.shoutoutz.api.magazine.presentation.dto.response.MagazineReactionResponse;
import com.shoutoutz.api.user.domain.account.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(controllers = MagazineReactionHttpApi.class)
@AutoConfigureRestDocs
class MagazineReactionHttpApiTest {

    private static final long USER_ID = 1L;
    private static final long MAGAZINE_ID = 100L;
    private static final String AUTHENTICATED_SESSION_ATTRIBUTE = AuthenticatedSession.class.getName();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MagazineReactionService magazineReactionService;

    @Test
    void 매거진_좋아요를_추가한다() throws Exception {
        given(magazineReactionService.add(MAGAZINE_ID, USER_ID, "LIKE"))
                .willReturn(new MagazineReactionResponse(MAGAZINE_ID, MagazineReactionType.LIKE, true, 21L));

        mockMvc.perform(put("/api/v1/magazines/{magazineId}/reactions/{type}", MAGAZINE_ID, "LIKE")
                        .with(authenticated())
                        .header(HttpHeaders.COOKIE, "JSESSIONID=user-session")
                        .header("X-CSRF-Token", "csrf-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.magazineId").value(MAGAZINE_ID))
                .andExpect(jsonPath("$.data.type").value("LIKE"))
                .andExpect(jsonPath("$.data.active").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(21))
                .andDo(document(
                        "magazine-reaction-add",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Magazine Reaction")
                                .summary("매거진 반응 추가")
                                .description("현재 로그인한 활성 사용자가 발행된 매거진에 type 반응을 추가한다. 현재는 LIKE만 지원하며, 이미 존재하는 반응이면 현재 상태를 반환한다.")
                                .pathParameters(
                                        parameterWithName("magazineId").type(SimpleType.INTEGER).description("매거진 ID"),
                                        parameterWithName("type").description("반응 타입. 현재 LIKE만 지원한다.")
                                )
                                .requestHeaders(
                                        headerWithName(HttpHeaders.COOKIE).description("활성 회원 세션의 JSESSIONID"),
                                        headerWithName("X-CSRF-Token").description("세션 조회로 발급받은 CSRF 토큰")
                                )
                                .responseSchema(Schema.schema("MagazineReactionSuccessResponse"))
                                .responseFields(successResponseFields())
                                .build())
                ));

        verify(magazineReactionService).add(MAGAZINE_ID, USER_ID, "LIKE");
    }

    @Test
    void 매거진_좋아요를_제거한다() throws Exception {
        given(magazineReactionService.remove(MAGAZINE_ID, USER_ID, "LIKE"))
                .willReturn(new MagazineReactionResponse(MAGAZINE_ID, MagazineReactionType.LIKE, false, 20L));

        mockMvc.perform(delete("/api/v1/magazines/{magazineId}/reactions/{type}", MAGAZINE_ID, "LIKE")
                        .with(authenticated())
                        .header(HttpHeaders.COOKIE, "JSESSIONID=user-session")
                        .header("X-CSRF-Token", "csrf-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.magazineId").value(MAGAZINE_ID))
                .andExpect(jsonPath("$.data.type").value("LIKE"))
                .andExpect(jsonPath("$.data.active").value(false))
                .andExpect(jsonPath("$.data.likeCount").value(20))
                .andDo(document(
                        "magazine-reaction-remove",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Magazine Reaction")
                                .summary("매거진 반응 제거")
                                .description("현재 로그인한 활성 사용자가 발행된 매거진의 type 반응을 제거한다. 현재는 LIKE만 지원하며, 반응이 존재하면 삭제 후 현재 상태를 반환하고, 반응이 없으면 REACTION_NOT_FOUND를 반환한다.")
                                .pathParameters(
                                        parameterWithName("magazineId").type(SimpleType.INTEGER).description("매거진 ID"),
                                        parameterWithName("type").description("반응 타입. 현재 LIKE만 지원한다.")
                                )
                                .requestHeaders(
                                        headerWithName(HttpHeaders.COOKIE).description("활성 회원 세션의 JSESSIONID"),
                                        headerWithName("X-CSRF-Token").description("세션 조회로 발급받은 CSRF 토큰")
                                )
                                .responseSchema(Schema.schema("MagazineReactionSuccessResponse"))
                                .responseFields(successResponseFields())
                                .build())
                ));

        verify(magazineReactionService).remove(MAGAZINE_ID, USER_ID, "LIKE");
    }

    @Test
    void 매거진에_반응이_없으면_삭제할_때_404를_반환한다() throws Exception {
        given(magazineReactionService.remove(MAGAZINE_ID, USER_ID, "LIKE"))
                .willThrow(new EntityNotFoundException(MagazineErrorCode.REACTION_NOT_FOUND));

        mockMvc.perform(delete("/api/v1/magazines/{magazineId}/reactions/{type}", MAGAZINE_ID, "LIKE")
                        .with(authenticated())
                        .header("X-CSRF-Token", "csrf-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value(MagazineErrorCode.REACTION_NOT_FOUND.name()))
                .andExpect(jsonPath("$.message").value("요청한 반응을 찾을 수 없습니다."))
                .andDo(document(
                        "magazine-reaction-remove-reaction-not-found",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Magazine Reaction")
                                .summary("매거진 반응 제거")
                                .pathParameters(
                                        parameterWithName("magazineId").type(SimpleType.INTEGER).description("매거진 ID"),
                                        parameterWithName("type").description("반응 타입. 현재 LIKE만 지원한다.")
                                )
                                .requestHeaders(
                                        headerWithName("X-CSRF-Token").description("세션 조회로 발급받은 CSRF 토큰")
                                )
                                .responseSchema(Schema.schema("ErrorResponse"))
                                .responseFields(RestDocsFields.errorResponse())
                                .build())
                ));

        verify(magazineReactionService).remove(MAGAZINE_ID, USER_ID, "LIKE");
    }

    @Test
    void 지원하지_않는_매거진_반응_타입은_400을_반환한다() throws Exception {
        given(magazineReactionService.add(MAGAZINE_ID, USER_ID, "BOOKMARK"))
                .willThrow(new InvalidInputException(MagazineErrorCode.REACTION_TYPE_INVALID));

        mockMvc.perform(put("/api/v1/magazines/{magazineId}/reactions/{type}", MAGAZINE_ID, "BOOKMARK")
                        .with(authenticated())
                        .header("X-CSRF-Token", "csrf-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(MagazineErrorCode.REACTION_TYPE_INVALID.name()))
                .andDo(document(
                        "magazine-reaction-add-invalid-type",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Magazine Reaction")
                                .summary("매거진 반응 추가")
                                .pathParameters(
                                        parameterWithName("magazineId").type(SimpleType.INTEGER).description("매거진 ID"),
                                        parameterWithName("type").description("반응 타입. 현재 LIKE만 지원한다.")
                                )
                                .requestHeaders(
                                        headerWithName("X-CSRF-Token").description("세션 조회로 발급받은 CSRF 토큰")
                                )
                                .responseSchema(Schema.schema("ErrorResponse"))
                                .responseFields(RestDocsFields.errorResponse())
                                .build())
                ));

        verify(magazineReactionService).add(MAGAZINE_ID, USER_ID, "BOOKMARK");
    }

    @Test
    void 지원하지_않는_타입으로_매거진_반응을_삭제하면_400을_반환한다() throws Exception {
        given(magazineReactionService.remove(MAGAZINE_ID, USER_ID, "BOOKMARK"))
                .willThrow(new InvalidInputException(MagazineErrorCode.REACTION_TYPE_INVALID));

        mockMvc.perform(delete("/api/v1/magazines/{magazineId}/reactions/{type}", MAGAZINE_ID, "BOOKMARK")
                        .with(authenticated())
                        .header("X-CSRF-Token", "csrf-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(MagazineErrorCode.REACTION_TYPE_INVALID.name()))
                .andDo(document(
                        "magazine-reaction-remove-invalid-type",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Magazine Reaction")
                                .summary("매거진 반응 제거")
                                .pathParameters(
                                        parameterWithName("magazineId").type(SimpleType.INTEGER).description("매거진 ID"),
                                        parameterWithName("type").description("반응 타입. 현재 LIKE만 지원한다.")
                                )
                                .requestHeaders(
                                        headerWithName("X-CSRF-Token").description("세션 조회로 발급받은 CSRF 토큰")
                                )
                                .responseSchema(Schema.schema("ErrorResponse"))
                                .responseFields(RestDocsFields.errorResponse())
                                .build())
                ));

        verify(magazineReactionService).remove(MAGAZINE_ID, USER_ID, "BOOKMARK");
    }

    @Test
    void 로그인하지_않으면_매거진_좋아요를_변경할_수_없다() throws Exception {
        mockMvc.perform(put("/api/v1/magazines/{magazineId}/reactions/{type}", MAGAZINE_ID, "LIKE"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.UNAUTHORIZED.name()))
                .andDo(document(
                        "magazine-reaction-add-unauthorized",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Magazine Reaction")
                                .summary("매거진 반응 추가")
                                .pathParameters(
                                        parameterWithName("magazineId").type(SimpleType.INTEGER).description("매거진 ID"),
                                        parameterWithName("type").description("반응 타입. 현재 LIKE만 지원한다.")
                                )
                                .responseSchema(Schema.schema("ErrorResponse"))
                                .responseFields(RestDocsFields.errorResponse())
                                .build())
                ));

        mockMvc.perform(delete("/api/v1/magazines/{magazineId}/reactions/{type}", MAGAZINE_ID, "LIKE"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.UNAUTHORIZED.name()))
                .andDo(document(
                        "magazine-reaction-remove-unauthorized",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Magazine Reaction")
                                .summary("매거진 반응 제거")
                                .pathParameters(
                                        parameterWithName("magazineId").type(SimpleType.INTEGER).description("매거진 ID"),
                                        parameterWithName("type").description("반응 타입. 현재 LIKE만 지원한다.")
                                )
                                .responseSchema(Schema.schema("ErrorResponse"))
                                .responseFields(RestDocsFields.errorResponse())
                                .build())
                ));

        verifyNoInteractions(magazineReactionService);
    }

    @Test
    void 삭제된_매거진에_좋아요를_추가하면_404를_반환한다() throws Exception {
        given(magazineReactionService.add(MAGAZINE_ID, USER_ID, "LIKE"))
                .willThrow(new EntityNotFoundException(MagazineErrorCode.MAGAZINE_NOT_FOUND));

        mockMvc.perform(put("/api/v1/magazines/{magazineId}/reactions/{type}", MAGAZINE_ID, "LIKE")
                        .with(authenticated())
                        .header("X-CSRF-Token", "csrf-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(MagazineErrorCode.MAGAZINE_NOT_FOUND.name()))
                .andDo(document(
                        "magazine-reaction-add-not-found",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Magazine Reaction")
                                .summary("매거진 반응 추가")
                                .pathParameters(
                                        parameterWithName("magazineId").type(SimpleType.INTEGER).description("매거진 ID"),
                                        parameterWithName("type").description("반응 타입. 현재 LIKE만 지원한다.")
                                )
                                .requestHeaders(
                                        headerWithName("X-CSRF-Token").description("세션 조회로 발급받은 CSRF 토큰")
                                )
                                .responseSchema(Schema.schema("ErrorResponse"))
                                .responseFields(RestDocsFields.errorResponse())
                                .build())
                ));

        verify(magazineReactionService).add(MAGAZINE_ID, USER_ID, "LIKE");
    }

    @Test
    void 삭제된_매거진에서_좋아요를_제거하면_404를_반환한다() throws Exception {
        given(magazineReactionService.remove(MAGAZINE_ID, USER_ID, "LIKE"))
                .willThrow(new EntityNotFoundException(MagazineErrorCode.MAGAZINE_NOT_FOUND));

        mockMvc.perform(delete("/api/v1/magazines/{magazineId}/reactions/{type}", MAGAZINE_ID, "LIKE")
                        .with(authenticated())
                        .header("X-CSRF-Token", "csrf-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(MagazineErrorCode.MAGAZINE_NOT_FOUND.name()))
                .andDo(document(
                        "magazine-reaction-remove-not-found",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Magazine Reaction")
                                .summary("매거진 반응 제거")
                                .pathParameters(
                                        parameterWithName("magazineId").type(SimpleType.INTEGER).description("매거진 ID"),
                                        parameterWithName("type").description("반응 타입. 현재 LIKE만 지원한다.")
                                )
                                .requestHeaders(
                                        headerWithName("X-CSRF-Token").description("세션 조회로 발급받은 CSRF 토큰")
                                )
                                .responseSchema(Schema.schema("ErrorResponse"))
                                .responseFields(RestDocsFields.errorResponse())
                                .build())
                ));

        verify(magazineReactionService).remove(MAGAZINE_ID, USER_ID, "LIKE");
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
                fieldWithPath("data").type(OBJECT).description("매거진 반응 변경 결과"),
                fieldWithPath("data.magazineId").type(NUMBER).description("매거진 ID"),
                fieldWithPath("data.type").type(STRING).description("반응 타입. 현재 LIKE만 지원하며 향후 확장할 수 있다."),
                fieldWithPath("data.active").type(BOOLEAN).description("요청한 반응의 활성 상태"),
                fieldWithPath("data.likeCount").type(NUMBER).description("매거진 좋아요 수")
        };
    }
}
