package com.shoutoutz.api.project.application;

import static com.shoutoutz.api.resourcesearch.support.ResourceSearchFixture.MODEL;
import static com.shoutoutz.api.resourcesearch.support.ResourceSearchFixture.vector;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.common.embedding.EmbeddingClient;
import com.shoutoutz.api.project.presentation.dto.request.ProjectFindAllRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 프로젝트 직접 검색이 외부 임베딩 API 응답을 기다리는 동안 DB 커넥션을 잡고 있지 않은지 확인한다.
 * 테스트 트랜잭션이 끼면 확인할 수 없으므로 클래스에 트랜잭션을 걸지 않는다. 검색만 하므로 남는 데이터가 없다.
 */
@ActiveProfiles("test")
@SpringBootTest
class ProjectSearchTransactionIntegrationTest {

    @Autowired
    private ProjectService projectService;

    @MockitoBean
    private EmbeddingClient embeddingClient;

    @Test
    @DisplayName("직접 검색은 트랜잭션 밖에서 검색어를 임베딩해, 외부 응답을 기다리는 동안 스레드에 묶인 커넥션이 없다.")
    void embedsKeywordOutsideTransaction() {
        AtomicBoolean transactionActive = new AtomicBoolean(true);
        Map<Object, Object> boundResources = new HashMap<>();
        when(embeddingClient.modelId()).thenReturn(MODEL);
        when(embeddingClient.embed(anyString())).thenAnswer(invocation -> {
            transactionActive.set(TransactionSynchronizationManager.isActualTransactionActive());
            boundResources.putAll(TransactionSynchronizationManager.getResourceMap());
            return vector(1, 0);
        });

        projectService.findAll(new ProjectFindAllRequest(
                "tx-scope-" + UUID.randomUUID(), null, null, null, null, null, null));

        verify(embeddingClient).embed(anyString());
        assertThat(transactionActive).isFalse();
        assertThat(boundResources).isEmpty();
    }
}
