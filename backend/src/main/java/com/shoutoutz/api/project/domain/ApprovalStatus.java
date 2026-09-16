package com.shoutoutz.api.project.domain;

public enum ApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED;

    /**
     * 프로젝트를 수정한 뒤의 승인 상태
     * 반려된 프로젝트를 고치는 것은 재심사 요청이므로, 승인 대기로 되돌린다.
     * 승인된 프로젝트는 수정해도 승인을 유지하고, 심사를 기다리는 중이면 그대로 기다린다.
     */
    public ApprovalStatus afterEdit() {
        if (this == REJECTED) {
            return PENDING;
        }
        return this;
    }
}
