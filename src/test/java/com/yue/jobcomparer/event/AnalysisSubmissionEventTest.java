package com.yue.jobcomparer.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yue.jobcomparer.AbstractIntegrationTest;
import com.yue.jobcomparer.dto.AnalysisCreateRequest;
import com.yue.jobcomparer.entity.Cv;
import com.yue.jobcomparer.entity.Job;
import com.yue.jobcomparer.entity.JobStatus;
import com.yue.jobcomparer.entity.User;
import com.yue.jobcomparer.repository.AnalysisRepository;
import com.yue.jobcomparer.repository.AuditLogRepository;
import com.yue.jobcomparer.repository.CvRepository;
import com.yue.jobcomparer.repository.JobRepository;
import com.yue.jobcomparer.repository.UserRepository;
import com.yue.jobcomparer.service.AnalysisRunner;
import com.yue.jobcomparer.service.AnalysisService;
import com.yue.jobcomparer.util.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the commit-ordering contract between submission and background
 * execution.
 *
 * <p>Deliberately not annotated with {@code @Transactional}: Spring's test
 * transaction rolls back instead of committing, which would prevent the
 * AFTER_COMMIT listener from ever firing. The test would still pass, but it
 * would be asserting on a path the framework had quietly disabled.
 */
class AnalysisSubmissionEventTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CvRepository cvRepository;
    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private AnalysisRepository analysisRepository;
    @Autowired
    private AuditLogRepository auditLogRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private AnalysisRunner analysisRunner;

    private static final String EMAIL = "async@example.com";

    private String token;
    private Long userId;
    private Long cvId;
    private Long jobId;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .email(EMAIL)
                .username("asyncuser")
                .password(passwordEncoder.encode("password123"))
                .build();
        userRepository.save(user);
        userId = user.getId();
        token = "Bearer " + jwtUtil.generateToken(EMAIL);

        Cv cv = Cv.builder().userId(userId).cvName("CV A").content("CV content").build();
        cvRepository.save(cv);
        cvId = cv.getId();

        Job job = Job.builder()
                .userId(userId)
                .jobTitle("Backend Engineer")
                .company("ACME")
                .jobDescription("Java, Spring Boot")
                .status(JobStatus.SAVED)
                .build();
        jobRepository.save(job);
        jobId = job.getId();
    }

    @AfterEach
    void tearDown() {
        // No test transaction to roll back, so state has to be removed by hand,
        // otherwise it leaks into every test class sharing this context.
        analysisRepository.deleteAll();
        auditLogRepository.deleteAll();
        cvRepository.deleteAll();
        jobRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void submit_shouldTriggerRunner_afterCommit() throws Exception {
        AnalysisCreateRequest request = new AnalysisCreateRequest();
        request.setCvId(cvId);
        request.setJobId(jobId);

        mockMvc.perform(post("/api/analyses")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());

        // The listener hands the task to a pool thread, so the call is not
        // visible immediately. timeout() polls instead of asserting once.
        verify(analysisRunner, timeout(2000)).run(anyLong(), eq(userId));
    }

    @Test
    @WithMockUser(username = EMAIL)
    void submit_shouldNotTriggerRunner_whenTransactionRollsBack() {
        AnalysisCreateRequest request = new AnalysisCreateRequest();
        request.setCvId(cvId);
        request.setJobId(jobId);

        transactionTemplate.execute(status -> {
            analysisService.submit(request);
            status.setRollbackOnly();
            return null;
        });

        // after() waits and then asserts the call never happened, unlike
        // timeout() which returns as soon as it does.
        verify(analysisRunner, after(500).never()).run(anyLong(), anyLong());
        assertThat(analysisRepository.findAll()).isEmpty();
    }
}
