package com.shoutoutz.api.resourcereport.application.dto;

/**
 * 필요 없다고 제보할 수 있는지 확인할 자료. 삭제되지 않은 자료만 담는다.
 *
 * @param projectId 자료를 만든 프로젝트. 테코톡처럼 프로젝트가 없으면 null
 */
public record ReportableResource(
        long resourceId,
        Long projectId,
        String source,
        String externalKey
) {

    public boolean belongsTo(long projectId) {
        return this.projectId != null && this.projectId == projectId;
    }
}
