package com.shoutoutz.api.category.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.category.domain.Category;
import com.shoutoutz.api.category.domain.CategoryRepository;
import com.shoutoutz.api.category.domain.CategoryType;
import com.shoutoutz.api.common.exception.custom.DuplicateEntityException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "aws.s3.bucket=test-bucket",
        "aws.s3.region=ap-northeast-2",
        "aws.s3.presigned-url-expiration-seconds=300",
        "spring.flyway.ignore-migration-patterns=*:missing"
})
@Transactional
class CategoryRepositoryIntegrationTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 활성_카테고리를_표시_순서로_조회한다() {
        long eventCategoryId = insertCategory(CategoryType.EVENT, 2, true);
        long generalCategoryId = insertCategory(CategoryType.GENERAL, 1, true);
        insertCategory(CategoryType.EVENT, 0, false);

        List<Category> categories = categoryRepository.findAllActiveByIds(List.of(
                eventCategoryId,
                generalCategoryId
        ));

        assertThat(categoryRepository.findAllActive())
                .filteredOn(category -> category.getId() == generalCategoryId
                        || category.getId() == eventCategoryId)
                .extracting(Category::getId)
                .containsExactly(generalCategoryId, eventCategoryId);
        assertThat(categories)
                .extracting(Category::getType)
                .containsExactlyInAnyOrder(CategoryType.GENERAL, CategoryType.EVENT);
    }

    @Test
    void 카테고리를_생성하고_수정한_뒤_비활성화한다() {
        Category savedCategory = categoryRepository.save(Category.create(
                "backend-new",
                "새 백엔드",
                CategoryType.GENERAL,
                3
        ));

        Category updatedCategory = categoryRepository.update(
                savedCategory.update("서버", 1)
        );
        categoryRepository.update(updatedCategory.deactivate());

        Category foundCategory = categoryRepository.findById(savedCategory.getId()).orElseThrow();
        assertThat(foundCategory.getDisplayName()).isEqualTo("서버");
        assertThat(foundCategory.getDisplayOrder()).isEqualTo(1);
        assertThat(foundCategory.isActive()).isFalse();
        assertThat(categoryRepository.findAllActive())
                .noneMatch(category -> category.getId().equals(savedCategory.getId()));
    }

    @Test
    void 중복된_slug나_표시_이름으로_저장할_수_없다() {
        categoryRepository.save(Category.create(
                "duplicate-category",
                "중복 카테고리",
                CategoryType.GENERAL,
                1
        ));

        assertThatThrownBy(() -> categoryRepository.save(Category.create(
                "duplicate-category",
                "다른 표시 이름",
                CategoryType.EVENT,
                2
        ))).isInstanceOf(DuplicateEntityException.class);
    }

    private long insertCategory(CategoryType type, int displayOrder, boolean active) {
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return jdbcTemplate.queryForObject(
                """
                        INSERT INTO categories (
                            slug, display_name, category_type, display_order, is_active
                        ) VALUES (?, ?, ?, ?, ?)
                        RETURNING id
                        """,
                Long.class,
                "category-" + token,
                "카테고리 " + token,
                type.name(),
                displayOrder,
                active
        );
    }
}
