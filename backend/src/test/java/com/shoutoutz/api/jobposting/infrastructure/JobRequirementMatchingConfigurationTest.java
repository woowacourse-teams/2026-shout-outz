package com.shoutoutz.api.jobposting.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.common.embedding.EmbeddingProperties;
import com.shoutoutz.api.common.llm.LlmClient;
import com.shoutoutz.api.common.llm.LlmProperties;
import com.shoutoutz.api.common.llm.LlmProvider;
import com.shoutoutz.api.jobposting.application.JobRequirementMatchingRepository;
import com.shoutoutz.api.jobposting.application.JobRequirementMatchingRetryPolicy;
import com.shoutoutz.api.jobposting.application.JobRequirementMatchingWorker;
import com.shoutoutz.api.jobposting.application.RequirementJudge;
import com.shoutoutz.api.jobposting.application.RequirementResourceSearcher;
import com.shoutoutz.api.resourcesearch.application.ResourceSearchService;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.TaskScheduler;
import tools.jackson.databind.ObjectMapper;

class JobRequirementMatchingConfigurationTest {

    private final ApplicationContextRunner contextRunner = contextRunner("embedding-key");

    private static ApplicationContextRunner contextRunner(String embeddingApiKey) {
        return contextRunner(embeddingApiKey, true);
    }

    private static ApplicationContextRunner contextRunner(String embeddingApiKey, boolean hasSearchableChunks) {
        return new ApplicationContextRunner()
                .withBean(JobRequirementMatchingRepository.class, () -> repository(hasSearchableChunks))
                .withBean(RequirementResourceSearcher.class, () -> mock(RequirementResourceSearcher.class))
                .withBean(RequirementJudge.class, () -> mock(RequirementJudge.class))
                .withBean(Clock.class, Clock::systemUTC)
                .withBean(LlmProperties.class, () -> new LlmProperties(null, null, null))
                .withBean(EmbeddingProperties.class, () -> embedding(embeddingApiKey, Duration.ofSeconds(30)))
                .withUserConfiguration(
                        JobRequirementMatchingConfiguration.class,
                        JobRequirementMatchingWorker.class,
                        JobRequirementMatchingRetryPolicy.class,
                        JobRequirementMatchingScheduler.class
                );
    }

    @Test
    void 꺼져_있으면_설정과_Worker와_스케줄러를_만들지_않는다() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(JobRequirementMatchingProperties.class);
            assertThat(context).doesNotHaveBean(JobRequirementMatchingWorker.class);
            assertThat(context).doesNotHaveBean(JobRequirementMatchingRetryPolicy.class);
            assertThat(context).doesNotHaveBean(JobRequirementMatchingScheduler.class);
        });
    }

    @Test
    void 켜면_yml_값으로_Worker와_스케줄러가_뜨고_TaskScheduler_빈은_만들지_않는다() {
        contextRunner.withPropertyValues(
                "job-posting.matching.enabled=true",
                "job-posting.matching.poll-interval=1h",
                "job-posting.matching.search-limit=5"
        ).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(JobRequirementMatchingProperties.class).searchLimit()).isEqualTo(5);
            assertThat(context).hasSingleBean(JobRequirementMatchingWorker.class);
            assertThat(context.getBean(JobRequirementMatchingScheduler.class).isRunning()).isTrue();
            assertThat(context).doesNotHaveBean(TaskScheduler.class);
        });
    }

    @Test
    void 설정값이_잘못되면_기동하지_않는다() {
        contextRunner.withPropertyValues(
                "job-posting.matching.enabled=true",
                "job-posting.matching.max-attempts=0"
        ).run(context -> assertThat(context).hasFailed());
    }

    @Test
    void 끄면_실제_검색과_판정_구현체를_만들지_않고_켜면_주입해_만든다() {
        ApplicationContextRunner realImplementations = new ApplicationContextRunner()
                .withBean(ResourceSearchService.class, () -> mock(ResourceSearchService.class))
                .withBean(JobRequirementMatchingRepository.class, () -> repository(true))
                .withBean("gpt", LlmClient.class, () -> llmClient(LlmProvider.BEDROCK_GPT))
                .withBean("claude", LlmClient.class, () -> llmClient(LlmProvider.BEDROCK_CLAUDE))
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .withBean(LlmProperties.class, () -> new LlmProperties(null, null, null))
                .withBean(EmbeddingProperties.class, () -> embedding("embedding-key", Duration.ofSeconds(30)))
                .withUserConfiguration(
                        JobRequirementMatchingConfiguration.class,
                        ResourceSearchRequirementResourceSearcher.class,
                        LlmRequirementJudge.class
                );

        realImplementations.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(RequirementResourceSearcher.class);
            assertThat(context).doesNotHaveBean(RequirementJudge.class);
        });
        realImplementations.withPropertyValues("job-posting.matching.enabled=true").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).getBean(RequirementResourceSearcher.class)
                    .isInstanceOf(ResourceSearchRequirementResourceSearcher.class);
            assertThat(context).getBean(RequirementJudge.class).isInstanceOf(LlmRequirementJudge.class);
        });
    }

    @Test
    void 켜져_있을_때만_기동_검사가_돌아_임베딩_키가_없으면_기동에_실패한다() {
        contextRunner(" ").run(context -> assertThat(context).hasNotFailed());
        contextRunner(" ").withPropertyValues("job-posting.matching.enabled=true")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void 기본_제한_시간과_Bedrock_제공자는_키_없이_검사를_통과한다() {
        assertThatCode(() -> validate(properties(Duration.ofSeconds(180), LlmProvider.BEDROCK_GPT,
                LlmProvider.BEDROCK_CLAUDE), new LlmProperties(null, null, null), embedding("key", Duration.ofSeconds(30))))
                .doesNotThrowAnyException();
    }

    @Test
    void 임베딩_키가_없으면_기동에_실패한다() {
        assertThatThrownBy(() -> validate(properties(Duration.ofSeconds(180), LlmProvider.BEDROCK_GPT),
                new LlmProperties(null, null, null), embedding(" ", Duration.ofSeconds(30))))
                .hasMessageContaining("임베딩 API 키");
    }

    @Test
    void 판정_제공자의_키가_없으면_기동에_실패한다() {
        assertThatThrownBy(() -> validate(properties(Duration.ofSeconds(180), LlmProvider.OPENAI),
                new LlmProperties(null, null, null), embedding("key", Duration.ofSeconds(30))))
                .hasMessageContaining("OPENAI를 사용하려면 API 키");
    }

    @Test
    void 임베딩까지_더한_제한_시간이_lease에_닿으면_기동에_실패한다() {
        // 임베딩 5초 + 30초, 제공자 2개 50초씩으로 합이 135초다.
        JobRequirementMatchingProperties justEnough = properties(Duration.ofSeconds(136),
                LlmProvider.BEDROCK_GPT, LlmProvider.BEDROCK_CLAUDE);
        JobRequirementMatchingProperties tooShort = properties(Duration.ofSeconds(135),
                LlmProvider.BEDROCK_GPT, LlmProvider.BEDROCK_CLAUDE);
        LlmProperties llm = new LlmProperties(null, null, null);
        EmbeddingProperties embedding = embedding("key", Duration.ofSeconds(30));

        assertThatCode(() -> validate(justEnough, llm, embedding)).doesNotThrowAnyException();
        assertThatThrownBy(() -> validate(tooShort, llm, embedding)).hasMessageContaining("lease");
    }

    @Test
    void 현재_임베딩_모델로_만든_자료_조각이_없으면_기동에_실패한다() {
        JobRequirementMatchingRepository repository = mock(JobRequirementMatchingRepository.class);
        when(repository.hasSearchableChunks("text-embedding-3-small@1024")).thenReturn(false);

        assertThatThrownBy(() -> validate(properties(Duration.ofSeconds(180), LlmProvider.BEDROCK_GPT),
                new LlmProperties(null, null, null), embedding("key", Duration.ofSeconds(30)), repository))
                .hasMessageContaining("text-embedding-3-small@1024로 만든 자료 조각");
    }

    @Test
    void 켜져_있을_때만_자료_조각을_검사해_없으면_기동에_실패한다() {
        contextRunner("embedding-key", false).run(context -> assertThat(context).hasNotFailed());
        contextRunner("embedding-key", false).withPropertyValues("job-posting.matching.enabled=true")
                .run(context -> assertThat(context).hasFailed());
    }

    private void validate(JobRequirementMatchingProperties properties, LlmProperties llm, EmbeddingProperties embedding)
            throws Exception {
        validate(properties, llm, embedding, repository(true));
    }

    private void validate(JobRequirementMatchingProperties properties, LlmProperties llm, EmbeddingProperties embedding,
            JobRequirementMatchingRepository repository) throws Exception {
        new JobRequirementMatchingConfiguration()
                .jobRequirementMatchingSettingsValidator(properties, llm, embedding, repository)
                .afterPropertiesSet();
    }

    private static JobRequirementMatchingRepository repository(boolean hasSearchableChunks) {
        JobRequirementMatchingRepository repository = mock(JobRequirementMatchingRepository.class);
        when(repository.hasSearchableChunks(anyString())).thenReturn(hasSearchableChunks);
        return repository;
    }

    private JobRequirementMatchingProperties properties(Duration lease, LlmProvider... providers) {
        return new JobRequirementMatchingProperties(true, Duration.ofSeconds(10), lease, 3,
                List.of(Duration.ofMinutes(1)), 10, List.of(providers));
    }

    private static EmbeddingProperties embedding(String apiKey, Duration readTimeout) {
        return new EmbeddingProperties("openai", null, 1024, true, apiKey, Duration.ofSeconds(5), readTimeout);
    }

    private static LlmClient llmClient(LlmProvider provider) {
        LlmClient client = mock(LlmClient.class);
        when(client.provider()).thenReturn(provider);
        return client;
    }
}
