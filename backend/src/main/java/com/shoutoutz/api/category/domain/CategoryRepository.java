package com.shoutoutz.api.category.domain;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository {

    Category save(Category category);

    Category update(Category category);

    Optional<Category> findById(long categoryId);

    List<Category> findAllActive();

    List<Category> findAllActiveByIds(List<Long> categoryIds);
}
