package com.shoutoutz.api.media.presentation;

import com.shoutoutz.api.auth.presentation.security.AuthenticatedUser;
import com.shoutoutz.api.auth.presentation.security.LoginUser;
import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.media.application.MediaStatusQueryService;
import com.shoutoutz.api.media.presentation.dto.response.MediaStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaStatusHttpApi {

    private final MediaStatusQueryService mediaStatusQueryService;

    @GetMapping("/{mediaId}/status")
    public ResponseEntity<SuccessResponse<MediaStatusResponse>> getStatus(
            @PathVariable long mediaId,
            @LoginUser AuthenticatedUser user
    ) {
        MediaStatusResponse response = mediaStatusQueryService.getStatus(user.userId(), mediaId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(SuccessResponse.success(response));
    }
}
