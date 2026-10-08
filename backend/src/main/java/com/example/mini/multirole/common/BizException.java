package com.example.mini.multirole.common;

import lombok.Getter;

/**
 * 业务异常：携带 {@link ErrorCode}，由 {@code GlobalExceptionHandler} 统一转换为 {@link Result}。
 *
 * <p>与“参数校验异常”“系统异常”区分：本异常表达可预期的业务拒绝（如角色未授予、限流）。</p>
 */
@Getter
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BizException(ErrorCode errorCode, String overrideMessage) {
        super(overrideMessage);
        this.errorCode = errorCode;
    }
}
