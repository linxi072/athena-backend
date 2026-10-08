package com.example.mini.multirole.domain;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 账号生命周期状态（对应 SRS §18.3 / ADD §3.5 账号全生命周期）。
 *
 * <ul>
 *   <li>NORMAL：正常</li>
 *   <li>FROZEN：已冻结（即时废止全部会话 Token，ST-03）</li>
 *   <li>CLOSING：注销冷静期中（EV-10，冷静期内可撤销）</li>
 *   <li>CLOSED：已注销（UW-06 阻断前提下生效）</li>
 * </ul>
 */
public enum AccountStatus {

    NORMAL("normal"),
    FROZEN("frozen"),
    CLOSING("closing"),
    CLOSED("closed");

    private final String code;

    AccountStatus(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public static AccountStatus of(String code) {
        for (AccountStatus s : values()) {
            if (s.code.equalsIgnoreCase(code)) {
                return s;
            }
        }
        throw new IllegalArgumentException("未知账号状态: " + code);
    }

    /** 是否允许登录与业务访问。冻结/注销/冷静期均阻断。 */
    public boolean accessible() {
        return this == NORMAL;
    }
}
