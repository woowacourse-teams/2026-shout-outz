package com.shoutoutz.api.feed.recommendation.infrastructure;

import com.shoutoutz.api.common.llm.LlmProperties;
import com.shoutoutz.api.common.llm.LlmProvider;
import com.shoutoutz.api.feed.recommendation.application.QuestionRecommendationWorker;
import java.time.Duration;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(QuestionRecommendationProperties.class)
public class QuestionRecommendationPropertiesConfiguration {

    @Bean
    InitializingBean questionRecommendationCredentialsValidator(
            QuestionRecommendationProperties properties,
            LlmProperties llmProperties
    ) {
        return () -> {
            if (!properties.sqs().enabled()) {
                return;
            }
            Duration budget = Duration.ZERO;
            for (LlmProvider provider : properties.llmProviders()) {
                if (!llmProperties.hasCredentials(provider)) {
                    throw new IllegalArgumentException(
                            "질문 추천 제공자 " + provider + "를 사용하려면 API 키를 설정해야 합니다.");
                }
                budget = budget.plus(llmProperties.timeout(provider));
            }
            if (budget.compareTo(Duration.ofSeconds(QuestionRecommendationWorker.GENERATION_LEASE_SECONDS)) >= 0
                    || budget.compareTo(properties.sqs().visibilityTimeout()) >= 0) {
                throw new IllegalArgumentException("LLM 총 제한 시간은 작업 선점 및 SQS 가시성 시간보다 짧아야 합니다.");
            }
        };
    }
}
