package com.shoutoutz.api.project.application;

import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_APPROVAL_ADMIN_FORBIDDEN;
import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_APPROVAL_NOT_PENDING;
import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_NOT_FOUND;
import static com.shoutoutz.api.user.domain.account.UserErrorCode.USER_NOT_FOUND;

import com.shoutoutz.api.common.exception.custom.ConflictException;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.common.response.SliceMetaResponse;
import com.shoutoutz.api.media.application.MediaUrlResolver;
import com.shoutoutz.api.media.infrastructure.s3.MediaVariant;
import com.shoutoutz.api.project.application.dto.AdminProjectItem;
import com.shoutoutz.api.project.application.dto.AdminProjectPage;
import com.shoutoutz.api.project.domain.ApprovalStatus;
import com.shoutoutz.api.project.domain.DescriptionMediaReferences;
import com.shoutoutz.api.project.domain.Project;
import com.shoutoutz.api.project.domain.ProjectApprovalHistory;
import com.shoutoutz.api.project.domain.ProjectApprovalHistoryRepository;
import com.shoutoutz.api.project.domain.ProjectDetail;
import com.shoutoutz.api.project.domain.ProjectMemberProfile;
import com.shoutoutz.api.project.domain.ProjectRepository;
import com.shoutoutz.api.project.domain.ProjectTechTag;
import com.shoutoutz.api.project.infrastructure.jdbc.ProjectTechTagAndMemberJdbcRepository;
import com.shoutoutz.api.project.presentation.dto.request.AdminProjectFindAllRequest;
import com.shoutoutz.api.project.presentation.dto.response.AdminProjectApproveResponse;
import com.shoutoutz.api.project.presentation.dto.response.AdminProjectDecisionActor;
import com.shoutoutz.api.project.presentation.dto.response.AdminProjectDetailResponse;
import com.shoutoutz.api.project.presentation.dto.response.AdminProjectFindAllResponse;
import com.shoutoutz.api.project.presentation.dto.response.AdminProjectHistoryResponse;
import com.shoutoutz.api.project.presentation.dto.response.AdminProjectRejectResponse;
import com.shoutoutz.api.project.presentation.dto.response.ProjectDetailResponse;
import com.shoutoutz.api.user.domain.account.User;
import com.shoutoutz.api.user.domain.account.UserRepository;
import com.shoutoutz.api.user.domain.account.UserRole;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminProjectService {

    private final AdminProjectQueryRepository queryRepository;
    private final AdminProjectCursorCodec cursorCodec;
    private final ProjectRepository projectRepository;
    private final ProjectApprovalHistoryRepository historyRepository;
    private final ProjectTechTagAndMemberJdbcRepository techTagAndMemberJdbcRepository;
    private final UserRepository userRepository;
    private final MediaUrlResolver mediaUrlResolver;
    private final Clock clock;

    @Transactional(readOnly = true)
    public AdminProjectFindAllResponse findAll(
            UserRole role,
            AdminProjectFindAllRequest request
    ) {
        validateAdmin(role);
        AdminProjectPage page = queryRepository.findAll(
                request.resolvedStatus(),
                cursorCodec.decode(request.cursor()),
                request.resolvedSize()
        );
        List<AdminProjectItem> items = withTechTagsAndMembers(page.items());
        String nextCursor = page.hasNext()
                ? cursorCodec.encode(items.getLast().toCursor())
                : null;
        return AdminProjectFindAllResponse.from(
                items,
                new SliceMetaResponse(nextCursor, page.hasNext(), page.totalCount()),
                resolveListMediaUrls(items)
        );
    }

    @Transactional(readOnly = true)
    public AdminProjectDetailResponse findDetail(long projectId, UserRole role) {
        validateAdmin(role);
        ProjectDetail detail = projectRepository.findDetailById(projectId, null)
                .orElseThrow(() -> new EntityNotFoundException(PROJECT_NOT_FOUND));
        Map<Long, URI> mediaUrls = resolveDetailMediaUrls(detail);
        String descriptionMd = mediaUrlResolver.replaceDescriptionReferences(
                detail.descriptionMd(),
                mediaUrls
        );
        if (descriptionMd == null && detail.descriptionMd() != null) {
            descriptionMd = detail.descriptionMd();
        }
        ProjectDetailResponse projectDetail = ProjectDetailResponse.from(
                detail,
                mediaUrls,
                descriptionMd,
                null
        );
        return AdminProjectDetailResponse.from(projectDetail, detail.id(), detail.registeredBy());
    }

    @Transactional
    public AdminProjectApproveResponse approve(
            long projectId,
            long adminUserId,
            UserRole role
    ) {
        validateAdmin(role);
        Project project = findPendingProject(projectId);
        User admin = findUser(adminUserId);
        Instant now = clock.instant();
        transition(projectId, project, ApprovalStatus.APPROVED);
        historyRepository.save(ProjectApprovalHistory.decision(
                projectId,
                adminUserId,
                ApprovalStatus.APPROVED,
                null,
                now
        ));
        return new AdminProjectApproveResponse(
                projectId,
                ApprovalStatus.APPROVED,
                actorOf(admin),
                now
        );
    }

    @Transactional
    public AdminProjectRejectResponse reject(
            long projectId,
            long adminUserId,
            UserRole role,
            String reason
    ) {
        validateAdmin(role);
        Project project = findPendingProject(projectId);
        User admin = findUser(adminUserId);
        Instant now = clock.instant();
        String normalizedReason = reason == null ? null : reason.trim();
        transition(projectId, project, ApprovalStatus.REJECTED);
        historyRepository.save(ProjectApprovalHistory.decision(
                projectId,
                adminUserId,
                ApprovalStatus.REJECTED,
                normalizedReason,
                now
        ));
        return new AdminProjectRejectResponse(
                projectId,
                ApprovalStatus.REJECTED,
                normalizedReason,
                actorOf(admin),
                now
        );
    }

    @Transactional(readOnly = true)
    public AdminProjectHistoryResponse findHistory(long projectId, UserRole role) {
        validateAdmin(role);
        projectRepository.findActiveById(projectId)
                .orElseThrow(() -> new EntityNotFoundException(PROJECT_NOT_FOUND));
        return new AdminProjectHistoryResponse(
                projectId,
                historyRepository.findAllByProjectId(projectId).stream()
                        .map(this::toHistoryItem)
                        .toList()
        );
    }

    private Project findPendingProject(long projectId) {
        Project project = projectRepository.findActiveById(projectId)
                .orElseThrow(() -> new EntityNotFoundException(PROJECT_NOT_FOUND));
        if (project.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new ConflictException(PROJECT_APPROVAL_NOT_PENDING);
        }
        return project;
    }

    private void transition(long projectId, Project project, ApprovalStatus toStatus) {
        boolean transitioned = projectRepository.transitionApprovalStatus(
                projectId,
                project.getApprovalStatus(),
                toStatus
        );
        if (!transitioned) {
            throw new ConflictException(PROJECT_APPROVAL_NOT_PENDING);
        }
    }

    private AdminProjectHistoryResponse.Item toHistoryItem(ProjectApprovalHistory history) {
        return new AdminProjectHistoryResponse.Item(
                history.getId(),
                history.getFromStatus(),
                history.getToStatus(),
                actorOf(history.getChangedBy()),
                history.getReason(),
                history.getChangedAt()
        );
    }

    private AdminProjectDecisionActor actorOf(Long userId) {
        if (userId == null) {
            return null;
        }
        return actorOf(findUser(userId));
    }

    private AdminProjectDecisionActor actorOf(User user) {
        return new AdminProjectDecisionActor(user.getId(), user.getHandle().value());
    }

    private User findUser(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND));
    }

    private List<AdminProjectItem> withTechTagsAndMembers(List<AdminProjectItem> items) {
        List<Long> projectIds = items.stream().map(AdminProjectItem::projectId).toList();
        Map<Long, List<ProjectTechTag>> techTags = techTagAndMemberJdbcRepository.findTechTags(projectIds);
        Map<Long, List<ProjectMemberProfile>> members = findMembers(items);
        return items.stream()
                .map(item -> item.withTechTagsAndMembers(
                        techTags.getOrDefault(item.projectId(), List.of()),
                        members.getOrDefault(item.projectId(), List.of())
                ))
                .toList();
    }

    private Map<Long, List<ProjectMemberProfile>> findMembers(List<AdminProjectItem> items) {
        Map<Long, List<ProjectMemberProfile>> members = new HashMap<>();
        List<Long> activeProjectIds = items.stream()
                .filter(item -> item.registeredBy() != null)
                .map(AdminProjectItem::projectId)
                .toList();
        members.putAll(techTagAndMemberJdbcRepository.findMembers(activeProjectIds));

        Map<Long, Integer> archivedProjectCohorts = new HashMap<>();
        items.stream()
                .filter(item -> item.registeredBy() == null)
                .forEach(item -> archivedProjectCohorts.put(item.projectId(), item.cohort()));
        members.putAll(techTagAndMemberJdbcRepository.findArchivedMembers(archivedProjectCohorts));
        return members;
    }

    private Map<Long, URI> resolveListMediaUrls(List<AdminProjectItem> items) {
        Set<Long> thumbnailIds = new HashSet<>();
        Set<Long> avatarIds = new HashSet<>();
        items.forEach(item -> {
            if (item.thumbnailImageId() != null) {
                thumbnailIds.add(item.thumbnailImageId());
            }
            item.members().stream()
                    .map(ProjectMemberProfile::avatarImageId)
                    .filter(Objects::nonNull)
                    .forEach(avatarIds::add);
        });
        Map<Long, URI> mediaUrls = new HashMap<>();
        if (!thumbnailIds.isEmpty()) {
            mediaUrls.putAll(mediaUrlResolver.resolveAll(thumbnailIds, MediaVariant.THUMBNAIL));
        }
        if (!avatarIds.isEmpty()) {
            mediaUrls.putAll(mediaUrlResolver.resolveAll(avatarIds, MediaVariant.DISPLAY));
        }
        return Map.copyOf(mediaUrls);
    }

    private Map<Long, URI> resolveDetailMediaUrls(ProjectDetail detail) {
        Set<Long> mediaIds = new HashSet<>();
        if (detail.thumbnailMediaId() != null) {
            mediaIds.add(detail.thumbnailMediaId());
        }
        detail.members().stream()
                .map(ProjectMemberProfile::avatarImageId)
                .filter(Objects::nonNull)
                .forEach(mediaIds::add);
        mediaIds.addAll(DescriptionMediaReferences.extractMediaIds(detail.descriptionMd()));
        return mediaUrlResolver.resolveAll(mediaIds);
    }

    private void validateAdmin(UserRole role) {
        if (role != UserRole.ADMIN) {
            throw new ForbiddenException(PROJECT_APPROVAL_ADMIN_FORBIDDEN);
        }
    }
}
