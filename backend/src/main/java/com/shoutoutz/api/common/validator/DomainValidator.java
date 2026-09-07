package com.shoutoutz.api.common.validator;

import com.shoutoutz.api.common.exception.code.ErrorCode;
import com.shoutoutz.api.common.exception.custom.DomainValidationException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * 도메인 Validator 추상 클래스
 * 모든 도메인 Validator가 상속받아 원자적 검증 단위를 조합하여 사용해야 한다.
 *
 * 도메인 검증 구현 컨벤션
 * 1. 각 도메인 Validator는 이 클래스를 상속받아야 함
 * 2. validate() 메소드를 static으로 구현
 * 3. 원자적 검증 메소드들을 조합하여 도메인별 검증 구성
 * 4. 구체적 검증 로직은 각 도메인에서 조합하여 구현
 */
public abstract class DomainValidator {
    // 생성자 protected 제한: 자식 클래스에서만 컴파일시 super() 호출 가능. 외부 인스턴스화 방지
    protected DomainValidator() {}

    /** null/empty/blank 검증 */
    protected static void validateNotNullOrBlank(String value, ErrorCode errorCode) {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException(errorCode);
        }
    }

    /** null이 가능한데, 값이 있는 경우에는 Blank인지 Check */
    protected static void validateNotBlank(String value, ErrorCode errorCode) {
        if (value != null && value.isBlank()) {
            throw new DomainValidationException(errorCode);
        }
    }

    /** null 검증 */
    protected static void validateNotNull(Object value, ErrorCode errorCode) {
        if (value == null) {
            throw new DomainValidationException(errorCode);
        }
    }

    /** 최소 길이 검증 */
    protected static void validateMinLength(String value, int minLength, ErrorCode errorCode) {
        if (value.codePointCount(0, value.length()) < minLength) {
            throw new DomainValidationException(errorCode);
        }
    }

    /** 최대 길이 검증 */
    protected static void validateMaxLength(String value, int maxLength, ErrorCode errorCode) {
        if (value.codePointCount(0, value.length()) > maxLength) {
            throw new DomainValidationException(errorCode);
        }
    }

    /** 정확한 길이 검증 */
    protected static void validateExactLength(String value, int exactLength, ErrorCode errorCode) {
        if (value.length() != exactLength) {
            throw new DomainValidationException(errorCode);
        }
    }

    protected static void validateExactLength(int count, int exactLength, ErrorCode errorCode) {
        if (count != exactLength) {
            throw new DomainValidationException(errorCode);
        }
    }

    /** 길이 범위 검증 */
    protected static void validateLengthRange(
            String value, int minLength, int maxLength, ErrorCode errorCode) {
        if (value.length() < minLength || value.length() > maxLength) {
            throw new DomainValidationException(errorCode);
        }
    }

    /** 패턴 검증 */
    protected static void validatePattern(String value, Pattern pattern, ErrorCode errorCode) {
        if (!pattern.matcher(value).matches()) {
            throw new DomainValidationException(errorCode);
        }
    }

    /** URL 프로토콜 검증 */
    protected static void validateUrlProtocol(String url, String protocol, ErrorCode errorCode) {
        if (!url.startsWith(protocol)) {
            throw new DomainValidationException(errorCode);
        }
    }

    /** 특정 문자 포함 여부 검증 */
    protected static void validateContains(
            String value, String requiredSubstring, ErrorCode errorCode) {
        if (!value.contains(requiredSubstring)) {
            throw new DomainValidationException(errorCode);
        }
    }

    /** 유효한 시간 범위 검증 */
    protected static void validateDateRange(
            LocalDateTime startDate, LocalDateTime endDate, ErrorCode errorCode) {
        // null 값에 대해 범위체크 생략
        if (startDate == null || endDate == null) return;
        if (startDate.isAfter(endDate)) {
            throw new DomainValidationException(errorCode);
        }
    }

    /** BigDecimal 범위 검증 */
    protected static void validateBigDecimalRange(
            BigDecimal value, BigDecimal minValue, BigDecimal maxValue, ErrorCode errorCode) {
        if (value.compareTo(minValue) < 0 || value.compareTo(maxValue) > 0) {
            throw new DomainValidationException(errorCode);
        }
    }

    /** int 범위 검증 */
    protected static void validateIntRange(
            int value, int minValue, int maxValue, ErrorCode errorCode) {
        if (value < minValue || value > maxValue) {
            throw new DomainValidationException(errorCode);
        }
    }

    protected static void validateLongRange(
            long value, long minValue, long maxValue, ErrorCode errorCode) {
        if (value < minValue || value > maxValue) {
            throw new DomainValidationException(errorCode);
        }
    }

    /** long 최소 사이즈 검증 */
    protected static void validateLongMinSize(
            long value, long minValue, ErrorCode errorCode) {
        if (value < minValue) {
            throw new DomainValidationException(errorCode);
        }
    }

    /** BigDecimal 소수점 자리수 검증 */
    protected static void validateBigDecimalScale(
            BigDecimal value, int maxScale, ErrorCode errorCode) {
        if (value.scale() > maxScale) {
            throw new DomainValidationException(errorCode);
        }
    }
}
