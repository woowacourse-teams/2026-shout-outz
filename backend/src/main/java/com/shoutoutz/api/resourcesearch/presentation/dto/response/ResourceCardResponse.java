package com.shoutoutz.api.resourcesearch.presentation.dto.response;

import com.shoutoutz.api.resourcesearch.application.dto.ResourceCard;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceCardImages;
import java.time.Instant;

/**
 * 자료 카드 응답. 기술 자료 검색 명세의 결과 카드와 같은 칸이며, 검색과 프로젝트 기술 이야기 응답이 같이 쓴다.
 * 종류 배지, 테코톡 썸네일은 프론트가 source, url 로 만든다.
 *
 * @param faviconUrl 원문 사이트 아이콘 주소. tistory, velog, rilog 블로그는 플랫폼 공식 아이콘, 그 밖의 자료는 원문 페이지의 파비콘이다.
 *                   아직 파비콘을 수집하지 않았거나 파비콘이 없으면(테코톡 등) null
 */
public record ResourceCardResponse(
        long resourceId,
        String source,
        String title,
        String url,
        String summary,
        Instant publishedAt,
        String platform,
        String faviconUrl,
        Team team
) {

    public static ResourceCardResponse from(ResourceCard card, ResourceCardImages images) {
        ResourceCard.Team team = card.team();
        return new ResourceCardResponse(
                card.resourceId(),
                card.source(),
                card.title(),
                card.url(),
                card.summary(),
                card.publishedAt(),
                card.platform(),
                images.faviconUrlOf(card),
                team == null ? null : new Team(
                        team.name(),
                        team.projectSlug(),
                        team.logoMediaId(),
                        images.logoUrlOf(team.logoMediaId())
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
