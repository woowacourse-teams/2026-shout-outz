package com.shoutoutz.api.jobposting.application.dto;

/**
 * 공고 id 는 등록 순서대로 커지므로, 최신순 목록의 다음 위치를 id 하나로 가리킨다.
 */
public record JobPostingCursor(long jobPostingId) {
}
