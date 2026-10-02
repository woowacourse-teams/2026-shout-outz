package com.shoutoutz.api.verification.infrastructure;

import com.shoutoutz.api.verification.domain.UserVerificationRequestHistory;
import com.shoutoutz.api.verification.domain.UserVerificationRequestHistoryRepository;
import com.shoutoutz.api.verification.domain.VerificationRequestStatus;
import com.shoutoutz.api.verification.infrastructure.jpa.UserVerificationRequestHistoryJpaRepository;
import com.shoutoutz.api.verification.infrastructure.mapper.UserVerificationRequestHistoryMapper;
import java.util.List;
import java.util.Optional;
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

    @Override
    public List<UserVerificationRequestHistory> findAllByRequestId(long requestId) {
        return historyJpaRepository.findAllByRequestIdOrderByChangedAtDescIdDesc(requestId)
                .stream()
                .map(UserVerificationRequestHistoryMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<UserVerificationRequestHistory> findLatestDecisionByRequestId(
            long requestId
    ) {
        return historyJpaRepository
                .findFirstByRequestIdAndToStatusInOrderByChangedAtDescIdDesc(
                        requestId,
                        List.of(
                                VerificationRequestStatus.APPROVED,
                                VerificationRequestStatus.REJECTED
                        )
                )
                .map(UserVerificationRequestHistoryMapper::toDomain);
    }
}
