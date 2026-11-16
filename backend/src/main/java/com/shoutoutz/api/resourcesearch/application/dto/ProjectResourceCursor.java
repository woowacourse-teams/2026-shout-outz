package com.shoutoutz.api.resourcesearch.application.dto;

import java.time.Instant;

/**
 * 프로젝트 자료를 최신순으로 이어 읽는 위치. 앞 페이지 마지막 자료의 발행 시각과 ID다.
 *
 * @param publishedAt 발행 시각이 없는 자료는 맨 뒤에 모여 있어, 그 구간을 읽는 중이면 null 이다
 */
public record ProjectResourceCursor(
        Instant publishedAt,
        long resourceId
) {

    public static ProjectResourceCursor after(ResourceCard card) {
        return new ProjectResourceCursor(card.publishedAt(), card.resourceId());
    }
}
