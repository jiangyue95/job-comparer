package com.yue.jobcomparer.controller;

import com.yue.jobcomparer.AbstractIntegrationTest;
import com.yue.jobcomparer.entity.Role;
import com.yue.jobcomparer.entity.User;
import com.yue.jobcomparer.repository.UserRepository;
import com.yue.jobcomparer.util.JwtUtil;
import org.springframework.transaction.annotation.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@Transactional
public class AdminControllerTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
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
    }

    // === Security Scenarios ===

    @Test
    void adminUsers_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminUsers_asRegularUser_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                .header("Authorization", userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminUsers_asAdmin_shouldReturn200_withUserList() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void adminUsers_shouldNotExposePasswordHash() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].role").exists())
                .andExpect(jsonPath("$.content[*].password").doesNotExist());
    }
}
