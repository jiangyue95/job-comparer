package com.yue.jobcomparer.controller;

import com.yue.jobcomparer.AbstractIntegrationTest;
import com.yue.jobcomparer.entity.AuditAction;
import com.yue.jobcomparer.entity.AuditLog;
import com.yue.jobcomparer.entity.Role;
import com.yue.jobcomparer.entity.User;
import com.yue.jobcomparer.repository.AuditLogRepository;
import com.yue.jobcomparer.repository.UserRepository;
import com.yue.jobcomparer.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
public class AuditLogControllerTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        User regularUser = User.builder()
                .email("user@example.com")
                .username("regularuser")
                .password(passwordEncoder.encode("password123"))
                .build();
        userRepository.save(regularUser);
        userToken = "Bearer " + jwtUtil.generateToken(regularUser.getEmail());

        User adminUser = User.builder()
                .email("admin@example.com")
                .username("adminuser")
                .password(passwordEncoder.encode("password123"))
                .role(Role.ADMIN)
                .build();
        userRepository.save(adminUser);
        adminToken = "Bearer " + jwtUtil.generateToken(adminUser.getEmail());

        auditLogRepository.save(AuditLog.builder()
                .action(AuditAction.LOGIN_SUCCESS)
                .email("user@example.com")
                .userId(regularUser.getId())
                .ipAddress("127.0.0.1")
                .build());

        auditLogRepository.save(AuditLog.builder()
                .action(AuditAction.LOGIN_FAILURE)
                .email("user@example.com")
                .userId(regularUser.getId())
                .ipAddress("127.0.0.1")
                .build());

        auditLogRepository.save(AuditLog.builder()
                .action(AuditAction.LOGIN_FAILURE)
                .email("ghost@example.com")
                .ipAddress("10.0.0.1")
                .build());
    }

    // === Security Scenarios ===

    @Test
    void auditLogs_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/admin/audit-logs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void auditLogs_asRegularUser_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/admin/audit-logs")
                        .header("Authorization", userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void auditLogs_asAdmin_shouldReturnAllEntries() throws Exception {
        mockMvc.perform(get("/api/admin/audit-logs")
                .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    // === Filtering ===

    @Test
    void auditLogs_filteredByAction_shouldOnlyReturnMatching() throws Exception {
        mockMvc.perform(get("/api/admin/audit-logs?action=LOGIN_FAILURE")
                .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void auditLogs_filteredByEmail_shouldOnlyReturnMatching() throws Exception {
        mockMvc.perform(get("/api/admin/audit-logs?email=ghost@example.com")
                .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].action").value("LOGIN_FAILURE"))
                .andExpect(jsonPath("$.content[0].userId").value(nullValue()));
    }

    @Test
    void auditLogs_filteredByActionAndEmail_shouldCombineFilters() throws Exception {
        mockMvc.perform(get("/api/admin/audit-logs?action=LOGIN_FAILURE&email=user@example.com")
                .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void auditLogs_withInvalidAction_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/admin/audit-logs?action=BANANA")
                .header("Authorization", adminToken))
                .andExpect(status().isBadRequest());
    }
}
