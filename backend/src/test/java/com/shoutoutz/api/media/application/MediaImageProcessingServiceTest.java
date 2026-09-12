package com.shoutoutz.api.media.application;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MediaImageProcessingServiceTest {

    @Mock
    private MediaImageProcessingWorker mediaImageProcessingWorker;

    private MediaImageProcessingService mediaImageProcessingService;

    @BeforeEach
    void setUp() {
        mediaImageProcessingService = new MediaImageProcessingService(mediaImageProcessingWorker);
    }

    @Test
    void 완료_커밋_이벤트를_워커에_위임한다() {
        mediaImageProcessingService.process(new MediaProcessingRequested(10L));

        verify(mediaImageProcessingWorker).processAsync(10L);
    }
}
