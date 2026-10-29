package com.shoutoutz.api.common.embedding;

public class EmbeddingException extends RuntimeException {
    private final Integer httpStatus;
    private final String errorCode;
    private final boolean retryable;

    public EmbeddingException(Integer httpStatus, String errorCode, boolean retryable) {
        // 원본 예외에는 인증 헤더나 입력 문장이 있을 수 있어 cause로 보관하지 않는다.
        super("임베딩 요청 실패: httpStatus=" + httpStatus + ", errorCode=" + safeCode(errorCode));
        this.httpStatus = httpStatus;
        this.errorCode = safeCode(errorCode);
        this.retryable = retryable;
    }

    static String safeCode(String code) {
        return code != null && code.matches("[A-Za-z0-9_]{1,64}") ? code : "UNKNOWN";
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
}
