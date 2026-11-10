package com.shoutoutz.api.resourcereport.application;

import com.shoutoutz.api.resourcereport.application.dto.ResourceReportAdminCursor;
import com.shoutoutz.api.resourcereport.application.dto.ResourceReportAdminPage;
import com.shoutoutz.api.resourcereport.domain.ResourceReportKind;
import com.shoutoutz.api.resourcereport.domain.ResourceReportStatus;

public interface ResourceReportAdminQueryRepository {

    /**
     * 관리자 목록을 최신순으로 한 페이지 조회한다. status, kind 가 null 이면 거르지 않는다.
     */
    ResourceReportAdminPage findAll(
            ResourceReportStatus status,
            ResourceReportKind kind,
            ResourceReportAdminCursor cursor,
            int size
    );
}
