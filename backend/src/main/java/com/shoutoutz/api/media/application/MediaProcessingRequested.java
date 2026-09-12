package com.shoutoutz.api.media.application;

/**
 * 업로드 완료 트랜잭션이 커밋된 뒤 이미지 처리를 시작하기 위한 이벤트.
 */
public record MediaProcessingRequested(long mediaId) {
}
