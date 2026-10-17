package com.shoutoutz.api.feed.recommendation.infrastructure.quality;

/**
 * 결정적 규칙 검증에서 후보를 탈락시킨 사유. 선언 순서대로 검사하며 처음 걸린 사유 하나만 기록한다.
 */
public enum RuleViolation {
    LENGTH,
    SOURCE_REFERENCE,
    FORBIDDEN_TOKEN,
    NOT_KOREAN,
    SOURCE_SIMILAR
}
