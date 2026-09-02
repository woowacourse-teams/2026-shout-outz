package com.shoutoutz.api.auth.presentation.authentication;

import com.shoutoutz.api.auth.presentation.session.AuthSessionAccessor;
import com.shoutoutz.api.auth.presentation.session.AuthenticatedSession;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
class SessionAuthenticationFilter extends OncePerRequestFilter {

    static final String AUTHENTICATED_SESSION_ATTRIBUTE =
            AuthenticatedSession.class.getName();

    private static final String API_PATH = "/api/";

    private final AuthSessionAccessor authSessionAccessor;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(API_PATH);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        authSessionAccessor.findAuthentication(request.getSession(false))
                .ifPresent(authentication -> request.setAttribute(
                        AUTHENTICATED_SESSION_ATTRIBUTE,
                        authentication
                ));

        filterChain.doFilter(request, response);
    }
}
