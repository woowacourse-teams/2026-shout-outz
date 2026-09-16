package com.shoutoutz.api.verification.infrastructure.jpa;

import com.shoutoutz.api.verification.domain.VerificationRequestStatus;
import com.shoutoutz.api.verification.infrastructure.UserVerificationRequestEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserVerificationRequestJpaRepository
        extends JpaRepository<UserVerificationRequestEntity, Long> {

    Optional<UserVerificationRequestEntity> findByUserIdAndStatus(
            long userId,
            VerificationRequestStatus status
    );
}
