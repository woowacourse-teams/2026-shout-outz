package com.shoutoutz.api.feed.recommendation.application;

import com.shoutoutz.api.feed.recommendation.application.dto.GeneratedQuestion;
import java.util.List;

public interface QuestionRecommendationGenerator {

    default List<GeneratedQuestion> generate(String title, String content, int count) {
        return generate(title, content, count, List.of());
    }

    List<GeneratedQuestion> generate(String title, String content, int count, List<String> excludedTitles);
}
