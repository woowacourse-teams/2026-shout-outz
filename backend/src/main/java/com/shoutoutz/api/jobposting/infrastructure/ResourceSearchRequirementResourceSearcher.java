package com.shoutoutz.api.jobposting.infrastructure;

import com.shoutoutz.api.common.embedding.EmbeddingException;
import com.shoutoutz.api.jobposting.application.JobRequirementMatchingException;
import com.shoutoutz.api.jobposting.application.RequirementResourceSearcher;
import com.shoutoutz.api.jobposting.application.dto.CandidateChunk;
import com.shoutoutz.api.jobposting.application.dto.ResourceCandidate;
import com.shoutoutz.api.jobposting.domain.JobRole;
import com.shoutoutz.api.jobposting.domain.MatchingFailure;
import com.shoutoutz.api.resourcesearch.application.ResourceSearchService;
import com.shoutoutz.api.resourcesearch.application.dto.MatchedChunk;
import com.shoutoutz.api.resourcesearch.application.dto.SearchHit;
import com.shoutoutz.api.resourcesearch.domain.ResourceTrack;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

/**
 * 자료 검색 서비스로 후보 자료를 찾는다.
 * 키 누락이나 결제 한도처럼 고친 뒤에야 풀리는 임베딩 실패도 시도 횟수 안에서 다시 시도한다.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "job-posting.matching", name = "enabled", havingValue = "true")
public class ResourceSearchRequirementResourceSearcher implements RequirementResourceSearcher {

    private final ResourceSearchService resourceSearchService;

    @Override
    public List<ResourceCandidate> search(String query, JobRole jobRole, int limit) {
        List<SearchHit> hits;
        try {
            hits = resourceSearchService.search(query, toTrack(jobRole), limit);
        } catch (IllegalArgumentException exception) {
            throw new JobRequirementMatchingException(MatchingFailure.INVALID_QUERY, exception);
        } catch (EmbeddingException | DataAccessException exception) {
            throw new JobRequirementMatchingException(MatchingFailure.SEARCH_FAILED, exception);
        }
        return hits.stream().map(this::toCandidate).toList();
    }

    /**
     * 자료 트랙이 없는 직무는 전체에서 찾는다. 맞지 않는 자료는 판정에서 걸러진다.
     */
    private ResourceTrack toTrack(JobRole jobRole) {
        return switch (jobRole) {
            case BACKEND -> ResourceTrack.BE;
            case FRONTEND -> ResourceTrack.FE;
            case ANDROID -> ResourceTrack.AN;
            case IOS, DEVOPS, DATA, AI, ETC -> ResourceTrack.ALL;
        };
    }

    private ResourceCandidate toCandidate(SearchHit hit) {
        return new ResourceCandidate(
                hit.source(),
                hit.externalKey(),
                hit.title(),
                hit.similarityScore(),
                hit.matchedChunks().stream().map(this::toChunk).toList()
        );
    }

    private CandidateChunk toChunk(MatchedChunk chunk) {
        return new CandidateChunk(toKind(chunk.chunkKind()), chunk.text(), chunk.similarityScore());
    }

    /**
     * DB CHECK 로 막혀 있는 값이라, 다른 값이 오면 데이터 이상으로 보고 Worker 가 UNEXPECTED 로 끝내게 한다.
     */
    private CandidateChunk.ChunkKind toKind(String chunkKind) {
        return switch (chunkKind) {
            case "experience" -> CandidateChunk.ChunkKind.EXPERIENCE;
            case "intro" -> CandidateChunk.ChunkKind.INTRO;
            default -> throw new IllegalStateException("지원하지 않는 자료 조각 종류입니다: " + chunkKind);
        };
    }
}
