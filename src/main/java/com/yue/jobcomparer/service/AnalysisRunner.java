package com.yue.jobcomparer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yue.jobcomparer.ai.AiClient;
import com.yue.jobcomparer.ai.AiClientResolver;
import com.yue.jobcomparer.dto.AiAnalysisResult;
import com.yue.jobcomparer.entity.Analysis;
import com.yue.jobcomparer.entity.AnalysisFailureReason;
import com.yue.jobcomparer.entity.Cv;
import com.yue.jobcomparer.entity.Job;
import com.yue.jobcomparer.repository.AnalysisRepository;
import com.yue.jobcomparer.repository.CvRepository;
import com.yue.jobcomparer.repository.JobRepository;
import com.yue.jobcomparer.util.AiResponseUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Executes a submitted analysis on a background thread.
 *
 * <p>Deliberately not annotated with {@code @Transactional}: this method spans a
 * multi-second LLM call, and a transaction covering it would hold a pooled
 * database connection for the whole duration. All persistence happens through
 * {@link AnalysisPersistenceService}, where each state transition is its own
 * short transaction.
 *
 * <p>Runs without a SecurityContext, so it must not call anything that reads
 * {@code SecurityContextHolder}. The user id arrives explicitly from the event.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisRunner {

    private final AnalysisRepository analysisRepository;
    private final CvRepository cvRepository;
    private final JobRepository jobRepository;
    private final AnalysisPersistenceService analysisPersistenceService;
    private final AiClientResolver aiClientResolver;
    private final ObjectMapper objectMapper;

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


    public void run(Long analysisId, Long userId) {
        try {
            // Not scoped by deletedAt: if the user deleted the analysis while it was
            // queued, there is nothing left to report a failure on. Exit quietly.
            Optional<Analysis> maybeAnalysis = analysisRepository.findByIdAndUserId(analysisId, userId);
            if (maybeAnalysis.isEmpty()) {
                log.warn("Analysis {} no longer exists, skipping execution", analysisId);
                return;
            }
            Analysis analysis = maybeAnalysis.get();

            analysisPersistenceService.markProcessing(analysisId);

            // Scoped by deletedAt, unlike the lookup above: a deleted CV or job means
            // the inputs are gone, and paying for an LLM call on them makes no sense.
            Optional<Cv> maybeCv = cvRepository.findByIdAndUserIdAndDeletedAtIsNull(analysis.getCvId(), userId);
            Optional<Job> maybeJob = jobRepository.findByIdAndUserIdAndDeletedAtIsNull(analysis.getJobId(), userId);
            if (maybeCv.isEmpty() || maybeJob.isEmpty()) {
                log.warn("Analysis {} has unavailable inputs (cvId={}, jobId={})",
                        analysisId, analysis.getCvId(), analysis.getJobId());
                analysisPersistenceService.markFailed(analysisId, AnalysisFailureReason.INPUT_UNAVAILABLE);
                return;
            }
            Cv cv = maybeCv.get();
            Job job = maybeJob.get();

            String prompt = PROMPT_TEMPLATE
                    .replace("{cv_content}", cv.getContent())
                    .replace("{job_title}", job.getJobTitle())
                    .replace("{job_description}", job.getJobDescription());

            AiClient aiClient = aiClientResolver.resolve(analysis.getAiProvider());
            String aiResponse = aiClient.chat(prompt);

            log.debug("AI raw response for analysis {}: {}", analysisId, aiResponse);

            AiAnalysisResult result;
            try {
                result = objectMapper.readValue(
                        AiResponseUtils.stripMarkdownFence(aiResponse), AiAnalysisResult.class);
            } catch (JsonProcessingException e) {
                log.error("Analysis {} could not parse AI response: {}", analysisId, aiResponse, e);
                analysisPersistenceService.markFailed(analysisId, AnalysisFailureReason.PARSE_ERROR);
                return;
            }

            analysisPersistenceService.saveResult(analysisId, result);
            log.info("Analysis {} completed", analysisId);

        } catch (Exception e) {
            // Last line of defence. An exception escaping this method would be swallowed
            // by the thread pool, leaving the row stuck in PROCESSING with no trace.
            log.error("Analysis {} failed unexpectedly", analysisId, e);
            analysisPersistenceService.markFailed(analysisId, AnalysisFailureReason.INTERNAL_ERROR);
        }
    }
}
