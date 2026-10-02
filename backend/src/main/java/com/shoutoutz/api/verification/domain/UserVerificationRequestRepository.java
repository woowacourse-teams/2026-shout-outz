package com.shoutoutz.api.verification.domain;

import java.util.Optional;

public interface UserVerificationRequestRepository {

    UserVerificationRequest save(UserVerificationRequest request);

    Optional<UserVerificationRequest> findById(long requestId);

    Optional<UserVerificationRequest> findPendingByUserId(long userId);

    Optional<UserVerificationRequest> findLatestByUserId(long userId);
}
