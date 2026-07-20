package com.yue.jobcomparer.service;

import com.yue.jobcomparer.AbstractIntegrationTest;
import com.yue.jobcomparer.entity.AuditAction;
import com.yue.jobcomparer.entity.AuditLog;
import com.yue.jobcomparer.entity.User;
import com.yue.jobcomparer.repository.AuditLogRepository;
import com.yue.jobcomparer.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AuditLogRecordingTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "password123";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Long existingUserId;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .email("user@example.com")
                .username("regularuser")
                .password(passwordEncoder.encode(PASSWORD))
                .build();
        userRepository.save(user);
        existingUserId = user.getId();
    }

    @AfterEach
    void tearDown() {
        auditLogRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void login_withValidCredentials_shouldRecordSuccess() throws Exception {
        login("user@example.com", PASSWORD).andExpect(status().isOk());

        AuditLog log = singleLog();
        assertThat(log.getAction()).isEqualTo(AuditAction.LOGIN_SUCCESS);
        assertThat(log.getEmail()).isEqualTo("user@example.com");
        assertThat(log.getUserId()).isEqualTo(existingUserId);
        assertThat(log.getIpAddress()).isEqualTo("127.0.0.1");
    }

    @Test
    void login_withWrongPassword_shouldRecordFailureWithUserId()  throws Exception {
        login("user@example.com", "wrong-password").andExpect(status().isBadRequest());

        AuditLog log = singleLog();
        assertThat(log.getAction()).isEqualTo(AuditAction.LOGIN_FAILURE);
        assertThat(log.getUserId()).isEqualTo(existingUserId);
    }

    @Test
    void login_withUnknownEmail_shouldRecordFailureWithNullUserId() throws Exception {
        login("ghost@example.com", PASSWORD).andExpect(status().isBadRequest());

        AuditLog log = singleLog();
        assertThat(log.getAction()).isEqualTo(AuditAction.LOGIN_FAILURE);
        assertThat(log.getEmail()).isEqualTo("ghost@example.com");
        assertThat(log.getUserId()).isNull();
    }

    @Test
    void register_shouldRecordRegisterAction() throws Exception {
        String body = """
                {
                  "email": "newuser@example.com",
                  "username": "newuser",
                  "password": "password123"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk());

        AuditLog log = singleLog();
        assertThat(log.getAction()).isEqualTo(AuditAction.REGISTER);
        assertThat(log.getEmail()).isEqualTo("newuser@example.com");
        assertThat(log.getUserId()).isNotNull();
    }

    // === helpers ===

    private ResultActions login(String email, String password) throws Exception {
        String body = """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);

        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private AuditLog singleLog() {
        List<AuditLog> logs = auditLogRepository.findAll();
        assertThat(logs).hasSize(1);
        return logs.get(0);
    }
}
