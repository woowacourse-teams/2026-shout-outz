package com.shoutoutz.api.feed.recommendation.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.common.llm.LlmProperties;
import com.shoutoutz.api.common.llm.LlmProvider;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class QuestionRecommendationPropertiesConfigurationTest {

    private final QuestionRecommendationPropertiesConfiguration configuration =
            new QuestionRecommendationPropertiesConfiguration();

    @Test
    void SQS가_꺼져있으면_API키_없이_기동할_수_있다() {
        assertThatCode(() -> configuration.questionRecommendationCredentialsValidator(
                properties(false, true, List.of(LlmProvider.GEMINI)), new LlmProperties(null, null, null))
                .afterPropertiesSet())
                .doesNotThrowAnyException();
    }

    @Test
    void 기본_Bedrock_제공자는_API키_없이_기동한다() {
        assertThatCode(() -> configuration.questionRecommendationCredentialsValidator(
                properties(true, true, null), new LlmProperties(null, null, null)).afterPropertiesSet())
                .doesNotThrowAnyException();
    }

    @Test
    void 기본_제공자_순서는_Bedrock_GPT_다음_Claude다() {
        assertThat(properties(true, true, null).llmProviders())
                .containsExactly(LlmProvider.BEDROCK_GPT, LlmProvider.BEDROCK_CLAUDE);
        assertThat(properties(true, false, null).llmProviders()).containsExactly(LlmProvider.BEDROCK_GPT);
    }

    @Test
    void 설정한_제공자에_필요한_API키가_없으면_실패한다() {
        assertThatThrownBy(() -> configuration.questionRecommendationCredentialsValidator(
                properties(true, true, List.of(LlmProvider.GEMINI, LlmProvider.OPENAI)),
                credentials("gemini-key", " ")).afterPropertiesSet())
                .hasMessageContaining("OPENAI를 사용하려면 API 키");
    }

    @Test
    void 자동대체가_꺼지면_첫_제공자의_API키만_확인한다() {
        assertThatCode(() -> configuration.questionRecommendationCredentialsValidator(
                properties(true, false, List.of(LlmProvider.GEMINI, LlmProvider.OPENAI)),
                credentials("gemini-key", null)).afterPropertiesSet())
                .doesNotThrowAnyException();
    }

    @Test
    void 같은_제공자를_두_번_넣으면_실패한다() {
        assertThatThrownBy(() -> properties(true, true, List.of(LlmProvider.BEDROCK_GPT, LlmProvider.BEDROCK_GPT)))
                .hasMessageContaining("같은 제공자");
    }

    private LlmProperties credentials(String geminiKey, String openaiKey) {
        return new LlmProperties(new LlmProperties.Gemini(geminiKey, null, null, null),
                new LlmProperties.OpenAi(openaiKey, null, null, null, null), null);
    }

    private QuestionRecommendationProperties properties(boolean enabled, boolean fallback,
            List<LlmProvider> providers) {
        return new QuestionRecommendationProperties(5, 5, 5, 5000, fallback,
                new QuestionRecommendationProperties.Sqs(enabled, "queue-url", "dlq-url", "ap-northeast-2",
                        1000, 1, Duration.ofMinutes(3)), providers);
    }
}
