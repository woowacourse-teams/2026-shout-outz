package com.shoutoutz.api.feed.recommendation.application;

/**
 * 원글이 추천에 적합하지 않아 생성을 건너뛰었음을 알린다. 재시도하지 않는 종료 상태로 저장한다.
 */
public class QuestionRecommendationSkippedException extends RuntimeException {

    private final String reason;

    public QuestionRecommendationSkippedException(String reason) {
        super("질문 추천 생성을 건너뛰었습니다.");
        this.reason = reason;
    }

    public String reason() {
        return reason;
    }
}
