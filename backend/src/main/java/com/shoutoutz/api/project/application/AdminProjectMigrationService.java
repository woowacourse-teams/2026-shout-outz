package com.shoutoutz.api.project.application;

import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_MIGRATION_ADMIN_FORBIDDEN;
import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_NOT_FOUND;

import com.shoutoutz.api.cohort.domain.Cohort;
import com.shoutoutz.api.common.exception.code.CommonErrorCode;
import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.common.exception.custom.ValidationFailedException;
import com.shoutoutz.api.common.response.ErrorResponse;
import com.shoutoutz.api.project.domain.ApprovalStatus;
import com.shoutoutz.api.project.domain.ServiceStatus;
import com.shoutoutz.api.project.domain.Slug;
import com.shoutoutz.api.project.infrastructure.AdminProjectMigrationRepository;
import com.shoutoutz.api.project.presentation.dto.response.AdminProjectMigrationUpdateResponse;
import com.shoutoutz.api.user.domain.account.UserRole;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

/**
 * 이관 프로젝트의 DB 값을 한 요청으로 보정하는 임시 서비스다.
 * 일반 프로젝트 수정의 작성자, 미디어 소유자, 승인 상태 전이 정책을 적용하지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminProjectMigrationService {

    private static final Map<String, Column> COLUMNS = Map.ofEntries(
            Map.entry("cohort", new Column("cohort", Kind.COHORT, false)),
            Map.entry("teamName", new Column("team_name", Kind.TEXT, false)),
            Map.entry("slug", new Column("slug", Kind.SLUG, false)),
            Map.entry("title", new Column("title", Kind.TEXT, false)),
            Map.entry("tagline", new Column("tagline", Kind.TEXT, false)),
            Map.entry("starCount", new Column("star_count", Kind.INTEGER, true)),
            Map.entry("starSyncedAt", new Column("star_synced_at", Kind.INSTANT, true)),
            Map.entry("viewCount", new Column("view_count", Kind.INTEGER, false)),
            Map.entry("serviceStatus", new Column("service_status", Kind.SERVICE_STATUS, false)),
            Map.entry("approvalStatus", new Column("approval_status", Kind.APPROVAL_STATUS, false)),
            Map.entry("descriptionMd", new Column("description_md", Kind.TEXT, true)),
            Map.entry("githubRepositoryUrl", new Column("github_repository_url", Kind.TEXT, false)),
            Map.entry("deploymentUrl", new Column("deployment_url", Kind.TEXT, true)),
            Map.entry("thumbnailImageId", new Column("thumbnail_media_id", Kind.LONG, true)),
            Map.entry("createdAt", new Column("created_at", Kind.INSTANT, false)),
            Map.entry("updatedAt", new Column("updated_at", Kind.INSTANT, false))
    );

    private final AdminProjectMigrationRepository repository;

    @Transactional
    public AdminProjectMigrationUpdateResponse update(
            long projectId,
            long adminUserId,
            UserRole role,
            JsonNode fields
    ) {
        if (role != UserRole.ADMIN) {
            throw new ForbiddenException(PROJECT_MIGRATION_ADMIN_FORBIDDEN);
        }
        if (fields == null || !fields.isObject() || fields.size() == 0) {
            throw invalid("body", "수정할 필드를 하나 이상 보내야 합니다.");
        }

        Map<String, Object> columnValues = new LinkedHashMap<>();
        List<String> updatedFields = new ArrayList<>();
        List<Long> techTagIds = null;
        for (var entry : fields.properties()) {
            String field = entry.getKey();
            if (field.equals("techTagIds")) {
                techTagIds = parseTechTagIds(entry.getValue());
            } else {
                Column column = COLUMNS.get(field);
                if (column == null) {
                    throw invalid(field, "수정할 수 없는 필드입니다.");
                }
                columnValues.put(column.name(), parseValue(field, entry.getValue(), column));
            }
            updatedFields.add(field);
        }

        try {
            Instant updatedAt = repository.update(projectId, columnValues)
                    .orElseThrow(() -> new EntityNotFoundException(PROJECT_NOT_FOUND));
            if (techTagIds != null) {
                repository.replaceTechTags(projectId, techTagIds);
            }
            log.info("Admin corrected migrated project: adminUserId={}, projectId={}, fields={}",
                    adminUserId, projectId, updatedFields);
            return new AdminProjectMigrationUpdateResponse(projectId, updatedFields, updatedAt);
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException(CommonErrorCode.VALIDATION_FAILED, e);
        }
    }

    private static List<Long> parseTechTagIds(JsonNode node) {
        if (!node.isArray()) {
            throw invalid("techTagIds", "기술 태그 ID 배열을 보내야 합니다.");
        }
        if (node.size() > Short.MAX_VALUE + 1) {
            throw invalid("techTagIds", "기술 태그가 너무 많습니다.");
        }
        List<Long> ids = new ArrayList<>();
        Set<Long> distinct = new HashSet<>();
        for (JsonNode item : node) {
            if (!item.isIntegralNumber() || !item.canConvertToLong() || item.longValue() <= 0) {
                throw invalid("techTagIds", "기술 태그 ID는 양의 정수여야 합니다.");
            }
            long id = item.longValue();
            if (!distinct.add(id)) {
                throw invalid("techTagIds", "기술 태그 ID가 중복됐습니다.");
            }
            ids.add(id);
        }
        return ids;
    }

    private static Object parseValue(String field, JsonNode node, Column column) {
        if (node.isNull()) {
            if (!column.nullable()) {
                throw invalid(field, "null로 바꿀 수 없는 필드입니다.");
            }
            return null;
        }
        return switch (column.kind()) {
            case TEXT -> textValue(field, node);
            case SLUG -> new Slug(textValue(field, node)).value();
            case INTEGER -> integerValue(field, node);
            case LONG -> longValue(field, node);
            case COHORT -> Cohort.from(integerValue(field, node)).getValue();
            case INSTANT -> instantValue(field, node);
            case SERVICE_STATUS -> enumValue(field, node, ServiceStatus.class);
            case APPROVAL_STATUS -> enumValue(field, node, ApprovalStatus.class);
        };
    }

    private static String textValue(String field, JsonNode node) {
        if (!node.isTextual()) {
            throw invalid(field, "문자열을 보내야 합니다.");
        }
        return node.textValue();
    }

    private static int integerValue(String field, JsonNode node) {
        if (!node.isIntegralNumber() || !node.canConvertToInt()) {
            throw invalid(field, "정수를 보내야 합니다.");
        }
        return node.intValue();
    }

    private static long longValue(String field, JsonNode node) {
        if (!node.isIntegralNumber() || !node.canConvertToLong()) {
            throw invalid(field, "정수를 보내야 합니다.");
        }
        return node.longValue();
    }

    private static OffsetDateTime instantValue(String field, JsonNode node) {
        String value = textValue(field, node);
        try {
            return OffsetDateTime.parse(value);
        } catch (DateTimeParseException timestampError) {
            try {
                return LocalDate.parse(value).atStartOfDay().atOffset(ZoneOffset.UTC);
            } catch (DateTimeParseException dateError) {
                throw invalid(field, "ISO 날짜 또는 날짜·시각을 보내야 합니다.");
            }
        }
    }

    private static <E extends Enum<E>> String enumValue(String field, JsonNode node, Class<E> enumType) {
        String value = textValue(field, node);
        try {
            return Enum.valueOf(enumType, value).name();
        } catch (IllegalArgumentException e) {
            throw invalid(field, "지원하지 않는 값입니다.");
        }
    }

    private static ValidationFailedException invalid(String field, String message) {
        return new ValidationFailedException(List.of(new ErrorResponse.ErrorDetail(field, message)));
    }

    private record Column(String name, Kind kind, boolean nullable) {
    }

    private enum Kind {
        TEXT, SLUG, INTEGER, LONG, COHORT, INSTANT, SERVICE_STATUS, APPROVAL_STATUS
    }
}
