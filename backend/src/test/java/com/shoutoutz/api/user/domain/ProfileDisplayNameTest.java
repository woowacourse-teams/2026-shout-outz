package com.shoutoutz.api.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class ProfileDisplayNameTest {

    @Test
    @DisplayName("50자 이하의 표시 이름을 생성한다")
    void createsDisplayNameUpToFiftyCharacters() {
        String value = "가".repeat(50);

        ProfileDisplayName displayName = new ProfileDisplayName(value);

        assertThat(displayName.value()).isEqualTo(value);
    }

    @Test
    @DisplayName("표시 이름 길이는 유니코드 문자 수를 기준으로 계산한다")
    void countsDisplayNameLengthByUnicodeCodePoint() {
        String value = "😀".repeat(50);

        ProfileDisplayName displayName = new ProfileDisplayName(value);

        assertThat(displayName.value()).isEqualTo(value);
    }

    @ParameterizedTest
    @DisplayName("비어 있거나 공백뿐인 표시 이름은 생성할 수 없다")
    @NullSource
    @ValueSource(strings = {"", " ", "\t", "\n"})
    void rejectsBlankDisplayName(String value) {
        assertThatThrownBy(() -> new ProfileDisplayName(value))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("50자를 초과하는 표시 이름은 생성할 수 없다")
    void rejectsDisplayNameLongerThanFiftyCharacters() {
        String value = "가".repeat(51);

        assertThatThrownBy(() -> new ProfileDisplayName(value))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
