package com.shoutoutz.api.media.presentation;

import com.shoutoutz.api.media.application.MediaQueryService;
import com.shoutoutz.api.media.infrastructure.s3.MediaVariant;
import com.shoutoutz.api.media.presentation.dto.response.MediaDownloadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 미디어(이미지) 조회(Query) API
 */
@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaQueryHttpApi {

    private final MediaQueryService mediaQueryService;

    /**
     * 미디어 ID로 READY 변형본의 CloudFront 공개 URL을 조회한다.
     *
     * <p>프로젝트/피드/사용자 등 일반 조회 응답은 이미 바로 사용할 URL을 포함해 전송함으로,
     * 화면 렌더링을 위해 이 API를 반복 호출하지 않는다. 클라이언트가 미디어 ID만 가지고
     * 있거나, 특정 변형본을 선택하거나, 원본 다운로드 URL을 요청하거나, 기존 호출과의
     * 호환성이 필요한 경우에 사용하는 보조 API다.</p>
     */
    @GetMapping("/{mediaId}")
    public ResponseEntity<MediaDownloadResponse> resolvePublicUrl(
            @PathVariable long mediaId,
            @RequestParam(defaultValue = "DISPLAY") MediaVariant variant
    ) {
        MediaDownloadResponse response = mediaQueryService.resolvePublicUrl(
                mediaId,
                variant
        );
        return ResponseEntity.ok(response);
    }

}
