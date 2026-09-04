package com.shoutoutz.api.common.exception.event;

import java.util.regex.Pattern;

/**
 * Discord로 전송하는 예외 메시지에서 민감한 정보를 마스킹합니다.
 */
public final class DiscordMessageSanitizer {

    private static final String MASKED_VALUE = "[MASKED]";

    private static final Pattern SENSITIVE_KEY_VALUE = Pattern.compile(
            "(?i)(\\b(?:password|passwd|pwd|secret|token|access[-_]?token|refresh[-_]?token|id[-_]?token|"
                    + "authorization|cookie|set[-_]?cookie|api[-_]?key|x[-_]?api[-_]?key|client[-_]?secret)"
                    + "\\b\\s*[:=]\\s*)(?:\"[^\"]*\"|'[^']*'|[^\\s,;&}]+)");
    private static final Pattern BEARER_TOKEN = Pattern.compile(
            "(?i)(\\bBearer\\s+)[A-Za-z0-9._~+/=-]+");
    private static final Pattern BASIC_CREDENTIAL = Pattern.compile(
            "(?i)(\\bBasic\\s+)[A-Za-z0-9+/=]+");
    private static final Pattern URL_CREDENTIAL = Pattern.compile(
            "(?i)((?:https?|jdbc:[a-z]+)://[^\\s/@:]+:)[^\\s/@]+(@)");
    private static final Pattern JWT = Pattern.compile(
            "\\b[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\b");
    private static final Pattern EMAIL = Pattern.compile(
            "(?i)(?<![a-z0-9._%+-])[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}(?![a-z0-9.-])");
    private static final Pattern KOREAN_PHONE_NUMBER = Pattern.compile(
            "(?<!\\d)(?:\\+82[- ]?10|01[016789])[- ]?\\d{3,4}[- ]?\\d{4}(?!\\d)");

    private DiscordMessageSanitizer() {
    }

    public static String mask(String value) {
        if (value == null) {
            return null;
        }

        String masked = BEARER_TOKEN.matcher(value).replaceAll("$1" + MASKED_VALUE);
        masked = BASIC_CREDENTIAL.matcher(masked).replaceAll("$1" + MASKED_VALUE);
        masked = SENSITIVE_KEY_VALUE.matcher(masked).replaceAll("$1" + MASKED_VALUE);
        masked = URL_CREDENTIAL.matcher(masked).replaceAll("$1" + MASKED_VALUE + "$2");
        masked = JWT.matcher(masked).replaceAll(MASKED_VALUE);
        masked = EMAIL.matcher(masked).replaceAll(MASKED_VALUE);
        return KOREAN_PHONE_NUMBER.matcher(masked).replaceAll(MASKED_VALUE);
    }
}
