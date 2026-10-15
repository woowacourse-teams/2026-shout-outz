package com.shoutoutz.api.feed.recommendation.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.common.llm.GeminiLlmClient;
import com.shoutoutz.api.common.llm.LlmConfiguration;
import com.shoutoutz.api.common.llm.LlmProperties;
import com.shoutoutz.api.common.llm.OpenAiLlmClient;
import com.shoutoutz.api.feed.recommendation.application.QuestionRecommendationGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

class LlmRecommendationConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(LlmConfiguration.class, QuestionRecommendationPropertiesConfiguration.class,
                    LlmQuestionRecommendationGenerator.class)
            .withBean(RestClient.Builder.class, RestClient::builder)
            .withBean(ObjectMapper.class, ObjectMapper::new);

    @Test
    void 기본_설정은_키_없이_두_클라이언트와_단일_추천_생성기를_연결한다() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed()
                    .hasSingleBean(GeminiLlmClient.class)
                    .hasSingleBean(OpenAiLlmClient.class)
                    .hasSingleBean(QuestionRecommendationGenerator.class);
            assertThat(context.getBean(QuestionRecommendationProperties.class).sqs().enabled()).isFalse();
            assertThat(context.getBean(QuestionRecommendationProperties.class).fallbackEnabled()).isTrue();
            assertThat(context.getBean(LlmProperties.class).openai().reasoningEffort()).isEqualTo("none");
        });
    }

    @Test
    void SQS와_자동대체를_활성화하면_llm_경로의_두_키를_바인딩한다() {
        enabledContext().withPropertyValues("llm.gemini.api-key=test-gemini", "llm.openai.api-key=test-openai",
                "llm.openai.model=custom-model", "llm.openai.reasoning-effort=low")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    LlmProperties properties = context.getBean(LlmProperties.class);
                    assertThat(properties.gemini().apiKey()).isEqualTo("test-gemini");
                    assertThat(properties.openai().apiKey()).isEqualTo("test-openai");
                    assertThat(properties.openai().model()).isEqualTo("custom-model");
                    assertThat(properties.openai().reasoningEffort()).isEqualTo("low");
                });
    }

    @Test
    void SQS_활성화_시_OpenAI키_누락은_컨텍스트_기동을_실패시킨다() {
        enabledContext().withPropertyValues("llm.gemini.api-key=test-gemini")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().hasRootCauseMessage("질문 추천 자동 대체를 활성화하려면 OpenAI API 키를 설정해야 합니다."));
    }

    @Test
    void 제공자_총_제한시간이_메시지_가시성시간을_넘으면_기동에_실패한다() {
        enabledContext().withPropertyValues("llm.gemini.api-key=test-gemini", "llm.openai.api-key=test-openai",
                "question-recommendation.sqs.visibility-timeout=90s")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().hasRootCauseMessage("LLM 총 제한 시간은 작업 선점 및 SQS 가시성 시간보다 짧아야 합니다."));
    }

    @Test
    void 작업_선점시간보다_긴_제한시간도_거부한다() {
        enabledContext().withPropertyValues("llm.gemini.api-key=test-gemini", "llm.openai.api-key=test-openai",
                "question-recommendation.sqs.visibility-timeout=10m", "llm.openai.read-timeout=150s")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().hasRootCauseMessage("LLM 총 제한 시간은 작업 선점 및 SQS 가시성 시간보다 짧아야 합니다."));
    }

    private ApplicationContextRunner enabledContext() {
        return contextRunner.withPropertyValues(
                "question-recommendation.sqs.enabled=true",
                "question-recommendation.sqs.queue-url=test-queue",
                "question-recommendation.sqs.dead-letter-queue-url=test-dlq");
    }
}
