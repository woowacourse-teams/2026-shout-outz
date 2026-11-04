package com.shoutoutz.api.jobposting.infrastructure;

import com.shoutoutz.api.common.embedding.EmbeddingProperties;
import com.shoutoutz.api.common.llm.LlmProperties;
import com.shoutoutz.api.common.llm.LlmProvider;
import com.shoutoutz.api.jobposting.application.JobRequirementMatchingRepository;
import java.time.Duration;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 역량 매칭 Worker 를 켰을 때만 설정을 읽는다. Worker, 재시도 정책, 스케줄러, 검색, 판정도 같은 조건으로 뜬다.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "job-posting.matching", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(JobRequirementMatchingProperties.class)
public class JobRequirementMatchingConfiguration {

    /**
     * 켠 뒤 역량마다 실패하고 나서야 알게 되지 않도록, 키 누락과 제한 시간 초과를 기동할 때 막는다.
     * 제한 시간의 합이 lease 를 넘으면 처리 중인 역량을 다른 Worker 가 다시 가져갈 수 있다.
     * 현재 임베딩 모델로 만든 자료 조각이 없으면 모든 역량이 추천 없이 완료로 끝나 되돌릴 수 없어서 함께 막는다.
     */
    @Bean
    InitializingBean jobRequirementMatchingSettingsValidator(
            JobRequirementMatchingProperties properties,
            LlmProperties llmProperties,
            EmbeddingProperties embeddingProperties,
            JobRequirementMatchingRepository repository
    ) {
        return () -> {
            if (embeddingProperties.apiKey() == null || embeddingProperties.apiKey().isBlank()) {
                throw new IllegalArgumentException("역량 매칭을 켜려면 임베딩 API 키를 설정해야 합니다.");
            }
            Duration budget = embeddingProperties.connectTimeout().plus(embeddingProperties.readTimeout());
            for (LlmProvider provider : properties.providers()) {
                if (!llmProperties.hasCredentials(provider)) {
                    throw new IllegalArgumentException("역량 판정 제공자 " + provider + "를 사용하려면 API 키를 설정해야 합니다.");
                }
                budget = budget.plus(llmProperties.timeout(provider));
            }
            if (budget.compareTo(properties.lease()) >= 0) {
                throw new IllegalArgumentException("임베딩과 판정 제공자의 제한 시간 합은 job-posting.matching.lease보다 짧아야 합니다.");
            }
            if (!repository.hasSearchableChunks(embeddingProperties.label())) {
                throw new IllegalArgumentException("역량 매칭을 켜려면 임베딩 모델 " + embeddingProperties.label()
                        + "로 만든 자료 조각이 있어야 합니다.");
            }
        };
    }
}
