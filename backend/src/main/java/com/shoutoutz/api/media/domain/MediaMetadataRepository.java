package com.shoutoutz.api.media.domain;

/**
 * 미디어 메타데이터 저장소에 대한 도메인 포트
 */
public interface MediaMetadataRepository {

    MediaMetadata save(MediaMetadata metadata);
}
