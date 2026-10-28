package com.shoutoutz.api.jobposting.application;

import com.shoutoutz.api.jobposting.application.dto.ClaimedJobRequirement;
import com.shoutoutz.api.jobposting.domain.MatchingFailure;
import java.time.Instant;
import java.util.Optional;

/**
 * 역량 매칭 작업의 상태를 바꾼다. 여러 서버가 같은 역량을 동시에 가져가지 않도록 가져오기는 한 SQL 문으로 선점한다.
 * 결과를 저장하는 메서드는 가져갈 때의 시도 횟수와 다르면 아무것도 바꾸지 않는다.
 * 작업 기한이 지나 다른 Worker 가 다시 가져간 역량에 늦게 끝난 결과를 덮어쓰지 않기 위해서다.
 */
public interface JobRequirementMatchingRepository {

    /**
     * 대기 중이고 다시 시도할 시각이 지난 역량, 또는 분석 중인데 작업 기한이 지난 역량 하나를 가져간다.
     * 시도 횟수를 하나 올리고, 공고가 대기 중이면 분석 중으로 바꾼다.
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
}
