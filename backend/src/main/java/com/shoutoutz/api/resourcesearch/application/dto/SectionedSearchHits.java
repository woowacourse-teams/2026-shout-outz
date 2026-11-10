package com.shoutoutz.api.resourcesearch.application.dto;

import com.shoutoutz.api.resourcesearch.domain.ResourceSection;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class SectionedSearchHits {

    private final Map<ResourceSection, List<SearchHit>> hitsBySection;

    private SectionedSearchHits(Map<ResourceSection, List<SearchHit>> hitsBySection) {
        this.hitsBySection = hitsBySection;
    }

    public static SectionedSearchHits from(List<SearchHit> rankedHits) {
        Map<ResourceSection, List<SearchHit>> hitsBySection = new EnumMap<>(ResourceSection.class);
        Arrays.stream(ResourceSection.values())
                .forEach(section -> hitsBySection.put(section, topOf(section, rankedHits)));
        return new SectionedSearchHits(hitsBySection);
    }

    private static List<SearchHit> topOf(ResourceSection section, List<SearchHit> rankedHits) {
        return rankedHits.stream()
                .filter(hit -> ResourceSection.of(hit.source()).equals(Optional.of(section)))
                .limit(section.limit())
                .toList();
    }

    public List<SearchHit> hits(ResourceSection section) {
        return hitsBySection.get(section);
    }

    public boolean isFull(ResourceSection section) {
        return hits(section).size() >= section.limit();
    }
}
