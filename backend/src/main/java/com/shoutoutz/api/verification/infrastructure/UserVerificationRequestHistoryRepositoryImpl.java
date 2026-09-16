package com.shoutoutz.api.verification.infrastructure;

import com.shoutoutz.api.verification.domain.UserVerificationRequestHistory;
import com.shoutoutz.api.verification.domain.UserVerificationRequestHistoryRepository;
import com.shoutoutz.api.verification.infrastructure.jpa.UserVerificationRequestHistoryJpaRepository;
import com.shoutoutz.api.verification.infrastructure.mapper.UserVerificationRequestHistoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserVerificationRequestHistoryRepositoryImpl
        implements UserVerificationRequestHistoryRepository {

    private final UserVerificationRequestHistoryJpaRepository historyJpaRepository;

    @Override
    public UserVerificationRequestHistory save(UserVerificationRequestHistory history) {
        UserVerificationRequestHistoryEntity entity =
                UserVerificationRequestHistoryMapper.toEntity(history);
        return UserVerificationRequestHistoryMapper.toDomain(historyJpaRepository.save(entity));
    }
}
