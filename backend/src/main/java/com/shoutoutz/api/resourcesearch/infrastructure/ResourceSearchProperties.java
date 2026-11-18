package com.shoutoutz.api.resourcesearch.infrastructure;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param queryTimeout   벡터 조회 한 번에 허용하는 시간. 넘으면 DB가 쿼리를 중단한다
 * @param maxConcurrent  동시에 실행하는 벡터 조회 수의 상한. 커넥션 풀보다 작게 둬 다른 기능이 쓸 커넥션을 남긴다
 * @param acquireTimeout 상한이 찼을 때 자리를 기다리는 시간. 넘으면 조회를 시작하지 않는다
 */
@ConfigurationProperties(prefix = "resource.search")
public record ResourceSearchProperties(
        @DefaultValue("10s") Duration queryTimeout,
        @DefaultValue("4") int maxConcurrent,
        @DefaultValue("200ms") Duration acquireTimeout
) {

    public ResourceSearchProperties {
        if (queryTimeout == null || queryTimeout.toMillis() < 1) {
            throw new IllegalArgumentException("자료 검색 쿼리 제한 시간은 1ms 이상이어야 합니다.");
        }
        if (maxConcurrent < 1) {
            throw new IllegalArgumentException("자료 검색 동시 실행 상한은 1 이상이어야 합니다.");
        }
        if (acquireTimeout == null || acquireTimeout.isNegative()) {
            throw new IllegalArgumentException("자료 검색 대기 시간은 0 이상이어야 합니다.");
        }
    }
}
