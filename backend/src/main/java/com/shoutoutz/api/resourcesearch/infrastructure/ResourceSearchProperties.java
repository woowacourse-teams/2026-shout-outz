package com.shoutoutz.api.resourcesearch.infrastructure;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 기본값은 운영 RDS 사양(메모리 1GB, vCPU 2)으로 맞춘 환경에서 조각 16,483개로 재서 정했다.
 * 동시 실행을 제한하지 않으면 조회끼리 느려져 처리량이 줄었고, 상한 1~3은 서로 비슷하게 더 많이 처리했다.
 * 1은 느린 조회 하나가 뒤의 검색을 모두 막으므로 2로 둔다. 벡터 조회 한 번은 평소 최대 0.4초, 10개가 겹쳐도 2초 안팎이었다.
 * 자료 수나 DB 사양이 바뀌면 다시 잰다.
 *
 * @param queryTimeout   벡터 조회 한 번에 허용하는 시간. 넘으면 DB가 쿼리를 중단한다
 * @param maxConcurrent  동시에 실행하는 벡터 조회 수의 상한. 커넥션 풀보다 작게 둬 다른 기능이 쓸 커넥션을 남긴다
 * @param acquireTimeout 상한이 찼을 때 자리를 기다리는 시간. 넘으면 조회를 시작하지 않는다
 */
@ConfigurationProperties(prefix = "resource.search")
public record ResourceSearchProperties(
        @DefaultValue("3s") Duration queryTimeout,
        @DefaultValue("2") int maxConcurrent,
        @DefaultValue("1s") Duration acquireTimeout
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
