package com.shoutoutz.api.verification.infrastructure;

import com.shoutoutz.api.common.exception.custom.DuplicateEntityException;
import com.shoutoutz.api.verification.domain.UserVerificationErrorCode;
import com.shoutoutz.api.verification.domain.UserVerificationRequest;
import com.shoutoutz.api.verification.domain.UserVerificationRequestRepository;
import com.shoutoutz.api.verification.domain.VerificationRequestStatus;
import com.shoutoutz.api.verification.infrastructure.jpa.UserVerificationRequestJpaRepository;
import com.shoutoutz.api.verification.infrastructure.mapper.UserVerificationRequestMapper;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserVerificationRequestRepositoryImpl implements UserVerificationRequestRepository {

    private static final String PENDING_USER_UNIQUE_INDEX =
            "uq_user_verification_requests_pending_user";

    private final UserVerificationRequestJpaRepository requestJpaRepository;

    @Override
    public UserVerificationRequest save(UserVerificationRequest request) {
        try {
            UserVerificationRequestEntity entity = UserVerificationRequestMapper.toEntity(request);
            return UserVerificationRequestMapper.toDomain(requestJpaRepository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException exception) {
            if (isPendingUserUniqueViolation(exception)) {
                throw new DuplicateEntityException(
                        UserVerificationErrorCode.VERIFICATION_REQUEST_ALREADY_PENDING,
                        exception
                );
            }
            throw exception;
        }
    }

    @Override
    public Optional<UserVerificationRequest> findById(long requestId) {
        return requestJpaRepository.findById(requestId)
                .map(UserVerificationRequestMapper::toDomain);
    }

    @Override
    public Optional<UserVerificationRequest> findPendingByUserId(long userId) {
        return requestJpaRepository.findByUserIdAndStatus(
                        userId,
                        VerificationRequestStatus.PENDING
                )
                .map(UserVerificationRequestMapper::toDomain);
    }

    @Override
    public Optional<UserVerificationRequest> findLatestByUserId(long userId) {
        return requestJpaRepository.findFirstByUserIdOrderByRequestedAtDescIdDesc(userId)
                .map(UserVerificationRequestMapper::toDomain);
    }

    private boolean isPendingUserUniqueViolation(Throwable exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation
                    && PENDING_USER_UNIQUE_INDEX.equals(violation.getConstraintName())) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
