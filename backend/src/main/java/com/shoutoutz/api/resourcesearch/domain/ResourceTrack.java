package com.shoutoutz.api.resourcesearch.domain;

public enum ResourceTrack {
    BE,
    FE,
    AN,
    ALL;

    public static ResourceTrack from(String value) {
        if (value == null) {
            return null;
        }
        for (ResourceTrack track : values()) {
            if (track.name().equals(value)) {
                return track;
            }
        }
        throw new IllegalArgumentException("지원하지 않는 자료 트랙입니다: " + value);
    }
}
