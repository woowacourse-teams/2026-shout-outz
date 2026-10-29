package com.shoutoutz.api.jobposting.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.jobposting.application.dto.CandidateChunk;
import com.shoutoutz.api.jobposting.application.dto.CandidateChunk.ChunkKind;
import com.shoutoutz.api.jobposting.application.dto.ClaimedJobRequirement;
import com.shoutoutz.api.jobposting.application.dto.RequirementJudgement;
import com.shoutoutz.api.jobposting.application.dto.RequirementRecommendation;
import com.shoutoutz.api.jobposting.application.dto.ResourceCandidate;
import com.shoutoutz.api.jobposting.domain.JobRole;
import com.shoutoutz.api.jobposting.domain.MatchingFailure;
import com.shoutoutz.api.jobposting.domain.RecommendationVerdict;
import com.shoutoutz.api.jobposting.domain.RequirementCategory;
import com.shoutoutz.api.jobposting.infrastructure.JobRequirementMatchingProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobRequirementMatchingWorkerTest {

    private static final Instant NOW = Instant.parse("2026-10-28T00:00:00Z");
    private static final Instant LEASE_UNTIL = NOW.plusSeconds(180);
    private static final String QUERY = "Spring 기반 API 개발 경험\nSpring Boot로 REST API를 설계하고 운영해 본 경험";

    @Mock
    private JobRequirementMatchingRepository repository;

    @Mock
    private RequirementResourceSearcher searcher;

    @Mock
    private RequirementJudge judge;

    private JobRequirementMatchingWorker worker;

    @BeforeEach
    void setUp() {
        JobRequirementMatchingProperties properties = new JobRequirementMatchingProperties(
                true,
                Duration.ofSeconds(10),
                Duration.ofSeconds(180),
                3,
                List.of(Duration.ofMinutes(1), Duration.ofMinutes(5)),
                10
        );
        worker = new JobRequirementMatchingWorker(
                repository,
                searcher,
                judge,
                properties,
                new JobRequirementMatchingRetryPolicy(properties),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void 가져올_역량이_없으면_검색하지_않는다() {
        when(repository.claimNext(NOW, LEASE_UNTIL)).thenReturn(Optional.empty());

        boolean processed = worker.processNext();

        assertThat(processed).isFalse();
        verifyNoInteractions(searcher, judge);
    }

    @Test
    void 역량_이름과_설명으로_검색하고_판정한_결과를_후보와_합쳐_저장한다() {
        ClaimedJobRequirement requirement = claimed(1);
        ResourceCandidate first = candidate("GITHUB_PR", "pr-1", 0.82);
        ResourceCandidate second = candidate("BLOG", "post-7", 0.61);
        when(repository.claimNext(NOW, LEASE_UNTIL)).thenReturn(Optional.of(requirement));
        when(searcher.search(QUERY, JobRole.BACKEND, 10)).thenReturn(List.of(first, second));
        when(judge.judge(requirement, List.of(first, second))).thenReturn(List.of(
                new RequirementJudgement("BLOG", "post-7", RecommendationVerdict.NOT_MATCH, "관련 없음", null),
                new RequirementJudgement("GITHUB_PR", "pr-1", RecommendationVerdict.MATCH, "API 설계 경험", "설명")
        ));
        when(repository.complete(any(), any(), any())).thenReturn(true);

        boolean processed = worker.processNext();

        assertThat(processed).isTrue();
        verify(repository).complete(requirement, List.of(
                new RequirementRecommendation("GITHUB_PR", "pr-1", RecommendationVerdict.MATCH, "API 설계 경험", "설명",
                        0.82, first.chunks()),
                new RequirementRecommendation("BLOG", "post-7", RecommendationVerdict.NOT_MATCH, "관련 없음", null,
                        0.61, second.chunks())
        ), NOW);
    }

    @Test
    void 검색_결과가_없으면_판정하지_않고_추천_없이_완료한다() {
        ClaimedJobRequirement requirement = claimed(1);
        when(repository.claimNext(NOW, LEASE_UNTIL)).thenReturn(Optional.of(requirement));
        when(searcher.search(QUERY, JobRole.BACKEND, 10)).thenReturn(List.of());

        worker.processNext();

        verifyNoInteractions(judge);
        verify(repository).complete(requirement, List.of(), NOW);
    }

    @Test
    void 늦게_끝나_저장되지_않아도_실패로_기록하지_않는다() {
        ClaimedJobRequirement requirement = claimed(1);
        when(repository.claimNext(NOW, LEASE_UNTIL)).thenReturn(Optional.of(requirement));
        when(searcher.search(QUERY, JobRole.BACKEND, 10)).thenReturn(List.of());
        when(repository.complete(requirement, List.of(), NOW)).thenReturn(false);

        worker.processNext();

        verify(repository, never()).scheduleRetry(any(), any(), any(), any());
        verify(repository, never()).markFailed(any(), any(), any());
    }

    @Test
    void 첫_시도에서_검색이_실패하면_1분_뒤로_다시_예약한다() {
        ClaimedJobRequirement requirement = claimed(1);
        when(repository.claimNext(NOW, LEASE_UNTIL)).thenReturn(Optional.of(requirement));
        when(searcher.search(QUERY, JobRole.BACKEND, 10)).thenThrow(
                new JobRequirementMatchingException(MatchingFailure.SEARCH_FAILED, new RuntimeException("timeout")));

        worker.processNext();

        verify(repository).scheduleRetry(requirement, MatchingFailure.SEARCH_FAILED, NOW.plusSeconds(60), NOW);
        verify(repository, never()).markFailed(any(), any(), any());
    }

    @Test
    void 두_번째_시도에서_판정이_실패하면_5분_뒤로_다시_예약한다() {
        ClaimedJobRequirement requirement = claimed(2);
        List<ResourceCandidate> candidates = List.of(candidate("GITHUB_PR", "pr-1", 0.82));
        when(repository.claimNext(NOW, LEASE_UNTIL)).thenReturn(Optional.of(requirement));
        when(searcher.search(QUERY, JobRole.BACKEND, 10)).thenReturn(candidates);
        when(judge.judge(requirement, candidates)).thenThrow(
                new JobRequirementMatchingException(MatchingFailure.JUDGE_FAILED, new RuntimeException("timeout")));

        worker.processNext();

        verify(repository).scheduleRetry(requirement, MatchingFailure.JUDGE_FAILED, NOW.plusSeconds(300), NOW);
    }

    @Test
    void 마지막_시도에서_실패하면_다시_예약하지_않고_실패로_끝낸다() {
        ClaimedJobRequirement requirement = claimed(3);
        when(repository.claimNext(NOW, LEASE_UNTIL)).thenReturn(Optional.of(requirement));
        when(searcher.search(QUERY, JobRole.BACKEND, 10)).thenThrow(
                new JobRequirementMatchingException(MatchingFailure.SEARCH_FAILED, new RuntimeException("timeout")));

        worker.processNext();

        verify(repository).markFailed(requirement, MatchingFailure.SEARCH_FAILED, NOW);
        verify(repository, never()).scheduleRetry(any(), any(), any(), any());
    }

    @Test
    void 잘못된_검색어는_시도_횟수가_남아도_바로_실패로_끝낸다() {
        ClaimedJobRequirement requirement = claimed(1);
        when(repository.claimNext(NOW, LEASE_UNTIL)).thenReturn(Optional.of(requirement));
        when(searcher.search(QUERY, JobRole.BACKEND, 10)).thenThrow(
                new JobRequirementMatchingException(MatchingFailure.INVALID_QUERY, new IllegalArgumentException()));

        worker.processNext();

        verify(repository).markFailed(requirement, MatchingFailure.INVALID_QUERY, NOW);
        verify(repository, never()).scheduleRetry(any(), any(), any(), any());
    }

    @Test
    void 분류되지_않은_예외는_UNEXPECTED로_바로_실패로_끝낸다() {
        ClaimedJobRequirement requirement = claimed(1);
        when(repository.claimNext(NOW, LEASE_UNTIL)).thenReturn(Optional.of(requirement));
        when(searcher.search(QUERY, JobRole.BACKEND, 10)).thenThrow(new IllegalStateException("bug"));

        worker.processNext();

        verify(repository).markFailed(requirement, MatchingFailure.UNEXPECTED, NOW);
    }

    @Test
    void 서버_종료로_검색이_멈추면_실패로_남기지_않는다() {
        ClaimedJobRequirement requirement = claimed(1);
        when(repository.claimNext(NOW, LEASE_UNTIL)).thenReturn(Optional.of(requirement));
        when(searcher.search(QUERY, JobRole.BACKEND, 10)).thenAnswer(invocation -> {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("검색이 멈췄습니다.");
        });

        try {
            worker.processNext();

            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
        verify(repository, never()).markFailed(any(), any(), any());
        verify(repository, never()).scheduleRetry(any(), any(), any(), any());
    }

    @Test
    void 감싸진_InterruptedException으로_멈춰도_실패로_남기지_않고_인터럽트_표시를_되살린다() {
        ClaimedJobRequirement requirement = claimed(1);
        when(repository.claimNext(NOW, LEASE_UNTIL)).thenReturn(Optional.of(requirement));
        when(searcher.search(QUERY, JobRole.BACKEND, 10)).thenThrow(new JobRequirementMatchingException(
                MatchingFailure.SEARCH_FAILED, new RuntimeException(new InterruptedException())));

        try {
            worker.processNext();

            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
        verify(repository, never()).markFailed(any(), any(), any());
        verify(repository, never()).scheduleRetry(any(), any(), any(), any());
    }

    @Test
    void 시도_횟수를_넘긴_역량은_검색하지_않고_ATTEMPTS_EXHAUSTED로_실패로_끝낸다() {
        ClaimedJobRequirement requirement = claimed(4);
        when(repository.claimNext(NOW, LEASE_UNTIL)).thenReturn(Optional.of(requirement));

        worker.processNext();

        verifyNoInteractions(searcher, judge);
        verify(repository).markFailed(requirement, MatchingFailure.ATTEMPTS_EXHAUSTED, NOW);
    }

    @Test
    void 판정_결과가_null이면_JUDGE_FAILED로_다시_예약한다() {
        assertJudgeFailed(null);
    }

    @Test
    void 판정_결과_수가_후보_수와_다르면_JUDGE_FAILED로_다시_예약한다() {
        assertJudgeFailed(List.of(judgement("GITHUB_PR", "pr-1", RecommendationVerdict.MATCH)));
    }

    @Test
    void 후보에_없는_자료를_판정하면_JUDGE_FAILED로_다시_예약한다() {
        assertJudgeFailed(List.of(
                judgement("GITHUB_PR", "pr-1", RecommendationVerdict.MATCH),
                judgement("BLOG", "post-999", RecommendationVerdict.PARTIAL)
        ));
    }

    @Test
    void 같은_자료를_두_번_판정하면_JUDGE_FAILED로_다시_예약한다() {
        assertJudgeFailed(List.of(
                judgement("GITHUB_PR", "pr-1", RecommendationVerdict.MATCH),
                judgement("GITHUB_PR", "pr-1", RecommendationVerdict.PARTIAL)
        ));
    }

    @Test
    void 판정_등급이_없으면_JUDGE_FAILED로_다시_예약한다() {
        assertJudgeFailed(List.of(
                judgement("GITHUB_PR", "pr-1", RecommendationVerdict.MATCH),
                judgement("BLOG", "post-7", null)
        ));
    }

    @Test
    void 판정_이유가_없으면_JUDGE_FAILED로_다시_예약한다() {
        assertJudgeFailed(List.of(
                judgement("GITHUB_PR", "pr-1", RecommendationVerdict.MATCH),
                new RequirementJudgement("BLOG", "post-7", RecommendationVerdict.PARTIAL, null, "설명")
        ));
    }

    @Test
    void 판정_결과에_null_항목이_있으면_JUDGE_FAILED로_다시_예약한다() {
        assertJudgeFailed(Arrays.asList(judgement("GITHUB_PR", "pr-1", RecommendationVerdict.MATCH), null));
    }

    private void assertJudgeFailed(List<RequirementJudgement> judgements) {
        ClaimedJobRequirement requirement = claimed(1);
        List<ResourceCandidate> candidates = List.of(
                candidate("GITHUB_PR", "pr-1", 0.82),
                candidate("BLOG", "post-7", 0.61)
        );
        when(repository.claimNext(NOW, LEASE_UNTIL)).thenReturn(Optional.of(requirement));
        when(searcher.search(anyString(), any(), anyInt())).thenReturn(candidates);
        when(judge.judge(requirement, candidates)).thenReturn(judgements == null ? null : new ArrayList<>(judgements));

        worker.processNext();

        verify(repository).scheduleRetry(requirement, MatchingFailure.JUDGE_FAILED, NOW.plusSeconds(60), NOW);
        verify(repository, never()).complete(any(), any(), any());
    }

    private ClaimedJobRequirement claimed(int attemptCount) {
        return new ClaimedJobRequirement(
                1L,
                10L,
                attemptCount,
                JobRole.BACKEND,
                "Spring 기반 API 개발 경험",
                RequirementCategory.REQUIRED,
                "Spring Boot로 REST API를 설계하고 운영해 본 경험"
        );
    }

    private ResourceCandidate candidate(String source, String externalKey, double score) {
        return new ResourceCandidate(source, externalKey, "자료 " + externalKey, score, List.of(
                new CandidateChunk(ChunkKind.INTRO, "소개 " + externalKey, score - 0.1),
                new CandidateChunk(ChunkKind.EXPERIENCE, "경험 " + externalKey, score)
        ));
    }

    private RequirementJudgement judgement(String source, String externalKey, RecommendationVerdict verdict) {
        return new RequirementJudgement(source, externalKey, verdict, "이유", "설명");
    }
}
