package com.shoutoutz.api.feed.infrastructure;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class LinkPreviewHttpConfiguration {

    @Bean
    PublicLinkDnsResolver publicLinkDnsResolver() {
        return new PublicLinkDnsResolver();
    }

    @Bean
    PublicLinkUrlPolicy publicLinkUrlPolicy(PublicLinkDnsResolver dnsResolver) {
        return new PublicLinkUrlPolicy(dnsResolver);
    }

    @Bean(name = "linkPreviewHttpClient", destroyMethod = "close")
    CloseableHttpClient linkPreviewHttpClient(PublicLinkDnsResolver dnsResolver) {
        var connections = PoolingHttpClientConnectionManagerBuilder.create()
                .setDnsResolver(dnsResolver)
                .setMaxConnTotal(2)
                .setMaxConnPerRoute(2)
                .setDefaultConnectionConfig(ConnectionConfig.custom()
                        .setConnectTimeout(Timeout.ofSeconds(2))
                        .setSocketTimeout(Timeout.ofSeconds(3))
                        .build())
                .build();
        return HttpClients.custom()
                .setConnectionManager(connections)
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectionRequestTimeout(Timeout.ofSeconds(1))
                        .setResponseTimeout(Timeout.ofSeconds(3))
                        .build())
                .disableRedirectHandling()
                .disableAutomaticRetries()
                .disableCookieManagement()
                .disableAuthCaching()
                .build();
    }
}
