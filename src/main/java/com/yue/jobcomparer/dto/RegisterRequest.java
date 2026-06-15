package com.yue.jobcomparer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min=3, max=50, message = "Username must be between {min} and {max} characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email()
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least {min} characters")
    private String password;
}
