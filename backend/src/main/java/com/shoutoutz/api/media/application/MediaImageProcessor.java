package com.shoutoutz.api.media.application;

import com.shoutoutz.api.media.domain.MediaMetadata;

/**
 * 미디어 원본 검증과 이미지 변형본 생성을 담당하는 처리 계약.
 */
public interface MediaImageProcessor {

    MediaImageProcessingResult process(MediaMetadata metadata);
}
