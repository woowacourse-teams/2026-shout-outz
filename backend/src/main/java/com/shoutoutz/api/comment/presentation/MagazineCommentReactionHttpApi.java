package com.shoutoutz.api.comment.presentation;

import com.shoutoutz.api.auth.presentation.security.AuthenticatedUser;
import com.shoutoutz.api.auth.presentation.security.LoginUser;
import com.shoutoutz.api.comment.application.MagazineCommentReactionService;
import com.shoutoutz.api.comment.presentation.dto.response.MagazineCommentReactionResponse;
import com.shoutoutz.api.common.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/magazines/{magazineId}/comments/{commentId}/reactions")
@RequiredArgsConstructor
public class MagazineCommentReactionHttpApi {

    private final MagazineCommentReactionService magazineCommentReactionService;

    @PutMapping("/{type}")
    public ResponseEntity<SuccessResponse<MagazineCommentReactionResponse>> add(
            @PathVariable long magazineId,
            @PathVariable long commentId,
            @PathVariable String type,
            @LoginUser AuthenticatedUser user
    ) {
        MagazineCommentReactionResponse response = magazineCommentReactionService.add(
                magazineId,
                commentId,
                user.userId(),
                type
        );
        return ResponseEntity.ok(SuccessResponse.success(response));
    }

    @DeleteMapping("/{type}")
    public ResponseEntity<SuccessResponse<MagazineCommentReactionResponse>> remove(
            @PathVariable long magazineId,
            @PathVariable long commentId,
            @PathVariable String type,
            @LoginUser AuthenticatedUser user
    ) {
        MagazineCommentReactionResponse response = magazineCommentReactionService.remove(
                magazineId,
                commentId,
                user.userId(),
                type
        );
        return ResponseEntity.ok(SuccessResponse.success(response));
    }
}
