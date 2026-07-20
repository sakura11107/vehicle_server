package com.vehicle.server.module.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MessageCreateRequest(
        @NotNull Long receiverId,
        @NotBlank @Size(max = 1000) String content) {
}
