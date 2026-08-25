package com.yue.jobcomparer.event;

import com.yue.jobcomparer.entity.AnalysisFailureReason;
import com.yue.jobcomparer.service.AnalysisPersistenceService;
import com.yue.jobcomparer.service.AnalysisRunner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * Hands a submitted analysis over to the background executor.
 *
 * <p>Bound to AFTER_COMMIT rather than invoked directly from the service: the
 * worker runs in its own transaction and cannot see rows the submitting
 * transaction has not committed yet. Triggering before commit is a race that
 * usually passes locally and fails intermittently under load.
 *
 * <p>Not annotated with {@code @Async}. Submission to the pool has to happen
 * here so that a RejectedExecutionException can be caught - with {@code @Async}
 * the rejection is thrown by the proxy, before the method body runs, and the
 * row would silently stary PENDING forever.
 */
@Slf4j
@Component
public class AnalysisEventListener {

    private final Executor analysisTaskExecutor;
    private final AnalysisRunner analysisRunner;
    private final AnalysisPersistenceService analysisPersistenceService;

    public AnalysisEventListener(
            @Qualifier("analysisTaskExecutor") Executor analysisTaskExecutor,
            AnalysisRunner analysisRunner,
            AnalysisPersistenceService analysisPersistenceService) {
        this.analysisTaskExecutor = analysisTaskExecutor;
        this.analysisRunner = analysisRunner;
        this.analysisPersistenceService = analysisPersistenceService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAnalysisSubmitted(AnalysisSubmittedEvent event) {
        try {
            analysisTaskExecutor.execute(() -> analysisRunner.run(event.analysisId(), event.userId()));
        } catch (RejectedExecutionException e) {
            log.error("Analysis {} rejected: executor queue is full", event.analysisId(), e);
            analysisPersistenceService.markFailed(event.analysisId(), AnalysisFailureReason.INTERNAL_ERROR);
        }
    }
}
