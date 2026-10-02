package com.shoutoutz.api.project.infrastructure;

import com.shoutoutz.api.project.domain.DeletionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 프로젝트 삭제 이력. 프로젝트가 하드 삭제된 뒤에도 남아야 하므로 projects 외래키를 두지 않는다.
 */
@Entity
@Table(name = "project_deletions")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ProjectDeletionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "project_slug", nullable = false, length = 100)
    private String projectSlug;

    @Column(name = "project_title", nullable = false, length = 100)
    private String projectTitle;

    @Column(name = "deleted_by")
    private Long deletedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "deletion_type", nullable = false, length = 30)
    private DeletionType deletionType;

    @Column(name = "deletion_reason", columnDefinition = "TEXT")
    private String deletionReason;

    @Column(name = "deleted_at", nullable = false)
    private Instant deletedAt;

    @Column(name = "restore_deadline_at")
    private Instant restoreDeadlineAt;

    @Column(name = "restored_by")
    private Long restoredBy;

    @Column(name = "restored_at")
    private Instant restoredAt;
}
