package com.shoutoutz.api.feed.infrastructure;

import com.shoutoutz.api.feed.application.LinkPreviewCollector;
import com.shoutoutz.api.feed.application.dto.LinkPreviewMetadata;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.Header;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class HttpLinkPreviewCollector implements LinkPreviewCollector {

    // YouTube watch 페이지는 일반적인 OG 페이지보다 HTML이 크다.
    // OG 태그 수집은 계속 제한하되, 짧은 URL이 youtube.com으로 리다이렉트된 뒤
    // 정상적인 watch 페이지를 읽을 수 있도록 기존 512 KiB보다 여유를 둔다.
    private static final int MAX_HTML_BYTES = 2 * 1_024 * 1_024;
    private static final int MAX_REDIRECTS = 3;

    private final CloseableHttpClient httpClient;
    private final PublicLinkUrlPolicy urlPolicy;

    public HttpLinkPreviewCollector(
            @Qualifier("linkPreviewHttpClient") CloseableHttpClient httpClient,
            PublicLinkUrlPolicy urlPolicy
    ) {
        this.httpClient = httpClient;
        this.urlPolicy = urlPolicy;
    }

    @Override
    public LinkPreviewMetadata collect(String url) throws IOException {
        URI input = URI.create(url);
        urlPolicy.validateSyntax(input);
        URI current = normalizeYoutubeShortUrl(input);
        for (int redirects = 0; redirects <= MAX_REDIRECTS; redirects++) {
            urlPolicy.validateSyntax(current);
            HttpGet request = new HttpGet(current);
            request.addHeader("Accept", "text/html,application/xhtml+xml");
            request.addHeader("User-Agent", "ShoutOutzLinkPreview/1.0");
            request.addHeader("Cache-Control", "no-cache");
            request.addHeader("Pragma", "no-cache");

            PageResponse page = httpClient.execute(request, response -> {
                int status = response.getCode();
                if (isRedirectStatus(status)) {
                    Header location = response.getFirstHeader("Location");
                    if (location == null) {
                        throw new IOException("리다이렉트 주소가 없습니다.");
                    }
                    return new PageResponse(location.getValue(), null);
                }
                if (status == 304) {
                    throw new IOException("304 Not Modified 응답에는 HTML 본문이 없습니다.");
                }
                if (status != 200 || response.getEntity() == null) {
                    throw new IOException("미리보기 페이지 응답이 올바르지 않습니다: " + status);
                }
                Header contentType = response.getFirstHeader("Content-Type");
                String mediaType = contentType == null ? "" : contentType.getValue().toLowerCase(Locale.ROOT);
                if (!mediaType.startsWith("text/html")
                        && !mediaType.startsWith("application/xhtml+xml")) {
                    throw new IOException("HTML 페이지가 아닙니다.");
                }
                if (response.getEntity().getContentLength() > MAX_HTML_BYTES) {
                    throw new IOException("HTML 응답이 너무 큽니다.");
                }
                byte[] body = response.getEntity().getContent().readNBytes(MAX_HTML_BYTES + 1);
                if (body.length > MAX_HTML_BYTES) {
                    throw new IOException("HTML 응답이 너무 큽니다.");
                }
                return new PageResponse(null, body);
            });

            if (page.redirect() == null) {
                return parse(page.body(), current);
            }
            current = current.resolve(page.redirect());
        }
        throw new IOException("리다이렉트 횟수를 초과했습니다.");
    }

    /** youtu.be는 리다이렉트 응답에 의존하지 않고 영상 페이지를 직접 요청한다. */
    private URI normalizeYoutubeShortUrl(URI url) {
        String host = url.getHost();
        if (host == null) {
            return url;
        }
        String normalizedHost = host.toLowerCase(Locale.ROOT);
        if (!normalizedHost.equals("youtu.be") && !normalizedHost.equals("www.youtu.be")) {
            return url;
        }

        String path = url.getPath();
        if (path == null || path.length() <= 1) {
            return url;
        }
        String videoId = path.substring(1);
        if (videoId.endsWith("/")) {
            videoId = videoId.substring(0, videoId.length() - 1);
        }
        if (!videoId.matches("[A-Za-z0-9_-]{11}")) {
            return url;
        }
        return URI.create("https://www.youtube.com/watch?v=" + videoId);
    }

    private boolean isRedirectStatus(int status) {
        return status == 301
                || status == 302
                || status == 303
                || status == 307
                || status == 308;
    }

    private LinkPreviewMetadata parse(byte[] body, URI pageUrl) throws IOException {
        Document document = Jsoup.parse(new ByteArrayInputStream(body), null, pageUrl.toString());
        String title = firstPresent(meta(document, "property", "og:title"), document.title());
        String description = firstPresent(
                meta(document, "property", "og:description"),
                meta(document, "name", "description")
        );
        String siteName = firstPresent(
                meta(document, "property", "og:site_name"), pageUrl.getHost()
        );
        return new LinkPreviewMetadata(
                limit(title, 300),
                limit(description, 500),
                imageUrl(meta(document, "property", "og:image"), pageUrl),
                limit(siteName, 100)
        );
    }

    private String meta(Document document, String attribute, String value) {
        Element element = document.selectFirst("meta[" + attribute + "=" + value + "]");
        return element == null ? null : element.attr("content");
    }

    private String imageUrl(String value, URI pageUrl) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            URI image = pageUrl.resolve(value.strip());
            return urlPolicy.requireFetchable(image).toString();
        } catch (IOException | IllegalArgumentException ignored) {
            return null;
        }
    }

    private String firstPresent(String preferred, String fallback) {
        return preferred != null && !preferred.isBlank() ? preferred : fallback;
    }

    private String limit(String value, int maxCodePoints) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.replaceAll("\\s+", " ").strip();
        int points = normalized.codePointCount(0, normalized.length());
        return points <= maxCodePoints
                ? normalized
                : normalized.substring(0, normalized.offsetByCodePoints(0, maxCodePoints));
    }

    private record PageResponse(String redirect, byte[] body) {
    }
}
