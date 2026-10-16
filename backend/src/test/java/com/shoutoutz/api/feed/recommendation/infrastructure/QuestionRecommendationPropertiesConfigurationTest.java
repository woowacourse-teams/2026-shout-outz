package com.shoutoutz.api.feed.recommendation.infrastructure;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.common.llm.LlmProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class QuestionRecommendationPropertiesConfigurationTest {

    private final QuestionRecommendationPropertiesConfiguration configuration =
            new QuestionRecommendationPropertiesConfiguration();

    @Test
    void SQS가_꺼져있으면_API키_없이_기동할_수_있다() {
        assertThatCode(() -> configuration.questionRecommendationCredentialsValidator(
                properties(false, true), new LlmProperties(null, null)).afterPropertiesSet())
                .doesNotThrowAnyException();
    }

    @Test
    void SQS가_켜지면_Gemini_API키가_필요하다() {
        assertThatThrownBy(() -> configuration.questionRecommendationCredentialsValidator(
                properties(true, true), credentials(null, "openai-key")).afterPropertiesSet())
                .hasMessageContaining("Gemini API 키");
    }

    @Test
    void 자동대체가_켜지면_OpenAI_API키도_필요하다() {
        assertThatThrownBy(() -> configuration.questionRecommendationCredentialsValidator(
                properties(true, true), credentials("gemini-key", " ")).afterPropertiesSet())
                .hasMessageContaining("OpenAI API 키");
    }

    @Test
    void 자동대체가_꺼지면_Gemini_API키만으로_기동한다() {
        assertThatCode(() -> configuration.questionRecommendationCredentialsValidator(
                properties(true, false), credentials("gemini-key", null)).afterPropertiesSet())
                .doesNotThrowAnyException();
    }

    @Test
    void 자동대체에_필요한_두_API키가_있으면_기동한다() {
        assertThatCode(() -> configuration.questionRecommendationCredentialsValidator(
                properties(true, true), credentials("gemini-key", "openai-key")).afterPropertiesSet())
                .doesNotThrowAnyException();
    }

    private LlmProperties credentials(String geminiKey, String openaiKey) {
        return new LlmProperties(new LlmProperties.Gemini(geminiKey, null, null, null),
                new LlmProperties.OpenAi(openaiKey, null, null, null, null));
    }

    private QuestionRecommendationProperties properties(boolean enabled, boolean fallback) {
        return new QuestionRecommendationProperties(5, 5, 5, 5000, fallback,
                new QuestionRecommendationProperties.Sqs(enabled, "queue-url", "dlq-url", "ap-northeast-2",
                        1000, 1, Duration.ofMinutes(3)));
    }
}
