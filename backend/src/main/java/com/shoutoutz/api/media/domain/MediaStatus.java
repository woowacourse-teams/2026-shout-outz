package com.shoutoutz.api.media.domain;

import java.util.Map;
import java.util.Set;

/**
 * 미디어 업로드와 처리의 생명주기 상태
 *
 * @author josangjun
 */
public enum MediaStatus {
    PENDING_UPLOAD,
    PROCESSING,
    READY,
    FAILED,
    EXPIRED;

    private static final Map<MediaStatus, Set<MediaStatus>> ALLOWED_TRANSITIONS = Map.of(
            PENDING_UPLOAD, Set.of(PROCESSING, FAILED, EXPIRED),
            PROCESSING, Set.of(READY, FAILED),
            READY, Set.of(),
            FAILED, Set.of(),
            EXPIRED, Set.of()
    );

    public boolean canTransitionTo(MediaStatus target) {
        return target != null && ALLOWED_TRANSITIONS.get(this).contains(target);
    }

    public MediaStatus transitionTo(MediaStatus target) {
        if (!canTransitionTo(target)) {
            throw new IllegalStateException("허용되지 않는 미디어 상태 전이입니다: " + this + " -> " + target);
        }
        return target;
    }
}
