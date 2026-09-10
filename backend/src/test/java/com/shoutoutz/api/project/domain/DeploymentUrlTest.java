package com.shoutoutz.api.project.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.common.exception.custom.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class DeploymentUrlTest {

    @ParameterizedTest
    @ValueSource(strings = {"https://loop.team", "http://loop.team/path?tab=1"})
    @DisplayName("http 또는 https 스킴과 호스트를 갖춘 URL을 허용한다.")
    void acceptsHttpUrl(String url) {
        assertThat(new DeploymentUrl(url).value()).isEqualTo(url);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ftp://loop.team", "loop.team", "https://", "https:// loop.team"})
    @DisplayName("http 형식이 아니거나, 호스트가 없는 URL은 도메인 예외를 던진다.")
    void rejectsInvalidUrl(String url) {
        assertThatThrownBy(() -> new DeploymentUrl(url))
                .isInstanceOfSatisfying(DomainValidationException.class,
                        error -> assertThat(error.getErrorCode())
                                .isEqualTo(ProjectErrorCode.PROJECT_INVALID_DEPLOYMENT_URL));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("선택 입력이 비어 있으면 null로 둔다.")
    void returnsNullWhenBlank(String value) {
        assertThat(DeploymentUrl.fromNullable(value)).isNull();
    }
}
