package com.shoutoutz.api.media.application;

import com.shoutoutz.api.media.domain.MediaPurpose;

/**
 * 미디어 업로드 요청자의 권한을 확인하는 애플리케이션 포트
 */
public interface MediaUploadAuthorizer {

    /**
     * 요청자가 미디어를 업로드할 수 없으면 예외를 던진다.
     *
     * @param requesterId 인증된 요청자 ID
     * @param purpose 업로드 용도
     */
    void authorize(long requesterId, MediaPurpose purpose);
}
