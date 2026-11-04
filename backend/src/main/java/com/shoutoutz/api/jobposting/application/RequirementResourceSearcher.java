package com.shoutoutz.api.jobposting.application;

import com.shoutoutz.api.jobposting.application.dto.ResourceCandidate;
import com.shoutoutz.api.jobposting.domain.JobRole;
import java.util.List;

/**
 * 역량 문장으로 관련 있어 보이는 자료를 찾는다. 검색 점수가 높다고 역량에 맞는 자료는 아니어서 판정을 거친다.
 * 구현은 자료 검색 서비스(ResourceSearchService)를 감싸 직무를 자료 트랙으로 바꿔 넘긴다.
 * 실패는 JobRequirementMatchingException 으로 분류해 던지고, 실패를 빈 목록으로 바꾸지 않는다.
 */
public interface RequirementResourceSearcher {

    /**
     * @param limit 돌려받을 최대 자료 수. 조각 수가 아니다
     * @return 관련 있어 보이는 자료가 없으면 빈 목록
     */
    List<ResourceCandidate> search(String query, JobRole jobRole, int limit);
}
