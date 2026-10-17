package com.shoutoutz.api.feed.recommendation.infrastructure;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.common.llm.LlmProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class QuestionRecommendationPropertiesConfigurationTest {

    private static final QuestionRecommendationQualityProperties QUALITY =
            new QuestionRecommendationQualityProperties(8, 3, 3, null);

    private final QuestionRecommendationPropertiesConfiguration configuration =
            new QuestionRecommendationPropertiesConfiguration();

    @Test
    void SQS가_꺼져있으면_API키_없이_기동할_수_있다() {
        assertThatCode(() -> configuration.questionRecommendationCredentialsValidator(
                properties(false, true), QUALITY, new LlmProperties(null, null)).afterPropertiesSet())
                .doesNotThrowAnyException();
    }

    @Test
    void SQS가_켜지면_Gemini_API키가_필요하다() {
        assertThatThrownBy(() -> configuration.questionRecommendationCredentialsValidator(
                properties(true, true), QUALITY, credentials(null, "openai-key")).afterPropertiesSet())
                .hasMessageContaining("Gemini API 키");
    }

    @Test
    void 교차_검수를_위해_OpenAI_API키도_필요하다() {
        assertThatThrownBy(() -> configuration.questionRecommendationCredentialsValidator(
                properties(true, true), QUALITY, credentials("gemini-key", " ")).afterPropertiesSet())
                .hasMessageContaining("OpenAI API 키");
    }

    @Test
    void 자동대체가_꺼져도_교차_검수를_위해_OpenAI_API키가_필요하다() {
        assertThatThrownBy(() -> configuration.questionRecommendationCredentialsValidator(
                properties(true, false), QUALITY, credentials("gemini-key", null)).afterPropertiesSet())
                .hasMessageContaining("OpenAI API 키");
    }

    @Test
    void 생성과_judge_대체를_합친_최악_시간이_선점_시간을_넘으면_기동하지_않는다() {
        LlmProperties slow = new LlmProperties(
                new LlmProperties.Gemini("gemini-key", null, Duration.ofSeconds(5), Duration.ofSeconds(65)),
                new LlmProperties.OpenAi("openai-key", null, Duration.ofSeconds(5), Duration.ofSeconds(45), null));

        // Gemini 70 + OpenAI 50 + 느린 쪽 70 = 190초. judge 대체 때문에 생성 대체가 꺼져 있어도 같다.
        assertThatThrownBy(() -> configuration.questionRecommendationCredentialsValidator(
                properties(true, true), QUALITY, slow).afterPropertiesSet())
                .hasMessageContaining("LLM 총 제한 시간");
        assertThatThrownBy(() -> configuration.questionRecommendationCredentialsValidator(
                properties(true, false), QUALITY, slow).afterPropertiesSet())
                .hasMessageContaining("LLM 총 제한 시간");
    }

    @Test
    void 기본_제한시간이면_최악_150초로_기동한다() {
        assertThatCode(() -> configuration.questionRecommendationCredentialsValidator(
                properties(true, false), QUALITY, credentials("gemini-key", "openai-key")).afterPropertiesSet())
                .doesNotThrowAnyException();
    }

    @Test
    void 추천_개수는_최소_개수와_후보_수_사이여야_한다() {
        assertThatThrownBy(() -> configuration.questionRecommendationCredentialsValidator(
                properties(false, true), new QuestionRecommendationQualityProperties(4, 3, 3, null),
                new LlmProperties(null, null)).afterPropertiesSet())
                .hasMessageContaining("candidate-count");
    }

    @Test
    void 자동대체에_필요한_두_API키가_있으면_기동한다() {
        assertThatCode(() -> configuration.questionRecommendationCredentialsValidator(
                properties(true, true), QUALITY, credentials("gemini-key", "openai-key")).afterPropertiesSet())
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
