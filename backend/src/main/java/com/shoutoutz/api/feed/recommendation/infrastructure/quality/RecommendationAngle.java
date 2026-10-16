package com.shoutoutz.api.feed.recommendation.infrastructure.quality;

/**
 * 추천 질문이 원글과 연결되는 방식. 후보 묶음의 다양성을 객관적으로 검사하기 위해 생성 단계에서 태그로 받는다.
 */
public enum RecommendationAngle {
    DEEPEN("원글과 같은 문제를 더 깊이 파고든다"),
    ADJACENT("원글의 문제를 풀다 보면 마주치는 옆 주제다"),
    COMPARE("관련 선택지 사이의 트레이드오프와 선택 기준을 묻는다"),
    PRACTICE("실무 적용, 운영, 협업, 학습처럼 경험으로 답한다");

    private final String definition;

    RecommendationAngle(String definition) {
        this.definition = definition;
    }

    public String definition() {
        return definition;
    }
}
