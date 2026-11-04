package com.shoutoutz.api.jobposting.application;

import com.shoutoutz.api.jobposting.application.dto.JobPostingCursor;
import com.shoutoutz.api.jobposting.application.dto.JobPostingPage;
import com.shoutoutz.api.jobposting.application.dto.RequirementRecommendationDetail;
import com.shoutoutz.api.jobposting.domain.MatchingFailure;
import java.util.List;
import java.util.Map;

public interface JobPostingAdminQueryRepository {

    /**
     * 최신 등록순으로 size 개를 읽는다. cursor 가 null 이면 처음부터 읽는다.
     */
    JobPostingPage findAll(JobPostingCursor cursor, int size);

    /**
     * 공고에 속한 역량들의 판정 결과를 한 번에 읽는다. 역량마다 등급(MATCH, PARTIAL, NOT_MATCH) 순, 같은 등급은 검색 점수가 높은 순이다.
     */
    List<RequirementRecommendationDetail> findRecommendations(long jobPostingId);

    /**
     * 공고에 속한 역량 중 마지막 실패 이유가 남아 있는 역량만 역량 ID 별로 읽는다. 매칭 작업이 쓰는 칸이라 도메인에 없어서 따로 읽는다.
     */
    Map<Long, MatchingFailure> findLastErrors(long jobPostingId);
}
