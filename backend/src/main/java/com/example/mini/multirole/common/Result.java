package com.example.mini.multirole.common;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 统一响应包装。所有 Controller 返回均经此结构，前端按 {@link #code} 判定业务结果。
 *
 * @param <T> 业务数据载荷
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 业务码，0 成功，其余见 {@link ErrorCode} */
    private int code;
    /** 提示信息 */
    private String message;
    /** 数据载荷 */
    private T data;
    /** 服务端时间戳 */
    private LocalDateTime timestamp;

    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> error(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> Result<T> error(ErrorCode errorCode, String overrideMessage) {
        return new Result<>(errorCode.getCode(), overrideMessage, null);
    }

    public static <T> Result<T> error(ErrorCode errorCode, String overrideMessage, T data) {
        return new Result<>(errorCode.getCode(), overrideMessage, data);
    }
}
