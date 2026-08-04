package com.dropit.backend.crew;

import com.dropit.backend.common.auth.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
@Tag(name = "Crew", description = "크루 인증")
public class CrewController {

    private final CrewService crewService;
    private final CurrentUser currentUser;

    public CrewController(CrewService crewService, CurrentUser currentUser) {
        this.crewService = crewService;
        this.currentUser = currentUser;
    }

    @PostMapping("/crew/verify")
    @Operation(summary = "크루 인증 코드 검증")
    @SecurityRequirement(name = "bearerAuth")
    public CrewVerifyResponse verify(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody CrewVerifyRequest request
    ) {
        return crewService.verify(currentUser.requireId(jwt), request.code());
    }

    @GetMapping("/users/me/crew-status")
    @Operation(summary = "내 크루 인증 상태 조회")
    @SecurityRequirement(name = "bearerAuth")
    public CrewStatusResponse status(@AuthenticationPrincipal Jwt jwt) {
        return crewService.status(currentUser.requireId(jwt));
    }
}
