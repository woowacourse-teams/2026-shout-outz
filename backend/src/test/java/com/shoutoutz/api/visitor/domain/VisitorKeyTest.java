package com.shoutoutz.api.visitor.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class VisitorKeyTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("해시 값이 비어 있으면 방문자 키를 만들 수 없다")
    void rejectsBlankHash(String hash) {
        assertThatThrownBy(() -> new VisitorKey(hash))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
