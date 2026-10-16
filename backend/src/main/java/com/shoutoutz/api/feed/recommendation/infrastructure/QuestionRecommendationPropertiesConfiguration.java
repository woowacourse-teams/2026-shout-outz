package com.shoutoutz.api.feed.recommendation.infrastructure;

import com.shoutoutz.api.common.llm.LlmProperties;
import com.shoutoutz.api.feed.recommendation.application.QuestionRecommendationWorker;
import java.time.Duration;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({QuestionRecommendationProperties.class, QuestionRecommendationQualityProperties.class})
public class QuestionRecommendationPropertiesConfiguration {

    @Bean
    InitializingBean questionRecommendationCredentialsValidator(
            QuestionRecommendationProperties properties,
            QuestionRecommendationQualityProperties quality,
            LlmProperties llmProperties
    ) {
        return () -> {
            if (quality.minCount() > properties.count() || properties.count() > quality.candidateCount()) {
                throw new IllegalArgumentException(
                        "질문 추천 개수는 quality.min-count 이상 quality.candidate-count 이하여야 합니다.");
            }
            if (!properties.sqs().enabled()) {
                return;
            }
            if (isBlank(llmProperties.gemini().apiKey())) {
                throw new IllegalArgumentException("SQS 질문 추천을 활성화하려면 Gemini API 키를 설정해야 합니다.");
            }
            if (isBlank(llmProperties.openai().apiKey())) {
                throw new IllegalArgumentException("질문 추천 교차 검수를 위해 OpenAI API 키를 설정해야 합니다.");
            }
            Duration gemini = llmProperties.gemini().connectTimeout().plus(llmProperties.gemini().readTimeout());
            Duration openai = llmProperties.openai().connectTimeout().plus(llmProperties.openai().readTimeout());
            // 최악의 경우: Gemini 생성 실패 → OpenAI 생성 → Gemini judge. 대체가 없으면 Gemini 생성 → OpenAI judge.
            Duration budget = gemini.plus(openai);
            if (properties.fallbackEnabled()) {
                budget = budget.plus(gemini);
            }
            if (budget.compareTo(Duration.ofSeconds(QuestionRecommendationWorker.GENERATION_LEASE_SECONDS)) >= 0
                    || budget.compareTo(properties.sqs().visibilityTimeout()) >= 0) {
                throw new IllegalArgumentException("LLM 총 제한 시간은 작업 선점 및 SQS 가시성 시간보다 짧아야 합니다.");
            }
        };
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
