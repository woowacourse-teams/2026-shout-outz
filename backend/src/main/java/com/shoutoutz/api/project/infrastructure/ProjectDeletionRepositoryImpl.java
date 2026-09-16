package com.shoutoutz.api.project.infrastructure;

import com.shoutoutz.api.project.domain.ProjectDeletion;
import com.shoutoutz.api.project.domain.ProjectDeletionRepository;
import com.shoutoutz.api.project.infrastructure.jdbc.ProjectRestoreJdbcRepository;
import com.shoutoutz.api.project.infrastructure.jpa.ProjectDeletionJpaRepository;
import com.shoutoutz.api.project.infrastructure.mapper.ProjectDeletionMapper;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 삭제 이력(project_deletions) 저장소. 프로젝트의 현재 삭제 상태는 ProjectRepository 가 projects 에 기록한다.
 * 삭제하면 새 이력을 INSERT 하고, 복구하면 기존 이력에 복구 주체와 시각을 UPDATE 한다.
 * 복구 UPDATE 는 복구 SQL 을 한곳에 모아 둔 ProjectRestoreJdbcRepository 에 맡긴다.
 */
@Repository
@RequiredArgsConstructor
public class ProjectDeletionRepositoryImpl implements ProjectDeletionRepository {

    private final ProjectDeletionJpaRepository projectDeletionJpaRepository;
    private final ProjectRestoreJdbcRepository projectRestoreJdbcRepository;

    @Override
    public ProjectDeletion save(ProjectDeletion deletion) {
        ProjectDeletionEntity savedDeletion = projectDeletionJpaRepository.save(
                ProjectDeletionMapper.toEntity(deletion)
        );
        return ProjectDeletionMapper.toDomain(savedDeletion);
    }

    @Override
    public int markRestored(long deletionId, long restoredBy, Instant restoredAt) {
        return projectRestoreJdbcRepository.restoreDeletion(deletionId, restoredBy, restoredAt);
    }
}
