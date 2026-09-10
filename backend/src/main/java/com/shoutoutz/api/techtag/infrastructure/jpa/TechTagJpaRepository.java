package com.shoutoutz.api.techtag.infrastructure.jpa;

import com.shoutoutz.api.techtag.infrastructure.TechTagEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TechTagJpaRepository extends JpaRepository<TechTagEntity, Long> {

    @Query("""
            SELECT t FROM TechTagEntity t
            WHERE t.active = true
            ORDER BY LOWER(t.displayName)
            """)
    List<TechTagEntity> findAllActive();

    @Query("""
            SELECT t FROM TechTagEntity t
            WHERE t.active = true
              AND (LOWER(t.displayName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(t.slug) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY LOWER(t.displayName)
            """)
    List<TechTagEntity> findAllActiveByKeyword(@Param("keyword") String keyword);
}
