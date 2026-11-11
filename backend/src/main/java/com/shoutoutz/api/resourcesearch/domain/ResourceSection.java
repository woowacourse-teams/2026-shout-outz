package com.shoutoutz.api.resourcesearch.domain;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

public enum ResourceSection {
    BLOG(20, Set.of("blog-post")),
    DOCUMENT(20, Set.of("github-pr", "github-teamdoc", "github-wiki", "github-discussion")),
    TALK(6, Set.of("youtube-tekotalk"));

    private final int limit;
    private final Set<String> sources;

    ResourceSection(int limit, Set<String> sources) {
        this.limit = limit;
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

    public Set<String> sources() {
        return sources;
    }
}
