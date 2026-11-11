package com.shoutoutz.api.projectresource.domain;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * 경력 카드 template 별 포트폴리오 소제목과 순서. 소제목은 프로토타입 데이터(카드 1,049장)의 소제목과 같다.
 * 소제목을 백엔드 한곳에서만 정하려고 응답에 소제목과 본문을 함께 준다.
 */
public enum CareerCardTemplate {

    PROBLEM_SOLVING("problem_solving", List.of(
            new Part("해결하려던 문제", CareerCard::problem),
            new Part("원인", CareerCard::cause),
            new Part("해결 과정", CareerCard::solution),
            new Part("확인한 결과", CareerCard::result),
            new Part("측정 범위와 고려한 점", CareerCard::limits)
    )),
    TECH_DECISION("tech_decision", List.of(
            new Part("해결하려던 문제", CareerCard::problem),
            new Part("원인", CareerCard::cause),
            new Part("고려한 방법", card -> String.join(", ", card.options())),
            new Part("선택한 방법과 이유", CareerCard::decisionReason),
            new Part("적용한 내용", CareerCard::solution),
            new Part("확인한 결과", CareerCard::result),
            new Part("측정 범위와 고려한 점", CareerCard::limits)
    )),
    BUILD("build", List.of(
            new Part("목표", CareerCard::problem),
            new Part("구현한 내용", CareerCard::solution),
            new Part("확인한 결과", CareerCard::result),
            new Part("측정 범위와 고려한 점", CareerCard::limits)
    ));

    private final String value;
    private final List<Part> parts;

    CareerCardTemplate(String value, List<Part> parts) {
        this.value = value;
        this.parts = parts;
    }

    public static Optional<CareerCardTemplate> from(String value) {
        return Arrays.stream(values())
                .filter(template -> template.value.equals(value))
                .findFirst();
    }

    /**
     * 소제목 순서대로 본문을 채우고, 비어 있는 칸은 뺀다.
     */
    public List<CareerCardSection> sections(CareerCard card) {
        return parts.stream()
                .map(part -> new CareerCardSection(part.heading(), part.body().apply(card)))
                .filter(section -> section.body() != null && !section.body().isBlank())
                .map(section -> new CareerCardSection(section.heading(), section.body().trim()))
                .toList();
    }

    private record Part(String heading, Function<CareerCard, String> body) {

        private Part {
            Objects.requireNonNull(heading);
            Objects.requireNonNull(body);
        }
    }
}
