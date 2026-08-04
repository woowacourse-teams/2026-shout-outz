package com.dropit.backend.auth;

import java.util.Map;
import java.util.UUID;

import com.dropit.backend.common.api.ApiException;
import com.dropit.backend.common.auth.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
@Tag(name = "Auth", description = "Supabase Auth 세션 정보")
public class AuthSessionController {

    private final CurrentUser currentUser;

    public AuthSessionController(CurrentUser currentUser) {
        this.currentUser = currentUser;
    }

    @GetMapping("/session")
    @Operation(summary = "현재 인증 세션 조회")
    @SecurityRequirement(name = "bearerAuth")
    public AuthSessionResponse session(@AuthenticationPrincipal Jwt jwt) {
        UUID id = currentUser.requireId(jwt);
        Map<String, Object> metadata = jwt.getClaimAsMap("user_metadata");
        String email = jwt.getClaimAsString("email");
        String name = firstText(metadata, "full_name", "user_name");
        if (name == null || name.isBlank()) {
            name = email == null || email.isBlank() ? "GitHub 사용자" : email.split("@", 2)[0];
        }
        String avatarUrl = firstText(metadata, "avatar_url");
        return new AuthSessionResponse(id, name, email, avatarUrl);
    }

    private String firstText(Map<String, Object> values, String... keys) {
        if (values == null) {
            return null;
        }
        for (String key : keys) {
            Object value = values.get(key);
            if (value instanceof String text && !text.isBlank()) {
                return text;
            }
        }
        return null;
    }
}
