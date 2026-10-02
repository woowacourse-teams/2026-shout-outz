package com.shoutoutz.api.verification.infrastructure.mapper;

import com.shoutoutz.api.verification.domain.UserVerificationRequestHistory;
import com.shoutoutz.api.verification.infrastructure.UserVerificationRequestHistoryEntity;

public final class UserVerificationRequestHistoryMapper {

    private UserVerificationRequestHistoryMapper() {
    }

    public static UserVerificationRequestHistoryEntity toEntity(
            UserVerificationRequestHistory history
    ) {
        return UserVerificationRequestHistoryEntity.builder()
                .id(history.getId())
                .requestId(history.getRequestId())
                .changedBy(history.getChangedBy())
                .fromStatus(history.getFromStatus())
                .toStatus(history.getToStatus())
                .reason(history.getReason())
                .changedAt(history.getChangedAt())
                .build();
    }

    public static UserVerificationRequestHistory toDomain(
            UserVerificationRequestHistoryEntity entity
    ) {
        return UserVerificationRequestHistory.reconstitute(
                entity.getId(),
                entity.getRequestId(),
                entity.getChangedBy(),
                entity.getFromStatus(),
                entity.getToStatus(),
                entity.getReason(),
                entity.getChangedAt()
        );
    }
}
