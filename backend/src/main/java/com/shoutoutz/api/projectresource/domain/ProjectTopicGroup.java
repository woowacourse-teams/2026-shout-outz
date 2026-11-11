package com.shoutoutz.api.projectresource.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * 탐색 화면 프로젝트 탭의 기술 주제 묶음. 주제 직군(resource_topic_catalog.side)으로 정하며, 이 순서대로 보여 준다.
 * 이름과 짧은 이름은 프론트 #607 목업과 같다.
 */
public enum ProjectTopicGroup {

    COMMON("common", "공통", "공통"),
    BE("backend", "백엔드", "BE"),
    FE("frontend", "프론트엔드", "FE"),
    AN("mobile", "안드로이드", "AN");

    private final String side;
    private final String displayName;
    private final String shortName;

    ProjectTopicGroup(String side, String displayName, String shortName) {
        this.side = side;
        this.displayName = displayName;
        this.shortName = shortName;
    }

    public static Optional<ProjectTopicGroup> fromSide(String side) {
        return Arrays.stream(values())
                .filter(group -> group.side.equals(side))
                .findFirst();
    }

    public String displayName() {
        return displayName;
    }

    public String shortName() {
        return shortName;
    }
}
