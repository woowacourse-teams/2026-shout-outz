package com.shoutoutz.api.jobposting.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.shoutoutz.api.jobposting.application.JobRequirementMatchingRepository;
import com.shoutoutz.api.jobposting.application.JobRequirementMatchingRetryPolicy;
import com.shoutoutz.api.jobposting.application.JobRequirementMatchingWorker;
import com.shoutoutz.api.jobposting.application.RequirementJudge;
import com.shoutoutz.api.jobposting.application.RequirementResourceSearcher;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.TaskScheduler;

class JobRequirementMatchingConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(JobRequirementMatchingRepository.class, () -> mock(JobRequirementMatchingRepository.class))
            .withBean(RequirementResourceSearcher.class, () -> mock(RequirementResourceSearcher.class))
            .withBean(RequirementJudge.class, () -> mock(RequirementJudge.class))
            .withBean(Clock.class, Clock::systemUTC)
            .withUserConfiguration(
                    JobRequirementMatchingConfiguration.class,
                    JobRequirementMatchingWorker.class,
                    JobRequirementMatchingRetryPolicy.class,
                    JobRequirementMatchingScheduler.class
            );

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
}
