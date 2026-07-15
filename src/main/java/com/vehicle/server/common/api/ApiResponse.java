package com.vehicle.server.common.api;

import com.vehicle.server.common.exception.ErrorCode;
import java.util.List;

/**
 * 后端统一响应结构。
 *
 * <p>code 是供前端 i18n 映射的稳定业务码；成功时返回 data，参数错误时返回 errors。</p>
 */
public record ApiResponse<T>(String code, T data, List<FieldError> errors) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("SUCCESS", data, null);
    }

    public static ApiResponse<Void> failure(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.name(), null, null);
    }

    public static ApiResponse<Void> validationFailure(List<FieldError> errors) {
        return new ApiResponse<>(ErrorCode.VALIDATION_FAILED.name(), null, errors);
    }
}
