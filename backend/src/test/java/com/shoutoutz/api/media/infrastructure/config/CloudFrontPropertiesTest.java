package com.shoutoutz.api.media.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CloudFrontPropertiesTest {

    @Test
    void 공개_URL의_끝에_슬래시를_보정한다() {
        CloudFrontProperties properties = new CloudFrontProperties("https://cdn.example.com");

        assertThat(properties.publicBaseUrl()).isEqualTo("https://cdn.example.com/");
        assertThat(properties.publicBaseUri()).hasToString("https://cdn.example.com/");
    }

    @Test
    void 쿼리와_fragment가_포함된_URL은_거부한다() {
        assertThatThrownBy(() -> new CloudFrontProperties("https://cdn.example.com/?token=1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CloudFront 공개 URL은 공백이 없는 절대 URL이어야 합니다.");
    }
}
