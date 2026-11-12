package com.shoutoutz.api.projectresource.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CareerCardTest {

    @Test
    @DisplayName("문제 해결 카드는 해결하려던 문제, 원인, 해결 과정, 확인한 결과, 측정 범위와 고려한 점 순으로 글을 만든다")
    void problemSolvingSections() {
        CareerCard card = card("problem_solving", "문제", "원인", List.of("방법"), "이유", "해결", "결과", "한계");

        assertThat(card.sections()).containsExactly(
                new CareerCardSection("해결하려던 문제", "문제"),
                new CareerCardSection("원인", "원인"),
                new CareerCardSection("해결 과정", "해결"),
                new CareerCardSection("확인한 결과", "결과"),
                new CareerCardSection("측정 범위와 고려한 점", "한계")
        );
    }

    @Test
    @DisplayName("기술 선택 카드는 고려한 방법을 쉼표로 이어 붙이고, 선택한 방법과 이유를 넣는다")
    void techDecisionSections() {
        CareerCard card = card(
                "tech_decision",
                "문제",
                "원인",
                List.of("기존 HTML 메타 태그 수집", "YouTube oEmbed API 우선 수집"),
                "이유",
                "적용",
                "결과",
                "한계"
        );

        assertThat(card.sections()).containsExactly(
                new CareerCardSection("해결하려던 문제", "문제"),
                new CareerCardSection("원인", "원인"),
                new CareerCardSection("고려한 방법", "기존 HTML 메타 태그 수집, YouTube oEmbed API 우선 수집"),
                new CareerCardSection("선택한 방법과 이유", "이유"),
                new CareerCardSection("적용한 내용", "적용"),
                new CareerCardSection("확인한 결과", "결과"),
                new CareerCardSection("측정 범위와 고려한 점", "한계")
        );
    }

    @Test
    @DisplayName("구현 카드는 목표, 구현한 내용, 확인한 결과, 측정 범위와 고려한 점 순으로 글을 만든다")
    void buildSections() {
        CareerCard card = card("build", "목표", "원인", List.of("방법"), "이유", "구현", "결과", "한계");

        assertThat(card.sections()).extracting(CareerCardSection::heading)
                .containsExactly("목표", "구현한 내용", "확인한 결과", "측정 범위와 고려한 점");
    }

    @Test
    @DisplayName("비어 있는 칸은 빼고, 본문의 앞뒤 공백은 뗀다")
    void skipsBlankParts() {
        CareerCard card = card("tech_decision", " 문제 ", " ", List.of(), null, "적용", "", null);

        assertThat(card.sections()).containsExactly(
                new CareerCardSection("해결하려던 문제", "문제"),
                new CareerCardSection("적용한 내용", "적용")
        );
    }

    @Test
    @DisplayName("모르는 template 이면 포트폴리오 글을 만들지 않는다")
    void unknownTemplate() {
        assertThat(card("unknown", "문제", "원인", List.of(), "이유", "해결", "결과", "한계").sections()).isEmpty();
    }

    @Test
    @DisplayName("이력서 문장, 기술, 고려한 방법이 없으면 빈 목록으로 둔다")
    void nullListsBecomeEmpty() {
        CareerCard card = new CareerCard(1L, "BE", "build", "분류", "제목", null, null, "목표", null, null, null, "구현",
                null, null);

        assertThat(card.resumeBullets()).isEmpty();
        assertThat(card.techs()).isEmpty();
        assertThat(card.options()).isEmpty();
    }

    private static CareerCard card(
            String template,
            String problem,
            String cause,
            List<String> options,
            String decisionReason,
            String solution,
            String result,
            String limits
    ) {
        return new CareerCard(1L, "BE", template, "분류", "제목", List.of("문장"), List.of("Spring"), problem, cause,
                options, decisionReason, solution, result, limits);
    }
}
