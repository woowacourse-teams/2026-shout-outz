package com.shoutoutz.api.project.domain;

import java.util.Locale;
import java.util.Objects;

/**
 * 프로젝트 탭 검색어를 프로젝트 이름과 비교하기 위한 값
 * "자리하나?"와 "자리하나", "Rilog."와 "rilog"처럼 표기만 다른 입력을 같게 보도록 소문자로 바꾸고 글자와 숫자만 남긴다.
 */
public record NormalizedKeyword(String value) {

    public NormalizedKeyword {
        Objects.requireNonNull(value, "정규화한 검색어는 null일 수 없습니다.");
    }

    public static NormalizedKeyword of(String raw) {
        StringBuilder letters = new StringBuilder();
        raw.toLowerCase(Locale.ROOT).codePoints()
                .filter(Character::isLetterOrDigit)
                .forEach(letters::appendCodePoint);
        return new NormalizedKeyword(letters.toString());
    }

    public boolean isEmpty() {
        return value.isEmpty();
    }
}
