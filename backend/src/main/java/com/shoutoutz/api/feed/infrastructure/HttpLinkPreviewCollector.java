package com.shoutoutz.api.feed.infrastructure;

import com.shoutoutz.api.feed.application.LinkPreviewCollector;
import com.shoutoutz.api.feed.application.dto.LinkPreviewMetadata;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.Header;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class HttpLinkPreviewCollector implements LinkPreviewCollector {

    // 일반 사이트의 HTML OG 수집 크기 제한.
    private static final int MAX_HTML_BYTES = 2 * 1_024 * 1_024;
    private static final int MAX_OEMBED_BYTES = 64 * 1_024;
    private static final int MAX_REDIRECTS = 3;
    private static final String YOUTUBE_HOST = "www.youtube.com";

    private final CloseableHttpClient httpClient;
    private final PublicLinkUrlPolicy urlPolicy;
    private final ObjectMapper objectMapper;

    public HttpLinkPreviewCollector(
            @Qualifier("linkPreviewHttpClient") CloseableHttpClient httpClient,
            PublicLinkUrlPolicy urlPolicy,
            ObjectMapper objectMapper
    ) {
        this.httpClient = httpClient;
        this.urlPolicy = urlPolicy;
        this.objectMapper = objectMapper;
    }

    @Override
    public LinkPreviewMetadata collect(String url) throws IOException {
        URI input = URI.create(url);
        urlPolicy.validateSyntax(input);
        Optional<String> youtubeVideoId = youtubeVideoId(input);
        if (youtubeVideoId.isPresent()) {
            return collectYoutubeOEmbed(youtubeVideoId.get());
        }

        URI current = input;
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

    private Optional<String> youtubeVideoId(URI url) {
        String host = url.getHost();
        if (host == null) {
            return Optional.empty();
        }
        String normalizedHost = host.toLowerCase(Locale.ROOT);
        String path = url.getPath();
        if (normalizedHost.equals("youtu.be") || normalizedHost.equals("www.youtu.be")) {
            return pathVideoId(path, 1);
        }
        if (!normalizedHost.equals("youtube.com")
                && !normalizedHost.equals("www.youtube.com")
                && !normalizedHost.equals("m.youtube.com")) {
            return Optional.empty();
        }

        if ("/watch".equals(path)) {
            String query = url.getRawQuery();
            if (query != null) {
                for (String parameter : query.split("&")) {
                    String[] pair = parameter.split("=", 2);
                    if (pair.length == 2 && pair[0].equals("v")) {
                        return validVideoId(pair[1]);
                    }
                }
            }
            return Optional.empty();
        }

        if (path != null) {
            String[] segments = path.split("/");
            if (segments.length == 3
                    && (segments[1].equals("shorts")
                    || segments[1].equals("live")
                    || segments[1].equals("embed"))) {
                return validVideoId(segments[2]);
            }
        }
        return Optional.empty();
    }

    private Optional<String> pathVideoId(String path, int expectedSegments) {
        if (path == null) {
            return Optional.empty();
        }
        String[] segments = path.split("/");
        if (segments.length == expectedSegments + 1 && segments[0].isEmpty()) {
            return validVideoId(segments[expectedSegments]);
        }
        return Optional.empty();
    }

    private Optional<String> validVideoId(String videoId) {
        return videoId.matches("[A-Za-z0-9_-]{11}") ? Optional.of(videoId) : Optional.empty();
    }

    private LinkPreviewMetadata collectYoutubeOEmbed(String videoId) throws IOException {
        String videoUrl = "https://" + YOUTUBE_HOST + "/watch?v=" + videoId;
        URI endpoint = URI.create("https://" + YOUTUBE_HOST + "/oembed?url="
                + URLEncoder.encode(videoUrl, StandardCharsets.UTF_8) + "&format=json");
        urlPolicy.requireFetchable(endpoint);

        byte[] body = httpClient.execute(new HttpGet(endpoint), response -> {
            int status = response.getCode();
            if (status != 200 || response.getEntity() == null) {
                throw new IOException("YouTube oEmbed 응답이 올바르지 않습니다: " + status);
            }
            Header contentType = response.getFirstHeader("Content-Type");
            String mediaType = contentType == null ? "" : contentType.getValue().toLowerCase(Locale.ROOT);
            if (!mediaType.startsWith("application/json")) {
                throw new IOException("YouTube oEmbed 응답이 JSON이 아닙니다.");
            }
            if (response.getEntity().getContentLength() > MAX_OEMBED_BYTES) {
                throw new IOException("YouTube oEmbed 응답이 너무 큽니다.");
            }
            byte[] responseBody = response.getEntity().getContent().readNBytes(MAX_OEMBED_BYTES + 1);
            if (responseBody.length > MAX_OEMBED_BYTES) {
                throw new IOException("YouTube oEmbed 응답이 너무 큽니다.");
            }
            return responseBody;
        });

        JsonNode data;
        try {
            data = objectMapper.readTree(body);
        } catch (RuntimeException exception) {
            throw new IOException("YouTube oEmbed 응답을 해석하지 못했습니다.", exception);
        }
        String title = text(data, "title");
        String thumbnailUrl = text(data, "thumbnail_url");
        String providerName = text(data, "provider_name");
        if (title == null || thumbnailUrl == null || !"YouTube".equals(providerName)) {
            throw new IOException("YouTube oEmbed 응답에 미리보기 정보가 없습니다.");
        }

        String validatedThumbnailUrl;
        try {
            validatedThumbnailUrl = urlPolicy.requireFetchable(URI.create(thumbnailUrl)).toString();
        } catch (IOException | IllegalArgumentException exception) {
            throw new IOException("YouTube 썸네일 주소를 가져올 수 없습니다.", exception);
        }
        return new LinkPreviewMetadata(
                limit(title, 300), null, validatedThumbnailUrl, limit(providerName, 100)
        );
    }

    private String text(JsonNode object, String field) {
        JsonNode value = object.get(field);
        return value == null || value.isNull() || !value.isString() || value.asString().isBlank()
                ? null
                : value.asString().strip();
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
