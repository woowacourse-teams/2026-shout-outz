package com.shoutoutz.api.feed.infrastructure;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.feed.application.dto.LinkPreviewMetadata;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.hc.client5.http.SystemDefaultDnsResolver;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.HttpHost;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * 운영과 같은 HttpClient 설정으로, 실제로 수집에 실패했던 사이트들의 응답을 로컬 서버에서 재현한다.
 * 수집기는 http://preview.test 로 요청하고, 로컬 서버가 프록시로 그 요청을 받는다.
 */
class HttpLinkPreviewCollectorTest {

    private static final String SITE = "http://preview.test";

    private final List<String> requestedPaths = new CopyOnWriteArrayList<>();
    private ExecutorService serverThreads;
    private HttpServer server;
    private CloseableHttpClient httpClient;
    private HttpLinkPreviewCollector collector;

    @BeforeEach
    void setUp() throws IOException {
        serverThreads = Executors.newCachedThreadPool();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(serverThreads);
        server.start();
        httpClient = LinkPreviewHttpConfiguration.linkPreviewHttpClientBuilder(SystemDefaultDnsResolver.INSTANCE)
                .setProxy(new HttpHost("http", "127.0.0.1", server.getAddress().getPort()))
                .build();
        collector = new HttpLinkPreviewCollector(
                httpClient, new PublicLinkUrlPolicy(new AnyHostIsPublic()), JsonMapper.builder().build()
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        httpClient.close();
        server.stop(0);
        serverThreads.shutdownNow();
    }

    @Test
    @DisplayName("헤더 한 줄이 8,192자를 넘어도(노션의 Content-Security-Policy) 미리보기를 만든다")
    void readsLongHeaderLine() throws IOException {
        server.createContext("/", exchange -> {
            exchange.getResponseHeaders().add("Content-Security-Policy", "default-src 'self' " + "a".repeat(9_000));
            sendHtml(exchange, page("노션 글"));
        });

        assertThat(collector.collect(SITE + "/2494b5223064801c8496f5a7d5feef19").title()).isEqualTo("노션 글");
    }

    @Test
    @DisplayName("2MB 가 넘는 HTML(큰 GitHub PR)은 앞부분의 og 태그로 미리보기를 만들고 나머지는 받지 않는다")
    void readsOnlyFrontOfLargeHtml() throws Exception {
        CompletableFuture<String> serverResult = new CompletableFuture<>();
        server.createContext("/", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, 0);
            byte[] filler = new byte[64 * 1_024];
            Arrays.fill(filler, (byte) 'a');
            try (OutputStream body = exchange.getResponseBody()) {
                body.write("<html><head><meta property=\"og:title\" content=\"큰 PR\"></head><body>".getBytes(UTF_8));
                for (int i = 0; i < 1_024; i++) {
                    body.write(filler);
                }
                serverResult.complete("64MB 를 모두 보냄");
            } catch (IOException clientStopped) {
                serverResult.complete("수집기가 연결을 끊음");
            }
        });

        LinkPreviewMetadata metadata = collector.collect(SITE + "/woowacourse-teams/2021-botobo/pull/378");

        assertThat(metadata.title()).isEqualTo("큰 PR");
        assertThat(serverResult.get(10, TimeUnit.SECONDS)).isEqualTo("수집기가 연결을 끊음");
    }

    @Test
    @DisplayName("Location 헤더에 인코딩 없이 UTF-8 로 온 한글(GitHub Pages)을 되살려 다음 주소를 요청한다")
    void followsRedirectWithRawUtf8Location() throws IOException {
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getRawPath();
            requestedPaths.add(path);
            if (path.equals("/Backend/%EC%8A%A4%ED%94%84%EB%A7%81")) {
                // GitHub Pages 는 끝에 / 를 붙인 주소로 넘기면서 한글을 UTF-8 바이트 그대로 보낸다.
                exchange.getResponseHeaders().add("Location", rawUtf8("http://preview.test/Backend/스프링/"));
                exchange.sendResponseHeaders(301, -1);
                exchange.close();
                return;
            }
            sendHtml(exchange, page("스프링 글"));
        });

        assertThat(collector.collect(SITE + "/Backend/%EC%8A%A4%ED%94%84%EB%A7%81").title()).isEqualTo("스프링 글");
        assertThat(requestedPaths).containsExactly(
                "/Backend/%EC%8A%A4%ED%94%84%EB%A7%81",
                "/Backend/%EC%8A%A4%ED%94%84%EB%A7%81/"
        );
    }

    @Test
    @DisplayName("주소 경로에 그대로 들어간 대괄호와 한글(GitHub 위키)을 인코딩해서 요청한다")
    void encodesIllegalCharactersInPath() throws IOException {
        server.createContext("/", exchange -> {
            requestedPaths.add(exchange.getRequestURI().getRawPath());
            sendHtml(exchange, page("위키 글"));
        });

        LinkPreviewMetadata metadata = collector.collect(SITE + "/woowacourse-teams/2021-zzimkkong/wiki/[BE]-DB-백업");

        assertThat(metadata.title()).isEqualTo("위키 글");
        assertThat(requestedPaths).containsExactly(
                "/woowacourse-teams/2021-zzimkkong/wiki/%5BBE%5D-DB-%EB%B0%B1%EC%97%85"
        );
    }

    @Test
    @DisplayName("og:image 주소의 한글도 인코딩해 미리보기 이미지로 쓴다")
    void encodesImageUrl() throws IOException {
        server.createContext("/", exchange -> sendHtml(exchange, """
                <html><head>
                <meta property="og:title" content="블로그 글">
                <meta property="og:image" content="/assets/대표 이미지.png">
                </head></html>
                """));

        assertThat(collector.collect(SITE + "/posts/1").imageUrl())
                .isEqualTo("http://preview.test/assets/%EB%8C%80%ED%91%9C%20%EC%9D%B4%EB%AF%B8%EC%A7%80.png");
    }

    private static String page(String title) {
        return "<html><head><meta property=\"og:title\" content=\"" + title + "\"></head><body></body></html>";
    }

    private static void sendHtml(HttpExchange exchange, String html) throws IOException {
        byte[] body = html.getBytes(UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    /** 헤더 값의 글자 하나가 바이트 하나로 나가므로, UTF-8 바이트를 그대로 헤더에 싣는다. */
    private static String rawUtf8(String value) {
        return new String(value.getBytes(UTF_8), ISO_8859_1);
    }

    /** 테스트 주소는 실제 DNS 에 없으므로 이미지 주소 검증에서 공개 주소로 본다. */
    private static final class AnyHostIsPublic extends PublicLinkDnsResolver {

        @Override
        public InetAddress[] resolve(String host) throws UnknownHostException {
            return new InetAddress[]{InetAddress.getByAddress(host, new byte[]{93, (byte) 184, (byte) 215, 14})};
        }
    }
}
