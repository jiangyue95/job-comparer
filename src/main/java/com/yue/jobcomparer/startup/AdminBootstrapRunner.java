package com.yue.jobcomparer.startup;

import com.yue.jobcomparer.entity.Role;
import com.yue.jobcomparer.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrapRunner implements ApplicationRunner {

    private final UserRepository userRepository;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        userRepository.findByEmail(adminEmail).ifPresentOrElse(
                user -> {
                    if (user.getRole() == Role.ADMIN) {
                        return;
                    }
                    user.setRole(Role.ADMIN);
                    log.info("Bootstrapped admin: {}", adminEmail);

                },
                () -> log.warn("Admin bootstrap skipped: no user with email {}", adminEmail)
        );
    }
}
