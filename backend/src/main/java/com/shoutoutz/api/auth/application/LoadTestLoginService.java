package com.shoutoutz.api.auth.application;

import com.shoutoutz.api.auth.exception.AuthErrorCode;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.common.util.DataResolveUtil;
import com.shoutoutz.api.user.domain.account.User;
import com.shoutoutz.api.user.domain.account.UserErrorCode;
import com.shoutoutz.api.user.domain.account.UserRepository;
import com.shoutoutz.api.user.domain.account.UserRole;
import com.shoutoutz.api.user.domain.account.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [부하 테스트 전용] GitHub OAuth 없이 로그인할 사용자를 찾는다.
 * loadtest 프로필에서만 등록되고 prod에서는 등록되지 않는다.
 * 사용자 데이터를 바꾸지 않도록 조회만 하고, 마지막 로그인 시각도 갱신하지 않는다.
 */
@Profile("loadtest & !prod")
@Service
@RequiredArgsConstructor
public class LoadTestLoginService {

    private static final String HANDLE_PREFIX = "@";

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User findLoginUser(String handle) {
        User user = userRepository.findByHandle(normalizeHandle(handle))
                .orElseThrow(() -> new EntityNotFoundException(UserErrorCode.USER_NOT_FOUND));
        if (user.getStatus() != UserStatus.ACTIVE || user.getRole() != UserRole.USER) {
            throw new ForbiddenException(AuthErrorCode.LOAD_TEST_LOGIN_NOT_ALLOWED);
        }
        return user;
    }

    private String normalizeHandle(String handle) {
        String sanitizedHandle = DataResolveUtil.sanitizeString(handle);
        if (sanitizedHandle == null || sanitizedHandle.startsWith(HANDLE_PREFIX)) {
            return sanitizedHandle;
        }
        return HANDLE_PREFIX + sanitizedHandle;
    }
}
