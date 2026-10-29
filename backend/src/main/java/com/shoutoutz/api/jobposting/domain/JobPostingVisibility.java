package com.shoutoutz.api.jobposting.domain;

import java.time.Instant;

/**
 * 숨김 시각을 바꾼 뒤의 공고 숨김 상태. 숨기지 않은 공고는 hiddenAt 이 비어 있다.
 */
public record JobPostingVisibility(long jobPostingId, Instant hiddenAt) {

    public boolean hidden() {
        return hiddenAt != null;
    }
}
