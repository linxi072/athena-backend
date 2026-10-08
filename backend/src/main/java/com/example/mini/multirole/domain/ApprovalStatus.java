package com.example.mini.multirole.domain;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 角色切换审批单状态（对应设计文档 SW-13 审批流）。
 */
public enum ApprovalStatus {

    PENDING("pending", "待审批"),
    APPROVED("approved", "已通过"),
    REJECTED("rejected", "已拒绝"),
    EXPIRED("expired", "已过期");

    private final String code;
    private final String desc;

    ApprovalStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
