package com.shoutoutz.api.auth.presentation.security;

import com.shoutoutz.api.auth.presentation.session.AuthSessionAccessor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
class CsrfProtectionFilter extends OncePerRequestFilter {

    static final String CSRF_HEADER = "X-CSRF-Token";

    private static final String API_PATH = "/api/";
    private static final Set<String> STATE_CHANGING_METHODS = Set.of(
            "POST",
            "PUT",
            "PATCH",
            "DELETE"
    );

    private final CsrfTokenManager csrfTokenManager;
    private final AuthSessionAccessor authSessionAccessor;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(API_PATH)
                || !STATE_CHANGING_METHODS.contains(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!authSessionAccessor.requiresCsrfProtection(request.getSession(false))) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestedToken = request.getHeader(CSRF_HEADER);
        if (!csrfTokenManager.matches(request.getSession(false), requestedToken)) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            return;
        }

        filterChain.doFilter(request, response);
    }
}
