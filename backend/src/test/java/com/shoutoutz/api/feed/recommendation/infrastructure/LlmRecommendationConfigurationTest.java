package com.shoutoutz.api.feed.recommendation.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.common.llm.BedrockLlmClient;
import com.shoutoutz.api.common.llm.GeminiLlmClient;
import com.shoutoutz.api.common.llm.LlmConfiguration;
import com.shoutoutz.api.common.llm.LlmProperties;
import com.shoutoutz.api.common.llm.LlmProvider;
import com.shoutoutz.api.common.llm.OpenAiLlmClient;
import com.shoutoutz.api.feed.recommendation.application.QuestionRecommendationGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import tools.jackson.databind.ObjectMapper;

class LlmRecommendationConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(LlmConfiguration.class, QuestionRecommendationPropertiesConfiguration.class,
                    LlmQuestionRecommendationGenerator.class)
            .withBean(RestClient.Builder.class, RestClient::builder)
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withBean(AwsCredentialsProvider.class, AnonymousCredentialsProvider::create);

    @Test
    void 기본_설정은_키_없이_모든_클라이언트와_단일_추천_생성기를_연결한다() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed()
                    .hasSingleBean(GeminiLlmClient.class)
                    .hasSingleBean(OpenAiLlmClient.class)
                    .hasSingleBean(QuestionRecommendationGenerator.class);
            assertThat(context.getBeansOfType(BedrockLlmClient.class)).hasSize(2);
            QuestionRecommendationProperties properties = context.getBean(QuestionRecommendationProperties.class);
            assertThat(properties.sqs().enabled()).isFalse();
            assertThat(properties.llmProviders()).containsExactly(LlmProvider.BEDROCK_GPT, LlmProvider.BEDROCK_CLAUDE);
            LlmProperties llm = context.getBean(LlmProperties.class);
            assertThat(llm.openai().reasoningEffort()).isEqualTo("none");
            assertThat(llm.bedrock().gptModel()).isEqualTo("global.openai.gpt-6.1-sol");
            assertThat(llm.bedrock().claudeModel()).isEqualTo("global.anthropic.claude-sonnet-5-5");
        });
    }

    @Test
    void SQS를_활성화해도_Bedrock_기본_제공자는_키_없이_기동한다() {
        enabledContext().run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void 제공자_순서와_Bedrock_모델을_설정으로_바꿀_수_있다() {
        enabledContext().withPropertyValues("question-recommendation.providers=BEDROCK_CLAUDE,GEMINI",
                        "llm.gemini.api-key=test-gemini", "llm.bedrock.claude-model=custom-claude",
                        "llm.openai.reasoning-effort=low")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(QuestionRecommendationProperties.class).llmProviders())
                            .containsExactly(LlmProvider.BEDROCK_CLAUDE, LlmProvider.GEMINI);
                    LlmProperties properties = context.getBean(LlmProperties.class);
                    assertThat(properties.gemini().apiKey()).isEqualTo("test-gemini");
                    assertThat(properties.bedrock().claudeModel()).isEqualTo("custom-claude");
                    assertThat(properties.openai().reasoningEffort()).isEqualTo("low");
                });
    }

    @Test
    void 외부_제공자를_설정했는데_키가_없으면_기동에_실패한다() {
        enabledContext().withPropertyValues("question-recommendation.providers=GEMINI,OPENAI",
                        "llm.gemini.api-key=test-gemini")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().hasRootCauseMessage("질문 추천 제공자 OPENAI를 사용하려면 API 키를 설정해야 합니다."));
    }

    @Test
    void 제공자_총_제한시간이_메시지_가시성시간을_넘으면_기동에_실패한다() {
        enabledContext().withPropertyValues("question-recommendation.sqs.visibility-timeout=90s")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().hasRootCauseMessage("LLM 총 제한 시간은 작업 선점 및 SQS 가시성 시간보다 짧아야 합니다."));
    }

    @Test
    void 작업_선점시간보다_긴_제한시간도_거부한다() {
        enabledContext().withPropertyValues("question-recommendation.sqs.visibility-timeout=10m",
                        "llm.bedrock.read-timeout=90s")
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
