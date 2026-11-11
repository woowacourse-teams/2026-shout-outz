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

    /**
     * 1024차원 벡터 하나는 약 4KiB라 1,000개면 약 4MiB다. 검색어 칩과 주제 칩, 자주 쓰는 검색어를 담기에 충분하다.
     */
    private static final long CACHE_MAXIMUM_SIZE = 1_000;

    @Bean
    EmbeddingClient embeddingClient(RestClient.Builder builder, EmbeddingProperties properties, ObjectMapper mapper) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(properties.readTimeout());
        RestClient restClient = builder.clone().baseUrl("https://api.openai.com").requestFactory(factory).build();
        return new CachingEmbeddingClient(new OpenAiEmbeddingClient(restClient, properties, mapper), CACHE_MAXIMUM_SIZE);
    }
}
