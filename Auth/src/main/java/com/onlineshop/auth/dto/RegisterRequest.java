package com.onlineshop.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Username is required and cannot be blank")
    private String username;

    @NotBlank(message = "Password is required and cannot be blank")
    private String password;
}
