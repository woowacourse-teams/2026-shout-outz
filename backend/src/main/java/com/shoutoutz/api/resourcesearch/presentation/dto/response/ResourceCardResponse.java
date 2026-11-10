package com.shoutoutz.api.resourcesearch.presentation.dto.response;

import com.shoutoutz.api.resourcesearch.application.dto.ResourceCard;
import java.time.Instant;

/**
 * 자료 카드 응답. 기술 자료 검색 명세의 결과 카드와 같은 칸이며, 검색과 프로젝트 기술 이야기 응답이 같이 쓴다.
 * 종류 배지, 블로그 아이콘, 테코톡 썸네일은 프론트가 source, platform, url 로 만든다.
 */
public record ResourceCardResponse(
        long resourceId,
        String source,
        String title,
        String url,
        String summary,
        Instant publishedAt,
        String platform,
        Team team
) {

    public static ResourceCardResponse from(ResourceCard card) {
        ResourceCard.Team team = card.team();
        return new ResourceCardResponse(
                card.resourceId(),
                card.source(),
                card.title(),
                card.url(),
                card.summary(),
                card.publishedAt(),
                card.platform(),
                team == null ? null : new Team(team.name(), team.projectSlug(), team.logoMediaId())
        );
    }

    public record Team(String name, String projectSlug, Long logoMediaId) {
    }
}
