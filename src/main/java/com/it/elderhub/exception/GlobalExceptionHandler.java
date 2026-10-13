package com.it.elderhub.exception;

import com.it.elderhub.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<?> handleIllegalArgument(IllegalArgumentException e) {
        return Result.error(400, e.getMessage());
    }

    /**
     * 业务异常（如床位状态校验、重复审批等）：返回 400 + 具体提示，不暴露 500
     */
    @ExceptionHandler(RuntimeException.class)
    public Result<?> handleRuntimeException(RuntimeException e) {
        log.warn("Business exception: {}", e.getMessage());
        return Result.error(400, e.getMessage() != null ? e.getMessage() : "操作失败");
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        // 打印完整堆栈到日志，便于排查；对外仍返回通用消息
        log.error("Unhandled exception", e);
        return Result.error("Internal server error");
    }
}