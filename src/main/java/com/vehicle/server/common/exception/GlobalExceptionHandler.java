package com.vehicle.server.common.exception;

import com.vehicle.server.common.api.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * 将业务异常和参数校验异常转换为前端可 i18n 的统一响应。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity.status(errorCode.status()).body(ApiResponse.failure(errorCode));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException exception) {
        List<com.vehicle.server.common.api.FieldError> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new com.vehicle.server.common.api.FieldError(error.getField(), toValidationCode(error)))
                .toList();
        return ResponseEntity.badRequest().body(ApiResponse.validationFailure(errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.failure(ErrorCode.INTERNAL_ERROR));
    }

    private String toValidationCode(org.springframework.validation.FieldError error) {
        return switch (error.getCode()) {
            case "NotBlank", "NotNull" -> "REQUIRED";
            case "Email" -> "INVALID_EMAIL";
            case "Size" -> "INVALID_LENGTH";
            case "Min", "Max" -> "OUT_OF_RANGE";
            default -> "INVALID_VALUE";
        };
    }
}
