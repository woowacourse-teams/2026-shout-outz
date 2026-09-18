package com.shoutoutz.api.project.presentation.dto.response;

/**
 * @param viewCount 이번 조회를 반영한 조회수. 같은 날 다시 조회해 집계되지 않았으면 현재 조회수다.
 */
public record ProjectViewRecordResponse(long viewCount) {
}
