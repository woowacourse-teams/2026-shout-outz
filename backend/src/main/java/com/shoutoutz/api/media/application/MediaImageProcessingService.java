package com.shoutoutz.api.media.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 업로드 완료 커밋 이후 이미지 처리 워커에 작업을 위임한다.
 */
@Service
@RequiredArgsConstructor
public class MediaImageProcessingService {

    private final MediaImageProcessingWorker mediaImageProcessingWorker;

    /**
     * 업로드 완료 트랜잭션이 성공적으로 커밋된 경우에만 처리한다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void process(MediaProcessingRequested event) {
        mediaImageProcessingWorker.processAsync(event.mediaId());
    }
}
