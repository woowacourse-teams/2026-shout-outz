package com.shoutoutz.api.techtag.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * tech_tags 테이블에는 생성/수정 시각 컬럼이 없어 BaseEntity 를 상속하지 않는다.
 */
@Entity
@Table(name = "tech_tags")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TechTagEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String slug;

    @Column(name = "display_name", nullable = false, unique = true, length = 50)
    private String displayName;

    @Column(name = "is_active", nullable = false)
    private boolean active;
}
