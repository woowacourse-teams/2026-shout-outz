package com.shoutoutz.api.homebanner.presentation.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class HomeBannerUpsertRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void 프로젝트는_slug로_가리키는_요청을_허용한다() {
        assertThat(validator.validate(targetRequest("PROJECT", null, "loop"))).isEmpty();
    }

    @Test
    void 소식과_피드는_ID로_가리키는_요청을_허용한다() {
        assertThat(validator.validate(targetRequest("NEWS", 2L, null))).isEmpty();
        assertThat(validator.validate(targetRequest("FEED", 2L, null))).isEmpty();
    }

    @Test
    void 프로젝트를_ID로_가리키거나_slug를_비우면_거부한다() {
        assertThat(validator.validate(targetRequest("PROJECT", 2L, null))).isNotEmpty();
        assertThat(validator.validate(targetRequest("PROJECT", 2L, "loop"))).isNotEmpty();
        assertThat(validator.validate(targetRequest("PROJECT", null, " "))).isNotEmpty();
    }

    @Test
    void 소식과_피드에_slug를_넣으면_거부한다() {
        assertThat(validator.validate(targetRequest("NEWS", 2L, "loop"))).isNotEmpty();
        assertThat(validator.validate(targetRequest("FEED", null, "loop"))).isNotEmpty();
    }

    @Test
    void 내부_경로로_이동하는_요청을_허용한다() {
        HomeBannerUpsertRequest request = new HomeBannerUpsertRequest(
                1L,
                "URL",
                null,
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
                null,
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
                null,
                "EXTERNAL_URL",
                "http://example.com",
                0,
                true
        );

        assertThat(validator.validate(request)).isNotEmpty();
    }

    private HomeBannerUpsertRequest targetRequest(String targetType, Long targetId, String targetSlug) {
        return new HomeBannerUpsertRequest(1L, "TARGET", targetType, targetId, targetSlug, null, null, 0, true);
    }
}
