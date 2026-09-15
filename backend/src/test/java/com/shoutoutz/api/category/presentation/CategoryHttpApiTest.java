package com.shoutoutz.api.category.presentation;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.SimpleType.INTEGER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
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
import com.shoutoutz.api.category.application.CategoryService;
import com.shoutoutz.api.category.domain.CategoryErrorCode;
import com.shoutoutz.api.category.domain.CategoryType;
import com.shoutoutz.api.category.presentation.dto.request.CategorySaveRequest;
import com.shoutoutz.api.category.presentation.dto.request.CategoryUpdateRequest;
import com.shoutoutz.api.category.presentation.dto.response.CategoryFindAllResponse;
import com.shoutoutz.api.category.presentation.dto.response.CategoryResponse;
import com.shoutoutz.api.common.exception.custom.DuplicateEntityException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.common.exception.custom.NotFoundException;
import com.shoutoutz.api.common.restdocs.RestDocsFields;
import com.shoutoutz.api.user.domain.account.UserRole;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(controllers = CategoryHttpApi.class)
@AutoConfigureRestDocs
class CategoryHttpApiTest {

    private static final long CATEGORY_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    @DisplayName("활성 카테고리 목록을 조회한다")
    void findAllCategories() throws Exception {
        given(categoryService.findAllCategories()).willReturn(List.of(
                new CategoryFindAllResponse(
                        1L,
                        "backend",
                        "백엔드",
                        CategoryType.GENERAL,
                        1
                ),
                new CategoryFindAllResponse(
                        2L,
                        "tecode-talk",
                        "테코드톡",
                        CategoryType.EVENT,
                        2
                )
        ));

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].type").value("GENERAL"))
                .andExpect(jsonPath("$.data[1].type").value("EVENT"))
                .andDo(document(
                        "category-find-all",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Category")
                                .summary("카테고리 목록 조회")
                                .description("피드 작성에 사용할 활성 카테고리를 표시 순서대로 조회한다.")
                                .responseSchema(Schema.schema("CategoryFindAllSuccessResponse"))
                                .responseFields(
                                        fieldWithPath("status").type(STRING).description("응답 상태"),
                                        fieldWithPath("data").type(ARRAY).description("활성 카테고리 목록"),
                                        fieldWithPath("data[].categoryId").type(NUMBER)
                                                .description("카테고리 ID"),
                                        fieldWithPath("data[].slug").type(STRING)
                                                .description("카테고리 slug"),
                                        fieldWithPath("data[].displayName").type(STRING)
                                                .description("표시 이름"),
                                        fieldWithPath("data[].type").type(STRING)
                                                .description("GENERAL 또는 EVENT"),
                                        fieldWithPath("data[].displayOrder").type(NUMBER)
                                                .description("표시 순서")
                                )
                                .build())
                ));
    }

    @Test
    @DisplayName("관리자가 카테고리를 생성한다")
    void saveCategory() throws Exception {
        given(categoryService.saveCategory(
                eq(UserRole.ADMIN),
                any(CategorySaveRequest.class)
        )).willReturn(categoryResponse());

        mockMvc.perform(post("/api/v1/categories")
                        .with(authenticated(UserRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validSaveRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.categoryId").value(CATEGORY_ID))
                .andDo(document(
                        "category-save",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Category")
                                .summary("카테고리 생성")
                                .description("관리자가 일반 또는 이벤트 카테고리를 생성한다.")
                                .requestSchema(Schema.schema("CategorySaveRequest"))
                                .responseSchema(Schema.schema("CategorySaveSuccessResponse"))
                                .requestFields(
                                        fieldWithPath("slug").type(STRING)
                                                .description("영문 소문자, 숫자, 하이픈으로 구성된 고유 slug"),
                                        fieldWithPath("displayName").type(STRING)
                                                .description("고유 표시 이름"),
                                        fieldWithPath("type").type(STRING)
                                                .description("GENERAL 또는 EVENT"),
                                        fieldWithPath("displayOrder").type(NUMBER)
                                                .description("0 이상 32767 이하 표시 순서")
                                )
                                .responseFields(successResponseFields())
                                .build())
                ));
    }

    @Test
    @DisplayName("관리자가 카테고리 표시 순서를 수정한다")
    void updateCategory() throws Exception {
        given(categoryService.updateCategory(
                eq(CATEGORY_ID),
                eq(UserRole.ADMIN),
                any(CategoryUpdateRequest.class)
        )).willReturn(categoryResponse());

        mockMvc.perform(put("/api/v1/categories/{categoryId}", CATEGORY_ID)
                        .with(authenticated(UserRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "백엔드",
                                  "displayOrder": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slug").value("backend"))
                .andDo(document(
                        "category-update",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Category")
                                .summary("카테고리 수정")
                                .description("관리자가 표시 이름과 표시 순서를 전체 교체한다.")
                                .pathParameters(
                                        parameterWithName("categoryId")
                                                .type(INTEGER)
                                                .description("카테고리 ID")
                                )
                                .requestSchema(Schema.schema("CategoryUpdateRequest"))
                                .responseSchema(Schema.schema("CategoryUpdateSuccessResponse"))
                                .requestFields(
                                        fieldWithPath("displayName").type(STRING)
                                                .description("변경할 고유 표시 이름"),
                                        fieldWithPath("displayOrder").type(NUMBER)
                                                .description("변경할 표시 순서")
                                )
                                .responseFields(successResponseFields())
                                .build())
                ));
    }

    @Test
    @DisplayName("관리자가 카테고리를 비활성화한다")
    void deleteCategory() throws Exception {
        mockMvc.perform(delete("/api/v1/categories/{categoryId}", CATEGORY_ID)
                        .with(authenticated(UserRole.ADMIN)))
                .andExpect(status().isNoContent())
                .andDo(document(
                        "category-delete",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Category")
                                .summary("카테고리 삭제")
                                .description("관리자가 카테고리를 비활성화해 목록과 피드 선택 대상에서 제외한다.")
                                .pathParameters(
                                        parameterWithName("categoryId")
                                                .type(INTEGER)
                                                .description("카테고리 ID")
                                )
                                .build())
                ));

        verify(categoryService).deleteCategory(CATEGORY_ID, UserRole.ADMIN);
    }

    @Test
    @DisplayName("인증 없이 카테고리를 생성하면 401을 반환한다")
    void rejectAnonymousSave() throws Exception {
        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validSaveRequest()))
                .andExpect(status().isUnauthorized())
                .andDo(document(
                        "category-save-unauthorized",
                        resource(errorResponse("카테고리 생성"))
                ));

        verifyNoInteractions(categoryService);
    }

    @Test
    @DisplayName("일반 사용자가 카테고리를 생성하면 403을 반환한다")
    void rejectNonAdminSave() throws Exception {
        given(categoryService.saveCategory(
                eq(UserRole.USER),
                any(CategorySaveRequest.class)
        )).willThrow(new ForbiddenException(CategoryErrorCode.CATEGORY_ADMIN_FORBIDDEN));

        mockMvc.perform(post("/api/v1/categories")
                        .with(authenticated(UserRole.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validSaveRequest()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CATEGORY_ADMIN_FORBIDDEN"))
                .andDo(document(
                        "category-save-forbidden",
                        resource(errorResponse("카테고리 생성"))
                ));
    }

    @Test
    @DisplayName("잘못된 카테고리 생성 요청은 400을 반환한다")
    void rejectInvalidSaveRequest() throws Exception {
        mockMvc.perform(post("/api/v1/categories")
                        .with(authenticated(UserRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "slug": "Backend!",
                                  "displayName": " ",
                                  "displayOrder": -1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andDo(document(
                        "category-save-invalid",
                        resource(errorResponse("카테고리 생성"))
                ));

        verifyNoInteractions(categoryService);
    }

    @Test
    @DisplayName("수정 필드가 누락되면 400을 반환한다")
    void rejectEmptyUpdateRequest() throws Exception {
        mockMvc.perform(put("/api/v1/categories/{categoryId}", CATEGORY_ID)
                        .with(authenticated(UserRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andDo(document(
                        "category-update-invalid",
                        resource(updateErrorResponse())
                ));

        verifyNoInteractions(categoryService);
    }

    @Test
    @DisplayName("없는 카테고리를 수정하면 404를 반환한다")
    void rejectMissingCategoryUpdate() throws Exception {
        given(categoryService.updateCategory(
                eq(CATEGORY_ID),
                eq(UserRole.ADMIN),
                any(CategoryUpdateRequest.class)
        )).willThrow(new NotFoundException(CategoryErrorCode.CATEGORY_NOT_FOUND));

        mockMvc.perform(put("/api/v1/categories/{categoryId}", CATEGORY_ID)
                        .with(authenticated(UserRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateRequest()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"))
                .andDo(document(
                        "category-update-not-found",
                        resource(updateErrorResponse())
                ));
    }

    @Test
    @DisplayName("중복된 카테고리를 생성하면 409를 반환한다")
    void rejectDuplicateCategorySave() throws Exception {
        given(categoryService.saveCategory(
                eq(UserRole.ADMIN),
                any(CategorySaveRequest.class)
        )).willThrow(new DuplicateEntityException(CategoryErrorCode.CATEGORY_ALREADY_EXISTS));

        mockMvc.perform(post("/api/v1/categories")
                        .with(authenticated(UserRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validSaveRequest()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CATEGORY_ALREADY_EXISTS"))
                .andDo(document(
                        "category-save-conflict",
                        resource(errorResponse("카테고리 생성"))
                ));
    }

    @Test
    @DisplayName("없는 범위의 카테고리 ID는 조회 결과에 따라 404를 반환한다")
    void rejectInvalidCategoryId() throws Exception {
        willThrow(new NotFoundException(CategoryErrorCode.CATEGORY_NOT_FOUND))
                .given(categoryService).deleteCategory(0L, UserRole.ADMIN);

        mockMvc.perform(delete("/api/v1/categories/{categoryId}", 0)
                        .with(authenticated(UserRole.ADMIN)))
                .andExpect(status().isNotFound())
                .andDo(document(
                        "category-delete-invalid",
                        resource(deleteErrorResponse())
                ));
    }

    @Test
    @DisplayName("인증 없이 카테고리를 수정하거나 삭제하면 401을 반환한다")
    void rejectAnonymousMutation() throws Exception {
        mockMvc.perform(put("/api/v1/categories/{categoryId}", CATEGORY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateRequest()))
                .andExpect(status().isUnauthorized())
                .andDo(document(
                        "category-update-unauthorized",
                        resource(updateErrorResponse())
                ));
        mockMvc.perform(delete("/api/v1/categories/{categoryId}", CATEGORY_ID))
                .andExpect(status().isUnauthorized())
                .andDo(document(
                        "category-delete-unauthorized",
                        resource(deleteErrorResponse())
                ));

        verifyNoInteractions(categoryService);
    }

    @Test
    @DisplayName("일반 사용자가 카테고리를 수정하거나 삭제하면 403을 반환한다")
    void rejectNonAdminMutation() throws Exception {
        given(categoryService.updateCategory(
                eq(CATEGORY_ID),
                eq(UserRole.USER),
                any(CategoryUpdateRequest.class)
        )).willThrow(new ForbiddenException(CategoryErrorCode.CATEGORY_ADMIN_FORBIDDEN));
        willThrow(new ForbiddenException(CategoryErrorCode.CATEGORY_ADMIN_FORBIDDEN))
                .given(categoryService).deleteCategory(CATEGORY_ID, UserRole.USER);

        mockMvc.perform(put("/api/v1/categories/{categoryId}", CATEGORY_ID)
                        .with(authenticated(UserRole.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateRequest()))
                .andExpect(status().isForbidden())
                .andDo(document(
                        "category-update-forbidden",
                        resource(updateErrorResponse())
                ));
        mockMvc.perform(delete("/api/v1/categories/{categoryId}", CATEGORY_ID)
                        .with(authenticated(UserRole.USER)))
                .andExpect(status().isForbidden())
                .andDo(document(
                        "category-delete-forbidden",
                        resource(deleteErrorResponse())
                ));
    }

    @Test
    @DisplayName("없는 카테고리를 삭제하면 404를 반환한다")
    void rejectMissingCategoryDelete() throws Exception {
        willThrow(new NotFoundException(CategoryErrorCode.CATEGORY_NOT_FOUND))
                .given(categoryService).deleteCategory(CATEGORY_ID, UserRole.ADMIN);

        mockMvc.perform(delete("/api/v1/categories/{categoryId}", CATEGORY_ID)
                        .with(authenticated(UserRole.ADMIN)))
                .andExpect(status().isNotFound())
                .andDo(document(
                        "category-delete-not-found",
                        resource(deleteErrorResponse())
                ));
    }

    @Test
    @DisplayName("중복된 표시 이름으로 카테고리를 수정하면 409를 반환한다")
    void rejectDuplicateCategoryUpdate() throws Exception {
        given(categoryService.updateCategory(
                eq(CATEGORY_ID),
                eq(UserRole.ADMIN),
                any(CategoryUpdateRequest.class)
        )).willThrow(new DuplicateEntityException(CategoryErrorCode.CATEGORY_ALREADY_EXISTS));

        mockMvc.perform(put("/api/v1/categories/{categoryId}", CATEGORY_ID)
                        .with(authenticated(UserRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "프론트엔드",
                                  "displayOrder": 1
                                }
                                """))
                .andExpect(status().isConflict())
                .andDo(document(
                        "category-update-conflict",
                        resource(updateErrorResponse())
                ));
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

    private String validSaveRequest() {
        return """
                {
                  "slug": "backend",
                  "displayName": "백엔드",
                  "type": "GENERAL",
                  "displayOrder": 1
                }
                """;
    }

    private String validUpdateRequest() {
        return """
                {
                  "displayName": "서버",
                  "displayOrder": 2
                }
                """;
    }

    private CategoryResponse categoryResponse() {
        return new CategoryResponse(
                CATEGORY_ID,
                "backend",
                "백엔드",
                CategoryType.GENERAL,
                1,
                true
        );
    }

    private ResourceSnippetParameters errorResponse(String summary) {
        return ResourceSnippetParameters.builder()
                .tag("Category")
                .summary(summary)
                .description("관리자가 일반 또는 이벤트 카테고리를 생성한다.")
                .requestSchema(Schema.schema("CategorySaveRequest"))
                .responseSchema(Schema.schema("ErrorResponse"))
                .responseFields(RestDocsFields.errorResponse())
                .build();
    }

    private ResourceSnippetParameters updateErrorResponse() {
        return ResourceSnippetParameters.builder()
                .tag("Category")
                .summary("카테고리 수정")
                .description("관리자가 표시 이름 또는 표시 순서를 수정한다.")
                .pathParameters(parameterWithName("categoryId")
                        .type(INTEGER)
                        .description("카테고리 ID"))
                .requestSchema(Schema.schema("CategoryUpdateRequest"))
                .responseSchema(Schema.schema("ErrorResponse"))
                .responseFields(RestDocsFields.errorResponse())
                .build();
    }

    private ResourceSnippetParameters deleteErrorResponse() {
        return ResourceSnippetParameters.builder()
                .tag("Category")
                .summary("카테고리 삭제")
                .description("관리자가 카테고리를 비활성화해 목록과 피드 선택 대상에서 제외한다.")
                .pathParameters(parameterWithName("categoryId")
                        .type(INTEGER)
                        .description("카테고리 ID"))
                .responseSchema(Schema.schema("ErrorResponse"))
                .responseFields(RestDocsFields.errorResponse())
                .build();
    }

    private List<FieldDescriptor> successResponseFields() {
        return List.of(
                fieldWithPath("status").type(STRING).description("응답 상태"),
                fieldWithPath("data").type(OBJECT).description("카테고리"),
                fieldWithPath("data.categoryId").type(NUMBER).description("카테고리 ID"),
                fieldWithPath("data.slug").type(STRING).description("카테고리 slug"),
                fieldWithPath("data.displayName").type(STRING).description("표시 이름"),
                fieldWithPath("data.type").type(STRING).description("GENERAL 또는 EVENT"),
                fieldWithPath("data.displayOrder").type(NUMBER).description("표시 순서"),
                fieldWithPath("data.active").type(BOOLEAN).description("활성 여부")
        );
    }
}
