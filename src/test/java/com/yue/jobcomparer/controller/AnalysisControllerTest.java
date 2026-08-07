package com.yue.jobcomparer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yue.jobcomparer.AbstractIntegrationTest;
import com.yue.jobcomparer.ai.AiClient;
import com.yue.jobcomparer.ai.AiClientResolver;
import com.yue.jobcomparer.ai.AiProvider;
import com.yue.jobcomparer.dto.AnalysisCreateRequest;
import com.yue.jobcomparer.entity.Cv;
import com.yue.jobcomparer.entity.Job;
import com.yue.jobcomparer.entity.JobStatus;
import com.yue.jobcomparer.entity.User;
import com.yue.jobcomparer.repository.AnalysisRepository;
import com.yue.jobcomparer.repository.CvRepository;
import com.yue.jobcomparer.repository.JobRepository;
import com.yue.jobcomparer.repository.UserRepository;
import com.yue.jobcomparer.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
public class AnalysisControllerTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private CvRepository cvRepository;
    @Autowired private JobRepository jobRepository;
    @Autowired private AnalysisRepository analysisRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    @MockitoBean private AiClientResolver aiClientResolver;

    private final AiClient mockAiClient = Mockito.mock(AiClient.class);

    private String token;
    private Long userId;
    private Long cvId;
    private Long jobId;

    private static final String FAKE_AI_JSON = """
        {
          "matchScore": 75,
          "matchedSkills": "Java, Spring Boot, PostgreSQL",
          "missingSkills": "Kafka, Docker",
          "actionableFeedback": "Add Kafka and Docker to your projects."
        }
        """;

    @BeforeEach
    void setUp() {
        // create test user and generate token
        User user = User.builder()
                .email("test@example.com")
                .username("testuser")
                .password(passwordEncoder.encode("password123"))
                .build();
        userRepository.save(user);
        userId = user.getId();
        token = "Bearer " + jwtUtil.generateToken(user.getEmail());

        // create a CV
        Cv cv = Cv.builder()
                .userId(userId)
                .cvName("CV A")
                .content("CV content")
                .build();
        cvRepository.save(cv);
        cvId = cv.getId();

        // create a Job
        Job job = Job.builder()
                .userId(userId)
                .jobTitle("Job A")
                .company("Company A")
                .jobDescription("Job description")
                .jobUrl("https://example.com")
                .status(JobStatus.SAVED)
                .notes("Test")
                .build();
        jobRepository.save(job);
        jobId = job.getId();

        // Stub the resolver to always return a mocked AiClient, and stub that client
        // to return a fake JSON response by default
        when(aiClientResolver.resolve(any())).thenReturn(mockAiClient);
        when(mockAiClient.chat(anyString())).thenReturn(FAKE_AI_JSON);
    }

    // === Happy path ===

    @Test
    void create_shouldReturn201_andPersistAnalysis() throws Exception {
        AnalysisCreateRequest request = new AnalysisCreateRequest();
        request.setCvId(cvId);
        request.setJobId(jobId);

        mockMvc.perform(post("/api/analyses")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.aiProvider").value("ANTHROPIC"));

        // Verify database state
        assertThat(analysisRepository.findAll()).hasSize(1);
    }

    @Test
    void create_withExplicitProvider_shouldUseAndPersistIt() throws Exception {
        AnalysisCreateRequest request = new AnalysisCreateRequest();
        request.setAiProvider(AiProvider.DEEPSEEK);
        request.setCvId(cvId);
        request.setJobId(jobId);

        mockMvc.perform(post("/api/analyses")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.aiProvider").value("DEEPSEEK"))
                .andExpect(jsonPath("$.cvId").value(cvId))
                .andExpect(jsonPath("$.jobId").value(jobId))
                .andExpect(jsonPath("$.matchScore").value(75))
                .andExpect(jsonPath("$.matchedSkills").value("Java, Spring Boot, PostgreSQL"))
                .andExpect(jsonPath("$.missingSkills").value("Kafka, Docker"))
                .andExpect(jsonPath("$.actionableFeedback").value("Add Kafka and Docker to your projects."))
                .andExpect(jsonPath("$.createdAt").exists());

        verify(aiClientResolver).resolve(AiProvider.DEEPSEEK);

        // Verify database state
        assertThat(analysisRepository.findAll()).hasSize(1);
    }

    // === Validation path ===

    @Test
    void create_withoutCvId_shouldReturn400_withFieldError() throws Exception {
        String invalidBody = """
                {
                  "jobId": %d
                }
                """.formatted(jobId);

        mockMvc.perform(post("/api/analyses")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.cvId").exists());
    }

    @Test
    void create_withoutJobId_shouldReturn400_withFieldError() throws Exception {
        String invalidBody = """
                {
                  "cvId": %d
                }
                """.formatted(cvId);

        mockMvc.perform(post("/api/analyses")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.jobId").exists());
    }

    @Test
    void create_withNegativeCvId_shouldReturn400_withFieldError() throws Exception {
        String invalidBody = """
                {
                  "cvId": -1,
                  "jobId": %d
                }
                """.formatted(jobId);

        mockMvc.perform(post("/api/analyses")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.cvId").exists());
    }

    @Test
    void create_withNegativeJobId_shouldReturn400_withFieldError() throws Exception {
        String invalidBody = """
                {
                  "cvId": %d,
                  "jobId": -1
                }
                """.formatted(cvId);

        mockMvc.perform(post("/api/analyses")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.jobId").exists());
    }

    // Resource not found path

    @Test
    void create_withNonExistentCvId_shouldReturn404() throws Exception {
        AnalysisCreateRequest request = new AnalysisCreateRequest();
        request.setCvId(99999L);
        request.setJobId(jobId);

        mockMvc.perform(post("/api/analyses")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString("Cv not found")));
    }

    @Test
    void create_withNonExistentJobId_shouldReturn404() throws Exception {
        AnalysisCreateRequest request = new AnalysisCreateRequest();
        request.setCvId(cvId);
        request.setJobId(99999L);

        mockMvc.perform(post("/api/analyses")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString("Job not found")));
    }

    // Authorization path

    @Test
    void create_withAnotherUsersCv_shouldReturn404() throws Exception {
        User userB = User.builder()
                .email("userB@example.com")
                .username("testuserb")
                .password(passwordEncoder.encode("password123"))
                .build();
        userRepository.save(userB);
        String tokenB = "Bearer " + jwtUtil.generateToken(userB.getEmail());

        Job jobB = Job.builder()
                .userId(userB.getId())
                .jobTitle("Job B")
                .company("Company B")
                .jobDescription("Job B description")
                .status(JobStatus.SAVED)
                .build();
        jobRepository.save(jobB);

        AnalysisCreateRequest request = new AnalysisCreateRequest();
        request.setCvId(cvId);
        request.setJobId(jobB.getId());

        mockMvc.perform(post("/api/analyses")
                        .header("Authorization", tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_withAnotherUsersJob_shouldReturn404() throws Exception {
        User userB = User.builder()
                .email("userB@example.com")
                .username("testuserb")
                .password(passwordEncoder.encode("password123"))
                .build();
        userRepository.save(userB);
        String tokenB = "Bearer " + jwtUtil.generateToken(userB.getEmail());

        Cv cvB = Cv.builder()
                .userId(userB.getId())
                .cvName("CV B")
                .content("CV B content")
                .build();
        cvRepository.save(cvB);

        AnalysisCreateRequest request = new AnalysisCreateRequest();
        request.setCvId(cvB.getId());
        request.setJobId(jobId);

        mockMvc.perform(post("/api/analyses")
                        .header("Authorization", tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/analyses"))
                .andExpect(status().isUnauthorized());
    }

    // AI failure path

    @Test
    void create_whenAiReturnsMalformedJson_shouldReturn502() throws Exception {
        // Override default stub to simulate AI returning malformed output
        when(mockAiClient.chat(anyString())).thenReturn("not a valid json at all");

        AnalysisCreateRequest request = new AnalysisCreateRequest();
        request.setCvId(cvId);
        request.setJobId(jobId);

        mockMvc.perform(post("/api/analyses")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadGateway());
    }
}
