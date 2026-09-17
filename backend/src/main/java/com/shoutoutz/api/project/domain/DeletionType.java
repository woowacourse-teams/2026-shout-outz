package com.shoutoutz.api.project.domain;

/**
 * 프로젝트 삭제 이력의 삭제 유형
 */
public enum DeletionType {
    SELF_DELETE,
    ADMIN_DELETE,
    SYSTEM_PURGE
}
