package com.shoutoutz.api.jobposting.application.dto;

import java.util.List;

/**
 * 역량으로 검색한 후보 자료(PR, 블로그 글 같은 문서) 하나. 자료는 source + externalKey 로 가리킨다 (자료 id 는 다시 적재하면 바뀐다).
 * chunks 는 이 자료에서 검색어와 관련 있게 걸린 조각이다. 자료마다 소개 1개와 경험 0~3개가 저장돼 있고,
 * 검색은 조각 단위로 찾은 뒤 같은 자료의 조각끼리 묶어 돌려준다. 자료의 모든 조각이 아니며 최소 1개다.
 * similarityScore 는 걸린 조각 중 가장 높은 점수다.
 */
public record ResourceCandidate(
        String source,
        String externalKey,
        String title,
        double similarityScore,
        List<CandidateChunk> chunks
) {
    public ResourceCandidate {
        chunks = List.copyOf(chunks);
    }
}
