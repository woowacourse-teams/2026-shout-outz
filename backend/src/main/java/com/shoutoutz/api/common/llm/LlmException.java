package com.shoutoutz.api.common.llm;

public class LlmException extends RuntimeException {
    private final LlmProvider provider;
    private final String model;
    private final Integer httpStatus;
    private final String errorCode;
    private final boolean retryable;
    private final long retryAfterSeconds;

    public LlmException(LlmProvider provider, String model, Integer httpStatus, String errorCode,
            boolean retryable, long retryAfterSeconds) {
        // 원본 예외에는 인증 헤더, 입력 또는 응답 본문이 있을 수 있어 cause로 보관하지 않는다.
        super("LLM 요청 실패: provider=" + provider + ", httpStatus=" + httpStatus
                + ", errorCode=" + safeCode(errorCode));
        this.provider = provider;
        this.model = model;
        this.httpStatus = httpStatus;
        this.errorCode = safeCode(errorCode);
        this.retryable = retryable;
        this.retryAfterSeconds = Math.max(0, retryAfterSeconds);
    }

    static String safeCode(String code) {
        return code != null && code.matches("[A-Za-z0-9_]{1,64}") ? code : "UNKNOWN";
    }

    public LlmProvider provider() {
        return provider;
    }

    public String model() {
        return model;
    }

    public Integer httpStatus() {
        return httpStatus;
    }

    public String errorCode() {
        return errorCode;
    }

    public boolean retryable() {
        return retryable;
    }

    public long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
