package com.shoutoutz.api.jobposting.application;

import com.shoutoutz.api.jobposting.application.dto.ClaimedJobRequirement;
import com.shoutoutz.api.jobposting.application.dto.RequirementJudgement;
import com.shoutoutz.api.jobposting.application.dto.RequirementRecommendation;
import com.shoutoutz.api.jobposting.application.dto.ResourceCandidate;
import com.shoutoutz.api.jobposting.domain.MatchingFailure;
import com.shoutoutz.api.jobposting.infrastructure.JobRequirementMatchingProperties;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 역량 하나를 가져가 검색 → 판정 → 저장한다. 검색·판정 중에는 트랜잭션을 열지 않고 작업 기한으로 다른 Worker 를 막는다.
 * 실패는 다시 하면 될 수 있는 실패만 시도 횟수 안에서 다시 시도하고, 나머지는 역량과 공고를 실패로 끝낸다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "job-posting.matching", name = "enabled", havingValue = "true")
public class JobRequirementMatchingWorker {

    private final JobRequirementMatchingRepository repository;
    private final RequirementResourceSearcher searcher;
    private final RequirementJudge judge;
    private final JobRequirementMatchingProperties properties;
    private final JobRequirementMatchingRetryPolicy retryPolicy;
    private final Clock clock;

    /**
     * @return 가져온 역량이 있었으면 true
     */
    public boolean processNext() {
        Instant now = clock.instant();
        Optional<ClaimedJobRequirement> claimed = repository.claimNext(now, now.plus(properties.lease()));
        claimed.ifPresent(this::process);
        return claimed.isPresent();
    }

    private void process(ClaimedJobRequirement requirement) {
        if (requirement.attemptCount() > properties.maxAttempts()) {
            fail(requirement, MatchingFailure.ATTEMPTS_EXHAUSTED, null);
            return;
        }
        try {
            List<RequirementRecommendation> recommendations = recommend(requirement);
            if (!repository.complete(requirement, recommendations, clock.instant())) {
                log.info("늦게 끝난 역량 매칭 결과를 버립니다. requirementId={}, attemptCount={}",
                        requirement.requirementId(), requirement.attemptCount());
            }
        } catch (JobRequirementMatchingException exception) {
            if (interrupted(exception)) {
                logInterrupted(requirement);
                return;
            }
            fail(requirement, exception.failure(), exception);
        } catch (RuntimeException exception) {
            if (interrupted(exception)) {
                logInterrupted(requirement);
                return;
            }
            fail(requirement, MatchingFailure.UNEXPECTED, exception);
        }
    }

    /**
     * 서버가 꺼지며 스케줄러가 멈추라는 신호를 보낸 경우다. 역량 탓이 아니라서 실패로 남기지 않고,
     * 분석 중으로 둔 채 작업 기한이 지나면 다시 가져가게 한다.
     * 구현체가 InterruptedException 을 감싸면서 인터럽트 표시를 지울 수 있어 원인도 따라가 본다.
     */
    private boolean interrupted(RuntimeException exception) {
        if (Thread.currentThread().isInterrupted()) {
            return true;
        }
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof InterruptedException) {
                Thread.currentThread().interrupt();
                return true;
            }
        }
        return false;
    }

    private void logInterrupted(ClaimedJobRequirement requirement) {
        log.warn("서버 종료로 멈춘 역량 매칭을 실패로 남기지 않습니다. 작업 기한이 지나면 다시 가져갑니다. requirementId={}, attemptCount={}",
                requirement.requirementId(), requirement.attemptCount());
    }

    private List<RequirementRecommendation> recommend(ClaimedJobRequirement requirement) {
        String query = requirement.name() + "\n" + requirement.summary();
        List<ResourceCandidate> candidates = searcher.search(query, requirement.jobRole(), properties.searchLimit());
        if (candidates.isEmpty()) {
            return List.of();
        }
        List<RequirementJudgement> judgements = judge.judge(requirement, candidates);
        return combine(candidates, judgements);
    }

    /**
     * 판정 결과가 후보와 하나씩 짝지어지지 않으면 판정 응답이 잘못된 것으로 보고 JUDGE_FAILED 로 다시 시도한다.
     */
    private List<RequirementRecommendation> combine(List<ResourceCandidate> candidates,
            List<RequirementJudgement> judgements) {
        if (judgements == null || judgements.size() != candidates.size()) {
            throw invalidJudgement("판정 결과 수가 후보 수와 다릅니다.");
        }
        Map<ResourceKey, RequirementJudgement> judgementsByKey = new HashMap<>();
        for (RequirementJudgement judgement : judgements) {
            if (judgement == null || judgement.verdict() == null || judgement.reason() == null) {
                throw invalidJudgement("판정 등급이나 이유가 없는 판정 결과가 있습니다.");
            }
            if (judgementsByKey.put(new ResourceKey(judgement.source(), judgement.externalKey()), judgement) != null) {
                throw invalidJudgement("같은 자료의 판정 결과가 두 번 있습니다.");
            }
        }
        return candidates.stream()
                .map(candidate -> toRecommendation(candidate, judgementsByKey))
                .toList();
    }

    private RequirementRecommendation toRecommendation(ResourceCandidate candidate,
            Map<ResourceKey, RequirementJudgement> judgementsByKey) {
        RequirementJudgement judgement = judgementsByKey.get(new ResourceKey(candidate.source(), candidate.externalKey()));
        if (judgement == null) {
            throw invalidJudgement("판정 결과가 없는 후보가 있습니다.");
        }
        return new RequirementRecommendation(
                candidate.source(),
                candidate.externalKey(),
                judgement.verdict(),
                judgement.reason(),
                judgement.description(),
                candidate.similarityScore(),
                candidate.chunks()
        );
    }

    private JobRequirementMatchingException invalidJudgement(String message) {
        return new JobRequirementMatchingException(MatchingFailure.JUDGE_FAILED, new IllegalStateException(message));
    }

    private void fail(ClaimedJobRequirement requirement, MatchingFailure failure, RuntimeException exception) {
        Instant now = clock.instant();
        if (failure.retryable() && requirement.attemptCount() < properties.maxAttempts()) {
            Instant nextAttemptAt = now.plus(retryPolicy.delayAfter(requirement.attemptCount()));
            if (repository.scheduleRetry(requirement, failure, nextAttemptAt, now)) {
                log.warn("역량 매칭에 실패해 다시 시도합니다. requirementId={}, attemptCount={}, failure={}, nextAttemptAt={}",
                        requirement.requirementId(), requirement.attemptCount(), failure, nextAttemptAt, exception);
                return;
            }
            logIgnoredFailure(requirement, failure, exception);
            return;
        }
        if (repository.markFailed(requirement, failure, now)) {
            log.warn("역량 매칭에 실패해 역량과 공고를 실패로 끝냅니다. requirementId={}, jobPostingId={}, attemptCount={}, failure={}",
                    requirement.requirementId(), requirement.jobPostingId(), requirement.attemptCount(), failure, exception);
            return;
        }
        logIgnoredFailure(requirement, failure, exception);
    }

    private void logIgnoredFailure(ClaimedJobRequirement requirement, MatchingFailure failure, RuntimeException exception) {
        log.info("늦게 끝난 역량 매칭 실패를 버립니다. requirementId={}, attemptCount={}, failure={}",
                requirement.requirementId(), requirement.attemptCount(), failure, exception);
    }

    private record ResourceKey(String source, String externalKey) {
    }
}
