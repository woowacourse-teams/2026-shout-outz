package com.shoutoutz.api.feed.recommendation.application;

import com.shoutoutz.api.feed.recommendation.application.dto.GeneratedQuestion;
import java.util.List;

public interface QuestionRecommendationGenerator {

    List<GeneratedQuestion> generate(String title, String content, int count);
}
