package com.shoutoutz.api.resourcereport.application;

import com.shoutoutz.api.resourcereport.application.dto.MyResourceReport;
import com.shoutoutz.api.resourcereport.application.dto.ReportableResource;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ResourceReportQueryRepository {

    /**
     * 사용자가 프로젝트 팀원인지 확인한다.
     * 서비스에 등록된 팀원(project_members)과, 이관 프로젝트 팀원 중 가입해서 맞춰진 사용자(woowa_archived_project_members.matched_user_id)를 함께 본다.
     */
    boolean isProjectMember(long projectId, long userId);

    /**
     * id 로 삭제되지 않은 자료를 찾는다. 없거나 삭제된 자료는 결과에 없다.
     */
    Map<Long, ReportableResource> findResources(Collection<Long> resourceIds);

    /**
     * 사용자가 프로젝트에 보낸 제보를 최신순으로 조회한다.
     */
    List<MyResourceReport> findMine(long projectId, long userId);
}
