package com.shoutoutz.api.projectresource.domain;

import java.util.List;

/**
 * 기술 이야기 탭의 직군 탭. 프로토타입과 같이 직군을 고르면 그 직군 주제와 공통 주제만 보이고,
 * 주제 안에서는 자료 직군이 그 직군, ALL, 없음인 자료만 보인다.
 */
public enum ProjectResourceJobRole {

    ALL(null),
    BE("backend"),
    FE("frontend"),
    AN("mobile");

    private static final String COMMON_SIDE = "common";
    private static final String ALL_TRACK = "ALL";

    private final String topicSide;

    ProjectResourceJobRole(String topicSide) {
        this.topicSide = topicSide;
    }

    /**
     * 자료 직군이 따로 있는 탭. ALL 은 늘 보이고, 이 탭들은 그 직군 자료가 있을 때만 보인다.
     */
    public static List<ProjectResourceJobRole> tracks() {
        return List.of(BE, FE, AN);
    }

    /**
     * @param side 주제 직군(resource_topic_catalog.side): backend, frontend, mobile, common
     */
    public boolean includesTopic(String side) {
        return this == ALL || topicSide.equals(side) || COMMON_SIDE.equals(side);
    }

    /**
     * @param track 자료 직군(resource_documents.track): BE, FE, AN, ALL 또는 null
     */
    public boolean includesTrack(String track) {
        return this == ALL || track == null || ALL_TRACK.equals(track) || name().equals(track);
    }
}
