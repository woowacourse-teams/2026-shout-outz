package com.shoutoutz.api.visitor.domain;

import lombok.Builder;
import lombok.Getter;

/**
 * TODO: 현재 코드는 예시 데이터를 기반으로한 스켈리톤 코드입니다.
 * 형식 참고를 위한 스켈레톤 코드이니, 해당 도메인 담당자는 편하게 수정하셔도 됩니다.
 * 수정 이후에 해당 주석도 지워주세요.
 *
 */
@Getter
public class Visitor {

    private final Long id;
    private final String example;

    @Builder
    private Visitor(
            Long id,
            String example
    ) {
        validate(id, example);
        this.id = id;
        this.example = example;
    }

    public static Visitor initialize(String example) {
        return new Visitor(null, example);
    }

    private void validate(Long id, String example) {
        if (example.isEmpty()) {
            throw new IllegalStateException();
        }
    }

    public Visitor update(String name) {
        return new Visitor(this.id, name);
    }
}
