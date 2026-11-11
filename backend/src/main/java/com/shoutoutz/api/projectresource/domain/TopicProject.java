package com.shoutoutz.api.projectresource.domain;

/**
 * 공개된 프로젝트 하나가 그 주제의 팀 자료를 하나 이상 가졌다는 사실
 */
public record TopicProject(String topicId, long projectId) {
}
