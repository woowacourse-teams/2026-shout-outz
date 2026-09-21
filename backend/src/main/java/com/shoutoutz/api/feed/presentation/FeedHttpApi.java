package com.shoutoutz.api.feed.presentation;

import com.shoutoutz.api.auth.presentation.security.AuthenticatedUser;
import com.shoutoutz.api.auth.presentation.security.LoginUser;
import com.shoutoutz.api.common.response.SliceMetaResponse;
import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.feed.application.FeedService;
import com.shoutoutz.api.feed.application.dto.FeedFindAllResult;
import com.shoutoutz.api.feed.presentation.dto.request.FeedFindAllRequest;
import com.shoutoutz.api.feed.presentation.dto.request.FeedSaveRequest;
import com.shoutoutz.api.feed.presentation.dto.request.FeedUpdateRequest;
import com.shoutoutz.api.feed.presentation.dto.response.FeedCommandResponse;
import com.shoutoutz.api.feed.presentation.dto.response.FeedResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/feeds")
@RequiredArgsConstructor
public class FeedHttpApi {

    private final FeedService feedService;

    @GetMapping
    public ResponseEntity<SuccessResponse<List<FeedResponse>>> findAllFeed(
            @Valid @ModelAttribute FeedFindAllRequest request
    ) {
        FeedFindAllResult result = feedService.findAllFeed(request);
        List<FeedResponse> response = FeedResponse.from(result.items(), result.mediaUrls());
        SliceMetaResponse meta = new SliceMetaResponse(result.nextCursor(), result.hasNext());

        return ResponseEntity.ok(SuccessResponse.success(response, meta));
    }

    @GetMapping("/{feedId}")
    public ResponseEntity<SuccessResponse<FeedResponse>> findFeed(
            @PathVariable long feedId
    ) {
        FeedResponse response = feedService.findFeed(feedId);

        return ResponseEntity.ok(SuccessResponse.success(response));
    }

    @PostMapping
    public ResponseEntity<SuccessResponse<FeedCommandResponse>> saveFeed(
            @LoginUser AuthenticatedUser user,
            @Valid @RequestBody FeedSaveRequest request
    ) {
        FeedCommandResponse response = feedService.saveFeed(user.userId(), request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SuccessResponse.success(response));
    }

    @PutMapping("/{feedId}")
    public ResponseEntity<SuccessResponse<FeedCommandResponse>> updateFeed(
            @PathVariable long feedId,
            @LoginUser AuthenticatedUser user,
            @Valid @RequestBody FeedUpdateRequest request
    ) {
        FeedCommandResponse response = feedService.updateFeed(feedId, user.userId(), request);

        return ResponseEntity.ok(SuccessResponse.success(response));
    }

    @DeleteMapping("/{feedId}")
    public ResponseEntity<Void> deleteFeed(
            @PathVariable long feedId,
            @LoginUser AuthenticatedUser user
    ) {
        feedService.deleteFeed(feedId, user.userId());

        return ResponseEntity.noContent().build();
    }
}
