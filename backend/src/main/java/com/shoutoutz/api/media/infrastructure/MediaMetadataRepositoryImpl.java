package com.shoutoutz.api.media.infrastructure;

import com.shoutoutz.api.media.domain.MediaMetadata;
import com.shoutoutz.api.media.domain.MediaMetadataRepository;
import com.shoutoutz.api.media.infrastructure.jpa.MediaMetadataJpaRepository;
import com.shoutoutz.api.media.infrastructure.mappper.MediaMetadataMapper;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 미디어 메타데이터 도메인 저장소의 JPA 구현
 */
@Repository
@RequiredArgsConstructor
public class MediaMetadataRepositoryImpl implements MediaMetadataRepository {

    private final MediaMetadataJpaRepository mediaMetadataJpaRepository;

    @Override
    public MediaMetadata save(MediaMetadata metadata) {
        MediaMetadataEntity entity = MediaMetadataMapper.toEntity(metadata);
        return MediaMetadataMapper.toDomain(mediaMetadataJpaRepository.save(entity));
    }

    @Override
    public Optional<MediaMetadata> findById(long id) {
        return mediaMetadataJpaRepository.findById(id).map(MediaMetadataMapper::toDomain);
    }
}
