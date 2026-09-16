package com.shoutoutz.api.verification.infrastructure.jpa;

import com.shoutoutz.api.verification.infrastructure.UserVerificationRequestHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserVerificationRequestHistoryJpaRepository
        extends JpaRepository<UserVerificationRequestHistoryEntity, Long> {
}
