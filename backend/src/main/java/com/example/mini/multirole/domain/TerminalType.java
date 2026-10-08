package com.example.mini.multirole.domain;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 接入端类型。
 *
 * <p>多端会话按 terminal 维度隔离（SRS §3.2 / 设计文档 SW-1~SW-11）：
 * 切换角色仅影响“当前端”，但默认角色（defaultRole）可跨端同步（SW-12）。</p>
 */
public enum TerminalType {

    MINI("mini", "微信小程序"),
    PC("pc", "PC 管理后台"),
    H5("h5", "H5 / 公众号"),
    APP("app", "移动 App");

    private final String code;
    private final String desc;

    TerminalType(String code, String desc) {
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

    public static TerminalType of(String code) {
        for (TerminalType t : values()) {
            if (t.code.equalsIgnoreCase(code)) {
                return t;
            }
        }
        throw new IllegalArgumentException("未知端类型: " + code);
    }
}
