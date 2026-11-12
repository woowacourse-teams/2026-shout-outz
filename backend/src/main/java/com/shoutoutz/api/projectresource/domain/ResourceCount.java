package com.shoutoutz.api.projectresource.domain;

/**
 * 한 프로젝트의 팀 자료를 주제, 자료 직군, 원천별로 센 수
 *
 * @param track 자료 직군: BE, FE, AN, ALL 또는 null
 */
public record ResourceCount(String topicId, String track, String source, long count) {
}
