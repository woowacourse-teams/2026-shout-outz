package com.shoutoutz.api.auth.presentation.security;

import com.shoutoutz.api.auth.exception.AuthErrorCode;
import com.shoutoutz.api.auth.presentation.session.AuthSessionAccessor;
import com.shoutoutz.api.common.response.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

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
    private final ObjectMapper objectMapper;

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
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(
                    response.getWriter(),
                    ErrorResponse.error(
                            AuthErrorCode.CSRF_TOKEN_INVALID.name(),
                            AuthErrorCode.CSRF_TOKEN_INVALID.getMessage()
                    )
            );
            return;
        }

        filterChain.doFilter(request, response);
    }
}
