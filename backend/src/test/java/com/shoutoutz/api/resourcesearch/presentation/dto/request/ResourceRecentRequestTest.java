package com.shoutoutz.api.resourcesearch.presentation.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.resourcesearch.domain.ResourceSection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ResourceRecentRequestTest {

    @Test
    @DisplayName("size가 없으면 4개를 조회한다")
    void resolvesDefaultSize() {
        assertThat(new ResourceRecentRequest(ResourceSection.BLOG, null).resolvedSize()).isEqualTo(4);
    }

    @Test
    @DisplayName("size가 있으면 그 값을 쓴다")
    void resolvesGivenSize() {
        assertThat(new ResourceRecentRequest(ResourceSection.TALK, 2).resolvedSize()).isEqualTo(2);
    }
}
