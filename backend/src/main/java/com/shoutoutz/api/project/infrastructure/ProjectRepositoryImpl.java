package com.shoutoutz.api.project.infrastructure;

import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_DUPLICATE_GITHUB_REPOSITORY;
import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_NOT_FOUND;
import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_DUPLICATE_SLUG;

import com.shoutoutz.api.common.exception.custom.DuplicateEntityException;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.project.domain.ApprovalStatus;
import com.shoutoutz.api.project.domain.GithubRepositoryUrl;
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
    private static final String GITHUB_REPOSITORY_URL_UNIQUE_CONSTRAINT = "projects_github_repository_url_key";

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
    public boolean existsByGithubRepositoryUrl(GithubRepositoryUrl githubRepositoryUrl) {
        return projectJpaRepository.existsByGithubRepositoryUrl(githubRepositoryUrl.value());
    }

    @Override
    public boolean existsByGithubRepositoryUrlExcluding(GithubRepositoryUrl githubRepositoryUrl, long projectId) {
        return projectJpaRepository.existsByGithubRepositoryUrlAndIdNot(githubRepositoryUrl.value(), projectId);
    }

    @Override
    public Optional<Project> findActiveById(long projectId) {
        return projectJpaRepository.findByIdAndDeletedAtIsNull(projectId).map(ProjectMapper::toDomain);
    }

    @Override
    public List<Long> findTechTagIds(long projectId) {
        return projectTagJpaRepository.findAllByIdProjectIdOrderByDisplayOrder(projectId).stream()
                .map(tag -> tag.getId().getTechTagId())
                .toList();
    }

    @Override
    public List<Long> findMemberIds(long projectId) {
        return projectMemberJpaRepository.findAllByIdProjectIdOrderByDisplayOrder(projectId).stream()
                .map(member -> member.getId().getUserId())
                .toList();
    }

    /**
     * 조회한 엔티티의 값만 바꿔 더티 체킹으로 반영한다.
     * 도메인으로 만든 엔티티를 save 하면 매퍼가 담지 않는 조회수와 스타 수 같은 값이 함께 덮인다.
     */
    @Override
    public Project update(Project project, List<Long> techTagIds, List<Long> memberIds) {
        ProjectEntity entity = projectJpaRepository.findByIdAndDeletedAtIsNull(project.getId())
                .orElseThrow(() -> new EntityNotFoundException(PROJECT_NOT_FOUND));
        updateProject(entity, project);
        replaceTechTags(project.getId(), techTagIds);
        replaceMembers(project.getId(), memberIds);
        return ProjectMapper.toDomain(entity);
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
     * 동시 요청이 사전 검사(existsBySlug, existsByGithubRepositoryUrl)를 함께 통과하면 UNIQUE 제약에 걸린다.
     * 이 경우에도 409 로 응답하도록, 어긴 제약에 맞는 중복 예외로 변환한다.
     */
    private ProjectEntity saveProject(Project project) {
        try {
            return projectJpaRepository.save(ProjectMapper.toEntity(project));
        } catch (DataIntegrityViolationException e) {
            if (isUniqueViolation(e, SLUG_UNIQUE_CONSTRAINT)) {
                throw new DuplicateEntityException(PROJECT_DUPLICATE_SLUG, e);
            }
            if (isUniqueViolation(e, GITHUB_REPOSITORY_URL_UNIQUE_CONSTRAINT)) {
                throw new DuplicateEntityException(PROJECT_DUPLICATE_GITHUB_REPOSITORY, e);
            }
            throw e;
        }
    }

    /**
     * 수정으로도 리포지토리 URL 이 겹칠 수 있으므로, 저장과 같은 방식으로 제약 위반을 중복 예외로 바꾼다.
     * 더티 체킹은 트랜잭션이 끝날 때 반영되어, flush 하지 않으면 제약 위반이 이 자리를 지나 500 으로 나간다.
     */
    private void updateProject(ProjectEntity entity, Project project) {
        entity.update(project);
        try {
            projectJpaRepository.flush();
        } catch (DataIntegrityViolationException e) {
            if (isUniqueViolation(e, GITHUB_REPOSITORY_URL_UNIQUE_CONSTRAINT)) {
                throw new DuplicateEntityException(PROJECT_DUPLICATE_GITHUB_REPOSITORY, e);
            }
            throw e;
        }
    }

    /**
     * 목록을 통째로 바꾸므로, 기존 항목을 모두 지우고 받은 목록을 새로 넣는다.
     * 그대로 남는 항목은 지웠다가 다시 넣는 셈이지만, 삭제와 삽입이 같은 영속성 컨텍스트를 거쳐
     * 같은 키를 다시 넣기 전에 삭제가 먼저 나가므로 제약에 걸리지 않는다.
     */
    private void replaceTechTags(Long projectId, List<Long> techTagIds) {
        projectTagJpaRepository.deleteAllByIdProjectId(projectId);
        projectTagJpaRepository.saveAll(ProjectMapper.toProjectTagEntities(projectId, techTagIds));
    }

    private void replaceMembers(Long projectId, List<Long> memberIds) {
        projectMemberJpaRepository.deleteAllByIdProjectId(projectId);
        projectMemberJpaRepository.saveAll(ProjectMapper.toProjectMemberEntities(projectId, memberIds));
    }

    private boolean isUniqueViolation(DataIntegrityViolationException e, String constraintName) {
        return e.getCause() instanceof ConstraintViolationException cause
                && constraintName.equals(cause.getConstraintName());
    }
}
