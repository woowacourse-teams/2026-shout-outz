package com.shoutoutz.api.project.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class KeywordCandidatesTest {

    private static final Instant NOW = Instant.parse("2026-11-12T00:00:00Z");

    @Test
    @DisplayName("이름에 검색어가 든 프로젝트, 자료 검색에 처음 나온 순서의 프로젝트, 나머지 글자 일치 프로젝트 순으로 줄 세운다.")
    void ranksNameMatchesThenResourceOrderThenOtherKeywordMatches() {
        KeywordCandidates candidates = new KeywordCandidates(
                List.of(
                        candidate(1L, false, 10),
                        candidate(2L, false, 20),
                        candidate(3L, false, 30),
                        candidate(4L, true, 40),
                        candidate(5L, false, 50),
                        candidate(6L, false, 60)
                ),
                List.of(3L, 1L, 2L)
        );

        assertThat(candidates.rankedIds(10)).containsExactly(4L, 3L, 1L, 2L, 5L, 6L);
    }

    @Test
    @DisplayName("나머지 글자 일치 프로젝트는 id가 아니라 등록 시각이 최근인 순서다. 이관 프로젝트는 id가 커도 등록 시각이 오래됐을 수 있다.")
    void ranksOtherKeywordMatchesByCreatedAtNotId() {
        KeywordCandidates candidates = new KeywordCandidates(
                List.of(
                        candidate(100L, false, 500),
                        candidate(5L, false, 1)
                ),
                List.of()
        );

        assertThat(candidates.rankedIds(10)).containsExactly(5L, 100L);
    }

    @Test
    @DisplayName("이름에 검색어가 든 프로젝트끼리는 자료 검색 순서를 먼저 따르고, 자료가 없으면 최근에 등록된 프로젝트가 앞이다.")
    void ranksNameMatchesByResourceOrderThenNewest() {
        KeywordCandidates candidates = new KeywordCandidates(
                List.of(
                        candidate(7L, true, 30),
                        candidate(8L, true, 10),
                        candidate(9L, true, 20)
                ),
                List.of(7L)
        );

        assertThat(candidates.rankedIds(10)).containsExactly(7L, 8L, 9L);
    }

    @Test
    @DisplayName("필터에 걸려 후보에 없는 자료 검색 프로젝트는 빼고, 정한 개수까지만 남긴다.")
    void skipsFilteredOutResourceProjectsAndLimits() {
        KeywordCandidates candidates = new KeywordCandidates(
                List.of(
                        candidate(1L, false, 10),
                        candidate(2L, false, 20),
                        candidate(3L, false, 30)
                ),
                List.of(9L, 3L, 2L, 1L)
        );

        assertThat(candidates.rankedIds(2)).containsExactly(3L, 2L);
    }

    private static KeywordCandidate candidate(long id, boolean nameMatched, long minutesAgo) {
        return new KeywordCandidate(id, nameMatched, NOW.minusSeconds(minutesAgo * 60));
    }
}
