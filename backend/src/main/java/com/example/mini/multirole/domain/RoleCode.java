package com.example.mini.multirole.domain;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 角色编码与权限等级。
 *
 * <p>权限等级严格递增：visitor &lt; member &lt; reviewer &lt; operator &lt; admin。
 * 等级用于“升权触发审批”（SW-13）与“角色回收强退”（SW-8）的判定。</p>
 *
 * <p>对应《小程序多角色切换功能设计》v1.3 的 5 类角色模型与权限等级表。</p>
 */
public enum RoleCode {

    VISITOR("visitor", 0, "游客（受限浏览态）"),
    MEMBER("member", 1, "注册会员"),
    REVIEWER("reviewer", 2, "内容审核员"),
    OPERATOR("operator", 3, "运营人员"),
    ADMIN("admin", 4, "系统管理员");

    private final String code;
    private final int level;
    private final String desc;

    RoleCode(String code, int level, String desc) {
        this.code = code;
        this.level = level;
        this.desc = desc;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public int getLevel() {
        return level;
    }

    public String getDesc() {
        return desc;
    }

    /** 按字符串解析角色编码，非法值抛 {@link IllegalArgumentException}。 */
    public static RoleCode of(String code) {
        for (RoleCode r : values()) {
            if (r.code.equalsIgnoreCase(code)) {
                return r;
            }
        }
        throw new IllegalArgumentException("未知角色编码: " + code);
    }

    /** 是否比 other 权限更高（用于升权判定）。 */
    public boolean isHigherThan(RoleCode other) {
        return this.level > other.level;
    }
}
