package com.yue.jobcomparer.service;

import com.yue.jobcomparer.dto.AdminUserResponse;
import com.yue.jobcomparer.entity.User;
import com.yue.jobcomparer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;

    public Page<AdminUserResponse> list(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(this::toResponse);
    }

    private AdminUserResponse toResponse(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
