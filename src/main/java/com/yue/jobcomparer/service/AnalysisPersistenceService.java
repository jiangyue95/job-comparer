package com.yue.jobcomparer.service;

import com.yue.jobcomparer.dto.AiAnalysisResult;
import com.yue.jobcomparer.entity.Analysis;
import com.yue.jobcomparer.entity.AnalysisFailureReason;
import com.yue.jobcomparer.entity.AnalysisStatus;
import com.yue.jobcomparer.entity.AuditAction;
import com.yue.jobcomparer.exception.AnalysisNotFoundException;
import com.yue.jobcomparer.repository.AnalysisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AnalysisPersistenceService {

    private final AnalysisRepository analysisRepository;
    private final AuditLogService auditLogService;

    /**
     * Persists a submitted analysis together with its audit entry in a single
     * short transaction. Called on the request thread: recordResourceEvent is
     * MANDATORY and reads the SecurityContext, neither of which is available on
     * a pool thread.
     */
    @Transactional
    public Analysis saveWithAudit(Analysis analysis){
        Analysis saved = analysisRepository.save(analysis);
        auditLogService.recordResourceEvent(AuditAction.ANALYSIS_CREATE, saved.getId());
        return saved;
    }

    /**
     * Marks an analysis as started. Runs in its own short transaction so the
     * PROCESSING state is visible to polling clients before the multi-second
     * LLM call begins
     */
    @Transactional
    public void markProcessing(Long analysisId) {
        Analysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new AnalysisNotFoundException("Analysis not found: " + analysisId));
        analysis.setStatus(AnalysisStatus.PROCESSING);
        analysis.setStartedAt(LocalDateTime.now());
    }

    /**
     * Stores the AI output and marks the analysis complete.
     */
    @Transactional
    public void saveResult(Long analysisId, AiAnalysisResult result) {
        Analysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new AnalysisNotFoundException("Analysis not found: " + analysisId));
        analysis.setMatchScore(result.getMatchScore());
        analysis.setMatchedSkills(result.getMatchedSkills());
        analysis.setMissingSkills(result.getMissingSkills());
        analysis.setActionableFeedback(result.getActionableFeedback());
        analysis.setStatus(AnalysisStatus.COMPLETED);
        analysis.setFinishedAt(LocalDateTime.now());
    }

    /**
     * Marks an analysis as failed. Never throws: it is the last line of defence
     * in the background worker, and an exception here would leave the row stuck
     * in PROCESSING forever.
     */
    @Transactional
    public void markFailed(Long analysisId, AnalysisFailureReason reason) {
        analysisRepository.findById(analysisId).ifPresent(analysis -> {
            analysis.setStatus(AnalysisStatus.FAILED);
            analysis.setFailureReason(reason);
            analysis.setFinishedAt(LocalDateTime.now());
        });
    }
}
