package com.shoutoutz.api.common.cache;

import java.time.Duration;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 로컬 캐시 설정.
 * enabled가 false면 모든 캐시가 값을 보관하지 않고 매번 DB에서 읽는다.
 */
@ConfigurationProperties(prefix = "local-cache")
public record LocalCacheProperties(@DefaultValue("true") boolean enabled, Map<String, Spec> caches) {

    public LocalCacheProperties {
        caches = caches == null ? Map.of() : Map.copyOf(caches);
    }

    public Spec spec(String name) {
        Spec spec = caches.get(name);
        if (spec == null) {
            throw new IllegalArgumentException("로컬 캐시 설정이 없습니다. name=" + name);
        }
        return spec;
    }

    /**
     * @param refreshAfter 보관한 지 이 시간이 지난 값은 일단 돌려주고, 뒤에서 새로 읽어 바꾼다.
     * @param expireAfter  보관한 지 이 시간이 지난 값은 돌려주지 않고 버린다. refreshAfter보다 길어야 한다.
     * @param maximumSize  보관할 최대 개수.
     */
    public record Spec(Duration refreshAfter, Duration expireAfter, long maximumSize) {

        public Spec {
            if (refreshAfter == null || refreshAfter.isNegative() || refreshAfter.isZero()) {
                throw new IllegalArgumentException("로컬 캐시 갱신 시간은 양수여야 합니다.");
            }
            if (expireAfter == null || expireAfter.compareTo(refreshAfter) <= 0) {
                throw new IllegalArgumentException("로컬 캐시 상한은 갱신 시간보다 길어야 합니다.");
            }
            if (maximumSize < 1) {
                throw new IllegalArgumentException("로컬 캐시 최대 개수는 1 이상이어야 합니다.");
            }
        }
    }
}
