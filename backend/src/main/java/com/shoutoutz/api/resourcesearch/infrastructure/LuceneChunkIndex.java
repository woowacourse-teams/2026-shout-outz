package com.shoutoutz.api.resourcesearch.infrastructure;

import com.shoutoutz.api.resourcesearch.application.dto.LexicalChunk;
import com.shoutoutz.api.resourcesearch.application.dto.LexicalMatch;
import com.shoutoutz.api.resourcesearch.application.dto.RankedResource;
import com.shoutoutz.api.resourcesearch.domain.ResourceTrack;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.lucene.analysis.ko.KoreanAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermInSetQuery;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.QueryBuilder;

/**
 * 조각 본문을 한국어 형태소로 나눠 메모리에 색인하고 BM25 점수로 찾는다.
 * 한 번 만들면 내용을 바꾸지 않는다. 자료가 바뀌면 새 색인을 만들어 교체한다.
 */
public final class LuceneChunkIndex implements AutoCloseable {

    private static final String BODY = "body";
    private static final String TRACK = "track";
    private static final String CHUNK_ID = "chunkId";
    private static final String CHUNK_ORDER = "chunkOrder";
    private static final String DOCUMENT_ID = "documentId";
    private static final String PROJECT_ID = "projectId";
    private static final String SOURCE = "source";
    // 트랙이 없는 조각도 트랙 조건으로 찾을 수 있게 색인에 넣는 값이다.
    private static final String UNCLASSIFIED = "UNCLASSIFIED";
    private static final float K1 = 1.2f;
    private static final float B = 0.75f;
    private static final Sort SCORE_THEN_CHUNK_ID =
            new Sort(SortField.FIELD_SCORE, new SortField(CHUNK_ORDER, SortField.Type.LONG));

    private final KoreanAnalyzer analyzer;
    private final Directory directory;
    private final DirectoryReader reader;
    private final IndexSearcher searcher;

    private LuceneChunkIndex(KoreanAnalyzer analyzer, Directory directory, DirectoryReader reader) {
        this.analyzer = analyzer;
        this.directory = directory;
        this.reader = reader;
        this.searcher = new IndexSearcher(reader);
        this.searcher.setSimilarity(new BM25Similarity(K1, B));
    }

    public static LuceneChunkIndex of(List<LexicalChunk> chunks) {
        validateDistinct(chunks);
        KoreanAnalyzer analyzer = new KoreanAnalyzer();
        Directory directory = new ByteBuffersDirectory();
        try {
            IndexWriterConfig config = new IndexWriterConfig(analyzer).setSimilarity(new BM25Similarity(K1, B));
            try (IndexWriter writer = new IndexWriter(directory, config)) {
                for (LexicalChunk chunk : chunks) {
                    writer.addDocument(toDocument(chunk));
                }
                writer.commit();
            }
            return new LuceneChunkIndex(analyzer, directory, DirectoryReader.open(directory));
        } catch (IOException exception) {
            analyzer.close();
            throw new UncheckedIOException("렉시컬 색인을 만들지 못했습니다.", exception);
        }
    }

    public List<LexicalMatch> search(String query, ResourceTrack track, int limit) {
        List<LexicalMatch> matches = new ArrayList<>();
        for (ScoredDocument scored : find(query, track, limit)) {
            long chunkId = scored.document().getField(CHUNK_ID).numericValue().longValue();
            matches.add(new LexicalMatch(chunkId, scored.score()));
        }
        return List.copyOf(matches);
    }

    public List<RankedResource> searchResources(String query, ResourceTrack track, int limit) {
        Map<Long, RankedResource> resources = new LinkedHashMap<>();
        for (ScoredDocument scored : find(query, track, limit)) {
            RankedResource resource = toResource(scored.document());
            resources.putIfAbsent(resource.documentId(), resource);
        }
        return List.copyOf(resources.values());
    }

    private List<ScoredDocument> find(String query, ResourceTrack track, int limit) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("검색어는 비어 있을 수 없습니다.");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("후보 수는 1 이상이어야 합니다.");
        }
        Query words = new QueryBuilder(analyzer).createBooleanQuery(BODY, normalize(query), BooleanClause.Occur.SHOULD);
        if (words == null) {
            return List.of();
        }
        try {
            ScoreDoc[] hits = searcher.search(withTrack(words, track), limit, SCORE_THEN_CHUNK_ID, true).scoreDocs;
            StoredFields storedFields = searcher.storedFields();
            List<ScoredDocument> found = new ArrayList<>(hits.length);
            for (ScoreDoc hit : hits) {
                found.add(new ScoredDocument(storedFields.document(hit.doc), hit.score));
            }
            return found;
        } catch (IOException exception) {
            throw new UncheckedIOException("렉시컬 색인을 읽지 못했습니다.", exception);
        }
    }

    private static RankedResource toResource(Document document) {
        Long projectId = null;
        if (document.getField(PROJECT_ID) != null) {
            projectId = document.getField(PROJECT_ID).numericValue().longValue();
        }
        return new RankedResource(
                document.getField(DOCUMENT_ID).numericValue().longValue(),
                projectId,
                document.get(SOURCE)
        );
    }

    private record ScoredDocument(Document document, double score) {
    }

    public int size() {
        return reader.numDocs();
    }

    @Override
    public void close() {
        try {
            reader.close();
            directory.close();
        } catch (IOException exception) {
            throw new UncheckedIOException("렉시컬 색인을 닫지 못했습니다.", exception);
        } finally {
            analyzer.close();
        }
    }

    private static void validateDistinct(List<LexicalChunk> chunks) {
        Set<Long> chunkIds = new HashSet<>();
        for (LexicalChunk chunk : chunks) {
            if (!chunkIds.add(chunk.chunkId())) {
                throw new IllegalArgumentException("같은 조각을 두 번 색인할 수 없습니다: " + chunk.chunkId());
            }
        }
    }

    private static Document toDocument(LexicalChunk chunk) {
        Document document = new Document();
        document.add(new StoredField(CHUNK_ID, chunk.chunkId()));
        document.add(new NumericDocValuesField(CHUNK_ORDER, chunk.chunkId()));
        document.add(new TextField(BODY, normalize(chunk.text()), Field.Store.NO));
        document.add(new StringField(TRACK, trackValue(chunk.track()), Field.Store.NO));
        document.add(new StoredField(DOCUMENT_ID, chunk.resource().documentId()));
        if (chunk.resource().projectId() != null) {
            document.add(new StoredField(PROJECT_ID, chunk.resource().projectId()));
        }
        if (chunk.resource().source() != null) {
            document.add(new StoredField(SOURCE, chunk.resource().source()));
        }
        return document;
    }

    private static String trackValue(ResourceTrack track) {
        if (track == null) {
            return UNCLASSIFIED;
        }
        return track.name();
    }

    // 전각 문자와 대소문자 차이로 같은 기술명을 놓치지 않게 한다. 본문과 검색어에 똑같이 적용해야 서로 맞는다.
    private static String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFKC)
                .strip()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }

    private static Query withTrack(Query words, ResourceTrack track) {
        BooleanQuery.Builder query = new BooleanQuery.Builder().add(words, BooleanClause.Occur.MUST);
        if (track != null && track != ResourceTrack.ALL) {
            List<BytesRef> tracks = List.of(
                    new BytesRef(track.name()),
                    new BytesRef(ResourceTrack.ALL.name()),
                    new BytesRef(UNCLASSIFIED)
            );
            query.add(new TermInSetQuery(TRACK, tracks), BooleanClause.Occur.FILTER);
        }
        return query.build();
    }
}
