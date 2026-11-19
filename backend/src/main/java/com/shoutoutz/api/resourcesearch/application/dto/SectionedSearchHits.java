package com.shoutoutz.api.resourcesearch.application.dto;

import com.shoutoutz.api.resourcesearch.domain.ResourceSection;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class SectionedSearchHits {

    private final Map<ResourceSection, List<RankedResource>> hitsBySection;

    private SectionedSearchHits(Map<ResourceSection, List<RankedResource>> hitsBySection) {
        this.hitsBySection = hitsBySection;
    }

    public static SectionedSearchHits from(List<SearchHit> rankedHits) {
        return fromRanked(rankedHits.stream()
                .map(RankedResource::from)
                .toList());
    }

    public static SectionedSearchHits fromRanked(List<RankedResource> rankedResources) {
        Map<ResourceSection, List<RankedResource>> hitsBySection = new EnumMap<>(ResourceSection.class);
        Arrays.stream(ResourceSection.values())
                .forEach(section -> hitsBySection.put(section, topOf(section, rankedResources)));
        return new SectionedSearchHits(hitsBySection);
    }

    private static List<RankedResource> topOf(ResourceSection section, List<RankedResource> rankedHits) {
        return rankedHits.stream()
                .filter(hit -> ResourceSection.of(hit.source()).equals(Optional.of(section)))
                .limit(section.depth())
                .toList();
    }

    /**
     * 첫 화면에 보여 줄 자료. 섹션 상한까지만 담는다.
     */
    public List<RankedResource> hits(ResourceSection section) {
        return hitsBySection.get(section).stream()
                .limit(section.limit())
                .toList();
    }

    /**
     * 첫 화면 다음에 이어 읽을 자료. 섹션의 깊이 상한까지만 담는다.
     */
    public List<RankedResource> rest(ResourceSection section) {
        return hitsBySection.get(section).stream()
                .skip(section.limit())
                .toList();
    }

    public boolean isFull(ResourceSection section) {
        return hits(section).size() >= section.limit();
    }
}
