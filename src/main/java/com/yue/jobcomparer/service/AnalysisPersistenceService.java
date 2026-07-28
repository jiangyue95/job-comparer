package com.yue.jobcomparer.service;

import com.yue.jobcomparer.entity.Analysis;
import com.yue.jobcomparer.entity.AuditAction;
import com.yue.jobcomparer.repository.AnalysisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnalysisPersistenceService {

    private final AnalysisRepository analysisRepository;
    private final AuditLogService auditLogService;

    /**
     * Saves an analysis together with its audit entry in a single short
     * transaction. Kept in a separate bean deliberately: the caller performs a
     * multi-second LLM request, and a transaction spanning that call would hold
     * a pooled connection for its entire duration. A private method in the
     * caller would not work either - self-invocation bypass the transaction
     * proxy, so the annotation would be silently ignored.
     */
    @Transactional
    public Analysis saveWithAudit(Analysis analysis){
        Analysis saved = analysisRepository.save(analysis);
        auditLogService.recordResourceEvent(AuditAction.ANALYSIS_CREATE, saved.getId());
        return saved;
    }
}
