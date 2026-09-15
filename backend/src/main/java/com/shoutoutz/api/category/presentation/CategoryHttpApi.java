package com.shoutoutz.api.category.presentation;

import com.shoutoutz.api.auth.presentation.security.AuthenticatedUser;
import com.shoutoutz.api.auth.presentation.security.LoginUser;
import com.shoutoutz.api.category.application.CategoryService;
import com.shoutoutz.api.category.presentation.dto.request.CategorySaveRequest;
import com.shoutoutz.api.category.presentation.dto.request.CategoryUpdateRequest;
import com.shoutoutz.api.category.presentation.dto.response.CategoryFindAllResponse;
import com.shoutoutz.api.category.presentation.dto.response.CategoryResponse;
import com.shoutoutz.api.common.response.SuccessResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryHttpApi {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<SuccessResponse<List<CategoryFindAllResponse>>> findAllCategories() {
        List<CategoryFindAllResponse> response = categoryService.findAllCategories();
        return ResponseEntity.ok(SuccessResponse.success(response));
    }

    @PostMapping
    public ResponseEntity<SuccessResponse<CategoryResponse>> saveCategory(
            @LoginUser AuthenticatedUser user,
            @Valid @RequestBody CategorySaveRequest request
    ) {
        CategoryResponse response = categoryService.saveCategory(user.role(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SuccessResponse.success(response));
    }

    @PutMapping("/{categoryId}")
    public ResponseEntity<SuccessResponse<CategoryResponse>> updateCategory(
            @PathVariable long categoryId,
            @LoginUser AuthenticatedUser user,
            @Valid @RequestBody CategoryUpdateRequest request
    ) {
        CategoryResponse response = categoryService.updateCategory(
                categoryId,
                user.role(),
                request
        );
        return ResponseEntity.ok(SuccessResponse.success(response));
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable long categoryId,
            @LoginUser AuthenticatedUser user
    ) {
        categoryService.deleteCategory(categoryId, user.role());
        return ResponseEntity.noContent().build();
    }
}
