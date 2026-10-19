package com.shoutoutz.api.comment.presentation;

import com.shoutoutz.api.auth.presentation.security.AuthenticatedUser;
import com.shoutoutz.api.auth.presentation.security.LoginUser;
import com.shoutoutz.api.comment.application.MagazineCommentService;
import com.shoutoutz.api.comment.presentation.dto.request.MagazineCommentCreateRequest;
import com.shoutoutz.api.comment.presentation.dto.request.MagazineCommentFindRequest;
import com.shoutoutz.api.comment.presentation.dto.request.MagazineCommentUpdateRequest;
import com.shoutoutz.api.comment.presentation.dto.response.MagazineCommentCreateResponse;
import com.shoutoutz.api.comment.presentation.dto.response.MagazineCommentDeleteResponse;
import com.shoutoutz.api.comment.presentation.dto.response.MagazineCommentFindResponse;
import com.shoutoutz.api.comment.presentation.dto.response.MagazineCommentUpdateResponse;
import com.shoutoutz.api.common.response.SuccessResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/magazines/{magazineId}/comments")
@RequiredArgsConstructor
public class MagazineCommentHttpApi {

    private final MagazineCommentService magazineCommentService;

    @GetMapping
    public ResponseEntity<SuccessResponse<List<MagazineCommentFindResponse.Comment>>> findAll(
            @PathVariable long magazineId,
            @LoginUser(required = false) AuthenticatedUser loginUser,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "LATEST") String sort
    ) {
        MagazineCommentFindResponse response = magazineCommentService.findAll(
                magazineId,
                new MagazineCommentFindRequest(cursor, size, sort),
                AuthenticatedUser.userIdOrNull(loginUser)
        );
        return ResponseEntity.ok(SuccessResponse.success(response.comments(), response.meta()));
    }

    @PostMapping
    public ResponseEntity<SuccessResponse<MagazineCommentCreateResponse>> create(
            @PathVariable long magazineId,
            @LoginUser AuthenticatedUser loginUser,
            @Valid @RequestBody MagazineCommentCreateRequest request
    ) {
        MagazineCommentCreateResponse response = magazineCommentService.create(
                magazineId,
                loginUser.userId(),
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.success(response));
    }

    @PatchMapping("/{commentId}")
    public ResponseEntity<SuccessResponse<MagazineCommentUpdateResponse>> update(
            @PathVariable long magazineId,
            @PathVariable long commentId,
            @LoginUser AuthenticatedUser loginUser,
            @Valid @RequestBody MagazineCommentUpdateRequest request
    ) {
        MagazineCommentUpdateResponse response = magazineCommentService.update(
                magazineId,
                commentId,
                loginUser.userId(),
                request
        );
        return ResponseEntity.ok(SuccessResponse.success(response));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<SuccessResponse<MagazineCommentDeleteResponse>> delete(
            @PathVariable long magazineId,
            @PathVariable long commentId,
            @LoginUser AuthenticatedUser loginUser
    ) {
        MagazineCommentDeleteResponse response = magazineCommentService.delete(
                magazineId,
                commentId,
                loginUser.userId()
        );
        return ResponseEntity.ok(SuccessResponse.success(response));
    }
}
