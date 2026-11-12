package com.shoutoutz.api.feed.infrastructure;

import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.ManagedHttpClientConnectionFactory;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.config.Http1Config;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class LinkPreviewHttpConfiguration {

    /**
     * 응답 헤더 한 줄의 최대 길이. httpcore5 5.4 기본값은 8,192자인데, 노션은 Content-Security-Policy 한 줄이 9천 자 가까이 된다.
     * 메모리 1GB 서버라서 제한을 없애지 않고 넉넉한 값으로 둔다.
     */
    static final int MAX_HEADER_LINE_LENGTH = 64 * 1_024;

    /**
     * 응답을 기다리는 시간. 큰 GitHub PR 페이지는 첫 바이트까지 3초 가까이, 미리보기를 만들기까지 4초 가까이 걸려 3초로는 자주 끊겼다.
     * 수집은 비동기라서 글을 쓴 사용자가 이 시간을 기다리지 않는다.
     */
    static final Timeout RESPONSE_TIMEOUT = Timeout.ofSeconds(10);

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
        return linkPreviewHttpClientBuilder(dnsResolver).build();
    }

    /** 수집기 테스트가 운영과 같은 설정에 로컬 서버 경로만 더해 쓰도록 빌더로 나눈다. */
    static HttpClientBuilder linkPreviewHttpClientBuilder(DnsResolver dnsResolver) {
        var connections = PoolingHttpClientConnectionManagerBuilder.create()
                .setDnsResolver(dnsResolver)
                .setConnectionFactory(ManagedHttpClientConnectionFactory.builder()
                        .http1Config(Http1Config.custom().setMaxLineLength(MAX_HEADER_LINE_LENGTH).build())
                        .build())
                .setMaxConnTotal(2)
                .setMaxConnPerRoute(2)
                .setDefaultConnectionConfig(ConnectionConfig.custom()
                        .setConnectTimeout(Timeout.ofSeconds(2))
                        .setSocketTimeout(RESPONSE_TIMEOUT)
                        .build())
                .build();
        return HttpClients.custom()
                .setConnectionManager(connections)
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectionRequestTimeout(Timeout.ofSeconds(1))
                        .setResponseTimeout(RESPONSE_TIMEOUT)
                        .build())
                .disableRedirectHandling()
                .disableAutomaticRetries()
                .disableCookieManagement()
                .disableAuthCaching();
    }
}
