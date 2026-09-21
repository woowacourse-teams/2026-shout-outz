package com.shoutoutz.api.homebanner.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.common.exception.custom.DomainValidationException;
import org.junit.jupiter.api.Test;

class HomeBannerTest {

    @Test
    void 서비스_콘텐츠_상세로_이동하는_배너를_생성한다() {
        HomeBanner banner = targetBanner();

        assertThat(banner.getDestinationType()).isEqualTo(BannerDestinationType.TARGET);
        assertThat(banner.getTargetType()).isEqualTo(BannerTargetType.PROJECT);
        assertThat(banner.getTargetId()).isEqualTo(20L);
        assertThat(banner.isActive()).isTrue();
    }

    @Test
    void 외부_URL로_이동하는_배너를_생성한다() {
        HomeBanner banner = HomeBanner.create(
                10L,
                BannerDestinationType.URL,
                null,
                null,
                BannerLinkType.EXTERNAL_URL,
                " https://example.com/promotion ",
                0,
                true,
                1L
        );

        assertThat(banner.getLinkUrl()).isEqualTo("https://example.com/promotion");
    }

    @Test
    void TARGET과_URL_필드를_함께_사용할_수_없다() {
        assertThatThrownBy(() -> HomeBanner.create(
                10L,
                BannerDestinationType.TARGET,
                BannerTargetType.NEWS,
                20L,
                BannerLinkType.INTERNAL_PATH,
                "/news/20",
                0,
                true,
                1L
        )).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void 내부_경로는_슬래시_하나로_시작해야_한다() {
        assertThatThrownBy(() -> HomeBanner.create(
                10L,
                BannerDestinationType.URL,
                null,
                null,
                BannerLinkType.INTERNAL_PATH,
                "//example.com/promotion",
                0,
                true,
                1L
        )).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void 외부_URL은_HTTPS만_허용한다() {
        assertThatThrownBy(() -> HomeBanner.create(
                10L,
                BannerDestinationType.URL,
                null,
                null,
                BannerLinkType.EXTERNAL_URL,
                "http://example.com/promotion",
                0,
                true,
                1L
        )).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void 표시_순서는_음수일_수_없다() {
        assertThatThrownBy(() -> HomeBanner.create(
                10L,
                BannerDestinationType.TARGET,
                BannerTargetType.FEED,
                20L,
                null,
                null,
                -1,
                true,
                1L
        )).isInstanceOf(DomainValidationException.class);
    }

    private HomeBanner targetBanner() {
        return HomeBanner.create(
                10L,
                BannerDestinationType.TARGET,
                BannerTargetType.PROJECT,
                20L,
                null,
                null,
                0,
                true,
                1L
        );
    }
}
