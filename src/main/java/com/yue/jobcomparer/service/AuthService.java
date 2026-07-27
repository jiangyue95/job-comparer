package com.yue.jobcomparer.service;

import com.yue.jobcomparer.dto.AuthResponse;
import com.yue.jobcomparer.dto.LoginRequest;
import com.yue.jobcomparer.dto.RegisterRequest;
import com.yue.jobcomparer.entity.AuditAction;
import com.yue.jobcomparer.entity.User;
import com.yue.jobcomparer.repository.UserRepository;
import com.yue.jobcomparer.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final AuditLogService auditLogService;

    @Transactional
    public AuthResponse register(RegisterRequest request, String ipAddress) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already registered: " + request.getUsername());
        }

        User user = User.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail());
        auditLogService.recordAuthEvent(AuditAction.REGISTER, user.getEmail(), user.getId(), ipAddress);
        return new AuthResponse(token);
    }

    public AuthResponse login(LoginRequest request, String ipAddress) {

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            auditLogService.recordAuthEvent(
                    AuditAction.LOGIN_FAILURE,
                    request.getEmail(),
                    findUserId(request.getEmail()),
                    ipAddress);
            throw new IllegalArgumentException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(request.getEmail());
        auditLogService.recordAuthEvent(
                AuditAction.LOGIN_SUCCESS,
                request.getEmail(),
                findUserId(request.getEmail()),
                ipAddress);
        return new AuthResponse(token);
    }

    private Long findUserId(String email) {
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElse(null);
    }
}
