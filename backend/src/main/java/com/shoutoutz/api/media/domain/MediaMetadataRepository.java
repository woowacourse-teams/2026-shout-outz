package com.shoutoutz.api.media.domain;

import java.util.Optional;
import java.util.Collection;
import java.util.List;

/**
 * 미디어 메타데이터 저장소에 대한 도메인 포트
 */
public interface MediaMetadataRepository {

    MediaMetadata save(MediaMetadata metadata);

    Optional<MediaMetadata> findById(long id);

    List<MediaMetadata> findAllByIds(Collection<Long> ids);
}
