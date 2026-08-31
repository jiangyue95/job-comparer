package com.yue.jobcomparer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yue.jobcomparer.ai.AiClient;
import com.yue.jobcomparer.ai.AiClientResolver;
import com.yue.jobcomparer.ai.AiProvider;
import com.yue.jobcomparer.dto.AiAnalysisResult;
import com.yue.jobcomparer.entity.Analysis;
import com.yue.jobcomparer.entity.AnalysisFailureReason;
import com.yue.jobcomparer.entity.AnalysisStatus;
import com.yue.jobcomparer.entity.Cv;
import com.yue.jobcomparer.entity.Job;
import com.yue.jobcomparer.repository.AnalysisRepository;
import com.yue.jobcomparer.repository.CvRepository;
import com.yue.jobcomparer.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalysisRunnerTest {

    @Mock
    private AnalysisRepository analysisRepository;
    @Mock
    private CvRepository cvRepository;
    @Mock
    private JobRepository jobRepository;
    @Mock
    private AnalysisPersistenceService analysisPersistenceService;
    @Mock
    private AiClientResolver aiClientResolver;
    @Mock
    private AiClient aiClient;

    // Not a mock: the real parser is what decides PARSE_ERROR, so stubbing it
    // would remove the behaviour under test.
    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    private AnalysisRunner runner;

    private static final Long ANALYSIS_ID = 1L;
    private static final Long USER_ID = 10L;
    private static final Long CV_ID = 100L;
    private static final Long JOB_ID = 200L;

    private static final String VALID_AI_JSON = """
            {
              "matchScore": 75,
              "matchedSkills": "Java, Spring Boot",
              "missingSkills": "Kubernetes",
              "actionableFeedback": "Learn Kubernetes."
            }
            """;

    @BeforeEach
    void setUp() {
        runner = new AnalysisRunner(analysisRepository, cvRepository, jobRepository,
                analysisPersistenceService, aiClientResolver, objectMapper);
    }

    private Analysis anAnalysis() {
        return Analysis.builder()
                .id(ANALYSIS_ID)
                .userId(USER_ID)
                .cvId(CV_ID)
                .jobId(JOB_ID)
                .aiProvider(AiProvider.ANTHROPIC)
                .status(AnalysisStatus.PENDING)
                .build();
    }

    private Cv aCv() {
        return Cv.builder()
                .id(CV_ID)
                .userId(USER_ID)
                .cvName("CV A")
                .content("Java developer with Spring experience")
                .build();
    }

    private Job aJob() {
        return Job.builder()
                .id(JOB_ID)
                .userId(USER_ID)
                .jobTitle("Backend Engineer")
                .company("ACME")
                .jobDescription("Java, Spring Boot, Kubernetes")
                .build();
    }

    // Happy path
    @Test
    void run_shouldSaveResult_whenAiReturnsValidJson() {
        Analysis analysis = anAnalysis();
        when(analysisRepository.findByIdAndUserId(ANALYSIS_ID, USER_ID))
                .thenReturn(Optional.of(analysis));
        when(cvRepository.findByIdAndUserIdAndDeletedAtIsNull(CV_ID, USER_ID))
                .thenReturn(Optional.of(aCv()));
        when(jobRepository.findByIdAndUserIdAndDeletedAtIsNull(JOB_ID, USER_ID))
                .thenReturn(Optional.of(aJob()));
        when(aiClientResolver.resolve(AiProvider.ANTHROPIC)).thenReturn(aiClient);
        when(aiClient.chat(anyString())).thenReturn(VALID_AI_JSON);

        runner.run(ANALYSIS_ID, USER_ID);

        verify(analysisPersistenceService).markProcessing(ANALYSIS_ID);
        verify(analysisPersistenceService).saveResult(eq(ANALYSIS_ID), any(AiAnalysisResult.class));
        verify(analysisPersistenceService, never()).markFailed(anyLong(), any());
    }

    // analysis doesn't exist
    @Test
    void run_shouldExitQuietly_whenAnalysisNoLongerExists() {
        when(analysisRepository.findByIdAndUserId(ANALYSIS_ID, USER_ID))
                .thenReturn(Optional.empty());

        runner.run(ANALYSIS_ID, USER_ID);

        verifyNoInteractions(analysisPersistenceService);
        verifyNoInteractions(aiClientResolver);
    }

    // CV is deleted
    @Test
    void run_shouldFailWithInputUnavailable_whenCvWasDeleted() {
        when(analysisRepository.findByIdAndUserId(ANALYSIS_ID, USER_ID))
                .thenReturn(Optional.of(anAnalysis()));
        when(cvRepository.findByIdAndUserIdAndDeletedAtIsNull(CV_ID, USER_ID))
                .thenReturn(Optional.empty());
        when(jobRepository.findByIdAndUserIdAndDeletedAtIsNull(JOB_ID, USER_ID))
                .thenReturn(Optional.of(aJob()));

        runner.run(ANALYSIS_ID, USER_ID);

        verify(analysisPersistenceService).markProcessing(ANALYSIS_ID);
        verify(analysisPersistenceService).markFailed(ANALYSIS_ID, AnalysisFailureReason.INPUT_UNAVAILABLE);
        verifyNoInteractions(aiClientResolver);
    }

    // parse failed
    @Test
    void run_shouldFailWithParseError_whenProviderThrows() {
        when(analysisRepository.findByIdAndUserId(ANALYSIS_ID, USER_ID))
                .thenReturn(Optional.of(anAnalysis()));
        when(cvRepository.findByIdAndUserIdAndDeletedAtIsNull(CV_ID, USER_ID))
                .thenReturn(Optional.of(aCv()));
        when(jobRepository.findByIdAndUserIdAndDeletedAtIsNull(JOB_ID, USER_ID))
                .thenReturn(Optional.of(aJob()));
        when(aiClientResolver.resolve(AiProvider.ANTHROPIC)).thenReturn(aiClient);
        when(aiClient.chat(anyString())).thenReturn("This is not JSON at all.");

        runner.run(ANALYSIS_ID, USER_ID);

        verify(analysisPersistenceService).markFailed(ANALYSIS_ID, AnalysisFailureReason.PARSE_ERROR);
        verify(analysisPersistenceService, never()).saveResult(anyLong(), any());
    }

    // provider throws exception
    @Test
    void run_shouldFailWithInternalError_whenProviderThrows() {
        when(analysisRepository.findByIdAndUserId(ANALYSIS_ID, USER_ID))
                .thenReturn(Optional.of(anAnalysis()));
        when(cvRepository.findByIdAndUserIdAndDeletedAtIsNull(CV_ID, USER_ID))
                .thenReturn(Optional.of(aCv()));
        when(jobRepository.findByIdAndUserIdAndDeletedAtIsNull(JOB_ID, USER_ID))
                .thenReturn(Optional.of(aJob()));
        when(aiClientResolver.resolve(AiProvider.ANTHROPIC)).thenReturn(aiClient);
        when(aiClient.chat(anyString())).thenThrow(new RuntimeException("provider exploded"));

        runner.run(ANALYSIS_ID, USER_ID);

        verify(analysisPersistenceService).markFailed(ANALYSIS_ID, AnalysisFailureReason.INTERNAL_ERROR);
    }
}
