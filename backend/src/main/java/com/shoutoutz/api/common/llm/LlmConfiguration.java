package com.shoutoutz.api.common.llm;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
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

    private RestClient client(RestClient.Builder builder, String baseUrl, Duration connect, Duration read) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(connect).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(read);
        return builder.clone().baseUrl(baseUrl).requestFactory(factory).build();
    }
}
