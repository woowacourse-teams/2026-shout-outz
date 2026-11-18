package com.shoutoutz.api.resourcesearch.infrastructure;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param queryTimeout 벡터 조회 한 번에 허용하는 시간. 넘으면 DB가 쿼리를 중단한다
 */
@ConfigurationProperties(prefix = "resource.search")
public record ResourceSearchProperties(
        @DefaultValue("10s") Duration queryTimeout
) {

    public ResourceSearchProperties {
        if (queryTimeout == null || queryTimeout.toMillis() < 1) {
            throw new IllegalArgumentException("자료 검색 쿼리 제한 시간은 1ms 이상이어야 합니다.");
        }
    }
}
