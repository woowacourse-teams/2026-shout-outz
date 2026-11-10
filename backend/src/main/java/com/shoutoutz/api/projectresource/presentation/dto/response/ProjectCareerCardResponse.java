package com.shoutoutz.api.projectresource.presentation.dto.response;

import com.shoutoutz.api.projectresource.domain.CareerCard;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceCard;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.ResourceCardResponse;
import java.util.List;

/**
 * 프로젝트 상세 전체 경험 탭의 경력 카드. 프론트 #607 목업의 목록 모양에 포트폴리오 글(sections)을 더했다.
 *
 * @param jobRole       직군: BE, FE, AN
 * @param resumeBullets 이력서 문장
 * @param sections      포트폴리오 글. template 별 소제목 순서이고 빈 칸은 없다
 * @param sources       근거 자료. 기술 자료 검색 결과 카드와 같은 모양이며 source_order 순(PR, 팀 문서, 블로그)이다
 */
public record ProjectCareerCardResponse(
        long cardId,
        String jobRole,
        String category,
        String title,
        List<String> resumeBullets,
        List<Section> sections,
        List<ResourceCardResponse> sources
) {

    public ProjectCareerCardResponse {
        resumeBullets = List.copyOf(resumeBullets);
        sections = List.copyOf(sections);
        sources = List.copyOf(sources);
    }

    public static ProjectCareerCardResponse of(CareerCard card, List<ResourceCard> sources) {
        return new ProjectCareerCardResponse(
                card.cardId(),
                card.jobRole(),
                card.category(),
                card.title(),
                card.resumeBullets(),
                card.sections().stream()
                        .map(section -> new Section(section.heading(), section.body()))
                        .toList(),
                sources.stream()
                        .map(ResourceCardResponse::from)
                        .toList()
        );
    }

    public record Section(String heading, String body) {
    }
}
