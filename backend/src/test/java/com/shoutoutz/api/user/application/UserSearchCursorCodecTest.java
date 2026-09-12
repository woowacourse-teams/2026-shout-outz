package com.shoutoutz.api.user.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class UserSearchCursorCodecTest {

    private final UserSearchCursorCodec codec = new UserSearchCursorCodec(
            JsonMapper.builder().build()
    );

    @Test
    @DisplayName("Base64 형식은 맞지만 내부 값이 잘못된 커서를 거절한다")
    void rejectCursorWithInvalidValue() {
        String json = """
                {
                  "relevanceRank": 0,
                  "displayName": "",
                  "handle": "valid-handle"
                }
                """;
        String encodedCursor = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(json.getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> codec.decode(encodedCursor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않은 커서입니다.");
    }
}
