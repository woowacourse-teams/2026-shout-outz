package com.shoutoutz.api.category.application;

import com.shoutoutz.api.category.domain.Category;
import com.shoutoutz.api.category.domain.CategoryErrorCode;
import com.shoutoutz.api.category.domain.CategoryRepository;
import com.shoutoutz.api.category.presentation.dto.request.CategorySaveRequest;
import com.shoutoutz.api.category.presentation.dto.request.CategoryUpdateRequest;
import com.shoutoutz.api.category.presentation.dto.response.CategoryFindAllResponse;
import com.shoutoutz.api.category.presentation.dto.response.CategoryResponse;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.common.exception.custom.NotFoundException;
import com.shoutoutz.api.user.domain.account.UserRole;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 카테고리 조회와 관리자 변경 흐름 조정.
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<CategoryFindAllResponse> findAllCategories() {
        List<Category> categories = categoryRepository.findAllActive();
        return CategoryFindAllResponse.from(categories);
    }

    @Transactional
    public CategoryResponse saveCategory(UserRole role, CategorySaveRequest request) {
        validateAdmin(role);
        Category savedCategory = categoryRepository.save(request.toCategory());
        return CategoryResponse.from(savedCategory);
    }

    @Transactional
    public CategoryResponse updateCategory(
            long categoryId,
            UserRole role,
            CategoryUpdateRequest request
    ) {
        validateAdmin(role);
        Category category = findCategory(categoryId);
        Category updatedCategory = category.update(
                request.displayName(),
                request.displayOrder()
        );
        return CategoryResponse.from(categoryRepository.update(updatedCategory));
    }

    @Transactional
    public void deleteCategory(long categoryId, UserRole role) {
        validateAdmin(role);
        Category category = findCategory(categoryId);
        categoryRepository.update(category.deactivate());
    }

    private void validateAdmin(UserRole role) {
        if (role != UserRole.ADMIN) {
            throw new ForbiddenException(CategoryErrorCode.CATEGORY_ADMIN_FORBIDDEN);
        }
    }

    private Category findCategory(long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException(CategoryErrorCode.CATEGORY_NOT_FOUND));
    }
}
