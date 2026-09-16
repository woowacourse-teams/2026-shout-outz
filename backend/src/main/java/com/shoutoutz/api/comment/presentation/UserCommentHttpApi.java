package com.shoutoutz.api.comment.presentation;

import com.shoutoutz.api.auth.presentation.security.AuthenticatedUser;
import com.shoutoutz.api.auth.presentation.security.LoginUser;
import com.shoutoutz.api.comment.application.UserCommentService;
import com.shoutoutz.api.comment.application.dto.UserCommentResult;
import com.shoutoutz.api.comment.presentation.dto.request.UserCommentFindRequest;
import com.shoutoutz.api.comment.presentation.dto.response.UserCommentResponse;
import com.shoutoutz.api.common.response.SliceMetaResponse;
import com.shoutoutz.api.common.response.SuccessResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 마이페이지의 댓글 목록 API.
 */
@RestController
@RequestMapping("/api/v1/users/me/comments")
@RequiredArgsConstructor
public class UserCommentHttpApi {

    private final UserCommentService userCommentService;

    @GetMapping
    public ResponseEntity<SuccessResponse<List<UserCommentResponse>>> findAll(
            @LoginUser AuthenticatedUser authenticatedUser,
            @Valid @ModelAttribute UserCommentFindRequest request
    ) {
        UserCommentResult result = userCommentService.findAll(authenticatedUser.userId(), request);
        List<UserCommentResponse> response = UserCommentResponse.from(result.comments());
        SliceMetaResponse meta = new SliceMetaResponse(result.nextCursor(), result.hasNext());

        return ResponseEntity.ok(SuccessResponse.success(response, meta));
    }
}
