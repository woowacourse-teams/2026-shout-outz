package com.shoutoutz.api.resourcesearch.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ResourceSectionTest {

    @ParameterizedTest
    @CsvSource({
            "blog-post, BLOG",
            "github-pr, DOCUMENT",
            "github-teamdoc, DOCUMENT",
            "github-wiki, DOCUMENT",
            "github-discussion, DOCUMENT",
            "youtube-tekotalk, TALK"
    })
    @DisplayName("자료 원천을 검색 섹션으로 분류한다")
    void classifiesSourceIntoSection(String source, ResourceSection expected) {
        assertThat(ResourceSection.of(source)).contains(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"unknown-source", "BLOG-POST", " blog-post"})
    @DisplayName("모르는 원천은 어느 섹션에도 넣지 않는다")
    void excludesUnknownSource(String source) {
        assertThat(ResourceSection.of(source)).isEmpty();
    }

    @Test
    @DisplayName("블로그와 문서는 20건, 테코톡은 6건까지 담는다")
    void limitsItemsBySection() {
        assertThat(ResourceSection.BLOG.limit()).isEqualTo(20);
        assertThat(ResourceSection.DOCUMENT.limit()).isEqualTo(20);
        assertThat(ResourceSection.TALK.limit()).isEqualTo(6);
    }

    @Test
    @DisplayName("섹션은 자기 원천 목록을 바꿀 수 없게 돌려준다")
    void exposesSourcesAsImmutableSet() {
        assertThat(ResourceSection.TALK.sources()).containsExactly("youtube-tekotalk");
        assertThat(ResourceSection.DOCUMENT.sources()).isUnmodifiable();
    }
}
