package com.shoutoutz.api.jobposting.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.jobposting.application.JobRequirementMatchingWorker;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class JobRequirementMatchingSchedulerTest {

    private final JobRequirementMatchingWorker worker = mock(JobRequirementMatchingWorker.class);
    private final JobRequirementMatchingScheduler scheduler = new JobRequirementMatchingScheduler(worker,
            new JobRequirementMatchingProperties(true, Duration.ofHours(1), Duration.ofSeconds(180), 3,
                    List.of(Duration.ofMinutes(1), Duration.ofMinutes(5)), 10));

    @AfterEach
    void tearDown() {
        scheduler.stop();
    }

    @Test
    void 한_번_깨어나면_가져올_역량이_없을_때까지_이어서_처리한다() {
        when(worker.processNext()).thenReturn(true, true, false);
        scheduler.start();

        scheduler.processPending();

        verify(worker, times(3)).processNext();
    }

    @Test
    void Worker가_예외를_던져도_밖으로_내보내지_않는다() {
        when(worker.processNext()).thenThrow(new IllegalStateException("db down"));
        scheduler.start();

        scheduler.processPending();

        verify(worker).processNext();
    }

    @Test
    void 멈췄다가_다시_켜면_다시_주기적으로_처리한다() {
        JobRequirementMatchingScheduler restartable = new JobRequirementMatchingScheduler(worker,
                new JobRequirementMatchingProperties(true, Duration.ofMillis(10), Duration.ofSeconds(180), 3,
                        List.of(Duration.ofMinutes(1)), 10));
        restartable.start();
        restartable.stop();
        clearInvocations(worker);

        restartable.start();

        try {
            verify(worker, timeout(1_000).atLeastOnce()).processNext();
            assertThat(restartable.isRunning()).isTrue();
        } finally {
            restartable.stop();
        }
    }

    @Test
    void 켜기_전에_멈춰도_예외가_나지_않는다() {
        assertThatCode(scheduler::stop).doesNotThrowAnyException();
    }

    @Test
    void 멈춘_뒤에는_역량을_가져가지_않는다() {
        scheduler.start();
        scheduler.stop();

        scheduler.processPending();

        verify(worker, times(0)).processNext();
        assertThat(scheduler.isRunning()).isFalse();
    }
}
