package com.vehicle.server.module.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MessageCreateRequest(
        @NotNull Long receiverId,
        @NotBlank String content) {
}
