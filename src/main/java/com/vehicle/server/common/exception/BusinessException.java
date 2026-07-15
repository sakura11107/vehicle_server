package com.vehicle.server.common.exception;

/**
 * 业务规则不满足时抛出的异常，仅携带可供前端翻译的错误码。
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
