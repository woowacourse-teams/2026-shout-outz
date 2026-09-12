package com.shoutoutz.api.user.infrastructure;

import com.shoutoutz.api.common.exception.custom.DuplicateEntityException;
import com.shoutoutz.api.user.domain.account.User;
import com.shoutoutz.api.user.domain.account.UserRepository;
import com.shoutoutz.api.user.exception.UserErrorCode;
import com.shoutoutz.api.user.infrastructure.jpa.UserJpaRepository;
import com.shoutoutz.api.user.infrastructure.mapper.UserMapper;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private static final String HANDLE_UNIQUE_CONSTRAINT = "uq_users_handle_lower";

    private final UserJpaRepository userJpaRepository;

    @Override
    public User save(User user) {
        try {
            UserEntity userEntity = UserMapper.toEntity(user);
            UserEntity savedUserEntity = userJpaRepository.save(userEntity);

            return UserMapper.toDomain(savedUserEntity);
        } catch (DataIntegrityViolationException exception) {
            if (isHandleUniqueViolation(exception)) {
                throw new DuplicateEntityException(UserErrorCode.HANDLE_ALREADY_EXISTS, exception);
            }
            throw exception;
        }
    }

    @Override
    public Optional<User> findById(long id) {
        return userJpaRepository.findById(id)
                .map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> findByHandle(String handle) {
        return userJpaRepository.findByHandleIgnoreCase(handle)
                .map(UserMapper::toDomain);
    }

    private boolean isHandleUniqueViolation(Throwable exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException violation
                    && HANDLE_UNIQUE_CONSTRAINT.equals(violation.getConstraintName())) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
