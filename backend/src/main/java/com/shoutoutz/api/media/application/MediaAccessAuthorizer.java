package com.shoutoutz.api.media.application;

import com.shoutoutz.api.media.domain.MediaMetadata;

/**
 * 미디어 조회 요청자의 접근 권한을 확인하는 애플리케이션 포트.
 */
public interface MediaAccessAuthorizer {

    /**
     * 요청자가 미디어를 조회할 수 없으면 예외를 던진다.
     *
     * @param requesterId 인증된 요청자 ID. 공개 게시글 미디어 조회에서는 null일 수 있다.
     * @param metadata 조회 대상 미디어 메타데이터
     */
    void authorize(Long requesterId, MediaMetadata metadata);
}
