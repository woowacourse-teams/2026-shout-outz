package com.shoutoutz.api.verification.infrastructure.mapper;

import com.shoutoutz.api.verification.domain.UserVerificationRequest;
import com.shoutoutz.api.verification.infrastructure.UserVerificationRequestEntity;

public final class UserVerificationRequestMapper {

    private UserVerificationRequestMapper() {
    }

    public static UserVerificationRequestEntity toEntity(UserVerificationRequest request) {
        return UserVerificationRequestEntity.builder()
                .id(request.getId())
                .userId(request.getUserId())
                .userType(request.getUserType())
                .nickname(request.getNickname())
                .cohort(request.getCohort())
                .track(request.getTrack())
                .status(request.getStatus())
                .requestedAt(request.getRequestedAt())
                .decidedAt(request.getDecidedAt())
                .build();
    }

    public static UserVerificationRequest toDomain(UserVerificationRequestEntity entity) {
        return UserVerificationRequest.reconstitute(
                entity.getId(),
                entity.getUserId(),
                entity.getUserType(),
                entity.getNickname(),
                entity.getCohort(),
                entity.getTrack(),
                entity.getStatus(),
                entity.getRequestedAt(),
                entity.getDecidedAt()
        );
    }
}
