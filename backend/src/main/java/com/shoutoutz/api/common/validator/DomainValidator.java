package com.shoutoutz.api.common.validator;

import com.shoutoutz.api.common.exception.code.ErrorCode;
import com.shoutoutz.api.common.exception.custom.DomainValidationException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * 도메인 검증에서 공통으로 사용하는 원자적 검증 유틸리티.
 *
 * 상태를 갖지 않는 정적 메서드만 제공하며, 구체적인 도메인 검증기는 필요한 메서드를 static import하여 도메인별 검증 규칙을 구성한다.
 */
public final class DomainValidator {
    private DomainValidator() {}

    /** null/empty/blank 검증 */
    public static void validateNotNullOrBlank(String value, ErrorCode errorCode) {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException(errorCode);
        }
    }

    /** null이 가능한데, 값이 있는 경우에는 Blank인지 Check */
    public static void validateNotBlank(String value, ErrorCode errorCode) {
        if (value != null && value.isBlank()) {
            throw new DomainValidationException(errorCode);
        }
    }

    /** null 검증 */
    public static void validateNotNull(Object value, ErrorCode errorCode) {
        if (value == null) {
            throw new DomainValidationException(errorCode);
        }
    }

    /** 최소 길이 검증 */
    public static void validateMinLength(String value, int minLength, ErrorCode errorCode) {
        if (value.codePointCount(0, value.length()) < minLength) {
            throw new DomainValidationException(errorCode);
        }
    }

    /** 최대 길이 검증 */
    public static void validateMaxLength(String value, int maxLength, ErrorCode errorCode) {
        if (value.codePointCount(0, value.length()) > maxLength) {
            throw new DomainValidationException(errorCode);
        }
    }

    /** 정확한 길이 검증 */
    public static void validateExactLength(String value, int exactLength, ErrorCode errorCode) {
        if (value.length() != exactLength) {
            throw new DomainValidationException(errorCode);
        }
    }

    public static void validateExactLength(int count, int exactLength, ErrorCode errorCode) {
        if (count != exactLength) {
            throw new DomainValidationException(errorCode);
        }
    }

    /** 길이 범위 검증 */
    public static void validateLengthRange(
            String value, int minLength, int maxLength, ErrorCode errorCode) {
        if (value.length() < minLength || value.length() > maxLength) {
            throw new DomainValidationException(errorCode);
        }
    }

    /** 패턴 검증 */
    public static void validatePattern(String value, Pattern pattern, ErrorCode errorCode) {
        if (!pattern.matcher(value).matches()) {
            throw new DomainValidationException(errorCode);
        }
    }

    /** URL 프로토콜 검증 */
    public static void validateUrlProtocol(String url, String protocol, ErrorCode errorCode) {
        if (!url.startsWith(protocol)) {
            throw new DomainValidationException(errorCode);
        }
    }

    /** 특정 문자 포함 여부 검증 */
    public static void validateContains(
            String value, String requiredSubstring, ErrorCode errorCode) {
        if (!value.contains(requiredSubstring)) {
            throw new DomainValidationException(errorCode);
        }
    }

    /** 유효한 시간 범위 검증 */
    public static void validateDateRange(
            LocalDateTime startDate, LocalDateTime endDate, ErrorCode errorCode) {
        // null 값에 대해 범위체크 생략
        if (startDate == null || endDate == null) return;
        if (startDate.isAfter(endDate)) {
            throw new DomainValidationException(errorCode);
        }
    }

    /** BigDecimal 범위 검증 */
    public static void validateBigDecimalRange(
            BigDecimal value, BigDecimal minValue, BigDecimal maxValue, ErrorCode errorCode) {
        if (value.compareTo(minValue) < 0 || value.compareTo(maxValue) > 0) {
            throw new DomainValidationException(errorCode);
        }
    }

    /** int 범위 검증 */
    public static void validateIntRange(
            int value, int minValue, int maxValue, ErrorCode errorCode) {
        if (value < minValue || value > maxValue) {
            throw new DomainValidationException(errorCode);
        }
    }

    public static void validateLongRange(
            long value, long minValue, long maxValue, ErrorCode errorCode) {
        if (value < minValue || value > maxValue) {
            throw new DomainValidationException(errorCode);
        }
    }

    /** long 최소 사이즈 검증 */
    public static void validateLongMinSize(
            long value, long minValue, ErrorCode errorCode) {
        if (value < minValue) {
            throw new DomainValidationException(errorCode);
        }
    }

    /** BigDecimal 소수점 자리수 검증 */
    public static void validateBigDecimalScale(
            BigDecimal value, int maxScale, ErrorCode errorCode) {
        if (value.scale() > maxScale) {
            throw new DomainValidationException(errorCode);
        }
    }
}
