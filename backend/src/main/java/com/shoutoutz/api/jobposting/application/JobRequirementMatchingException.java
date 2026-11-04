package com.shoutoutz.api.jobposting.application;

import com.shoutoutz.api.jobposting.domain.MatchingFailure;
import java.util.Objects;

/**
 * 검색이나 판정 구현이 실패를 분류해 Worker 에 알린다. 분류하지 못한 예외는 Worker 가 UNEXPECTED 로 본다.
 */
public class JobRequirementMatchingException extends RuntimeException {

    private final MatchingFailure failure;

    public JobRequirementMatchingException(MatchingFailure failure, Throwable cause) {
        super("역량 매칭에 실패했습니다. failure=" + failure, cause);
        this.failure = Objects.requireNonNull(failure);
    }

    public MatchingFailure failure() {
        return failure;
    }
}
