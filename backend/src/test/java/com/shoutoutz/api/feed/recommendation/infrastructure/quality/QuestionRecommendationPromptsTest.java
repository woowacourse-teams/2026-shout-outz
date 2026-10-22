package com.shoutoutz.api.feed.recommendation.infrastructure.quality;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;

class QuestionRecommendationPromptsTest {

    private final QuestionRecommendationPrompts prompts = new QuestionRecommendationPrompts();

    @Test
    void 생성_프롬프트에_후보_수와_각도_정의와_원글을_넣는다() {
        String prompt = prompts.generation("{\"title\":\"원 제목\"}", 8, 3);

        assertThat(prompt)
                .contains("후보 8개", "최소 3종", "{\"title\":\"원 제목\"}")
                .contains(RecommendationAngle.DEEPEN.name() + ": " + RecommendationAngle.DEEPEN.definition())
                .doesNotContain("{{");
    }

    @Test
    void judge_프롬프트에_모든_체크_항목과_후보를_넣는다() {
        String prompt = prompts.judge("{\"title\":\"원 제목\"}", "[{\"id\":\"c1\"}]");

        for (RecommendationCheck check : RecommendationCheck.values()) {
            assertThat(prompt).contains(check.name() + ": " + check.question());
        }
        assertThat(prompt).contains("[{\"id\":\"c1\"}]").doesNotContain("{{");
    }

    @Test
    void 입력에_섞인_자리표시자는_다시_치환하지_않는다() {
        String prompt = prompts.judge("{\"title\":\"{{candidates}}\"}", "[]");

        assertThat(prompt).contains("{\"title\":\"{{candidates}}\"}");
    }

    @Test
    void 값이_없는_자리표시자는_거부한다() {
        assertThatThrownBy(() -> QuestionRecommendationPrompts.render("{{missing}}", Map.of()))
                .isInstanceOf(IllegalStateException.class);
    }
}
