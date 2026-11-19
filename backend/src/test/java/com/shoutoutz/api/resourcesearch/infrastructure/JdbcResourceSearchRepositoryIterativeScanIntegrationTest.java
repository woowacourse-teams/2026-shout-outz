package com.shoutoutz.api.resourcesearch.infrastructure;

import static com.shoutoutz.api.resourcesearch.support.ResourceSearchFixture.vector;
import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.resourcesearch.application.ResourceSearchRepository;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceSearchCandidate;
import com.shoutoutz.api.resourcesearch.domain.ResourceTrack;
import com.shoutoutz.api.resourcesearch.support.ResourceSearchFixture;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * 조각을 천 개 넘게 넣는 테스트라 트랜잭션 롤백에 맡기지 않는다. 롤백된 조각은 인덱스에 죽은 항목으로 남고,
 * 같은 벡터의 죽은 항목이 쌓이면 인덱스 조회가 살아 있는 조각을 놓쳐 뒤따르는 테스트가 실패한다.
 * 커밋한 뒤 지우고 VACUUM 으로 인덱스에서도 걷어낸다.
 */
@ActiveProfiles("test")
@SpringBootTest
class JdbcResourceSearchRepositoryIterativeScanIntegrationTest {

    private static final String MODEL = "iterative-scan-test@1024";

    @Autowired
    private ResourceSearchRepository resourceSearchRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final List<Long> documentIds = new ArrayList<>();

    @AfterEach
    void tearDown() {
        for (Long documentId : documentIds) {
            jdbcTemplate.update("DELETE FROM resource_documents WHERE id = ?", documentId);
        }
        jdbcTemplate.execute("VACUUM resource_chunks");
    }

    /**
     * 질문과 가까운 1,050개는 FE, 그 바깥 50개는 BE 다. 인덱스가 처음 읽는 후보(ef_search 1000개)가 모두 트랙 조건에 걸러져도
     * 더 읽어서 BE 후보를 채워야 한다. 두 무리 사이가 끊기면 그래프에서 건너가지 못하므로 같은 간격으로 이어 둔다.
     */
    @Test
    void 가까운_조각이_트랙_조건에_모두_걸러져도_후보를_채운다() {
        ResourceSearchFixture fixture = new ResourceSearchFixture(jdbcTemplate);
        for (int i = 0; i < 1050; i++) {
            insert(fixture, ResourceTrack.FE, i * 0.05);
        }
        for (int i = 0; i < 50; i++) {
            insert(fixture, ResourceTrack.BE, 52.5 + i * 0.05);
        }

        List<ResourceSearchCandidate> result =
                resourceSearchRepository.findCandidates(vector(1, 0), MODEL, ResourceTrack.BE, 40, List.of()).nearest();

        assertThat(result).hasSize(40)
                .allMatch(candidate -> candidate.track() == ResourceTrack.BE)
                .extracting(candidate -> candidate.chunk().similarityScore())
                .isSortedAccordingTo(Comparator.reverseOrder());
    }

    private void insert(ResourceSearchFixture fixture, ResourceTrack track, double degrees) {
        long documentId = fixture.document(track);
        documentIds.add(documentId);
        double radians = Math.toRadians(degrees);
        fixture.chunk(documentId, 0, "intro", "본문", vector((float) Math.cos(radians), (float) Math.sin(radians)), MODEL);
    }
}
