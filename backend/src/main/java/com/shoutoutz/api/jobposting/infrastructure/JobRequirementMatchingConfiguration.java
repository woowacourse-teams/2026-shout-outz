package com.shoutoutz.api.jobposting.infrastructure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 역량 매칭 Worker 를 켰을 때만 설정을 읽는다. Worker, 재시도 정책, 스케줄러도 같은 조건으로 뜬다.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "job-posting.matching", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(JobRequirementMatchingProperties.class)
public class JobRequirementMatchingConfiguration {
}
