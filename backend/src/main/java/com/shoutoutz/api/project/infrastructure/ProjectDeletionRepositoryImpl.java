package com.shoutoutz.api.project.infrastructure;

import com.shoutoutz.api.project.domain.ProjectDeletion;
import com.shoutoutz.api.project.domain.ProjectDeletionRepository;
import com.shoutoutz.api.project.infrastructure.jpa.ProjectDeletionJpaRepository;
import com.shoutoutz.api.project.infrastructure.mapper.ProjectDeletionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProjectDeletionRepositoryImpl implements ProjectDeletionRepository {

    private final ProjectDeletionJpaRepository projectDeletionJpaRepository;

    @Override
    public ProjectDeletion save(ProjectDeletion deletion) {
        ProjectDeletionEntity savedDeletion = projectDeletionJpaRepository.save(
                ProjectDeletionMapper.toEntity(deletion)
        );
        return ProjectDeletionMapper.toDomain(savedDeletion);
    }
}
