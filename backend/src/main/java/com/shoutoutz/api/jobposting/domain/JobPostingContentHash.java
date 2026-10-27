package com.shoutoutz.api.jobposting.domain;

import static com.shoutoutz.api.common.validator.DomainValidator.validateNotNull;
import static com.shoutoutz.api.common.validator.DomainValidator.validateNotNullOrBlank;
import static com.shoutoutz.api.common.validator.DomainValidator.validatePattern;
import static com.shoutoutz.api.jobposting.domain.JobPostingErrorCode.JOB_POSTING_CONTENT_NULL_OR_BLANK;
import static com.shoutoutz.api.jobposting.domain.JobPostingErrorCode.JOB_POSTING_INVALID_CONTENT_HASH;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Pattern;

/**
 * 채용공고 원문의 SHA-256 해시
 * 원문은 저작권 위험 때문에 저장하지 않고, 같은 내용인지 판별할 이 값만 남긴다.
 * 복사할 때마다 달라지는 공백 차이로 해시가 바뀌지 않도록, 연속 공백과 줄바꿈을 공백 하나로 모은 뒤 해시를 만든다.
 */
public record JobPostingContentHash(String value) {

    private static final Pattern HEX_PATTERN = Pattern.compile("^[0-9a-f]{64}$");
    // UNICODE_CHARACTER_CLASS 를 켜서 웹 페이지에서 복사할 때 섞이는 줄바꿈 없는 공백(U+00A0)도 공백으로 본다.
    private static final Pattern WHITESPACES = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);
    private static final String ALGORITHM = "SHA-256";

    /**
     * 저장된 해시 값을 다시 만들 때 쓴다.
     */
    public JobPostingContentHash {
        validateNotNull(value, JOB_POSTING_INVALID_CONTENT_HASH);
        validatePattern(value, HEX_PATTERN, JOB_POSTING_INVALID_CONTENT_HASH);
    }

    public static JobPostingContentHash from(String content) {
        validateNotNullOrBlank(content, JOB_POSTING_CONTENT_NULL_OR_BLANK);
        String normalized = WHITESPACES.matcher(content).replaceAll(" ").strip();
        return new JobPostingContentHash(sha256Hex(normalized));
    }

    private static String sha256Hex(String content) {
        try {
            byte[] digest = MessageDigest.getInstance(ALGORITHM)
                    .digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // 모든 JVM 구현은 SHA-256 을 제공해야 하므로 여기에 오지 않는다.
            throw new IllegalStateException(ALGORITHM + " 알고리즘을 찾을 수 없습니다.", e);
        }
    }
}
