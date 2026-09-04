package com.shoutoutz.api.common.exception.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.common.exception.code.CommonErrorCode;
import org.junit.jupiter.api.Test;

class DiscordMessageSanitizerTest {

    @Test
    void masksSensitiveValuesInMessage() {
        String value = "password=secret token:token-value email=test@example.com 010-1234-5678";

        String masked = DiscordMessageSanitizer.mask(value);

        assertThat(masked).doesNotContain("secret", "token-value", "test@example.com", "010-1234-5678");
        assertThat(masked).contains("[MASKED]");
    }

    @Test
    void discordEventContainsMaskedUriAndMessage() {
        DiscordGeneralErrorEvent event = new DiscordGeneralErrorEvent(
                "/users?access_token=access-secret&email=test@example.com",
                CommonErrorCode.INVALID_PARAMETER,
                "Authorization: Bearer bearer-secret");

        assertThat(event.getMessage())
                .doesNotContain("access-secret", "test@example.com", "bearer-secret")
                .contains("[MASKED]");
    }
}
