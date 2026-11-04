package com.shoutoutz.api.jobposting.application.dto;

/**
 * 후보 자료에서 검색된 조각 하나. 자료 하나를 데이터 파이프라인이 LLM 으로 정리해 여러 조각으로 나눠 저장한 것이다.
 * text 는 원문이 아니라 정리한 글이라, 조각을 이어 붙여도 원문이 되지 않는다.
 * 판정할 때 LLM 이 이 글을 읽고, 판정한 조각은 판정 근거(evidence)로 복사해 저장한다.
 */
public record CandidateChunk(ChunkKind kind, String text, double similarityScore) {

    public enum ChunkKind {
        /** 팀이 실제로 해결한 경험 (문제, 원인, 해결, 결과, 기술) */
        EXPERIENCE,
        /** 자료의 소개 (한 줄 안내와 배워 갈 점). 모든 자료에 하나씩 있다. 소개만으로는 팀이 실제로 해결한 경험이라고 보지 않는다 */
        INTRO
    }
}
