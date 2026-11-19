package com.shoutoutz.api.resourcesearch.domain;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

public enum ResourceSection {
    BLOG(20, 60, Set.of("blog-post")),
    DOCUMENT(20, 60, Set.of("github-pr", "github-teamdoc", "github-wiki", "github-discussion")),
    TALK(6, 6, Set.of("youtube-tekotalk"));

    private final int limit;
    private final int depth;
    private final Set<String> sources;

    ResourceSection(int limit, int depth, Set<String> sources) {
        this.limit = limit;
        this.depth = depth;
        this.sources = sources;
    }

    public static Optional<ResourceSection> of(String source) {
        if (source == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(section -> section.sources.contains(source))
                .findFirst();
    }

    public int limit() {
        return limit;
    }

    /**
     * 한 검색에서 이 섹션에 이어 읽기까지 내보내는 자료 수의 상한
     */
    public int depth() {
        return depth;
    }

    public Set<String> sources() {
        return sources;
    }
}
