package com.shoutoutz.api.project.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class DescriptionMediaReferencesTest {

    @Test
    @DisplayName("본문의 media://{id} 참조를 처음 등장한 순서대로, 중복 없이 뽑는다.")
    void extractsMediaIdsInOrderWithoutDuplicates() {
        String descriptionMd = "## 화면\n![상세](media://22)\n![목록](media://21)\n![상세 확대](media://22)";

        assertThat(DescriptionMediaReferences.extractMediaIds(descriptionMd)).containsExactly(22L, 21L);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "## 문제\n회고 도구와 액션 아이템 관리가 흩어져 있습니다.",
            "![외부 이미지](https://example.com/a.png)",
            "![숫자 아님](media://abc)",
            "![너무 긴 id](media://1234567890123456789)"
    })
    @DisplayName("media://{id} 형식의 참조가 없으면 빈 목록을 돌려준다.")
    void returnsEmptyWhenNoReference(String descriptionMd) {
        assertThat(DescriptionMediaReferences.extractMediaIds(descriptionMd)).isEmpty();
    }
}
