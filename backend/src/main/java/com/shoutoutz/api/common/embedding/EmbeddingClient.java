package com.shoutoutz.api.common.embedding;

import java.util.List;

/**
 * 선배 기록 검색의 문서 조각과 질문을 같은 모델, 같은 차원, 같은 정규화로 임베딩한다.
 * 데이터 파이프라인과 검색이 이 설정(resource.embedding)을 함께 쓴다.
 */
public interface EmbeddingClient {

    /** 문서 조각과 질문 모두 이 메서드로 임베딩한다. 설정이 normalize 면 길이 1로 정규화된 벡터를 돌려준다. */
    float[] embed(String text);

    /** 여러 개를 한 번에 임베딩한다. 결과 순서는 입력 순서와 같다. */
    List<float[]> embedAll(List<String> texts);

    /** resource_chunks.embedding_model 에 저장되는 값(예: text-embedding-3-small@1024). 질의할 때 이 값으로 거른다. */
    String modelId();
}
