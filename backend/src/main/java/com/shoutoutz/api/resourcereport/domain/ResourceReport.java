package com.shoutoutz.api.resourcereport.domain;

import com.shoutoutz.api.common.exception.code.CommonErrorCode;
import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.DomainValidationException;
import java.time.Instant;
import java.util.Set;
import lombok.Getter;

/**
 * 프로젝트 기술 이야기 자료 제보
 * 필요 없어요(REMOVE)는 제보한 자료를 (source, external_key) 로, 추가해 주세요(ADD)는 알려 준 GitHub 주소로 가리킨다.
 * 운영자가 처리하기 전까지는 OPEN 이다.
 */
@Getter
public class ResourceReport {

    /**
     * 필요 없다고 제보할 수 있는 자료 원천. 팀이 남긴 PR과 팀 문서만 받고, 블로그와 테코톡은 받지 않는다(프로토타입과 같음).
     */
    private static final Set<String> REMOVABLE_SOURCES = Set.of(
            "github-pr",
            "github-teamdoc",
            "github-wiki",
            "github-discussion"
    );
    private static final int MEMO_MAX_LENGTH = 500;

    private final Long id;
    private final long projectId;
    private final long userId;
    private final ResourceReportKind kind;
    private final String resourceSource;
    private final String resourceExternalKey;
    private final String url;
    private final String memo;
    private final ResourceReportStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Instant handledAt;
    private final Long handledByUserId;

    private ResourceReport(
            Long id,
            long projectId,
            long userId,
            ResourceReportKind kind,
            String resourceSource,
            String resourceExternalKey,
            String url,
            String memo,
            ResourceReportStatus status,
            Instant createdAt,
            Instant updatedAt,
            Instant handledAt,
            Long handledByUserId
    ) {
        this.id = id;
        this.projectId = projectId;
        this.userId = userId;
        this.kind = kind;
        this.resourceSource = resourceSource;
        this.resourceExternalKey = resourceExternalKey;
        this.url = url;
        this.memo = memo;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.handledAt = handledAt;
        this.handledByUserId = handledByUserId;
    }

    public static boolean isRemovableSource(String source) {
        return source != null && REMOVABLE_SOURCES.contains(source);
    }

    /**
     * 필요 없는 자료 제보를 만든다. 여러 자료를 함께 고르면 같은 이유가 모든 제보에 붙는다.
     */
    public static ResourceReport removal(
            long projectId,
            long userId,
            String resourceSource,
            String resourceExternalKey,
            String memo,
            Instant createdAt
    ) {
        if (!isRemovableSource(resourceSource)) {
            throw new BadRequestException(ResourceReportErrorCode.RESOURCE_REPORT_RESOURCE_NOT_REMOVABLE);
        }
        if (resourceExternalKey == null || resourceExternalKey.isBlank()) {
            throw new DomainValidationException(CommonErrorCode.INTERNAL_SERVER_ERROR);
        }
        validateOwner(projectId, userId, createdAt);
        return new ResourceReport(
                null,
                projectId,
                userId,
                ResourceReportKind.REMOVE,
                resourceSource,
                resourceExternalKey,
                null,
                normalizeMemo(memo),
                ResourceReportStatus.OPEN,
                createdAt,
                createdAt,
                null,
                null
        );
    }

    /**
     * 빠진 자료 제보를 만든다. 주소는 이 프로젝트 리포지토리 아래인지 ResourceReportUrl 에서 확인을 마친 값이다.
     */
    public static ResourceReport addition(
            long projectId,
            long userId,
            ResourceReportUrl url,
            String memo,
            Instant createdAt
    ) {
        if (url == null) {
            throw new BadRequestException(ResourceReportErrorCode.RESOURCE_REPORT_URL_INVALID);
        }
        validateOwner(projectId, userId, createdAt);
        return new ResourceReport(
                null,
                projectId,
                userId,
                ResourceReportKind.ADD,
                null,
                null,
                url.value(),
                normalizeMemo(memo),
                ResourceReportStatus.OPEN,
                createdAt,
                createdAt,
                null,
                null
        );
    }

    public static ResourceReport reconstitute(
            Long id,
            long projectId,
            long userId,
            ResourceReportKind kind,
            String resourceSource,
            String resourceExternalKey,
            String url,
            String memo,
            ResourceReportStatus status,
            Instant createdAt,
            Instant updatedAt,
            Instant handledAt,
            Long handledByUserId
    ) {
        return new ResourceReport(
                id,
                projectId,
                userId,
                kind,
                resourceSource,
                resourceExternalKey,
                url,
                memo,
                status,
                createdAt,
                updatedAt,
                handledAt,
                handledByUserId
        );
    }

    /**
     * 운영자가 처리 상태를 바꾼다. 같은 상태로 바꾸면 그대로 두고 처리 시각도 바꾸지 않는다(버그 제보와 같음).
     */
    public ResourceReport changeStatus(ResourceReportStatus newStatus, long adminUserId, Instant changedAt) {
        if (newStatus == null || adminUserId < 1 || changedAt == null) {
            throw new BadRequestException(ResourceReportErrorCode.RESOURCE_REPORT_STATUS_INVALID);
        }
        if (status == newStatus) {
            return this;
        }
        return new ResourceReport(
                id,
                projectId,
                userId,
                kind,
                resourceSource,
                resourceExternalKey,
                url,
                memo,
                newStatus,
                createdAt,
                changedAt,
                changedAt,
                adminUserId
        );
    }

    private static void validateOwner(long projectId, long userId, Instant createdAt) {
        if (projectId < 1 || userId < 1 || createdAt == null) {
            throw new DomainValidationException(CommonErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private static String normalizeMemo(String memo) {
        if (memo == null) {
            return null;
        }
        String trimmed = memo.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.codePointCount(0, trimmed.length()) > MEMO_MAX_LENGTH) {
            throw new BadRequestException(ResourceReportErrorCode.RESOURCE_REPORT_MEMO_INVALID);
        }
        return trimmed;
    }
}
