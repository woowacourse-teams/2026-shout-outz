package com.shoutoutz.api.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class HandleTest {

    @ParameterizedTest
    @DisplayName("영숫자와 하이픈, 언더스코어로 구성된 2자 이상 30자 이하 핸들을 생성한다")
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
    @DisplayName("핸들 명세에 맞지 않는 값은 생성할 수 없다")
    @NullSource
    @ValueSource(strings = {
            "",
            "a",
            "재키",
            "user handle",
            "user@handle",
            "abcdefghijklmnopqrstuvwxyz12345"
    })
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
