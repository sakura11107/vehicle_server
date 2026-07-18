package com.vehicle.server.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 后端对前端公开的稳定错误码及其 HTTP 状态。
 */
public enum ErrorCode {
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND),
    USERNAME_EXISTS(HttpStatus.CONFLICT),
    EMAIL_EXISTS(HttpStatus.CONFLICT),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED),
    USER_DISABLED(HttpStatus.FORBIDDEN),
    FORBIDDEN(HttpStatus.FORBIDDEN),
    VEHICLE_NOT_FOUND(HttpStatus.NOT_FOUND),
    VEHICLE_NOT_AVAILABLE(HttpStatus.CONFLICT),
    VEHICLE_RESERVATION_CONFLICT(HttpStatus.CONFLICT),
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND),
    MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND),
    MESSAGE_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_STATUS_TRANSITION(HttpStatus.CONFLICT),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
