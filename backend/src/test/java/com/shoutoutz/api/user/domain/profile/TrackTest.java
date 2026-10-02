package com.shoutoutz.api.user.domain.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TrackTest {

    @Test
    @DisplayName("트랙 문자열을 도메인 타입으로 변환한다")
    void convertsValueToTrack() {
        assertThat(Track.from("BACKEND")).isEqualTo(Track.BACKEND);
        assertThat(Track.from("ANDROID")).isEqualTo(Track.ANDROID);
        assertThat(Track.from("FRONTEND")).isEqualTo(Track.FRONTEND);
    }

    @Test
    @DisplayName("정의되지 않은 트랙 문자열을 거절한다")
    void rejectsUnknownTrack() {
        assertThatThrownBy(() -> Track.from("BE"))
                .isInstanceOf(BadRequestException.class)
                .isInstanceOf(InvalidTrackException.class);
    }
}
