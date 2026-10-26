package com.shoutoutz.api.resourcesearch.infrastructure;

import static com.shoutoutz.api.resourcesearch.support.ResourceSearchFixture.MODEL;
import static com.shoutoutz.api.resourcesearch.support.ResourceSearchFixture.vector;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.shoutoutz.api.resourcesearch.application.ResourceSearchRepository;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceSearchCandidate;
import com.shoutoutz.api.resourcesearch.domain.ResourceTrack;
import com.shoutoutz.api.resourcesearch.support.ResourceSearchFixture;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class JdbcResourceSearchRepositoryIntegrationTest {

    @Autowired
    private ResourceSearchRepository resourceSearchRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ResourceSearchFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new ResourceSearchFixture(jdbcTemplate);
    }

    @Test
    void 코사인_유사도순으로_후보를_제한한다() {
        long distant = fixture.intro(fixture.document(ResourceTrack.BE), vector(0, 1));
        long nearest = fixture.intro(fixture.document(ResourceTrack.BE), vector(1, 0));
        long middle = fixture.intro(fixture.document(ResourceTrack.BE), vector(0.8f, 0.6f));

        List<ResourceSearchCandidate> result = search(ResourceTrack.BE, 2);

        assertThat(result).extracting(candidate -> candidate.chunk().chunkId()).containsExactly(nearest, middle);
        assertThat(result.getFirst().chunk().similarityScore()).isCloseTo(1, within(0.000001));
        assertThat(result.getLast().chunk().similarityScore()).isCloseTo(0.8, within(0.000001));
        assertThat(result).noneMatch(candidate -> candidate.chunk().chunkId() == distant);
    }

    @Test
    void 트랙은_후보_제한_전에_적용하고_공통과_미분류도_포함한다() {
        fixture.intro(fixture.document(ResourceTrack.FE), vector(1, 0));
        long backend = fixture.intro(fixture.document(ResourceTrack.BE), vector(0.8f, 0.6f));
        long common = fixture.intro(fixture.document(ResourceTrack.ALL), vector(0.6f, 0.8f));
        long unknown = fixture.intro(fixture.document(null), vector(0, 1));

        List<ResourceSearchCandidate> result = search(ResourceTrack.BE, 3);

        assertThat(result).extracting(candidate -> candidate.chunk().chunkId()).containsExactly(backend, common, unknown);
        assertThat(result).extracting(ResourceSearchCandidate::track).containsExactly(ResourceTrack.BE, ResourceTrack.ALL, null);
    }

    @Test
    void ALL과_null_요청은_모든_트랙을_검색한다() {
        for (ResourceTrack track : List.of(ResourceTrack.BE, ResourceTrack.FE, ResourceTrack.AN, ResourceTrack.ALL)) {
            fixture.intro(fixture.document(track), vector(1, 0));
        }
        fixture.intro(fixture.document(null), vector(1, 0));

        assertThat(search(ResourceTrack.ALL, 10)).hasSize(5);
        assertThat(search(null, 10)).hasSize(5);
    }

    @Test
    void 삭제된_문서와_조각_및_다른_모델을_제외한다() {
        long deletedDocument = fixture.document(ResourceTrack.BE);
        fixture.intro(deletedDocument, vector(1, 0));
        jdbcTemplate.update("UPDATE resource_documents SET deleted_at = now() WHERE id = ?", deletedDocument);
        long deletedChunk = fixture.intro(fixture.document(ResourceTrack.BE), vector(1, 0));
        jdbcTemplate.update("UPDATE resource_chunks SET deleted_at = now() WHERE id = ?", deletedChunk);
        fixture.chunk(fixture.document(ResourceTrack.BE), 0, "intro", "다른 모델", vector(1, 0), "other@1024");
        long available = fixture.intro(fixture.document(ResourceTrack.BE), vector(0, 1));

        assertThat(search(ResourceTrack.BE, 10)).extracting(candidate -> candidate.chunk().chunkId()).containsExactly(available);
    }

    @Test
    void 원본_식별자와_문서_메타데이터를_반환하고_null을_보존한다() {
        long connected = fixture.document("github-pr", ResourceTrack.BE, 42L, "example/backend", "연결된 자료");
        fixture.intro(connected, vector(1, 0));
        fixture.intro(fixture.document("blog-post", null, null, null, "독립 블로그"), vector(0, 1));

        List<ResourceSearchCandidate> result = search(null, 10);

        ResourceSearchCandidate first = result.getFirst();
        assertThat(first.documentId()).isEqualTo(connected);
        assertThat(first.projectId()).isEqualTo(42L);
        assertThat(first.repo()).isEqualTo("example/backend");
        assertThat(first.externalKey()).isEqualTo(jdbcTemplate.queryForObject(
                "SELECT external_key FROM resource_documents WHERE id = ?", String.class, connected));
        assertThat(first.source()).isEqualTo("github-pr");
        assertThat(first.title()).isEqualTo("연결된 자료");
        assertThat(first.url()).startsWith("https://example.com/");
        ResourceSearchCandidate last = result.getLast();
        assertThat(last.projectId()).isNull();
        assertThat(last.repo()).isNull();
        assertThat(last.summary()).isNull();
        assertThat(last.track()).isNull();
    }

    @Test
    void 같은_유사도는_조각_ID순으로_반환한다() {
        long first = fixture.intro(fixture.document(ResourceTrack.BE), vector(1, 0));
        long second = fixture.intro(fixture.document(ResourceTrack.BE), vector(1, 0));

        assertThat(search(null, 10)).extracting(candidate -> candidate.chunk().chunkId()).containsExactly(first, second);
    }

    @Test
    void 벡터의_크기가_달라도_방향이_같으면_유사도는_같다() {
        fixture.intro(fixture.document(ResourceTrack.BE), vector(4, 0));

        assertThat(resourceSearchRepository.findNearest(vector(2, 0), MODEL, null, 1)
                .getFirst().chunk().similarityScore()).isCloseTo(1, within(0.000001));
    }

    @Test
    void 해당_모델의_후보가_없으면_빈_목록을_반환한다() {
        assertThat(search(null, 10)).isEmpty();
    }

    @Test
    void 알수없는_DB_트랙을_미분류로_숨기지_않는다() {
        long documentId = fixture.document(ResourceTrack.BE);
        fixture.intro(documentId, vector(1, 0));
        jdbcTemplate.update("UPDATE resource_documents SET track = 'UNKNOWN' WHERE id = ?", documentId);
        jdbcTemplate.update("UPDATE resource_chunks SET track = 'UNKNOWN' WHERE document_id = ?", documentId);

        assertThatThrownBy(() -> search(null, 10))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 차원과_영벡터_및_유한하지_않은_벡터를_거절한다() {
        assertThatThrownBy(() -> resourceSearchRepository.findNearest(new float[2], MODEL, null, 1))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> resourceSearchRepository.findNearest(new float[1024], MODEL, null, 1))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> resourceSearchRepository.findNearest(vector(Float.NaN, 0), MODEL, null, 1))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> resourceSearchRepository.findNearest(vector(Float.POSITIVE_INFINITY, 0), MODEL, null, 1))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    private List<ResourceSearchCandidate> search(ResourceTrack track, int limit) {
        return resourceSearchRepository.findNearest(vector(1, 0), MODEL, track, limit);
    }
}
