package com.dropit.backend.maker;

import java.util.UUID;

import com.dropit.backend.common.auth.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/makers")
@Tag(name = "Makers", description = "메이커 프로필")
public class MakerController {

    private final MakerService makerService;
    private final CurrentUser currentUser;

    public MakerController(MakerService makerService, CurrentUser currentUser) {
        this.makerService = makerService;
        this.currentUser = currentUser;
    }

    @GetMapping("/{makerId}")
    @Operation(summary = "메이커 공개 프로필 조회")
    public MakerResponse get(@PathVariable UUID makerId) {
        return makerService.get(makerId);
    }

    @GetMapping("/me")
    @Operation(summary = "내 프로필 조회")
    @SecurityRequirement(name = "bearerAuth")
    public MakerResponse getMe(@AuthenticationPrincipal Jwt jwt) {
        return MakerResponse.from(makerService.getEntity(currentUser.requireId(jwt)));
    }

    @PutMapping("/me")
    @Operation(summary = "내 프로필 생성 또는 수정")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<MakerResponse> upsert(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody MakerRequest request
    ) {
        return ResponseEntity.ok(makerService.upsert(currentUser.requireId(jwt), request));
    }
}
