package com.shoutoutz.api.media.presentation;

import com.shoutoutz.api.media.application.MediaUploadService;
import com.shoutoutz.api.media.presentation.dto.request.MediaUploadStartRequest;
import com.shoutoutz.api.media.presentation.dto.response.MediaUploadStartResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * 미디어 업로드 시작 HTTP API
 */
@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaUploadHttpApi {

    private final MediaUploadService mediaUploadService;

    /**
     * 1. 이미지 업로드 요청
     * @author josangjun 현재 S3 연동으로 등록 전용 presigned url 발급
     */
    @PostMapping("/uploads")
    public ResponseEntity<MediaUploadStartResponse> startUpload(
            @Valid @RequestBody MediaUploadStartRequest request,
            Principal principal
    ) {
        long requesterId = requireRequesterId(principal);
        MediaUploadStartResponse response = mediaUploadService.startUpload(requesterId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private static long requireRequesterId(Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증된 사용자만 미디어를 업로드할 수 있습니다.");
        }
        try {
            return Long.parseLong(principal.getName());
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증 주체의 사용자 ID가 올바르지 않습니다.");
        }
    }
}
