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

    private static final int MAX_HTML_BYTES = 512 * 1_024;
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
        URI current = URI.create(url);
        for (int redirects = 0; redirects <= MAX_REDIRECTS; redirects++) {
            urlPolicy.validateSyntax(current);
            HttpGet request = new HttpGet(current);
            request.addHeader("Accept", "text/html,application/xhtml+xml");
            request.addHeader("User-Agent", "ShoutOutzLinkPreview/1.0");

            PageResponse page = httpClient.execute(request, response -> {
                int status = response.getCode();
                if (status >= 300 && status < 400) {
                    Header location = response.getFirstHeader("Location");
                    if (location == null) {
                        throw new IOException("리다이렉트 주소가 없습니다.");
                    }
                    return new PageResponse(location.getValue(), null);
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
