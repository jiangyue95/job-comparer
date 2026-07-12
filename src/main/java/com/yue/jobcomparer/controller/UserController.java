package com.yue.jobcomparer.controller;

import com.yue.jobcomparer.dto.UserResponse;
import com.yue.jobcomparer.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        UserResponse user = userService.getByEmail(email);
        return ResponseEntity.ok(user);
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadAvatar(
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername();
        userService.updateAvatar(email, file);
        return ResponseEntity.noContent().build();
    }
}
