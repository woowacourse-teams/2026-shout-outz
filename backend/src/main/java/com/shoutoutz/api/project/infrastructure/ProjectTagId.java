package com.shoutoutz.api.project.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * project_tags 의 복합 기본 키
 * PRIMARY KEY (project_id, tech_tag_id)
 */
@Embeddable
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class ProjectTagId implements Serializable {

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "tech_tag_id", nullable = false)
    private Long techTagId;
}
