package com.shoutoutz.api.feed.recommendation.infrastructure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(
        prefix = "question-recommendation.sqs",
        name = "enabled",
        havingValue = "true"
)
class QuestionRecommendationSqsConfiguration {

    @Bean(name = "questionRecommendationTaskScheduler")
    ThreadPoolTaskScheduler questionRecommendationTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("question-recommendation-sqs-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(5);
        return scheduler;
    }

    @Bean(destroyMethod = "close")
    SqsClient questionRecommendationSqsClient(
            QuestionRecommendationProperties properties,
            AwsCredentialsProvider credentialsProvider
    ) {
        return SqsClient.builder()
                .region(Region.of(properties.sqs().region()))
                .credentialsProvider(credentialsProvider)
                .build();
    }
}
