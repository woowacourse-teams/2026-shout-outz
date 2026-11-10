package com.shoutoutz.api.resourcereport.domain;

import java.util.Optional;

public interface ResourceReportRepository {

    /**
     * 제보를 저장하고 id 를 돌려준다.
     * 같은 사람이 같은 자료(필요 없어요)나 같은 주소(추가해 주세요)를 이미 제보했으면 새로 만들지 않고 그 제보의 id 를 돌려준다.
     */
    long saveIfAbsent(ResourceReport report);

    Optional<ResourceReport> findById(long reportId);

    /**
     * 처리 상태와 처리한 관리자, 처리 시각, 수정 시각을 저장한다.
     */
    ResourceReport updateStatus(ResourceReport report);
}
