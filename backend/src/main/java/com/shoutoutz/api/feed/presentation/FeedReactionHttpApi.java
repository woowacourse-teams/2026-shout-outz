package com.shoutoutz.api.feed.presentation;

import com.shoutoutz.api.auth.presentation.security.AuthenticatedUser;
import com.shoutoutz.api.auth.presentation.security.LoginUser;
import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.feed.application.FeedReactionService;
import com.shoutoutz.api.feed.presentation.dto.response.FeedReactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/feeds/{feedId}/reactions")
@RequiredArgsConstructor
public class FeedReactionHttpApi {

    private final FeedReactionService feedReactionService;

    @PutMapping("/{type}")
    public ResponseEntity<SuccessResponse<FeedReactionResponse>> add(
            @PathVariable long feedId,
            @PathVariable String type,
            @LoginUser AuthenticatedUser user
    ) {
        FeedReactionResponse response = feedReactionService.add(feedId, user.userId(), type);
        return ResponseEntity.ok(SuccessResponse.success(response));
    }

    @DeleteMapping("/{type}")
    public ResponseEntity<SuccessResponse<FeedReactionResponse>> remove(
            @PathVariable long feedId,
            @PathVariable String type,
            @LoginUser AuthenticatedUser user
    ) {
        FeedReactionResponse response = feedReactionService.remove(feedId, user.userId(), type);
        return ResponseEntity.ok(SuccessResponse.success(response));
    }
}
