package com.shoutoutz.api.media.presentation;

import com.shoutoutz.api.media.application.MediaQueryService;
import com.shoutoutz.api.media.presentation.dto.response.MediaDownloadResponse;
import com.shoutoutz.api.media.infrastructure.s3.MediaVariant;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * 미디어 조회용 HTTP API.
 */
@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaQueryHttpApi {

    private final MediaQueryService mediaQueryService;

    /**
     * READY 미디어의 변형본을 조회할 Presigned GET URL을 발급한다.
     * 공개 게시글 미디어는 Principal 없이도 요청할 수 있다.
     */
    @GetMapping("/{mediaId}")
    public ResponseEntity<MediaDownloadResponse> createDownloadUrl(
            @PathVariable long mediaId,
            @RequestParam(defaultValue = "DISPLAY") MediaVariant variant,
            Principal principal
    ) {
        Long requesterId = optionalRequesterId(principal);
        MediaDownloadResponse response = mediaQueryService.createDownloadUrl(
                requesterId,
                mediaId,
                variant
        );
        return ResponseEntity.ok(response);
    }

    private static Long optionalRequesterId(Principal principal) {
        if (principal == null) {
            return null;
        }
        if (principal.getName() == null) {
            throw unauthorized();
        }
        try {
            long requesterId = Long.parseLong(principal.getName());
            if (requesterId <= 0) {
                throw unauthorized();
            }
            return requesterId;
        } catch (NumberFormatException exception) {
            throw unauthorized();
        }
    }

    private static ResponseStatusException unauthorized() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "인증 주체의 사용자 ID가 올바르지 않습니다."
        );
    }
}
