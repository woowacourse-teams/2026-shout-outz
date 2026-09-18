package com.shoutoutz.api.feed.presentation;

import com.shoutoutz.api.common.response.SliceMetaResponse;
import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.feed.application.FeedService;
import com.shoutoutz.api.feed.application.dto.FeedFindAllResult;
import com.shoutoutz.api.feed.presentation.dto.request.UserFeedFindRequest;
import com.shoutoutz.api.feed.presentation.dto.response.UserFeedResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자 페이지의 피드 목록 API.
 */
@RestController
@RequestMapping("/api/v1/users/{handle}/feeds")
@RequiredArgsConstructor
@Validated
public class UserFeedHttpApi {

    private final FeedService feedService;

    @GetMapping
    public ResponseEntity<SuccessResponse<List<UserFeedResponse>>> findAll(
            @Pattern(
                    regexp = "^[A-Za-z0-9_-]{2,30}$",
                    message = "handle 형식이 올바르지 않습니다."
            )
            @PathVariable String handle,
            @Valid @ModelAttribute UserFeedFindRequest request
    ) {
        FeedFindAllResult result = feedService.findAllByUser(handle, request);
        List<UserFeedResponse> response = UserFeedResponse.from(
                result.items(),
                result.mediaUrls()
        );
        SliceMetaResponse meta = new SliceMetaResponse(result.nextCursor(), result.hasNext());

        return ResponseEntity.ok(SuccessResponse.success(response, meta));
    }
}
