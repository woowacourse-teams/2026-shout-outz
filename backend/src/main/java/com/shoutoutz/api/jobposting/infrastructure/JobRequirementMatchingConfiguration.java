package com.shoutoutz.api.jobposting.infrastructure;

import com.shoutoutz.api.common.embedding.EmbeddingProperties;
import com.shoutoutz.api.common.llm.LlmProperties;
import com.shoutoutz.api.common.llm.LlmProvider;
import com.shoutoutz.api.jobposting.application.JobRequirementMatchingRepository;
import com.shoutoutz.api.resourcesearch.infrastructure.ResourceSearchProperties;
import java.time.Duration;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
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
     * 임베딩, 벡터 조회, 판정의 제한 시간과 결과 저장의 커넥션 대기 합이 lease 를 넘으면 처리 중인 역량을 다른 Worker 가 다시 가져갈 수 있다.
     * 현재 임베딩 모델로 만든 자료 조각이 없으면 모든 역량이 추천 없이 완료로 끝나 되돌릴 수 없어서 함께 막는다.
     */
    @Bean
    InitializingBean jobRequirementMatchingSettingsValidator(
            JobRequirementMatchingProperties properties,
            LlmProperties llmProperties,
            EmbeddingProperties embeddingProperties,
            ResourceSearchProperties resourceSearchProperties,
            @Value("${spring.datasource.hikari.connection-timeout:30000}") long connectionTimeoutMillis,
            JobRequirementMatchingRepository repository
    ) {
        return () -> {
            if (embeddingProperties.apiKey() == null || embeddingProperties.apiKey().isBlank()) {
                throw new IllegalArgumentException("역량 매칭을 켜려면 임베딩 API 키를 설정해야 합니다.");
            }
            // HikariCP 는 0 을 제한 없이 기다리는 것으로 해석해서, 0 이면 아래 합계가 실제 대기 시간을 담지 못한다.
            if (connectionTimeoutMillis <= 0) {
                throw new IllegalArgumentException(
                        "역량 매칭을 켜려면 spring.datasource.hikari.connection-timeout이 0보다 커야 합니다.");
            }
            Duration connectionTimeout = Duration.ofMillis(connectionTimeoutMillis);
            // 역량 하나를 처리하는 동안 벡터 조회는 한 번 한다. 자리를 기다리고, 커넥션을 기다리고, 쿼리가 제한 시간까지 걸릴 수 있다.
            // 판정 결과를 저장하는 트랜잭션도 커넥션을 한 번 더 기다릴 수 있다.
            Duration budget = embeddingProperties.connectTimeout().plus(embeddingProperties.readTimeout())
                    .plus(resourceSearchProperties.acquireTimeout())
                    .plus(connectionTimeout)
                    .plus(resourceSearchProperties.queryTimeout())
                    .plus(connectionTimeout);
            for (LlmProvider provider : properties.providers()) {
                if (!llmProperties.hasCredentials(provider)) {
                    throw new IllegalArgumentException("역량 판정 제공자 " + provider + "를 사용하려면 API 키를 설정해야 합니다.");
                }
                budget = budget.plus(llmProperties.timeout(provider));
            }
            if (budget.compareTo(properties.lease()) >= 0) {
                throw new IllegalArgumentException(
                        "임베딩, 벡터 조회, 판정 제공자의 제한 시간과 결과 저장의 커넥션 대기 합은 job-posting.matching.lease보다 짧아야 합니다.");
            }
            if (!repository.hasSearchableChunks(embeddingProperties.label())) {
                throw new IllegalArgumentException("역량 매칭을 켜려면 임베딩 모델 " + embeddingProperties.label()
                        + "로 만든 자료 조각이 있어야 합니다.");
            }
        };
    }
}
