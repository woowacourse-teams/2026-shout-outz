package com.shoutoutz.api.project.domain;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;
import java.util.zip.CRC32;

/**
 * 프로젝트 탭 검색어를 프로젝트 이름과 비교하기 위한 값
 * "자리하나?"와 "자리하나", "Rilog."와 "rilog"처럼 표기만 다른 입력을 같게 보도록 소문자로 바꾸고 글자와 숫자만 남긴다.
 * 맥에서 복사한 한글은 자모로 분해된(NFD) 경우가 있어, 먼저 조합형(NFC)으로 맞춘다.
 */
public record NormalizedKeyword(String value) {

    private static final int NEAR_NAME_MINIMUM_SYLLABLES = 3;
    private static final char HANGUL_FIRST = '\uAC00';
    private static final char HANGUL_LAST = '\uD7A3';
    // 한글 음절 코드는 (초성, 중성) 조합마다 받침 없음을 포함해 28개씩 이어진다.
    private static final int FINAL_CONSONANT_COUNT = 28;

    public NormalizedKeyword {
        Objects.requireNonNull(value, "정규화한 검색어는 null일 수 없습니다.");
    }

    public static NormalizedKeyword of(String raw) {
        StringBuilder letters = new StringBuilder();
        Normalizer.normalize(raw, Normalizer.Form.NFC).toLowerCase(Locale.ROOT).codePoints()
                .filter(Character::isLetterOrDigit)
                .forEach(letters::appendCodePoint);
        return new NormalizedKeyword(letters.toString());
    }

    /**
     * 순위 목록 커서가 어느 검색어의 결과인지 확인하는 짧은 지문. 검색어 원문을 커서에 남기지 않으려고 해시를 쓴다.
     */
    public String fingerprint() {
        CRC32 checksum = new CRC32();
        checksum.update(value.getBytes(StandardCharsets.UTF_8));
        return String.format("%08x", checksum.getValue());
    }

    public boolean isEmpty() {
        return value.isEmpty();
    }

    /**
     * 한글 이름의 받침 하나를 다르게 적은 검색어("샤라웃"과 "샤라웆")를 받아 주기 위한 비교.
     * 둘 다 세 글자 이상의 한글이고 글자 수가 같으며, 한 글자의 받침만 다르면(바뀌거나 빠지거나 더해지면) 가까운 표기다.
     * 초성이나 모음까지 허용하면 "레벨업"이 "데벨업"을 가리키고, 두 글자 이름은 "달록"과 "달력"처럼 다른 낱말과 겹쳐 제외한다.
     */
    public boolean isOneFinalConsonantApart(NormalizedKeyword other) {
        if (!isHangulName() || !other.isHangulName() || value.length() != other.value.length()) {
            return false;
        }
        int differentFinals = 0;
        for (int index = 0; index < value.length(); index++) {
            int syllable = value.charAt(index) - HANGUL_FIRST;
            int otherSyllable = other.value.charAt(index) - HANGUL_FIRST;
            if (syllable / FINAL_CONSONANT_COUNT != otherSyllable / FINAL_CONSONANT_COUNT) {
                return false;
            }
            if (syllable != otherSyllable) {
                differentFinals++;
            }
        }
        return differentFinals == 1;
    }

    private boolean isHangulName() {
        return value.length() >= NEAR_NAME_MINIMUM_SYLLABLES
                && value.chars().allMatch(letter -> letter >= HANGUL_FIRST && letter <= HANGUL_LAST);
    }
}
