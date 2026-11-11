package com.shoutoutz.api.resourcesearch.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResourceLinkPreviewServiceTest {

    @Mock
    private ResourceLinkPreviewRepository resourceLinkPreviewRepository;

    @InjectMocks
    private ResourceLinkPreviewService resourceLinkPreviewService;

    @Test
    @DisplayName("피드 미리보기 수집이 밀리지 않도록 한 번에 20개까지만 연결한다")
    void linksAtMostOneBatch() {
        given(resourceLinkPreviewRepository.linkUnlinkedResources(20)).willReturn(20);

        int linked = resourceLinkPreviewService.linkUnlinkedResources();

        assertThat(linked).isEqualTo(20);
    }
}
