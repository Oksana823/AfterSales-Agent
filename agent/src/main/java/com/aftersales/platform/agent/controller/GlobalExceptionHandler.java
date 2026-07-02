package com.aftersales.platform.agent.controller;

import com.aftersales.platform.agent.service.AiUnavailableException;
import com.aftersales.platform.agent.service.RunExecutionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

/**
 * Agent API 统一异常处理器，将参数、业务、模型和运行异常转换为稳定的错误协议。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("请求参数错误");
        return ResponseEntity.badRequest().body(ApiError.of(400, message));
    }

    /**
     * 任务失败时返回 runId，前端仍可展示本次失败运行及其 Trace。
     */
    @ExceptionHandler(RunExecutionException.class)
    public ResponseEntity<ApiError> runFailure(RunExecutionException exception) {
        Throwable cause = exception.getCause();
        if (cause instanceof AiUnavailableException) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiError.ofRun(503, exception.getMessage(), exception.getRunId()));
        }
        if (cause instanceof IllegalArgumentException || cause instanceof IllegalStateException) {
            return ResponseEntity.badRequest()
                    .body(ApiError.ofRun(400, exception.getMessage(), exception.getRunId()));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.ofRun(500, "系统执行失败", exception.getRunId()));
    }

    @ExceptionHandler(AiUnavailableException.class)
    public ResponseEntity<ApiError> unavailable(AiUnavailableException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiError.of(503, exception.getMessage()));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiError> business(RuntimeException exception) {
        return ResponseEntity.badRequest().body(ApiError.of(400, exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of(500, "系统执行失败"));
    }

    public record ApiError(int code, String message, Long runId, LocalDateTime timestamp) {
        static ApiError of(int code, String message) {
            return new ApiError(code, message, null, LocalDateTime.now());
        }

        static ApiError ofRun(int code, String message, Long runId) {
            return new ApiError(code, message, runId, LocalDateTime.now());
        }
    }
}
