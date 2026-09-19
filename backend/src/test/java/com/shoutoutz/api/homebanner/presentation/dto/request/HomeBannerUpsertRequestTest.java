package com.shoutoutz.api.homebanner.presentation.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class HomeBannerUpsertRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void 대상_상세로_이동하는_요청을_허용한다() {
        HomeBannerUpsertRequest request = new HomeBannerUpsertRequest(
                1L,
                "TARGET",
                "PROJECT",
                2L,
                null,
                null,
                0,
                true
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void 내부_경로로_이동하는_요청을_허용한다() {
        HomeBannerUpsertRequest request = new HomeBannerUpsertRequest(
                1L,
                "URL",
                null,
                null,
                "INTERNAL_PATH",
                "/projects",
                0,
                true
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void 이동_방식과_맞지_않는_필드_조합은_거부한다() {
        HomeBannerUpsertRequest request = new HomeBannerUpsertRequest(
                1L,
                "TARGET",
                "FEED",
                2L,
                "INTERNAL_PATH",
                "/feeds/2",
                0,
                true
        );

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void 외부_URL은_HTTPS만_허용한다() {
        HomeBannerUpsertRequest request = new HomeBannerUpsertRequest(
                1L,
                "URL",
                null,
                null,
                "EXTERNAL_URL",
                "http://example.com",
                0,
                true
        );

        assertThat(validator.validate(request)).isNotEmpty();
    }
}
