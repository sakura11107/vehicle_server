package com.vehicle.server.common.exception;

import com.vehicle.server.common.api.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * 将业务异常和参数校验异常转换为前端可 i18n 的统一响应。
 */
@Slf4j
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

    /**
     * 方法级安全（@PreAuthorize）拒绝时抛出的 AccessDeniedException（含其子类
     * AuthorizationDeniedException）在 DispatcherServlet 内抛出，过滤器链无法捕获，
     * 需在此统一转为 403。
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.failure(ErrorCode.FORBIDDEN));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
        log.error("未处理的异常", exception);
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
