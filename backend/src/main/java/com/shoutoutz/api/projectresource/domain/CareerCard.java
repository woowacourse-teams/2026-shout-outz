package com.shoutoutz.api.projectresource.domain;

import java.util.List;

/**
 * 프로젝트 상세 전체 경험 탭의 경력 카드(resource_career_cards). 같은 일을 다룬 경험을 묶은 것이다.
 * 포트폴리오 칸(problem ~ limits)은 template 에 따라 소제목과 순서가 정해진다.
 *
 * @param jobRole       직군: BE, FE, AN
 * @param template      problem_solving, tech_decision, build
 * @param resumeBullets 이력서 문장
 * @param options       고려한 다른 방법
 */
public record CareerCard(
        long cardId,
        String jobRole,
        String template,
        String category,
        String title,
        List<String> resumeBullets,
        String problem,
        String cause,
        List<String> options,
        String decisionReason,
        String solution,
        String result,
        String limits
) {

    public CareerCard {
        resumeBullets = resumeBullets == null ? List.of() : List.copyOf(resumeBullets);
        options = options == null ? List.of() : List.copyOf(options);
    }

    /**
     * template 의 소제목 순서대로 포트폴리오 글을 만든다. 빈 칸은 뺀다. 모르는 template 이면 빈 목록이다.
     */
    public List<CareerCardSection> sections() {
        return CareerCardTemplate.from(template)
                .map(cardTemplate -> cardTemplate.sections(this))
                .orElse(List.of());
    }
}
