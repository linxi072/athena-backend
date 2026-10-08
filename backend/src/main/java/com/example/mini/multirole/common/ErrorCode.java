package com.example.mini.multirole.common;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 业务错误码（对应《小程序多角色切换功能设计》v1.1 §4 错误码表）。
 *
 * <p>约定：0 表示成功；1xxx 为角色切换领域错误；2xxx 为账号/绑定相关；4xxx 为参数/通用。</p>
 */
public enum ErrorCode {

    SUCCESS(0, "成功"),

    // —— 角色切换领域（设计文档 §4 错误码）——
    ROLE_NOT_GRANTED(1001, "目标角色未授予当前用户"),
    ROLE_REVOKED(1002, "当前角色已被回收，请重新选择"),
    ROLE_SWITCH_RATE_LIMIT(1003, "角色切换过于频繁，请稍后再试"),
    SESSION_INVALID(1004, "会话失效，请重新登录"),
    ROLE_SWITCH_PENDING(1005, "角色切换已提交审批，待管理员通过"),
    ROLE_SWITCH_REJECTED(1006, "角色切换审批被拒绝"),
    APPROVAL_EXPIRED(1007, "角色切换审批已过期"),

    // —— 账号 / 绑定 ——
    PHONE_NOT_BOUND(2001, "未绑定手机号，当前为受限浏览态"),
    ACCOUNT_FROZEN(2002, "账号已冻结"),
    ACCOUNT_CLOSED(2003, "账号已注销或注销冷静期中"),

    // —— 参数 / 通用 ——
    INVALID_PARAM(4000, "请求参数非法"),
    SYSTEM_ERROR(5000, "系统内部错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @JsonValue
    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
