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
        return valueOf(value);
    }
}
