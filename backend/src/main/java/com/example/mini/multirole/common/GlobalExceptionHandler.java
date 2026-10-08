package com.example.mini.multirole.common;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器：将 {@link BizException} 与其它异常统一转换为 {@link Result} 结构，
 * 保证前端始终收到一致的错误体（设计文档 §4 错误码契约）。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 业务异常：返回对应业务码，HTTP 状态按语义选择（默认 200，便于前端统一解析）。 */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handleBiz(BizException ex, HttpServletRequest request) {
        log.warn("业务异常 [{}] {} => {}", ex.getErrorCode().getCode(), ex.getMessage(), request.getRequestURI());
        return ResponseEntity.ok(Result.error(ex.getErrorCode(), ex.getMessage()));
    }

    /** 参数校验异常（@Valid）：聚合字段错误为可读信息。 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValid(MethodArgumentNotValidException ex) {
        StringBuilder sb = new StringBuilder();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            sb.append(fe.getField()).append(":").append(fe.getDefaultMessage()).append("; ");
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.error(ErrorCode.INVALID_PARAM, sb.toString()));
    }

    /** 非法参数（如 RoleCode.of 失败）。 */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<Void>> handleIllegal(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.error(ErrorCode.INVALID_PARAM, ex.getMessage()));
    }

    /** 兜底系统异常。 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleOther(Exception ex, HttpServletRequest request) {
        log.error("系统异常 {}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(ErrorCode.SYSTEM_ERROR));
    }
}
