package com.shoutoutz.api.media.application;

import com.shoutoutz.api.media.domain.MediaPurpose;

/**
 * 업로드 대상과 요청 사용자의 권한을 확인하는 애플리케이션 포트
 */
public interface MediaUploadAuthorizer {

    /**
     * 요청자가 해당 대상에 미디어를 업로드할 수 없으면 예외를 던진다.
     *
     * @param requesterId 인증된 요청자 ID
     * @param purpose 업로드 용도
     * @param targetId 업로드 대상 ID. 홈 배너는 등록 전 업로드이므로 null이다.
     */
    void authorize(long requesterId, MediaPurpose purpose, Long targetId);
}
