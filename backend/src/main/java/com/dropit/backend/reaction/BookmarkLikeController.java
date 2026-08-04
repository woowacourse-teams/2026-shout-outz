package com.dropit.backend.reaction;

import com.dropit.backend.common.auth.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
@Tag(name = "Bookmarks & Likes", description = "북마크와 좋아요")
@SecurityRequirement(name = "bearerAuth")
public class BookmarkLikeController {

    private final BookmarkLikeService service;
    private final CurrentUser currentUser;

    public BookmarkLikeController(BookmarkLikeService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping("/users/me/bookmarks")
    @Operation(summary = "내 북마크 목록 조회")
    public AppIdsResponse bookmarks(@AuthenticationPrincipal Jwt jwt) {
        return service.bookmarks(currentUser.requireId(jwt));
    }

    @PutMapping("/apps/{appId}/bookmark")
    @Operation(summary = "북마크 추가")
    public ResponseEntity<Void> bookmark(@PathVariable String appId, @AuthenticationPrincipal Jwt jwt) {
        service.bookmark(currentUser.requireId(jwt), appId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/apps/{appId}/bookmark")
    @Operation(summary = "북마크 해제")
    public ResponseEntity<Void> unbookmark(@PathVariable String appId, @AuthenticationPrincipal Jwt jwt) {
        service.unbookmark(currentUser.requireId(jwt), appId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/me/likes")
    @Operation(summary = "내 좋아요 목록 조회")
    public AppIdsResponse likes(@AuthenticationPrincipal Jwt jwt) {
        return service.likes(currentUser.requireId(jwt));
    }

    @PostMapping("/apps/{appId}/like")
    @Operation(summary = "좋아요 토글")
    public LikeResponse toggleLike(@PathVariable String appId, @AuthenticationPrincipal Jwt jwt) {
        return service.toggleLike(currentUser.requireId(jwt), appId);
    }
}
