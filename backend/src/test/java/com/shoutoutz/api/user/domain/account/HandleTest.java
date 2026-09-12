package com.shoutoutz.api.user.domain.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class HandleTest {

    @ParameterizedTest
    @DisplayName("핸들을 생성한다")
    @ValueSource(strings = {
            "ab",
            "DaHye",
            "user-name_01",
            "abcdefghijklmnopqrstuvwxyz1234"
    })
    void createsValidHandle(String value) {
        Handle handle = new Handle(value);

        assertThat(handle.value()).isEqualTo(value);
    }

    @ParameterizedTest
    @DisplayName("비어 있거나 공백뿐인 핸들은 생성할 수 없다")
    @NullSource
    @ValueSource(strings = {"", " ", "\t", "\n"})
    void rejectsInvalidHandle(String value) {
        assertThatThrownBy(() -> new Handle(value))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("핸들은 대소문자를 구분하지 않고 동일성을 판단한다")
    void comparesHandleIgnoringCase() {
        Handle lowerCaseHandle = new Handle("dahye");
        Handle mixedCaseHandle = new Handle("DaHye");

        assertThat(lowerCaseHandle).isEqualTo(mixedCaseHandle);
        assertThat(lowerCaseHandle.hashCode()).isEqualTo(mixedCaseHandle.hashCode());
    }
}
