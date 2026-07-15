package com.vehicle.server.common.dto;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * 通用分页响应结构。
 */
public record PageResponse<T>(
        List<T> records,
        long total,
        int page,
        int size,
        int totalPages) {

    public static <T> PageResponse<T> of(IPage<?> page, List<T> records) {
        return new PageResponse<>(
                records,
                page.getTotal(),
                (int) page.getCurrent(),
                (int) page.getSize(),
                (int) page.getPages()
        );
    }
}
