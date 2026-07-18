package com.yue.jobcomparer.controller;

import com.yue.jobcomparer.dto.AdminUserResponse;
import com.yue.jobcomparer.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    public Page<AdminUserResponse> list(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable){
        return adminService.list(pageable);
    }
}
