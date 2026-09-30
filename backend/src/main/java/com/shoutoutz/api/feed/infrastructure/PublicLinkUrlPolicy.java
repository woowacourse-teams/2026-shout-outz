package com.shoutoutz.api.feed.infrastructure;

import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PublicLinkUrlPolicy {

    private final PublicLinkDnsResolver dnsResolver;

    public URI requireFetchable(URI url) throws UnknownHostException {
        URI validated = validateSyntax(url);
        dnsResolver.resolve(validated.getHost());
        return validated;
    }

    public URI validateSyntax(URI url) throws UnknownHostException {
        if (url == null || url.getHost() == null || url.getUserInfo() != null
                || url.getHost().contains("%") || url.toString().length() > 2_048) {
            throw new UnknownHostException("가져올 수 없는 URL 형식입니다.");
        }
        String scheme = url.getScheme() == null ? "" : url.getScheme().toLowerCase(Locale.ROOT);
        int port = url.getPort();
        if (!(scheme.equals("http") && (port == -1 || port == 80))
                && !(scheme.equals("https") && (port == -1 || port == 443))) {
            throw new UnknownHostException("HTTP(S) 기본 포트만 가져올 수 있습니다.");
        }
        return url;
    }
}
