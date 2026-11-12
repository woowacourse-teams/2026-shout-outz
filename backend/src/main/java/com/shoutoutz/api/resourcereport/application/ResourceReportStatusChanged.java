package com.shoutoutz.api.resourcereport.application;

import com.shoutoutz.api.resourcereport.domain.ResourceReportKind;

/**
 * 관리자가 자료 제보의 상태를 바꿨다. 필요 없어요(REMOVE) 제보의 승인 여부는 기술 검색 탭에 보이는 자료를 바꾼다.
 */
public record ResourceReportStatusChanged(ResourceReportKind kind) {
}
