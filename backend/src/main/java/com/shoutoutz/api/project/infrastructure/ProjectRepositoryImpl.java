package com.shoutoutz.api.project.infrastructure;

import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_DUPLICATE_SLUG;

import com.shoutoutz.api.common.exception.custom.DuplicateEntityException;
import com.shoutoutz.api.project.domain.ApprovalStatus;
import com.shoutoutz.api.project.domain.Project;
import com.shoutoutz.api.project.domain.ProjectDetail;
import com.shoutoutz.api.project.domain.ProjectFilterCondition;
import com.shoutoutz.api.project.domain.ProjectFilterOptions;
import com.shoutoutz.api.project.domain.ProjectPage;
import com.shoutoutz.api.project.domain.ProjectRepository;
import com.shoutoutz.api.project.domain.ProjectSearchCondition;
import com.shoutoutz.api.project.domain.Slug;
import com.shoutoutz.api.project.infrastructure.jdbc.ProjectDetailJdbcRepository;
import com.shoutoutz.api.project.infrastructure.jdbc.ProjectListJdbcRepository;
import com.shoutoutz.api.project.infrastructure.jpa.ProjectJpaRepository;
import com.shoutoutz.api.project.infrastructure.jpa.ProjectMemberJpaRepository;
import com.shoutoutz.api.project.infrastructure.jpa.ProjectTagJpaRepository;
import com.shoutoutz.api.project.infrastructure.mapper.ProjectMapper;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProjectRepositoryImpl implements ProjectRepository {

    private static final String SLUG_UNIQUE_CONSTRAINT = "projects_slug_key";

    private final ProjectJpaRepository projectJpaRepository;
    private final ProjectTagJpaRepository projectTagJpaRepository;
    private final ProjectMemberJpaRepository projectMemberJpaRepository;
    private final ProjectDetailJdbcRepository projectDetailJdbcRepository;
    private final ProjectListJdbcRepository projectListJdbcRepository;

    @Override
    public Project save(Project project, List<Long> techTagIds, List<Long> memberIds) {
        ProjectEntity savedProject = saveProject(project);
        Long projectId = savedProject.getId();

        projectTagJpaRepository.saveAll(ProjectMapper.toProjectTagEntities(projectId, techTagIds));
        projectMemberJpaRepository.saveAll(ProjectMapper.toProjectMemberEntities(projectId, memberIds));

        return ProjectMapper.toDomain(savedProject);
    }

    @Override
    public boolean existsBySlug(Slug slug) {
        return projectJpaRepository.existsBySlug(slug.value());
    }

    @Override
    public Optional<ProjectDetail> findDetailById(long projectId, Long viewerId) {
        return projectDetailJdbcRepository.findDetailById(projectId, viewerId);
    }

    /**
     * 승인 상태가 Approval이며 삭제되지 않은 프로젝트인지 확인
     */
    @Override
    public boolean existsPublicById(long projectId) {
        return projectJpaRepository.existsByIdAndApprovalStatusAndDeletedAtIsNull(
                projectId,
                ApprovalStatus.APPROVED
        );
    }

    /**
     * 검색 조건이 동적으로 붙는 여러 테이블 조회라 JDBC 조회에 맡긴다.
     */
    @Override
    public ProjectPage findAll(ProjectSearchCondition condition) {
        return projectListJdbcRepository.findAll(condition);
    }

    /**
     * 목록 조회와 같은 조건으로 세야 하므로, 목록 JDBC 조회에 맡긴다.
     */
    @Override
    public ProjectFilterOptions findFilterOptions(ProjectFilterCondition condition) {
        return projectListJdbcRepository.findFilterOptions(condition);
    }

    /**
     * 동시 요청이 사전 검사(existsBySlug)를 함께 통과하면 slug UNIQUE 제약에 걸린다.
     * 이 경우에도 409 로 응답하도록 slug 중복 예외로 변환한다.
     */
    private ProjectEntity saveProject(Project project) {
        try {
            return projectJpaRepository.save(ProjectMapper.toEntity(project));
        } catch (DataIntegrityViolationException e) {
            if (isSlugUniqueViolation(e)) {
                throw new DuplicateEntityException(PROJECT_DUPLICATE_SLUG, e);
            }
            throw e;
        }
    }

    private boolean isSlugUniqueViolation(DataIntegrityViolationException e) {
        return e.getCause() instanceof ConstraintViolationException cause
                && SLUG_UNIQUE_CONSTRAINT.equals(cause.getConstraintName());
    }
}
