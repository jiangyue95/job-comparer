package com.yue.jobcomparer.startup;

import com.yue.jobcomparer.entity.Analysis;
import com.yue.jobcomparer.entity.AnalysisFailureReason;
import com.yue.jobcomparer.entity.AnalysisStatus;
import com.yue.jobcomparer.repository.AnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Fails analyses left in a non-terminal state by a previous process.
 *
 * <p>Assumes a single running instance: at startup no task of this process can
 * be in flight, so any non-terminal row is an orphan. This precondition breaks
 * under rolling deployment or horizontal scaling, where a starting instance
 * would fail rows another instance is still working on. Moving to either would
 * require a time threshold or an instance-owned lease on each row.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StaleAnalysisCleaner implements ApplicationRunner {

    private final AnalysisRepository analysisRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<AnalysisStatus> nonTerminal = Arrays.stream(AnalysisStatus.values())
                .filter(status -> !status.isTerminal())
                .toList();

        List<Analysis> orphans = analysisRepository.findByStatusIn(nonTerminal);
        if (orphans.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        orphans.forEach(analysis -> {
            analysis.setStatus(AnalysisStatus.FAILED);
            analysis.setFailureReason(AnalysisFailureReason.INTERRUPTED);
            analysis.setFinishedAt(now);
        });

        log.warn("Failed {} analyses left in a non-terminal state by a previous process", orphans.size());
    }
}
