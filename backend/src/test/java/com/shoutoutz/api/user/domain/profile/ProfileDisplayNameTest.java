package com.shoutoutz.api.user.domain.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.common.exception.custom.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class ProfileDisplayNameTest {

    @ParameterizedTest
    @DisplayName("비어 있거나 공백뿐인 표시 이름은 생성할 수 없다")
    @NullSource
    @ValueSource(strings = {"", " ", "\t", "\n"})
    void rejectsBlankDisplayName(String value) {
        assertThatThrownBy(() -> new ProfileDisplayName(value))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("표시 이름의 앞뒤 공백을 제거한다")
    void sanitizesDisplayName() {
        ProfileDisplayName displayName = new ProfileDisplayName("  재키  ");

        assertThat(displayName.value()).isEqualTo("재키");
    }
}
