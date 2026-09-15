package com.shoutoutz.api.category.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.category.domain.Category;
import com.shoutoutz.api.category.domain.CategoryErrorCode;
import com.shoutoutz.api.category.domain.CategoryRepository;
import com.shoutoutz.api.category.domain.CategoryType;
import com.shoutoutz.api.category.presentation.dto.request.CategorySaveRequest;
import com.shoutoutz.api.category.presentation.dto.request.CategoryUpdateRequest;
import com.shoutoutz.api.category.presentation.dto.response.CategoryFindAllResponse;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.common.exception.custom.NotFoundException;
import com.shoutoutz.api.user.domain.account.UserRole;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category category;

    @BeforeEach
    void setUp() {
        category = Category.reconstitute(
                1L,
                "backend",
                "백엔드",
                CategoryType.GENERAL,
                1,
                true
        );
    }

    @Test
    void 활성_카테고리_목록을_반환한다() {
        when(categoryRepository.findAllActive()).thenReturn(List.of(category));

        List<CategoryFindAllResponse> response = categoryService.findAllCategories();

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().type()).isEqualTo(CategoryType.GENERAL);
    }

    @Test
    void 관리자가_카테고리를_생성한다() {
        CategorySaveRequest request = new CategorySaveRequest(
                "frontend",
                "프론트엔드",
                CategoryType.GENERAL,
                2
        );
        Category savedCategory = Category.reconstitute(
                2L,
                "frontend",
                "프론트엔드",
                CategoryType.GENERAL,
                2,
                true
        );
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        var response = categoryService.saveCategory(UserRole.ADMIN, request);

        assertThat(response.categoryId()).isEqualTo(2L);
        assertThat(response.active()).isTrue();
    }

    @Test
    void 일반_사용자는_카테고리를_변경할_수_없다() {
        CategorySaveRequest request = new CategorySaveRequest(
                "frontend",
                "프론트엔드",
                CategoryType.GENERAL,
                2
        );

        assertThatThrownBy(() -> categoryService.saveCategory(UserRole.USER, request))
                .isInstanceOf(ForbiddenException.class)
                .satisfies(exception -> assertThat(((ForbiddenException) exception)
                        .getErrorCode()).isEqualTo(CategoryErrorCode.CATEGORY_ADMIN_FORBIDDEN));
        verifyNoInteractions(categoryRepository);
    }

    @Test
    void 관리자가_카테고리_이름과_순서를_수정한다() {
        CategoryUpdateRequest request = new CategoryUpdateRequest("서버", 2);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.update(any(Category.class)))
                .thenReturn(category.update("서버", 2));

        var response = categoryService.updateCategory(1L, UserRole.ADMIN, request);

        assertThat(response.displayName()).isEqualTo("서버");
        assertThat(response.displayOrder()).isEqualTo(2);
    }

    @Test
    void 관리자가_카테고리를_비활성화한다() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        categoryService.deleteCategory(1L, UserRole.ADMIN);

        verify(categoryRepository).update(argThat(saved -> !saved.isActive()));
    }

    @Test
    void 없는_카테고리는_수정할_수_없다() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.updateCategory(
                1L,
                UserRole.ADMIN,
                new CategoryUpdateRequest("서버", 2)
        )).isInstanceOf(NotFoundException.class);
    }
}
