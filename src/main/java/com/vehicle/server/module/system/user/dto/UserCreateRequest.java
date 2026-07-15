package com.vehicle.server.module.system.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
        @NotBlank @Size(max = 50) String username,
        @NotBlank @Size(min = 6, max = 100) String password,
        @NotBlank @Email @Size(max = 100) String email,
        @Min(0) @Max(2) Integer role,
        @Min(0) @Max(1) Integer status) {
}
