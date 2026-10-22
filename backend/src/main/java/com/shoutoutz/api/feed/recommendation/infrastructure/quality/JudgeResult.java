package com.shoutoutz.api.feed.recommendation.infrastructure.quality;

import java.util.List;

/**
 * judge 응답을 검증한 결과. duplicateGroups의 각 묶음은 서로 같은 답을 요구하는 후보 id 목록이다.
 */
public record JudgeResult(List<CandidateVerdict> verdicts, List<List<String>> duplicateGroups) {

    public JudgeResult {
        verdicts = List.copyOf(verdicts);
        duplicateGroups = duplicateGroups.stream().map(List::copyOf).toList();
    }
}
