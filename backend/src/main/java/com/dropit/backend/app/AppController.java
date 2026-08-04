package com.dropit.backend.app;

import java.util.UUID;

import com.dropit.backend.common.auth.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/apps")
@Tag(name = "Apps", description = "서비스 등록·조회·관리")
public class AppController {

    private final AppService appService;
    private final CurrentUser currentUser;

    public AppController(AppService appService, CurrentUser currentUser) {
        this.appService = appService;
        this.currentUser = currentUser;
    }

    @GetMapping
    @Operation(summary = "공개 서비스 목록 조회")
    public AppListResponse list(
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String q,
        @RequestParam(defaultValue = "popular") String sort,
        @RequestParam(required = false) UUID makerId,
        @RequestParam(defaultValue = "40") int limit,
        @RequestParam(defaultValue = "0") long offset
    ) {
        return appService.list(category, q, sort, makerId, limit, offset);
    }

    @GetMapping("/trash")
    @Operation(summary = "내가 삭제한 서비스 목록 조회")
    @SecurityRequirement(name = "bearerAuth")
    public AppTrashResponse trash(@AuthenticationPrincipal Jwt jwt) {
        return appService.trash(currentUser.requireId(jwt));
    }

    @GetMapping("/{appId}")
    @Operation(summary = "서비스 상세 조회")
    public AppResponse get(@PathVariable String appId, @AuthenticationPrincipal Jwt jwt) {
        return appService.get(appId, currentUser.optionalId(jwt).orElse(null));
    }

    @PostMapping
    @Operation(summary = "서비스 등록")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<AppResponse> create(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody AppRequest request
    ) {
        return ResponseEntity.status(201).body(appService.create(currentUser.requireId(jwt), request));
    }

    @PatchMapping("/{appId}")
    @Operation(summary = "서비스 수정")
    @SecurityRequirement(name = "bearerAuth")
    public AppResponse update(
        @PathVariable String appId,
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody AppPatchRequest request
    ) {
        return appService.update(currentUser.requireId(jwt), appId, request);
    }

    @DeleteMapping("/{appId}")
    @Operation(summary = "서비스 소프트 삭제")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> delete(@PathVariable String appId, @AuthenticationPrincipal Jwt jwt) {
        appService.delete(currentUser.requireId(jwt), appId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{appId}/restore")
    @Operation(summary = "삭제한 서비스 복구")
    @SecurityRequirement(name = "bearerAuth")
    public AppResponse restore(@PathVariable String appId, @AuthenticationPrincipal Jwt jwt) {
        return appService.restore(currentUser.requireId(jwt), appId);
    }

    @PostMapping("/{appId}/play")
    @Operation(summary = "서비스 실행 횟수 증가")
    public PlayResponse play(@PathVariable String appId) {
        return appService.play(appId);
    }
}
