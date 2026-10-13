package com.shoutoutz.api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.shoutoutz.api.auth.application.LoadTestLoginService;
import com.shoutoutz.api.auth.presentation.LoadTestLoginHttpApi;
import com.shoutoutz.api.auth.presentation.security.CsrfTokenManager;
import com.shoutoutz.api.auth.presentation.session.AuthSessionAccessor;
import com.shoutoutz.api.auth.presentation.session.AuthSessionManager;
import com.shoutoutz.api.user.domain.account.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class LoadTestProfileGuardTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(UserRepository.class, () -> mock(UserRepository.class))
            .withUserConfiguration(
                    AuthSessionAccessor.class,
                    AuthSessionManager.class,
                    CsrfTokenManager.class,
                    LoadTestLoginService.class,
                    LoadTestLoginHttpApi.class,
                    LoadTestProfileGuard.class
            );

    @Test
    @DisplayName("loadtest 프로필이면 부하 테스트 로그인 API가 등록된다.")
    void registersLoginApiInLoadTestProfile() {
        contextRunner.withPropertyValues("spring.profiles.active=loadtest").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(LoadTestLoginHttpApi.class);
            assertThat(context).hasSingleBean(LoadTestLoginService.class);
        });
    }

    @ParameterizedTest(name = "프로필: [{0}]")
    @ValueSource(strings = {"", "prod", "local", "test"})
    @DisplayName("loadtest 프로필이 아니면(미지정 포함) 부하 테스트 로그인 API가 등록되지 않는다.")
    void doesNotRegisterLoginApiWithoutLoadTestProfile(String profiles) {
        contextRunner.withPropertyValues("spring.profiles.active=" + profiles).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(LoadTestLoginHttpApi.class);
            assertThat(context).doesNotHaveBean(LoadTestLoginService.class);
        });
    }

    @Test
    @DisplayName("prod와 loadtest 프로필을 함께 켜면 앱이 시작되지 않는다.")
    void failsToStartWithProdAndLoadTestProfiles() {
        contextRunner.withPropertyValues("spring.profiles.active=prod,loadtest").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .rootCause()
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("prod와 loadtest 프로필은 함께 활성화할 수 없습니다.");
        });
    }
}
