package com.shoutoutz.api.common.embedding;

import java.net.http.HttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(EmbeddingProperties.class)
public class EmbeddingConfiguration {
    @Bean
    EmbeddingClient embeddingClient(RestClient.Builder builder, EmbeddingProperties properties, ObjectMapper mapper) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(properties.readTimeout());
        RestClient restClient = builder.clone().baseUrl("https://api.openai.com").requestFactory(factory).build();
        return new OpenAiEmbeddingClient(restClient, properties, mapper);
    }
}
