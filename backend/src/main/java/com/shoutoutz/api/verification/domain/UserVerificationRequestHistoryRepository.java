package com.shoutoutz.api.verification.domain;

import java.util.List;
import java.util.Optional;

public interface UserVerificationRequestHistoryRepository {

    UserVerificationRequestHistory save(UserVerificationRequestHistory history);

    List<UserVerificationRequestHistory> findAllByRequestId(long requestId);

    Optional<UserVerificationRequestHistory> findLatestDecisionByRequestId(long requestId);
}
