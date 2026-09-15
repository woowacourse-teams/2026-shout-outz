package com.shoutoutz.api.post.application.dto;

import com.shoutoutz.api.media.domain.MediaPurpose;
import com.shoutoutz.api.media.domain.MediaStatus;

/**
 * 본문 미디어 연결 검증에 필요한 조회 데이터
 */
public record PostMediaReference(
        long mediaId,
        long uploadedBy,
        MediaPurpose purpose,
        MediaStatus status
) {
}
