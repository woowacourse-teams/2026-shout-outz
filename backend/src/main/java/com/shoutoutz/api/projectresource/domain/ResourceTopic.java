package com.shoutoutz.api.projectresource.domain;

/**
 * 기술 주제 카탈로그(resource_topic_catalog)의 주제 하나
 *
 * @param topicId  주제 id. 자료의 topic 이 이 값을 가리킨다
 * @param side     주제 직군: backend, frontend, mobile, common
 * @param category 기술 이야기 탭에서 주제를 묶는 분야
 */
public record ResourceTopic(String topicId, String name, String side, String category) {
}
