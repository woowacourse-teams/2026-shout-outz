package com.shoutoutz.api.feed.recommendation.infrastructure;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(QuestionRecommendationProperties.class)
public class QuestionRecommendationPropertiesConfiguration {
}
