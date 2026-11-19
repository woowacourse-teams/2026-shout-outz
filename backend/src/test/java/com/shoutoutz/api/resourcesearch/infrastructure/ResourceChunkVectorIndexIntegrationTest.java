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

    @Test
    void 조각_테이블은_원본_벡터에서_계산한_반정밀도_벡터를_행_안에_저장한다() {
        String generation = jdbcTemplate.queryForObject("""
                SELECT generation_expression
                FROM information_schema.columns
                WHERE table_name = 'resource_chunks' AND column_name = 'embedding_half' AND is_generated = 'ALWAYS'
                """, String.class);
        String storage = jdbcTemplate.queryForObject("""
                SELECT attstorage::text
                FROM pg_attribute
                WHERE attrelid = 'resource_chunks'::regclass AND attname = 'embedding_half'
                """, String.class);

        assertThat(generation).contains("embedding").contains("halfvec(1024)");
        // p(PLAIN)는 값을 압축하거나 행 밖으로 옮기지 않는다.
        assertThat(storage).isEqualTo("p");
    }

    @Test
    void 반정밀도_벡터를_담은_행에서_다른_열이_행_밖으로_밀려나지_않게_행_크기_목표를_올려_둔다() {
        List<String> options = jdbcTemplate.queryForList("""
                SELECT unnest(reloptions) FROM pg_class WHERE relname = 'resource_chunks'
                """, String.class);

        assertThat(options).contains("toast_tuple_target=4080");
    }
}
