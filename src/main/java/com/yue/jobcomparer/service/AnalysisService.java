package com.yue.jobcomparer.service;

import com.yue.jobcomparer.ai.AiProvider;
import com.yue.jobcomparer.config.QuotaProperties;
import com.yue.jobcomparer.dto.AnalysisCreateRequest;
import com.yue.jobcomparer.dto.AnalysisResponse;
import com.yue.jobcomparer.dto.AnalysisSummaryResponse;
import com.yue.jobcomparer.entity.Analysis;
import com.yue.jobcomparer.entity.AnalysisStatus;
import com.yue.jobcomparer.entity.AuditAction;
import com.yue.jobcomparer.entity.Cv;
import com.yue.jobcomparer.entity.Job;
import com.yue.jobcomparer.entity.User;
import com.yue.jobcomparer.event.AnalysisSubmittedEvent;
import com.yue.jobcomparer.exception.AnalysisNotFoundException;
import com.yue.jobcomparer.exception.CvNotFoundException;
import com.yue.jobcomparer.exception.JobNotFoundException;
import com.yue.jobcomparer.exception.RateLimitExceededException;
import com.yue.jobcomparer.repository.AnalysisRepository;
import com.yue.jobcomparer.repository.CvRepository;
import com.yue.jobcomparer.repository.JobRepository;
import com.yue.jobcomparer.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class AnalysisService {

    private final CvRepository cvRepository;
    private final JobRepository jobRepository;
    private final AnalysisRepository analysisRepository;
    private final SecurityUtils securityUtils;
    private final AnalysisPersistenceService analysisPersistenceService;
    private final AuditLogService auditLogService;
    private final QuotaProperties quotaProperties;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.analysis.daily-limit-global}")
    private int dailyLimitGlobal;

    @Value("${app.ai.default-provider}")
    private AiProvider defaultProvider;

    public AnalysisService(
            CvRepository cvRepository,
            JobRepository jobRepository,
            AnalysisRepository analysisRepository,
            SecurityUtils securityUtils,
            AnalysisPersistenceService analysisPersistenceService,
            AuditLogService auditLogService,
            QuotaProperties quotaProperties,
            ApplicationEventPublisher eventPublisher) {
        this.cvRepository = cvRepository;
        this.jobRepository = jobRepository;
        this.analysisRepository = analysisRepository;
        this.securityUtils = securityUtils;
        this.analysisPersistenceService = analysisPersistenceService;
        this.auditLogService = auditLogService;
        this.quotaProperties = quotaProperties;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public AnalysisResponse submit(AnalysisCreateRequest request) {
        User user = securityUtils.getCurrentUser();
        Long userId = user.getId();

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();

        // Global cap: a cost circuit breaker, deliberately not exempted by any plan.
        long globalCount = analysisRepository.countByCreatedAtAfterAndStatusNot(startOfToday, AnalysisStatus.FAILED);

        if (globalCount >= dailyLimitGlobal) {
            throw new RateLimitExceededException(
                    "The service has reached its daily capacity. Please try again tomorrow.");
        }

        // Per-user cap: a fairness rule, scoped to the user's plan
        int dailyLimit = quotaProperties.limitsFor(user.getPlan()).getDailyAnalyses();
        long userCount = analysisRepository.countByUserIdAndCreatedAtAfterAndStatusNot(
                userId, startOfToday, AnalysisStatus.FAILED);

        if (userCount >= dailyLimit) {
            throw new RateLimitExceededException(
                    "You have reached your daily limit of "
                            + dailyLimit + " analyses. Please try again tomorrow.");
        }

        Cv cv = cvRepository.findByIdAndUserIdAndDeletedAtIsNull(request.getCvId(), userId)
                .orElseThrow(() -> new CvNotFoundException("Cv not found: " + request.getCvId()));

        Job job = jobRepository.findByIdAndUserIdAndDeletedAtIsNull(request.getJobId(), userId)
                .orElseThrow(() -> new JobNotFoundException("Job not found: " + request.getJobId()));

        AiProvider aiProvider = request.getAiProvider() != null ? request.getAiProvider() : defaultProvider;

        Analysis analysis = Analysis.builder()
                .userId(userId)
                .aiProvider(aiProvider)
                .cvId(cv.getId())
                .jobId(job.getId())
                .status(AnalysisStatus.PENDING)
                .cvName(cv.getCvName())
                .jobTitle(job.getJobTitle())
                .company(job.getCompany())
                .build();

        Analysis saved = analysisPersistenceService.saveWithAudit(analysis);

        eventPublisher.publishEvent(new AnalysisSubmittedEvent(saved.getId(), userId));

        return toResponse(saved);
    }

    public List<AnalysisResponse> getHistory() {
        Long userId = securityUtils.getCurrentUserId();
        return analysisRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void markViewed(Long id) {
        Long userId = securityUtils.getCurrentUserId();
        Analysis analysis = analysisRepository.findByIdAndUserIdAndDeletedAtIsNull(id, userId)
                .orElseThrow(() -> new AnalysisNotFoundException("Analysis not found: " + id));

        if (analysis.getViewedAt() == null) {
            analysis.setViewedAt(LocalDateTime.now());
        }
    }

    @Transactional
    public void deleteAnalysis(Long id) {
        Long userId = securityUtils.getCurrentUserId();
        Analysis analysis = analysisRepository.findByIdAndUserIdAndDeletedAtIsNull(id, userId)
                .orElseThrow(() -> new AnalysisNotFoundException("Analysis not found: " + id));
        analysis.setDeletedAt(LocalDateTime.now());
        analysisRepository.save(analysis);

        auditLogService.recordResourceEvent(AuditAction.ANALYSIS_DELETE, id);
    }

    @Transactional(readOnly = true)
    public AnalysisSummaryResponse getSummary() {
        Long userId = securityUtils.getCurrentUserId();

        List<AnalysisStatus> terminal = Arrays.stream(AnalysisStatus.values())
                .filter(AnalysisStatus::isTerminal)
                .toList();
        List<AnalysisStatus> nonTerminal = Arrays.stream(AnalysisStatus.values())
                .filter(status -> !status.isTerminal())
                .toList();

        return AnalysisSummaryResponse.builder()
                .unread(analysisRepository
                        .countByUserIdAndDeletedAtIsNullAndViewedAtIsNullAndStatusIn(userId, terminal))
                .active(analysisRepository
                        .countByUserIdAndDeletedAtIsNullAndStatusIn(userId, nonTerminal))
                .build();
    }

    private AnalysisResponse toResponse(Analysis analysis) {
        return AnalysisResponse.builder()
                .id(analysis.getId())
                .aiProvider(analysis.getAiProvider())
                .cvId(analysis.getCvId())
                .jobId(analysis.getJobId())
                .matchScore(analysis.getMatchScore())
                .matchedSkills(analysis.getMatchedSkills())
                .missingSkills(analysis.getMissingSkills())
                .actionableFeedback(analysis.getActionableFeedback())
                .cvName(analysis.getCvName())
                .jobTitle(analysis.getJobTitle())
                .company(analysis.getCompany())
                .status(analysis.getStatus())
                .failureReason(analysis.getFailureReason())
                .createdAt(analysis.getCreatedAt())
                .viewedAt(analysis.getViewedAt())
                .build();
    }
}
