package com.vehicle.server.common.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 通用分页请求参数。
 */
public record PageRequest(
        @Min(1) int page,
        @Min(1) @Max(100) int size) {

    public PageRequest {
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        if (size > 100) size = 100;
    }

    public PageRequest() {
        this(1, 10);
    }
}
