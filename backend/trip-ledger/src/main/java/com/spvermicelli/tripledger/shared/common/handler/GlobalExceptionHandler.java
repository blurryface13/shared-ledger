package com.spvermicelli.tripledger.shared.common.handler;

import com.spvermicelli.tripledger.shared.common.enums.ErrorCode;
import com.spvermicelli.tripledger.shared.common.exception.BusinessException;
import com.spvermicelli.tripledger.shared.common.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException exception) {
        return ApiResponse.failure(exception.getErrorCode(), exception.getMessage());
    }

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        BindException.class,
        ConstraintViolationException.class
    })
    public ApiResponse<Void> handleValidationException(Exception exception) {
        return ApiResponse.failure(ErrorCode.INVALID_PARAM, resolveValidationMessage(exception));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ApiResponse<Void> handleDuplicateKeyException(DuplicateKeyException exception) {
        log.warn("Duplicate key conflict", exception);
        String message = "数据已存在或唯一键冲突";
        String rawMessage = exception.getMessage();
        if (rawMessage != null && rawMessage.contains("uk_book_owner_name")) {
            message = "当前账号下已存在同名账本";
        }
        return ApiResponse.failure(ErrorCode.CONFLICT, message);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ApiResponse<Void> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException exception) {
        log.warn("Upload file too large", exception);
        return ApiResponse.failure(ErrorCode.INVALID_PARAM, "上传文件大小不能超过 10MB");
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleUnexpectedException(Exception exception) {
        log.error("Unexpected system exception", exception);
        return ApiResponse.failure(ErrorCode.SYSTEM_ERROR, ErrorCode.SYSTEM_ERROR.getDefaultMessage());
    }

    private String resolveValidationMessage(Exception exception) {
        if (exception instanceof MethodArgumentNotValidException methodArgumentNotValidException) {
            String message = firstBindingMessage(methodArgumentNotValidException.getBindingResult().getFieldErrors(),
                methodArgumentNotValidException.getBindingResult().getGlobalErrors());
            return normalizeMessage(message);
        }
        if (exception instanceof BindException bindException) {
            String message = firstBindingMessage(bindException.getBindingResult().getFieldErrors(),
                bindException.getBindingResult().getGlobalErrors());
            return normalizeMessage(message);
        }
        if (exception instanceof ConstraintViolationException constraintViolationException) {
            String message = constraintViolationException.getConstraintViolations().stream()
                .map(violation -> violation == null ? null : violation.getMessage())
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
            return normalizeMessage(message);
        }
        return "请求参数不合法";
    }

    private String firstBindingMessage(java.util.List<FieldError> fieldErrors, java.util.List<ObjectError> objectErrors) {
        if (fieldErrors != null) {
            String fieldMessage = fieldErrors.stream()
                .map(FieldError::getDefaultMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
            if (fieldMessage != null) {
                return fieldMessage;
            }
        }
        if (objectErrors != null) {
            return objectErrors.stream()
                .map(ObjectError::getDefaultMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        }
        return null;
    }

    private String normalizeMessage(String rawMessage) {
        if (rawMessage == null) {
            return "请求参数不合法";
        }
        String message = rawMessage.trim();
        return message.isEmpty() ? "请求参数不合法" : message;
    }
}
