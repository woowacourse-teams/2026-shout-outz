package com.shoutoutz.api.verification.infrastructure.jpa;

import com.shoutoutz.api.verification.domain.VerificationRequestStatus;
import com.shoutoutz.api.verification.infrastructure.UserVerificationRequestHistoryEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserVerificationRequestHistoryJpaRepository
        extends JpaRepository<UserVerificationRequestHistoryEntity, Long> {

    Optional<UserVerificationRequestHistoryEntity>
    findFirstByRequestIdAndToStatusInOrderByChangedAtDescIdDesc(
            long requestId,
            List<VerificationRequestStatus> statuses
    );
}
