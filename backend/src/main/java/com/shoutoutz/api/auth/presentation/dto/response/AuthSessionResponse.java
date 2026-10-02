package com.shoutoutz.api.auth.presentation.dto.response;

import com.shoutoutz.api.auth.presentation.session.AuthenticatedSession;
import com.shoutoutz.api.user.domain.account.UserRole;

public record AuthSessionResponse(
        Status status,
        Long userId,
        UserRole role,
        String csrfToken
) {

    public static AuthSessionResponse unauthenticated(String csrfToken) {
        return new AuthSessionResponse(Status.UNAUTHENTICATED, null, null, csrfToken);
    }

    public static AuthSessionResponse signupRequired(String csrfToken) {
        return new AuthSessionResponse(Status.SIGNUP_REQUIRED, null, null, csrfToken);
    }

    public static AuthSessionResponse authenticated(
            AuthenticatedSession authenticatedSession,
            String csrfToken
    ) {
        return new AuthSessionResponse(
                Status.AUTHENTICATED,
                authenticatedSession.userId(),
                authenticatedSession.role(),
                csrfToken
        );
    }

    public enum Status {
        UNAUTHENTICATED,
        SIGNUP_REQUIRED,
        AUTHENTICATED
    }
}
