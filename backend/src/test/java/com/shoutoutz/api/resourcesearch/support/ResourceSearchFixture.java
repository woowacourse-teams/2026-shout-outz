package com.shoutoutz.api.resourcesearch.support;

import java.util.Arrays;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

public class ResourceSearchFixture {

    public static final String MODEL = "search-test@1024";

    private final JdbcTemplate jdbcTemplate;

    public ResourceSearchFixture(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long document(String track) {
        return document("github-pr", track, null, null, "작업 유실을 방지한 기록");
    }

    public long document(String source, String track, Long projectId, String repo, String title) {
        String externalKey = UUID.randomUUID().toString();
        return jdbcTemplate.queryForObject(
                """
                        INSERT INTO resource_documents (
                            source, external_key, version, title, url, project_id, repo,
                            track, prompt_version, updated_at
                        ) VALUES (?, ?, 1, ?, ?, ?, ?, ?, 'test-v1', now())
                        RETURNING id
                        """,
                Long.class,
                source, externalKey, title, "https://example.com/" + externalKey, projectId, repo, track
        );
    }

    public long intro(long documentId, float[] vector) {
        return chunk(documentId, 0, "intro", "배워 갈 점: 작업 유실을 방지하는 방법", vector, MODEL);
    }

    public long chunk(long documentId, int index, String kind, String text, float[] vector, String model) {
        return jdbcTemplate.queryForObject(
                """
                        INSERT INTO resource_chunks (
                            document_id, chunk_index, chunk_kind, chunk_text, token_count,
                            embedding, embedding_model, source, track
                        )
                        SELECT id, ?, ?, ?, 20, CAST(? AS vector), ?, source, track
                        FROM resource_documents WHERE id = ?
                        RETURNING resource_chunks.id
                        """,
                Long.class,
                index, kind, text, Arrays.toString(vector), model, documentId
        );
    }

    public static float[] vector(float first, float second) {
        float[] vector = new float[1024];
        vector[0] = first;
        vector[1] = second;
        return vector;
    }
}
