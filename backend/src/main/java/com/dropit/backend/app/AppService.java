package com.dropit.backend.app;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.dropit.backend.common.api.ApiException;
import com.dropit.backend.common.paging.OffsetLimitPageable;
import com.dropit.backend.crew.CrewService;
import com.dropit.backend.maker.MakerEntity;
import com.dropit.backend.maker.MakerService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppService {

    private final AppRepository appRepository;
    private final MakerService makerService;
    private final CrewService crewService;

    public AppService(AppRepository appRepository, MakerService makerService, CrewService crewService) {
        this.appRepository = appRepository;
        this.makerService = makerService;
        this.crewService = crewService;
    }

    @Transactional(readOnly = true)
    public AppListResponse list(String categoryValue, String query, String sort, UUID makerId, int limit, long offset) {
        if (limit < 1 || limit > 100) {
            throw ApiException.badRequest("limit은 1~100 사이여야 합니다.");
        }
        if (offset < 0) {
            throw ApiException.badRequest("offset은 0 이상이어야 합니다.");
        }

        String category = normalizeCategory(categoryValue);
        String normalizedQuery = normalizeQuery(query);
        String normalizedSort = sort == null || sort.isBlank() ? "popular" : sort.trim().toLowerCase();
        OffsetLimitPageable pageable = new OffsetLimitPageable(limit, offset);

        Page<AppEntity> page = switch (normalizedSort) {
            case "popular" -> appRepository.findPopular(category, normalizedQuery, makerId, pageable);
            case "latest" -> appRepository.findLatest(category, normalizedQuery, makerId, pageable);
            default -> throw ApiException.badRequest("sort는 popular 또는 latest여야 합니다.");
        };

        return new AppListResponse(page.getContent().stream().map(AppResponse::from).toList(), page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public AppResponse get(String appId, UUID currentUserId) {
        AppEntity app = appRepository.findById(appId).orElseThrow(() -> ApiException.notFound("서비스를 찾을 수 없습니다."));
        if (app.getDeletedAt() != null && !app.getOwnerId().equals(currentUserId)) {
            throw ApiException.notFound("서비스를 찾을 수 없습니다.");
        }
        return AppResponse.from(app);
    }

    @Transactional(readOnly = true)
    public AppTrashResponse trash(UUID ownerId) {
        Page<AppEntity> page = appRepository.findByOwnerIdAndDeletedAtIsNotNullOrderByDeletedAtDesc(
            ownerId,
            new OffsetLimitPageable(100, 0)
        );
        return new AppTrashResponse(page.getContent().stream().map(AppResponse::from).toList());
    }

    @Transactional
    public AppResponse create(UUID ownerId, AppRequest request) {
        if (!crewService.isCrewMember(ownerId)) {
            throw ApiException.forbidden("크루 인증을 완료한 사용자만 서비스를 등록할 수 있습니다.");
        }
        MakerEntity maker = makerService.getEntity(ownerId);
        List<String> categories = validateCategories(request.categories());
        List<String> techTags = normalizeTags(request.techTags());
        AppEntity app = new AppEntity(
            UUID.randomUUID().toString(),
            ownerId,
            requiredTrimmed(request.name()),
            requiredTrimmed(request.tagline()),
            request.description() == null ? "" : request.description().trim(),
            categories,
            request.thumbnailVariant().value(),
            normalizeNullable(request.thumbnailUrl()),
            requiredTrimmed(request.appUrl()),
            normalizeNullable(request.githubUrl()),
            makerSnapshot(maker),
            techTags
        );
        return AppResponse.from(appRepository.save(app));
    }

    @Transactional
    public AppResponse update(UUID ownerId, String appId, AppPatchRequest request) {
        AppEntity app = requireOwnedActiveApp(ownerId, appId);
        MakerEntity maker = makerService.getEntity(ownerId);

        List<String> categories = request.categories() == null
            ? app.getCategories()
            : validateCategories(request.categories());
        List<String> techTags = request.techTags() == null
            ? app.getTechTags()
            : normalizeTags(request.techTags());

        app.update(
            request.name() == null ? app.getName() : requiredTrimmed(request.name()),
            request.tagline() == null ? app.getTagline() : requiredTrimmed(request.tagline()),
            request.description() == null ? app.getDescription() : request.description().trim(),
            categories,
            request.thumbnailVariant() == null ? app.getThumbnailVariant() : request.thumbnailVariant().value(),
            request.thumbnailUrl() == null ? app.getThumbnailUrl() : normalizeNullable(request.thumbnailUrl()),
            request.appUrl() == null ? app.getAppUrl() : requiredTrimmed(request.appUrl()),
            request.githubUrl() == null ? app.getGithubUrl() : normalizeNullable(request.githubUrl()),
            makerSnapshot(maker),
            techTags
        );
        return AppResponse.from(appRepository.save(app));
    }

    @Transactional
    public void delete(UUID ownerId, String appId) {
        AppEntity app = requireOwnedActiveApp(ownerId, appId);
        app.softDelete();
        appRepository.save(app);
    }

    @Transactional
    public AppResponse restore(UUID ownerId, String appId) {
        AppEntity app = appRepository.findById(appId).orElseThrow(() -> ApiException.notFound("서비스를 찾을 수 없습니다."));
        requireOwner(ownerId, app);
        if (app.getDeletedAt() == null) {
            throw ApiException.notFound("삭제된 서비스만 복구할 수 있습니다.");
        }
        app.restore();
        return AppResponse.from(appRepository.save(app));
    }

    @Transactional
    public PlayResponse play(String appId) {
        if (appRepository.incrementPlays(appId) == 0) {
            throw ApiException.notFound("서비스를 찾을 수 없습니다.");
        }
        return new PlayResponse(appRepository.findById(appId).orElseThrow().getPlays());
    }

    private AppEntity requireOwnedActiveApp(UUID ownerId, String appId) {
        AppEntity app = appRepository.findById(appId).orElseThrow(() -> ApiException.notFound("서비스를 찾을 수 없습니다."));
        requireOwner(ownerId, app);
        if (app.getDeletedAt() != null) {
            throw ApiException.notFound("서비스를 찾을 수 없습니다.");
        }
        return app;
    }

    private void requireOwner(UUID ownerId, AppEntity app) {
        if (!app.getOwnerId().equals(ownerId)) {
            throw ApiException.forbidden("서비스 소유자만 이 작업을 수행할 수 있습니다.");
        }
    }

    private String normalizeCategory(String categoryValue) {
        if (categoryValue == null || categoryValue.isBlank() || "전체".equals(categoryValue.trim())) {
            return null;
        }
        return AppCategory.fromValue(categoryValue.trim()).value();
    }

    private String normalizeQuery(String query) {
        return query == null || query.isBlank() ? null : query.trim();
    }

    private List<String> validateCategories(List<AppCategory> categories) {
        if (categories == null || categories.isEmpty() || categories.size() > 2) {
            throw ApiException.badRequest("카테고리는 1~2개여야 합니다.");
        }
        List<String> values = categories.stream().map(AppCategory::value).distinct().toList();
        if (values.size() != categories.size()) {
            throw ApiException.badRequest("카테고리는 중복해서 선택할 수 없습니다.");
        }
        return values;
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null) {
            return List.of();
        }
        List<String> normalized = tags.stream()
            .map(tag -> tag == null ? "" : tag.trim())
            .filter(tag -> !tag.isBlank())
            .distinct()
            .limit(5)
            .toList();
        return new ArrayList<>(normalized);
    }

    private String requiredTrimmed(String value) {
        if (value == null || value.isBlank()) {
            throw ApiException.badRequest("필수 값이 비어 있습니다.");
        }
        return value.trim();
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Map<String, Object> makerSnapshot(MakerEntity maker) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", maker.getId().toString());
        snapshot.put("name", maker.getName());
        snapshot.put("initials", maker.getInitials());
        snapshot.put("avatarUrl", maker.getAvatarUrl());
        snapshot.put("role", maker.getRole());
        snapshot.put("bio", maker.getBio());
        snapshot.put("tone", maker.getTone());
        return snapshot;
    }
}
