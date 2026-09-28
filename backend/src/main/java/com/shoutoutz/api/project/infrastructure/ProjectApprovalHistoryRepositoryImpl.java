package com.shoutoutz.api.project.infrastructure;

import com.shoutoutz.api.project.domain.ProjectApprovalHistory;
import com.shoutoutz.api.project.domain.ProjectApprovalHistoryRepository;
import com.shoutoutz.api.project.infrastructure.jpa.ProjectApprovalHistoryJpaRepository;
import com.shoutoutz.api.project.infrastructure.mapper.ProjectApprovalHistoryMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProjectApprovalHistoryRepositoryImpl implements ProjectApprovalHistoryRepository {

    private final ProjectApprovalHistoryJpaRepository historyJpaRepository;

    @Override
    public ProjectApprovalHistory save(ProjectApprovalHistory history) {
        ProjectApprovalHistoryEntity entity = ProjectApprovalHistoryMapper.toEntity(history);
        return ProjectApprovalHistoryMapper.toDomain(historyJpaRepository.save(entity));
    }

    @Override
    public List<ProjectApprovalHistory> findAllByProjectId(long projectId) {
        return historyJpaRepository.findAllByProjectIdOrderByChangedAtDescIdDesc(projectId)
                .stream()
                .map(ProjectApprovalHistoryMapper::toDomain)
                .toList();
    }
}
