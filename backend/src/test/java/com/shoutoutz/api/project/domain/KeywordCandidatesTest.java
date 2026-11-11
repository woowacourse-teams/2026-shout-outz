package com.shoutoutz.api.project.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class KeywordCandidatesTest {

    @Test
    @DisplayName("이름에 검색어가 든 프로젝트, 자료 검색에 처음 나온 순서의 프로젝트, 나머지 글자 일치 프로젝트 순으로 줄 세운다.")
    void ranksTitleMatchesThenResourceOrderThenOtherKeywordMatches() {
        KeywordCandidates candidates = new KeywordCandidates(
                List.of(
                        new KeywordCandidate(1L, false),
                        new KeywordCandidate(2L, false),
                        new KeywordCandidate(3L, false),
                        new KeywordCandidate(4L, true),
                        new KeywordCandidate(5L, false),
                        new KeywordCandidate(6L, false)
                ),
                List.of(3L, 1L, 2L)
        );

        assertThat(candidates.rankedIds(10)).containsExactly(4L, 3L, 1L, 2L, 6L, 5L);
    }

    @Test
    @DisplayName("이름에 검색어가 든 프로젝트끼리는 자료 검색 순서를 먼저 따르고, 자료가 없으면 최근 프로젝트가 앞이다.")
    void ranksTitleMatchesByResourceOrderThenNewest() {
        KeywordCandidates candidates = new KeywordCandidates(
                List.of(
                        new KeywordCandidate(7L, true),
                        new KeywordCandidate(8L, true),
                        new KeywordCandidate(9L, true)
                ),
                List.of(7L)
        );

        assertThat(candidates.rankedIds(10)).containsExactly(7L, 9L, 8L);
    }

    @Test
    @DisplayName("필터에 걸려 후보에 없는 자료 검색 프로젝트는 빼고, 정한 개수까지만 남긴다.")
    void skipsFilteredOutResourceProjectsAndLimits() {
        KeywordCandidates candidates = new KeywordCandidates(
                List.of(
                        new KeywordCandidate(1L, false),
                        new KeywordCandidate(2L, false),
                        new KeywordCandidate(3L, false)
                ),
                List.of(9L, 3L, 2L, 1L)
        );

        assertThat(candidates.rankedIds(2)).containsExactly(3L, 2L);
    }
}
