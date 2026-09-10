package com.shoutoutz.api.project.infrastructure;

import com.shoutoutz.api.project.domain.Project;
import com.shoutoutz.api.project.domain.ProjectRepository;
import com.shoutoutz.api.project.domain.Slug;
import com.shoutoutz.api.project.infrastructure.jpa.ProjectJpaRepository;
import com.shoutoutz.api.project.infrastructure.jpa.ProjectMemberJpaRepository;
import com.shoutoutz.api.project.infrastructure.jpa.ProjectTagJpaRepository;
import com.shoutoutz.api.project.infrastructure.mapper.ProjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProjectRepositoryImpl implements ProjectRepository {

    private final ProjectJpaRepository projectJpaRepository;
    private final ProjectTagJpaRepository projectTagJpaRepository;
    private final ProjectMemberJpaRepository projectMemberJpaRepository;

    @Override
    public Project save(Project project, List<Long> techTagIds, List<Long> memberIds) {
        ProjectEntity savedProject = projectJpaRepository.save(ProjectMapper.toEntity(project));
        Long projectId = savedProject.getId();

        projectTagJpaRepository.saveAll(ProjectMapper.toProjectTagEntities(projectId, techTagIds));
        projectMemberJpaRepository.saveAll(ProjectMapper.toProjectMemberEntities(projectId, memberIds));

        return ProjectMapper.toDomain(savedProject);
    }

    @Override
    public boolean existsBySlug(Slug slug) {
        return projectJpaRepository.existsBySlug(slug.value());
    }
}
