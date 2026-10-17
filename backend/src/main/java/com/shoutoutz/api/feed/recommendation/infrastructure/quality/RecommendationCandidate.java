package com.shoutoutz.api.feed.recommendation.infrastructure.quality;

import com.shoutoutz.api.feed.recommendation.application.dto.GeneratedQuestion;

/**
 * 생성 단계에서 나온 추천 후보. id는 생성 순서대로 c1, c2, ...로 붙이며 동점일 때의 우선순위로도 쓴다.
 */
public record RecommendationCandidate(String id, int order, RecommendationAngle angle, String title, String content) {

    public RecommendationCandidate {
        if (id == null || id.isBlank() || angle == null || title == null || content == null) {
            throw new IllegalArgumentException("추천 후보의 id, angle, 제목과 본문은 필수입니다.");
        }
    }

    public GeneratedQuestion toGeneratedQuestion() {
        return new GeneratedQuestion(title, content);
    }
}
