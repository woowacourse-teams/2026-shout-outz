package com.shoutoutz.api.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class CookieConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer());

    @Test
    @DisplayName("로컬 환경은 세션 쿠키와 방문자 쿠키에 SameSite Lax를 적용하고 Secure를 끈다")
    void configuresSameSiteCookiesLocally() {
        contextRunner.run(context -> {
            assertThat(context.getEnvironment()
                    .getProperty("server.servlet.session.cookie.same-site"))
                    .isEqualTo("Lax");
            assertThat(context.getEnvironment()
                    .getProperty("server.servlet.session.cookie.secure", Boolean.class))
                    .isFalse();
            assertThat(context.getEnvironment().getProperty("visitor.cookie-same-site"))
                    .isEqualTo("Lax");
            assertThat(context.getEnvironment()
                    .getProperty("visitor.cookie-secure", Boolean.class))
                    .isFalse();
        });
    }

    @Test
    @DisplayName("운영 환경은 세션 쿠키와 방문자 쿠키에 SameSite None과 Secure를 적용한다")
    void configuresCrossSiteCookiesInProduction() {
        contextRunner.withPropertyValues("spring.profiles.active=prod").run(context -> {
            assertThat(context.getEnvironment()
                    .getProperty("server.servlet.session.cookie.same-site"))
                    .isEqualTo("None");
            assertThat(context.getEnvironment()
                    .getProperty("server.servlet.session.cookie.secure", Boolean.class))
                    .isTrue();
            assertThat(context.getEnvironment().getProperty("visitor.cookie-same-site"))
                    .isEqualTo("None");
            assertThat(context.getEnvironment()
                    .getProperty("visitor.cookie-secure", Boolean.class))
                    .isTrue();
        });
    }
}
