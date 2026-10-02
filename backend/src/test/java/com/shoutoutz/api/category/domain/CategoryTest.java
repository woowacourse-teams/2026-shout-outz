package com.shoutoutz.api.category.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.common.exception.custom.DomainValidationException;
import org.junit.jupiter.api.Test;

class CategoryTest {

    @Test
    void 카테고리를_복원한다() {
        Category category = Category.reconstitute(
                1L,
                "backend",
                "백엔드",
                CategoryType.GENERAL,
                1,
                true
        );

        assertThat(category.getType()).isEqualTo(CategoryType.GENERAL);
        assertThat(category.isGeneral()).isTrue();
    }

    @Test
    void 카테고리를_생성하고_비활성화한다() {
        Category category = Category.create(
                "backend",
                "백엔드",
                CategoryType.GENERAL,
                1
        );

        assertThat(category.getId()).isNull();
        assertThat(category.isActive()).isTrue();
        assertThat(category.deactivate().isActive()).isFalse();
    }

    @Test
    void 표시_이름과_순서를_수정해도_slug와_유형은_유지한다() {
        Category category = Category.reconstitute(
                1L,
                "backend",
                "백엔드",
                CategoryType.GENERAL,
                1,
                true
        );

        Category updated = category.update("서버", 2);

        assertThat(updated.getDisplayName()).isEqualTo("서버");
        assertThat(updated.getDisplayOrder()).isEqualTo(2);
        assertThat(updated.getSlug()).isEqualTo("backend");
        assertThat(updated.getType()).isEqualTo(CategoryType.GENERAL);
    }

    @Test
    void 카테고리_slug는_빈_문자열일_수_없다() {
        assertThatThrownBy(() -> Category.reconstitute(
                1L,
                " ",
                "백엔드",
                CategoryType.GENERAL,
                1,
                true
        )).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void 표시_순서는_음수일_수_없다() {
        assertThatThrownBy(() -> Category.create(
                "backend",
                "백엔드",
                CategoryType.GENERAL,
                -1
        )).isInstanceOf(DomainValidationException.class);
    }
}
