package com.shoutoutz.api.resourcesearch.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class ResourceChunkVectorIndexIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 조각의_벡터_인덱스는_halfvec_하나만_있다() {
        List<String> definitions = jdbcTemplate.queryForList("""
                SELECT indexdef
                FROM pg_indexes
                WHERE tablename = 'resource_chunks' AND indexdef LIKE '%USING hnsw%'
                """, String.class);

        assertThat(definitions).singleElement().asString()
                .contains("resource_chunks_embedding_half_idx")
                .contains("halfvec(1024)")
                .contains("halfvec_cosine_ops");
    }

    @Test
    void 벡터_인덱스는_쓸_수_있는_상태다() {
        Boolean valid = jdbcTemplate.queryForObject("""
                SELECT i.indisvalid AND i.indisready
                FROM pg_index i
                JOIN pg_class c ON c.oid = i.indexrelid
                WHERE c.relname = 'resource_chunks_embedding_half_idx'
                """, Boolean.class);

        assertThat(valid).isTrue();
    }
}
