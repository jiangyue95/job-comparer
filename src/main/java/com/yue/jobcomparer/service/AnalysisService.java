package com.yue.jobcomparer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yue.jobcomparer.ai.AiClient;
import com.yue.jobcomparer.dto.AiAnalysisResult;
import com.yue.jobcomparer.dto.AnalysisCreateRequest;
import com.yue.jobcomparer.dto.AnalysisResponse;
import com.yue.jobcomparer.entity.Analysis;
import com.yue.jobcomparer.entity.Cv;
import com.yue.jobcomparer.entity.Job;
import com.yue.jobcomparer.entity.User;
import com.yue.jobcomparer.exception.AiResponseParseException;
import com.yue.jobcomparer.exception.CvNotFoundException;
import com.yue.jobcomparer.exception.JobNotFoundException;
import com.yue.jobcomparer.exception.RateLimitExceededException;
import com.yue.jobcomparer.repository.AnalysisRepository;
import com.yue.jobcomparer.repository.CvRepository;
import com.yue.jobcomparer.repository.JobRepository;
import com.yue.jobcomparer.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
public class AnalysisService {

    private final CvRepository cvRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final AnalysisRepository analysisRepository;
    private final AiClient aiClient;
    private final ObjectMapper objectMapper;

    @Value("${app.analysis.daily-limit-per-user}")
    private int dailyLimitPerUser;

    @Value("${app.analysis.daily-limit-global}")
    private int dailyLimitGlobal;

    public AnalysisService(
            AiClient aiClient,
            CvRepository cvRepository,
            JobRepository jobRepository,
            UserRepository userRepository,
            AnalysisRepository analysisRepository,
            ObjectMapper objectMapper) {
        this.aiClient = aiClient;
        this.cvRepository = cvRepository;
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
        this.analysisRepository = analysisRepository;
        this.objectMapper = objectMapper;
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

        Long userId = getCurrentUserId();

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

        String aiResponse = aiClient.chat(prompt);

        log.debug("AI raw response: {}", aiResponse);

        String cleanedResponse = stripMarkdownFence(aiResponse);

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
                .build();

        Analysis saved = analysisRepository.save(analysis);

        return toResponse(saved);
    }

    private Long getCurrentUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in DB: " + email));
        return user.getId();
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
                .createdAt(analysis.getCreatedAt())
                .build();
    }

    private String stripMarkdownFence(String response) {
        if (response == null) {
            return null;
        }

        String trimmed = response.trim();
        if (trimmed.startsWith("```")) {
            // remove opening fence(e.g., "```json" or "```")
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline != -1) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            // remove closing fence
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        }
        return trimmed.trim();
    }
}
