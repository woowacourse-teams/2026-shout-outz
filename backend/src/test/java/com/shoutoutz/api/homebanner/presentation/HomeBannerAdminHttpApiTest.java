package com.shoutoutz.api.homebanner.presentation;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.SimpleType.INTEGER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.BOOLEAN;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.OBJECT;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.shoutoutz.api.auth.presentation.session.AuthenticatedSession;
import com.shoutoutz.api.common.restdocs.RestDocsFields;
import com.shoutoutz.api.homebanner.application.HomeBannerAdminService;
import com.shoutoutz.api.homebanner.presentation.dto.request.HomeBannerUpsertRequest;
import com.shoutoutz.api.homebanner.presentation.dto.response.HomeBannerAdminResponse;
import com.shoutoutz.api.user.domain.account.UserRole;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(controllers = HomeBannerAdminHttpApi.class)
@AutoConfigureRestDocs
class HomeBannerAdminHttpApiTest {

    private static final long BANNER_ID = 100L;
    private static final String ADMIN_LIST_DESCRIPTION =
            "관리자가 활성 여부와 관계없이 홈 배너를 표시 순서대로 조회한다.";
    private static final String UPSERT_DESCRIPTION =
            "TARGET은 소식·프로젝트·피드 상세를, URL은 내부 경로 또는 외부 HTTPS 주소를 지정한다.";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HomeBannerAdminService homeBannerAdminService;

    @Test
    void 관리자가_전체_배너를_조회한다() throws Exception {
        given(homeBannerAdminService.findAll(UserRole.ADMIN))
                .willReturn(List.of(response()));

        mockMvc.perform(get("/api/v1/admin/home/banners")
                        .with(authenticated(UserRole.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].bannerId").value(BANNER_ID))
                .andDo(document(
                        "admin-home-banner-find-all",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Home Banner Admin")
                                .summary("관리자 홈 배너 목록 조회")
                                .description(ADMIN_LIST_DESCRIPTION)
                                .responseSchema(Schema.schema("HomeBannerAdminFindAllSuccessResponse"))
                                .responseFields(listResponseFields())
                                .build())
                ));
    }

    @Test
    void 관리자가_배너를_등록한다() throws Exception {
        given(homeBannerAdminService.save(
                eq(1L),
                eq(UserRole.ADMIN),
                any(HomeBannerUpsertRequest.class)
        )).willReturn(response());

        mockMvc.perform(post("/api/v1/admin/home/banners")
                        .with(authenticated(UserRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.bannerId").value(BANNER_ID))
                .andDo(document(
                        "admin-home-banner-save",
                        resource(upsertResource("홈 배너 등록", "HomeBannerAdminSaveSuccessResponse"))
                ));
    }

    @Test
    void 관리자가_배너를_전체_교체한다() throws Exception {
        given(homeBannerAdminService.update(
                eq(BANNER_ID),
                eq(UserRole.ADMIN),
                any(HomeBannerUpsertRequest.class)
        )).willReturn(response());

        mockMvc.perform(put("/api/v1/admin/home/banners/{bannerId}", BANNER_ID)
                        .with(authenticated(UserRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.destinationType").value("TARGET"))
                .andDo(document(
                        "admin-home-banner-update",
                        resource(upsertResource("홈 배너 수정", "HomeBannerAdminUpdateSuccessResponse"))
                ));
    }

    @Test
    void 관리자가_배너만_삭제한다() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/home/banners/{bannerId}", BANNER_ID)
                        .with(authenticated(UserRole.ADMIN)))
                .andExpect(status().isNoContent())
                .andDo(document(
                        "admin-home-banner-delete",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Home Banner Admin")
                                .summary("홈 배너 삭제")
                                .description("관리자가 배너 데이터만 삭제하며 연결된 미디어는 삭제하지 않는다.")
                                .pathParameters(parameterWithName("bannerId")
                                        .type(INTEGER)
                                        .description("홈 배너 ID"))
                                .build())
                ));

        verify(homeBannerAdminService).delete(BANNER_ID, UserRole.ADMIN);
    }

    @Test
    void 인증하지_않으면_관리_API를_사용할_수_없다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/home/banners"))
                .andExpect(status().isUnauthorized())
                .andDo(document(
                        "admin-home-banner-find-all-unauthorized",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Home Banner Admin")
                                .summary("관리자 홈 배너 목록 조회")
                                .description(ADMIN_LIST_DESCRIPTION)
                                .responseSchema(Schema.schema("ErrorResponse"))
                                .responseFields(RestDocsFields.errorResponse())
                                .build())
                ));

        verifyNoInteractions(homeBannerAdminService);
    }

    @Test
    void 이동_필드를_혼용하면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/admin/home/banners")
                        .with(authenticated(UserRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mediaId": 10,
                                  "destinationType": "TARGET",
                                  "targetType": "PROJECT",
                                  "targetId": 20,
                                  "linkType": "INTERNAL_PATH",
                                  "linkUrl": "/projects/20",
                                  "displayOrder": 0,
                                  "active": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andDo(document(
                        "admin-home-banner-save-invalid",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Home Banner Admin")
                                .summary("홈 배너 등록")
                                .description(UPSERT_DESCRIPTION)
                                .requestSchema(Schema.schema("HomeBannerUpsertRequest"))
                                .responseSchema(Schema.schema("ErrorResponse"))
                                .requestFields(requestFields())
                                .responseFields(RestDocsFields.errorResponse())
                                .build())
                ));

        verifyNoInteractions(homeBannerAdminService);
    }

    private ResourceSnippetParameters upsertResource(String summary, String responseSchema) {
        var builder = ResourceSnippetParameters.builder()
                .tag("Home Banner Admin")
                .summary(summary)
                .description(UPSERT_DESCRIPTION)
                .requestSchema(Schema.schema("HomeBannerUpsertRequest"))
                .responseSchema(Schema.schema(responseSchema))
                .requestFields(requestFields())
                .responseFields(singleResponseFields());
        if (summary.contains("수정")) {
            builder.pathParameters(parameterWithName("bannerId")
                    .type(INTEGER)
                    .description("홈 배너 ID"));
        }
        return builder.build();
    }

    private List<FieldDescriptor> requestFields() {
        return List.of(
                fieldWithPath("mediaId").type(NUMBER).description("READY HOME_BANNER 미디어 ID"),
                fieldWithPath("destinationType").type(STRING).description("TARGET 또는 URL"),
                fieldWithPath("targetType").type(STRING)
                        .description("NEWS, PROJECT, FEED 중 하나").optional(),
                fieldWithPath("targetId").type(NUMBER).description("대상 리소스 ID").optional(),
                fieldWithPath("linkType").type(STRING)
                        .description("INTERNAL_PATH 또는 EXTERNAL_URL").optional(),
                fieldWithPath("linkUrl").type(STRING)
                        .description("내부 경로 또는 외부 HTTPS URL").optional(),
                fieldWithPath("displayOrder").type(NUMBER).description("0 이상 표시 순서"),
                fieldWithPath("active").type(BOOLEAN).description("즉시 노출 여부")
        );
    }

    private List<FieldDescriptor> singleResponseFields() {
        return responseFields("data", false);
    }

    private List<FieldDescriptor> listResponseFields() {
        return responseFields("data[]", true);
    }

    private List<FieldDescriptor> responseFields(String path, boolean list) {
        return List.of(
                fieldWithPath("status").type(STRING).description("응답 상태"),
                fieldWithPath("data").type(list ? ARRAY : OBJECT).description("홈 배너 데이터"),
                fieldWithPath(path + ".bannerId").type(NUMBER).description("배너 ID"),
                fieldWithPath(path + ".mediaId").type(NUMBER).description("미디어 ID"),
                fieldWithPath(path + ".imageUrl").type(STRING).description("표시용 이미지 URL"),
                fieldWithPath(path + ".destinationType").type(STRING).description("TARGET 또는 URL"),
                fieldWithPath(path + ".targetType").type(STRING)
                        .description("NEWS, PROJECT, FEED 중 하나").optional(),
                fieldWithPath(path + ".targetId").type(NUMBER)
                        .description("대상 리소스 ID").optional(),
                fieldWithPath(path + ".linkType").type(STRING)
                        .description("INTERNAL_PATH 또는 EXTERNAL_URL").optional(),
                fieldWithPath(path + ".linkUrl").type(STRING)
                        .description("내부 경로 또는 외부 HTTPS URL").optional(),
                fieldWithPath(path + ".displayOrder").type(NUMBER).description("표시 순서"),
                fieldWithPath(path + ".active").type(BOOLEAN).description("활성 여부"),
                fieldWithPath(path + ".createdBy").type(NUMBER).description("등록 관리자 ID"),
                fieldWithPath(path + ".createdAt").type(STRING).description("생성 시각"),
                fieldWithPath(path + ".updatedAt").type(STRING).description("수정 시각")
        );
    }

    private RequestPostProcessor authenticated(UserRole role) {
        return request -> {
            request.setAttribute(
                    AuthenticatedSession.class.getName(),
                    new AuthenticatedSession(1L, role)
            );
            return request;
        };
    }

    private String validRequest() {
        return """
                {
                  "mediaId": 10,
                  "destinationType": "TARGET",
                  "targetType": "PROJECT",
                  "targetId": 20,
                  "linkType": null,
                  "linkUrl": null,
                  "displayOrder": 0,
                  "active": true
                }
                """;
    }

    private HomeBannerAdminResponse response() {
        Instant now = Instant.parse("2026-09-17T00:00:00Z");
        return new HomeBannerAdminResponse(
                BANNER_ID,
                10L,
                URI.create("https://cdn.example.com/banner"),
                "TARGET",
                "PROJECT",
                20L,
                null,
                null,
                0,
                true,
                1L,
                now,
                now
        );
    }
}
