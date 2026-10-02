package com.shoutoutz.api.auth.infrastructure.oauth.github;

import java.net.http.HttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
class GitHubOAuthHttpClientConfig {

    @Bean
    @Qualifier("githubOAuthRestClient")
    RestClient githubOAuthRestClient(
            RestClient.Builder restClientBuilder,
            GitHubOAuthProperties properties
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());

        return restClientBuilder.requestFactory(requestFactory).build();
    }
}
