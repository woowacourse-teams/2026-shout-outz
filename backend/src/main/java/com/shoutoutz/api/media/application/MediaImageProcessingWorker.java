package com.shoutoutz.api.media.application;

import com.shoutoutz.api.media.domain.MediaMetadata;
import com.shoutoutz.api.media.domain.MediaMetadataRepository;
import com.shoutoutz.api.media.domain.MediaStatus;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PROCESSING 상태의 미디어를 비동기로 처리하고 READY 또는 FAILED로 전환한다.
 */
@Service
@RequiredArgsConstructor
public class MediaImageProcessingWorker {

    private static final String UNKNOWN_FAILURE_REASON = "이미지 처리 중 오류가 발생했습니다.";

    private final MediaMetadataRepository mediaMetadataRepository;
    private final MediaImageProcessor mediaImageProcessor;

    /**
     * 이벤트 리스너와 분리된 프록시 호출 지점이다. 따라서 비동기 실행과 새 트랜잭션이
     * 항상 워커 스레드에서 함께 시작된다.
     */
    @Async("mediaProcessingExecutor")
    @Transactional
    public void processAsync(long mediaId) {
        mediaMetadataRepository.findById(mediaId).ifPresent(this::process);
    }

    /**
     * Spring 이벤트를 거치지 않고 처리 로직 자체를 검증할 때 사용하는 진입점이다.
     */
    void process(MediaMetadata metadata) {
        if (metadata.getStatus() != MediaStatus.PROCESSING) {
            return;
        }

        MediaImageProcessingResult result;
        try {
            result = mediaImageProcessor.process(metadata);
            if (result == null || result.originalSizeBytes() <= 0) {
                throw new ImageProcessingException("이미지 처리 결과가 올바르지 않습니다.");
            }
        } catch (ImageProcessingException exception) {
            mediaMetadataRepository.save(
                    metadata.markFailed(exception.failureReason(), Instant.now())
            );
            return;
        } catch (RuntimeException exception) {
            mediaMetadataRepository.save(
                    metadata.markFailed(UNKNOWN_FAILURE_REASON, Instant.now())
            );
            return;
        }

        mediaMetadataRepository.save(metadata.markReady(result.originalSizeBytes(), Instant.now()));
    }
}
