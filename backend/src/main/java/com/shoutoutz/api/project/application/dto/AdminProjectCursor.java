package com.shoutoutz.api.project.application.dto;

import java.time.Instant;
import java.util.Objects;

public record AdminProjectCursor(
        Instant createdAt,
        long projectId
) {

    public AdminProjectCursor {
        Objects.requireNonNull(createdAt, "프로젝트 등록 시각은 null일 수 없습니다.");
        if (projectId <= 0) {
            throw new IllegalArgumentException("프로젝트 ID는 0보다 커야 합니다.");
        }
    }
}
