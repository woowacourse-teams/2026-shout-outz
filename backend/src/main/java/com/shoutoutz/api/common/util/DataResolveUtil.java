package com.shoutoutz.api.common.util;

public final class DataResolveUtil {
    private DataResolveUtil() {}

    /**
     * 문자열 정제 메소드
     *
     * @param string
     * @return null Or Trimed String
     */
    public static String sanitizeString(String string) {
        String sanitizedString = string != null ? string.trim() : null;
        return emptyToNull(sanitizedString);
    }

    /**
     * 이메일 정제 메소드 (trim + 소문자 변환)
     *
     * @param email
     * @return null Or Trimed and Lowercased Email
     */
    public static String sanitizeEmail(String email) {
        String sanitizedEmail = email != null ? email.trim().toLowerCase() : null;
        return emptyToNull(sanitizedEmail);
    }

    /**
     * Trim 이후 빈문자열("")인 경우 null 취급
     *
     * @param string
     * @return
     */
    private static String emptyToNull(String string) {
        return (string != null && string.isEmpty()) ? null : string;
    }
}
