package com.shoutoutz.api.jobposting.domain;

/**
 * 역량 매칭이 실패한 이유. 이름이 job_requirements.last_error 에 그대로 저장된다.
 * 외부 장애처럼 다시 하면 될 수 있는 실패만 재시도하고, 다시 해도 같은 실패는 바로 FAILED 로 끝낸다.
 */
public enum MatchingFailure {
    /** 임베딩 호출이나 자료 조회 실패 */
    SEARCH_FAILED(true),
    /** 판정 LLM 호출 실패나 판정 응답이 형식에 맞지 않음 */
    JUDGE_FAILED(true),
    /** 검색에 넘긴 값이 잘못됨 */
    INVALID_QUERY(false),
    /** 분류하지 못한 예외 */
    UNEXPECTED(false),
    /** 마지막 시도 중에 서버가 꺼져 작업 기한이 지난 뒤 다시 가져온 역량. 시도 횟수를 이미 다 써서 검색하지 않는다 */
    ATTEMPTS_EXHAUSTED(false);

    private final boolean retryable;

    MatchingFailure(boolean retryable) {
        this.retryable = retryable;
    }

    public boolean retryable() {
        return retryable;
    }
}
