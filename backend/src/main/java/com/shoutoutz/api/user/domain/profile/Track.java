package com.shoutoutz.api.user.domain.profile;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 우아한테크코스 트랙.
 */
@Getter
@RequiredArgsConstructor
public enum Track {
    BACKEND("BACKEND"),
    ANDROID("ANDROID"),
    FRONTEND("FRONTEND");

    private final String value;

    public static Track from(String value) {
        return Arrays.stream(values())
                .filter(track -> track.value.equals(value))
                .findFirst()
                .orElseThrow(InvalidTrackException::new);
    }
}
