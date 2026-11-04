package com.shoutoutz.api.jobposting.application;

import com.shoutoutz.api.jobposting.application.dto.ClaimedJobRequirement;
import com.shoutoutz.api.jobposting.application.dto.RequirementRecommendation;
import com.shoutoutz.api.jobposting.domain.MatchingFailure;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 역량 매칭 작업의 상태를 바꾼다. 여러 서버가 같은 역량을 동시에 가져가지 않도록 가져오기는 한 SQL 문으로 선점한다.
 * 결과를 저장하는 메서드는 가져갈 때 받은 claimToken 과 다르면 아무것도 바꾸지 않는다.
 * 작업 기한이 지나 다른 Worker 가 다시 가져갔거나 운영자가 다시 분석한 역량에, 늦게 끝난 결과를 덮어쓰지 않기 위해서다.
 */
public interface JobRequirementMatchingRepository {

    /**
     * 대기 중이고 다시 시도할 시각이 지난 역량, 또는 분석 중인데 작업 기한이 지난 역량 하나를 가져간다.
     * 시도 횟수를 하나 올리고 claimToken 을 새로 만들며, 공고가 대기 중이면 분석 중으로 바꾼다.
     */
    Optional<ClaimedJobRequirement> claimNext(Instant now, Instant leaseUntil);

    /**
     * 역량을 대기로 되돌려 nextAttemptAt 이 지난 뒤 다시 가져가게 한다.
     *
     * @return 바꿨으면 true, 늦게 끝난 결과라 무시했으면 false
     */
    boolean scheduleRetry(ClaimedJobRequirement requirement, MatchingFailure failure, Instant nextAttemptAt, Instant now);

    /**
     * 역량을 실패로 끝내고 공고도 실패로 바꾼다. 같은 공고의 다른 역량은 계속 처리한다.
     *
     * @return 바꿨으면 true, 늦게 끝난 결과라 무시했으면 false
     */
    boolean markFailed(ClaimedJobRequirement requirement, MatchingFailure failure, Instant now);

    /**
     * 역량의 판정 결과를 저장하고 역량을 완료로 바꾼다. 이전에 저장한 판정 결과는 지우고 새로 넣는다.
     * 공고의 역량이 모두 완료면 공고도 완료로 바꾼다. 이미 실패한 공고는 실패로 둔다.
     *
     * @return 저장했으면 true, 늦게 끝난 결과라 무시했으면 false
     */
    boolean complete(ClaimedJobRequirement requirement, List<RequirementRecommendation> recommendations, Instant now);

    /**
     * 이 임베딩 모델로 만든, 지워지지 않은 자료 조각이 하나라도 있는지 본다.
     * 없으면 검색 결과가 늘 비어 모든 역량이 추천 없이 완료된다.
     */
    boolean hasSearchableChunks(String embeddingModel);
}
