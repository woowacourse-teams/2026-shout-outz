package com.shoutoutz.api.verification.domain;

import java.util.Optional;

public interface UserVerificationRequestHistoryRepository {

    UserVerificationRequestHistory save(UserVerificationRequestHistory history);

    Optional<UserVerificationRequestHistory> findLatestDecisionByRequestId(long requestId);
}
