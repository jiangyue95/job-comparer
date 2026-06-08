package com.yue.jobcomparer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yue.jobcomparer.AbstractIntegrationTest;
import com.yue.jobcomparer.dto.JobCreateRequest;
import com.yue.jobcomparer.dto.JobUpdateRequest;
import com.yue.jobcomparer.dto.JobUpdateStatusRequest;
import com.yue.jobcomparer.entity.JobStatus;
import com.yue.jobcomparer.entity.User;
import com.yue.jobcomparer.repository.JobRepository;
import com.yue.jobcomparer.repository.UserRepository;
import com.yue.jobcomparer.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
public class JobControllerTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private JobRepository jobRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    private String token;
    private Long userId;

    @BeforeEach
    void setUp() {
        // create test user and generate token
        User user = User.builder()
                .email("test@example.com")
                .password(passwordEncoder.encode("password123"))
                .build();
        userRepository.save(user);
        userId = user.getId();
        token = "Bearer " + jwtUtil.generateToken(user.getEmail());
    }

    // === Happy path ===

    @Test
    void create_shouldReturn201_andPersistJob() throws Exception {
        JobCreateRequest request = new JobCreateRequest();
        request.setJobTitle("Java Developer");
        request.setCompany("TestCo");
        request.setStatus(JobStatus.SAVED);

        mockMvc.perform(post("/api/jobs")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.jobTitle").value("Java Developer"))
                .andExpect(jsonPath("$.company").value("TestCo"))
                .andExpect(jsonPath("$.status").value("SAVED"));
    }

    @Test
    void list_shouldReturnUserJobsOrderedBYCreatedByCreatedAtDesc() throws Exception {
        createJob("Job A", "Company A", JobStatus.SAVED);
        createJob("Job B", "Company B", JobStatus.APPLIED);

        mockMvc.perform(get("/api/jobs").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].jobTitle").value("Job B")) // The latest is first
                .andExpect(jsonPath("$[1].jobTitle").value("Job A"));
    }

    @Test
    void list_withStatusFilter_shouldOnlyReturnMatchingJobs() throws Exception {
        createJob("Job A", "Company A", JobStatus.SAVED);
        createJob("Job B", "Company B", JobStatus.APPLIED);
        createJob("Job C", "Company C", JobStatus.APPLIED);

        mockMvc.perform(get("/api/jobs?status=APPLIED").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getById_shouldReturnFullDetails() throws Exception {
        Long jobId = createJob("Job A", "Company A", JobStatus.SAVED);

        mockMvc.perform(get("/api/jobs/" + jobId).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(jobId))
                .andExpect(jsonPath("$.jobTitle").value("Job A"));
    }

    @Test
    void update_shouldReplaceAllFields() throws Exception {
        Long jobId = createJob("Old Title", "Old Co", JobStatus.SAVED);

        JobUpdateRequest request = new JobUpdateRequest();
        request.setJobTitle("New Title");
        request.setCompany("New Co");
        request.setStatus(JobStatus.APPLIED);
        request.setAppliedAt(LocalDateTime.now());

        mockMvc.perform(put("/api/jobs/" + jobId)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobTitle").value("New Title"))
                .andExpect(jsonPath("$.status").value("APPLIED"));
    }

    @Test
    void update_shouldOnlyChangeStatus() throws Exception {
        Long jobId = createJob("Job A", "Company A", JobStatus.SAVED);

        JobUpdateStatusRequest request = new JobUpdateStatusRequest();
        request.setStatus(JobStatus.INTERVIEWING);

        mockMvc.perform(patch("/api/jobs/" + jobId + "/status")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INTERVIEWING"))
                .andExpect(jsonPath("$.jobTitle").value("Job A"));
    }

    @Test
    void delete_shouldSoftDeleteAndHideFromList() throws Exception {
        Long jobId = createJob("Job A", "Company A", JobStatus.SAVED);

        mockMvc.perform(delete("/api/jobs/" + jobId).header("Authorization", token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/jobs").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

    }

    // === Error Scenarios ===

    @Test
    void create_withoutJobTitle_shouldReturn400_withFieldError() throws Exception {
        String invalidBody = """
                {
                  "company": "TestCo",
                  "status": "SAVED"
                }
                """;

        mockMvc.perform(post("/api/jobs")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.jobTitle").exists());
    }

    @Test
    void create_withBlankJobTitle_shouldReturn400() throws Exception {
        String invalidBody = """
                {
                  "jobTitle": "",
                  "company": "TestCo",
                  "status": "SAVED"
                }
                """;

        mockMvc.perform(post("/api/jobs")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.jobTitle").exists());
    }

    @Test
    void create_withoutStatus_shouldReturn400() throws Exception {
        String invalidBody = """
                {
                  "jobTitle": "Java Dev",
                  "company": "TestCo"
                }
                """;

        mockMvc.perform(post("/api/jobs")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.status").exists());
    }

    @Test
    void create_withInvalidStatus_shouldReturn400() throws Exception {
        String invalidBody = """
                {
                  "jobTitle": "Java Dev",
                  "company": "TestCo",
                  "status": "BANANA"
                }
                """;

        mockMvc.perform(post("/api/jobs")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getById_withNonExistentId_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/jobs/99999").header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString("Job not found")));
    }

    @Test
    void delete_withNonExistentId_shouldReturn404() throws Exception {
        mockMvc.perform(delete("/api/jobs/99999").header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    @Test
    void list_withInvalidStatusEnum_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/jobs?status=INVALID").header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_withBlankJobTitle_shouldReturn400() throws Exception {
        Long jobId = createJob("Job A", "Company A", JobStatus.SAVED);

        String invalidBody = """
            {
              "jobTitle": "",
              "company": "TestCo",
              "status": "SAVED"
            }
            """;
        mockMvc.perform(put("/api/jobs/" + jobId)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.jobTitle").exists());
    }

    @Test
    void update_withNonExistentId_shouldReturn404() throws Exception {
        JobUpdateRequest request = new JobUpdateRequest();
        request.setJobTitle("New Title");
        request.setCompany("New Co");
        request.setStatus(JobStatus.APPLIED);
        request.setAppliedAt(LocalDateTime.now());

        mockMvc.perform(put("/api/jobs/99999")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString("Job not found")));
    }

    // === Security Scenarios ===

    @Test
    void anyEndpoint_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/jobs/"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getById_anotherUsersJob_shouldReturn404_notExposingExistence() throws Exception {
        // User A creates a Job
        Long jobId = createJob("Secret Job", "Secret Co", JobStatus.SAVED);

        // Create User B and generate token.
        User userB = User.builder()
                .email("userB@example.com")
                .password(passwordEncoder.encode("password123"))
                .build();
        userRepository.save(userB);
        String tokenB = "Bearer " + jwtUtil.generateToken(userB.getEmail());

        // User B try to access User A's Job
        mockMvc.perform(get("/api/jobs/" + jobId).header("Authorization", tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_anotherUsersJob_shouldReturn404() throws Exception {
        // User A creates a Job
        Long jobId = createJob("Secret Job", "Secret Co", JobStatus.SAVED);

        // Create User B and generate token.
        User userB = User.builder()
                .email("userB@example.com")
                .password(passwordEncoder.encode("password123"))
                .build();
        userRepository.save(userB);
        String tokenB = "Bearer " + jwtUtil.generateToken(userB.getEmail());

        JobUpdateRequest request = new JobUpdateRequest();
        request.setJobTitle("New Title");
        request.setCompany("New Co");
        request.setStatus(JobStatus.APPLIED);
        request.setAppliedAt(LocalDateTime.now());

        mockMvc.perform(put("/api/jobs/" + jobId)
                        .header("Authorization", tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_anotherUsersJobStatus_shouldReturn404() throws Exception {
        // User A creates a Job
        Long jobId = createJob("Secret Job", "Secret Co", JobStatus.SAVED);

        // Create User B and generate token
        User userB = User.builder()
                .email("userB@example.com")
                .password(passwordEncoder.encode("password123"))
                .build();
        userRepository.save(userB);
        String tokenB = "Bearer " + jwtUtil.generateToken(userB.getEmail());

        JobUpdateStatusRequest request = new JobUpdateStatusRequest();
        request.setStatus(JobStatus.INTERVIEWING);

        mockMvc.perform(patch("/api/jobs/" + jobId + "/status")
                        .header("Authorization", tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_anotherUsersJob_shouldReturn404() throws Exception {
        Long jobId = createJob("Secret Job", "Secret Co", JobStatus.SAVED);

        User userB = User.builder()
                .email("userB@example.com")
                .password(passwordEncoder.encode("password456"))
                .build();
        userRepository.save(userB);
        String tokenB = "Bearer " + jwtUtil.generateToken(userB.getEmail());

        mockMvc.perform(delete("/api/jobs/" + jobId).header("Authorization", tokenB))
                .andExpect(status().isNotFound());
    }

    // === helpers ===
    private Long createJob(String title, String company, JobStatus status) throws Exception {
        JobCreateRequest request = new JobCreateRequest();
        request.setJobTitle(title);
        request.setCompany(company);
        request.setStatus(status);

        String response = mockMvc.perform(post("/api/jobs")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }
}
