package com.shoutoutz.api.common.llm;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import tools.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(LlmProperties.class)
public class LlmConfiguration {
    @Bean
    GeminiLlmClient geminiLlmClient(RestClient.Builder builder, LlmProperties properties, ObjectMapper mapper) {
        var config = properties.gemini();
        return new GeminiLlmClient(client(builder, "https://generativelanguage.googleapis.com",
                config.connectTimeout(), config.readTimeout()), config, mapper);
    }

    @Bean
    OpenAiLlmClient openAiLlmClient(RestClient.Builder builder, LlmProperties properties, ObjectMapper mapper) {
        var config = properties.openai();
        return new OpenAiLlmClient(client(builder, "https://api.openai.com",
                config.connectTimeout(), config.readTimeout()), config, mapper);
    }

    @Bean(destroyMethod = "close")
    BedrockRuntimeClient bedrockRuntimeClient(LlmProperties properties,
            ObjectProvider<AwsCredentialsProvider> credentialsProvider) {
        var config = properties.bedrock();
        return BedrockRuntimeClient.builder()
                .region(Region.of(config.region()))
                .credentialsProvider(credentialsProvider.getIfAvailable(
                        () -> DefaultCredentialsProvider.builder().build()))
                // 재시도는 질문 추천 작업이 맡으므로 SDK 는 한 번만 호출하고, 연결과 읽기를 합친 시간 안에 끝낸다.
                .overrideConfiguration(override -> override
                        .retryStrategy(AwsRetryStrategy.doNotRetry())
                        .apiCallTimeout(config.connectTimeout().plus(config.readTimeout())))
                .build();
    }

    @Bean
    BedrockLlmClient bedrockGptLlmClient(BedrockRuntimeClient client, LlmProperties properties, ObjectMapper mapper,
            ObjectProvider<MeterRegistry> meterRegistry) {
        return new BedrockLlmClient(client, LlmProvider.BEDROCK_GPT, properties.bedrock().gptModel(), mapper,
                meterRegistry.getIfAvailable(SimpleMeterRegistry::new));
    }

    @Bean
    BedrockLlmClient bedrockClaudeLlmClient(BedrockRuntimeClient client, LlmProperties properties,
            ObjectMapper mapper, ObjectProvider<MeterRegistry> meterRegistry) {
        return new BedrockLlmClient(client, LlmProvider.BEDROCK_CLAUDE, properties.bedrock().claudeModel(), mapper,
                meterRegistry.getIfAvailable(SimpleMeterRegistry::new));
    }

    private RestClient client(RestClient.Builder builder, String baseUrl, Duration connect, Duration read) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(connect).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(read);
        return builder.clone().baseUrl(baseUrl).requestFactory(factory).build();
    }
}
