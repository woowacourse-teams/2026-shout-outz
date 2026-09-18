package com.shoutoutz.api.visitor.domain;

/**
 * 방문자 식별 쿠키 값을 해시한 저장용 키.
 * 쿠키 원래 값과 섞이지 않도록, 해시한 값은 항상 이 타입으로 주고받는다.
 */
public record VisitorKey(String hash) {

    public VisitorKey {
        if (hash == null || hash.isBlank()) {
            throw new IllegalArgumentException("방문자 키 해시가 없습니다.");
        }
    }
}
