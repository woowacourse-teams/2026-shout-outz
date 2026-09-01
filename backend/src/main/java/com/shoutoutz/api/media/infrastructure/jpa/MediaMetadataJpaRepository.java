package com.shoutoutz.api.media.infrastructure.jpa;

import com.shoutoutz.api.media.infrastructure.MediaMetadataEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaMetadataJpaRepository extends JpaRepository<MediaMetadataEntity, Long> {
}
