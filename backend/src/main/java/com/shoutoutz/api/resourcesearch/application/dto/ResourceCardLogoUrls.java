package com.shoutoutz.api.resourcesearch.application.dto;

import java.net.URI;
import java.util.Map;

/**
 * 자료 카드 팀 로고의 미디어 ID별 이미지 주소. 응답 하나에 담긴 카드들의 로고를 한 번에 바꿔 둔 결과다.
 * 업로드가 끝나지 않았거나 없는 미디어는 들어 있지 않아 주소가 null 이 된다.
 */
public record ResourceCardLogoUrls(Map<Long, URI> urls) {

    public static final ResourceCardLogoUrls NONE = new ResourceCardLogoUrls(Map.of());

    public ResourceCardLogoUrls {
        urls = Map.copyOf(urls);
    }

    public String urlOf(Long mediaId) {
        if (mediaId == null) {
            return null;
        }
        URI url = urls.get(mediaId);
        return url == null ? null : url.toString();
    }
}
