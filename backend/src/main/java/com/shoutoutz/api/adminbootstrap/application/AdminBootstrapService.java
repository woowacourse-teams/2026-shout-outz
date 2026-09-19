package com.shoutoutz.api.adminbootstrap.application;

import com.shoutoutz.api.adminbootstrap.domain.AdminBootstrapCode;
import com.shoutoutz.api.adminbootstrap.domain.AdminBootstrapErrorCode;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.media.infrastructure.config.S3Properties;
import com.shoutoutz.api.user.domain.account.User;
import com.shoutoutz.api.user.domain.account.UserErrorCode;
import com.shoutoutz.api.user.domain.account.UserRepository;
import com.shoutoutz.api.user.domain.account.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminBootstrapService {

    private final UserRepository userRepository;
    private final S3Properties s3Properties;

    @Transactional
    public AdminBootstrapResult promote(long userId, String code) {
        if (!AdminBootstrapCode.matches(code, s3Properties.bucket())) {
            throw new ForbiddenException(AdminBootstrapErrorCode.CODE_INVALID);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(UserErrorCode.USER_NOT_FOUND));
        if (user.getRole() == UserRole.ADMIN) {
            return new AdminBootstrapResult(user.getId(), user.getRole());
        }

        User promotedUser = User.builder()
                .id(user.getId())
                .handle(user.getHandle().value())
                .status(user.getStatus())
                .role(UserRole.ADMIN)
                .lastLoginAt(user.getLastLoginAt())
                .deletedAt(user.getDeletedAt())
                .purgedAt(user.getPurgedAt())
                .build();
        User savedUser = userRepository.save(promotedUser);

        return new AdminBootstrapResult(savedUser.getId(), savedUser.getRole());
    }
}
