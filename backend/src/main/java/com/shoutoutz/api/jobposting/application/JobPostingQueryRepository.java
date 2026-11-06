package com.shoutoutz.api.jobposting.application;

import com.shoutoutz.api.jobposting.application.dto.JobPostingListPage;
import com.shoutoutz.api.jobposting.application.dto.JobPostingSearchCondition;
import com.shoutoutz.api.jobposting.domain.JobRole;
import java.time.LocalDate;
import java.util.Map;

/**
 * 사용자에게 보여 줄 채용공고를 읽는다.
 * 분석이 끝났고(READY), 숨기지 않았고, 마감일이 없거나 오늘 이후인 공고만 읽는다.
 */
public interface JobPostingQueryRepository {

    /**
     * 조건에 맞는 공고를 최신 등록순으로 size 개 읽고, 공고마다 역량 이름 앞 3개와 역량 수를 붙인다.
     */
    JobPostingListPage findAll(JobPostingSearchCondition condition);

    /**
     * 검색어에 맞는 공고 수를 직군별로 센다. 직군 조건은 걸지 않는다. 공고가 없는 직군은 결과에 없다.
     */
    Map<JobRole, Long> countByJobRole(String keyword, LocalDate today);

    /**
     * 공고의 역량마다 사용자에게 추천할 자료 수를 센다. MATCH, PARTIAL 판정만 세고, 삭제된 자료는 뺀다.
     * 키는 역량 ID 이고, 추천할 자료가 없는 역량은 결과에 없다.
     */
    Map<Long, Long> countRecommendationsByRequirement(long jobPostingId);
}
