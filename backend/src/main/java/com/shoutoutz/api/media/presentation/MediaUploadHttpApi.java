package com.shoutoutz.api.media.presentation;

import com.shoutoutz.api.auth.presentation.security.AuthenticatedUser;
import com.shoutoutz.api.auth.presentation.security.LoginUser;
import com.shoutoutz.api.media.application.MediaUploadService;
import com.shoutoutz.api.media.application.MediaUploadCompletionService;
import com.shoutoutz.api.media.presentation.dto.request.MediaUploadStartRequest;
import com.shoutoutz.api.media.presentation.dto.response.MediaUploadCompleteResponse;
import com.shoutoutz.api.media.presentation.dto.response.MediaUploadStartResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 미디어(이미지) 등록 API
 */
@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaUploadHttpApi {

    private final MediaUploadService mediaUploadService;
    private final MediaUploadCompletionService mediaUploadCompletionService;

    /**
     * 1. 이미지 업로드 요청
     * @author josangjun 현재 S3 연동으로 등록 전용 presigned url 발급
     */
    @PostMapping("/uploads")
    public ResponseEntity<MediaUploadStartResponse> startUpload(
            @LoginUser AuthenticatedUser user,
            @Valid @RequestBody MediaUploadStartRequest request
    ) {
        MediaUploadStartResponse response = mediaUploadService.startUpload(user.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{mediaId}/complete")
    public ResponseEntity<MediaUploadCompleteResponse> completeUpload(
            @PathVariable long mediaId,
            @LoginUser AuthenticatedUser user
    ) {
        MediaUploadCompleteResponse response = mediaUploadCompletionService.completeUpload(user.userId(), mediaId);
        return ResponseEntity.ok(response);
    }
}
