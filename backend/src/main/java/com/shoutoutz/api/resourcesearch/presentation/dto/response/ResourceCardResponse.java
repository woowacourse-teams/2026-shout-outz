package com.shoutoutz.api.resourcesearch.presentation.dto.response;

import com.shoutoutz.api.resourcesearch.application.dto.ResourceCard;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceCardLogoUrls;
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

    public static ResourceCardResponse from(ResourceCard card, ResourceCardLogoUrls logoUrls) {
        ResourceCard.Team team = card.team();
        return new ResourceCardResponse(
                card.resourceId(),
                card.source(),
                card.title(),
                card.url(),
                card.summary(),
                card.publishedAt(),
                card.platform(),
                team == null ? null : new Team(
                        team.name(),
                        team.projectSlug(),
                        team.logoMediaId(),
                        logoUrls.urlOf(team.logoMediaId())
                )
        );
    }

    /**
     * @param logoMediaId 프로젝트 로고 미디어 ID. 등록 전이거나 로고가 없으면 null
     * @param logoUrl     팀 로고 이미지 주소(작은 크기). logoMediaId 가 없거나 이미지가 준비되지 않았으면 null
     */
    public record Team(String name, String projectSlug, Long logoMediaId, String logoUrl) {
    }
}
