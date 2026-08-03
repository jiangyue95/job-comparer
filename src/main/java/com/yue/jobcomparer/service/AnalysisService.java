package com.yue.jobcomparer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yue.jobcomparer.ai.AiClient;
import com.yue.jobcomparer.ai.AiClientResolver;
import com.yue.jobcomparer.ai.AiProvider;
import com.yue.jobcomparer.dto.AiAnalysisResult;
import com.yue.jobcomparer.dto.AnalysisCreateRequest;
import com.yue.jobcomparer.dto.AnalysisResponse;
import com.yue.jobcomparer.entity.*;
import com.yue.jobcomparer.exception.*;
import com.yue.jobcomparer.repository.AnalysisRepository;
import com.yue.jobcomparer.repository.CvRepository;
import com.yue.jobcomparer.repository.JobRepository;
import com.yue.jobcomparer.util.AiResponseUtils;
import com.yue.jobcomparer.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class AnalysisService {

    private final CvRepository cvRepository;
    private final JobRepository jobRepository;
    private final AnalysisRepository analysisRepository;
    private final AiClientResolver aiClientResolver;
    private final ObjectMapper objectMapper;
    private final SecurityUtils securityUtils;
    private final AnalysisPersistenceService analysisPersistenceService;
    private final AuditLogService auditLogService;

    @Value("${app.analysis.daily-limit-per-user}")
    private int dailyLimitPerUser;

    @Value("${app.analysis.daily-limit-global}")
    private int dailyLimitGlobal;

    @Value("${app.ai.default-provider}")
    private AiProvider defaultProvider;

    public AnalysisService(
            AiClientResolver aiClientResolver,
            CvRepository cvRepository,
            JobRepository jobRepository,
            AnalysisRepository analysisRepository,
            ObjectMapper objectMapper,
            SecurityUtils securityUtils,
            AnalysisPersistenceService analysisPersistenceService,
            AuditLogService auditLogService) {
        this.aiClientResolver = aiClientResolver;
        this.cvRepository = cvRepository;
        this.jobRepository = jobRepository;
        this.analysisRepository = analysisRepository;
        this.objectMapper = objectMapper;
        this.securityUtils = securityUtils;
        this.analysisPersistenceService = analysisPersistenceService;
        this.auditLogService = auditLogService;
    }

    private static final String PROMPT_TEMPLATE = """
                You are an experienced technical recruiter analyzing the fit between a candidate's CV and a job description for software engineering roles.
                
                Focus ONLY on technical skills, technologies, frameworks, languages, and concrete experience. Do NOT evaluate soft skills, communication, or cultural fit.
                
                Be direct and specific in your feedback. Avoid hedging language like "you might consider" or "perhaps". State clearly what the candidate is missing and what they should do.
                
                <cv>
                {cv_content}
                </cv>
                
                <job_title>
                {job_title}
                </job_title>
                
                <job_description>
                {job_description}
                </job_description>
                
                Return ONLY a JSON object with this exact structure:
                {
                  "matchScore": <integer between 0 and 100, where 100 means the candidate fully meets the technical requirements>,
                  "matchedSkills": "<comma-separated list of technical skills present in both CV and JD>",
                  "missingSkills": "<comma-separated list of technical skills required by the JD but absent from the CV>",
                  "actionableFeedback": "<2-3 paragraphs of specific, direct advice for the candidate to close the gap>"
                }
                
                Do not include any text, markdown formatting, or explanation outside the JSON object.
                """;

    public AnalysisResponse analyze(AnalysisCreateRequest request) {

        Long userId = securityUtils.getCurrentUserId();

        // Limit checking
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();

        long globalCount = analysisRepository.countByCreatedAtAfter(startOfToday);
        if (globalCount >= dailyLimitGlobal) {
            throw new RateLimitExceededException(
                    "The service has reached its daily capacity. Please try again tomorrow.");
        }

        long userCount = analysisRepository.countByUserIdAndCreatedAtAfter(userId, startOfToday);
        if (userCount >= dailyLimitPerUser) {
            throw new RateLimitExceededException(
                    "You have reached your daily limit of "
                            + dailyLimitPerUser + " analyses. Please try again tomorrow.");
        }

        Cv cv = cvRepository.findByIdAndUserIdAndDeletedAtIsNull(request.getCvId(), userId)
                .orElseThrow(() -> new CvNotFoundException("Cv not found: " + request.getCvId()));

        Job job = jobRepository.findByIdAndUserIdAndDeletedAtIsNull(request.getJobId(), userId)
                .orElseThrow(() -> new JobNotFoundException("Job not found: " + request.getJobId()));

        String prompt = PROMPT_TEMPLATE
                .replace("{cv_content}", cv.getContent())
                .replace("{job_title}", job.getJobTitle())
                .replace("{job_description}", job.getJobDescription());

        AiClient aiClient = aiClientResolver.resolve(defaultProvider);
        String aiResponse = aiClient.chat(prompt);

        log.debug("AI raw response: {}", aiResponse);

        String cleanedResponse = AiResponseUtils.stripMarkdownFence(aiResponse);

        AiAnalysisResult result;
        try {
            result = objectMapper.readValue(cleanedResponse, AiAnalysisResult.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse AI response as JSON: {}", aiResponse, e);
            throw new AiResponseParseException("AI response could not be parsed as JSON");
        }

        Analysis analysis = Analysis.builder()
                .userId(userId)
                .cvId(cv.getId())
                .jobId(job.getId())
                .matchScore(result.getMatchScore())
                .matchedSkills(result.getMatchedSkills())
                .missingSkills(result.getMissingSkills())
                .actionableFeedback(result.getActionableFeedback())
                .cvName(cv.getCvName())
                .jobTitle(job.getJobTitle())
                .company(job.getCompany())
                .build();

        Analysis saved = analysisPersistenceService.saveWithAudit(analysis);

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
    public void deleteAnalysis(Long id) {
        Long userId = securityUtils.getCurrentUserId();
        Analysis analysis = analysisRepository.findByIdAndUserIdAndDeletedAtIsNull(id, userId)
                .orElseThrow(() -> new AnalysisNotFoundException("Analysis not found: " + id));
        analysis.setDeletedAt(LocalDateTime.now());
        analysisRepository.save(analysis);

        auditLogService.recordResourceEvent(AuditAction.ANALYSIS_DELETE, id);
    }

    private AnalysisResponse toResponse(Analysis analysis) {
        return AnalysisResponse.builder()
                .id(analysis.getId())
                .cvId(analysis.getCvId())
                .jobId(analysis.getJobId())
                .matchScore(analysis.getMatchScore())
                .matchedSkills(analysis.getMatchedSkills())
                .missingSkills(analysis.getMissingSkills())
                .actionableFeedback(analysis.getActionableFeedback())
                .cvName(analysis.getCvName())
                .jobTitle(analysis.getJobTitle())
                .company(analysis.getCompany())
                .createdAt(analysis.getCreatedAt())
                .build();
    }
}
