package com.shoutoutz.api.feed.recommendation.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class QuestionRecommendationRetryPolicyTest {

    @Test
    void 재시도마다_대기시간이_두배로_증가하고_최대_300초를_넘지_않는다() {
        QuestionRecommendationRetryPolicy policy = new QuestionRecommendationRetryPolicy(bound -> 0);

        assertThat(policy.delaySeconds(1, 0)).isEqualTo(5);
        assertThat(policy.delaySeconds(2, 0)).isEqualTo(10);
        assertThat(policy.delaySeconds(3, 0)).isEqualTo(20);
        assertThat(policy.delaySeconds(4, 0)).isEqualTo(40);
        assertThat(policy.delaySeconds(5, 0)).isEqualTo(80);
        assertThat(policy.delaySeconds(6, 0)).isEqualTo(160);
        assertThat(policy.delaySeconds(7, 0)).isEqualTo(300);
        assertThat(policy.delaySeconds(Integer.MAX_VALUE, 0)).isEqualTo(300);
    }

    @Test
    void 지터는_기본_대기시간의_20퍼센트이며_합계가_300초를_넘지_않는다() {
        QuestionRecommendationRetryPolicy policy = new QuestionRecommendationRetryPolicy(bound -> bound - 1);

        assertThat(policy.delaySeconds(1, 0)).isEqualTo(6);
        assertThat(policy.delaySeconds(3, 0)).isEqualTo(24);
        assertThat(policy.delaySeconds(6, 0)).isEqualTo(192);
        assertThat(policy.delaySeconds(7, 0)).isEqualTo(300);
    }

    @Test
    void 제공자의_대기시간과_백오프_중_큰_값을_선택한다() {
        QuestionRecommendationRetryPolicy policy = new QuestionRecommendationRetryPolicy(bound -> 0);

        assertThat(policy.delaySeconds(1, 120)).isEqualTo(120);
        assertThat(policy.delaySeconds(3, 1)).isEqualTo(20);
        assertThat(policy.delaySeconds(1, -1)).isEqualTo(5);
    }

    @Test
    void 제공자의_대기시간은_SQS의_최대_가시성_시간_12시간으로_제한한다() {
        QuestionRecommendationRetryPolicy policy = new QuestionRecommendationRetryPolicy(bound -> 0);

        assertThat(policy.delaySeconds(1, 53_798)).isEqualTo(43_200);
        assertThat(policy.delaySeconds(1, Long.MAX_VALUE)).isEqualTo(43_200);
    }
}
