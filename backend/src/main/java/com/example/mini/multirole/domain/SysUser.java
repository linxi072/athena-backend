package com.example.mini.multirole.domain;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 系统用户（多角色切换的承载实体）。
 *
 * <p>字段与《架构设计说明书》v1.4 §5.1 数据实体对齐：
 * phone_bound（强制绑定手机号，受限浏览态前提）、
 * account_status / freeze_reason / close_cooldown_until（账号全生命周期 §18.3）、
 * default_role（跨端默认角色 SW-12）。</p>
 *
 * <p>注意：本模块仅管理“已授予角色集合 + 默认角色”，
 * “当前生效角色”属于会话态，由 {@code SessionService} 按 terminal 维度维护。</p>
 */
@Data
public class SysUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户唯一标识 */
    private Long id;

    /** 登录名 / 手机号（脱敏展示由展示层处理） */
    private String username;

    /** 是否已完成手机号绑定（SRS EV-07 / UW-03：未绑定则受限浏览态） */
    private boolean phoneBound;

    /** 账号生命周期状态 */
    private AccountStatus accountStatus = AccountStatus.NORMAL;

    /** 冻结原因（仅 FROZEN 态有意义） */
    private String freezeReason;

    /** 注销冷静期截止时间（仅 CLOSING 态有意义，EV-10） */
    private LocalDateTime closeCooldownUntil;

    /** 已授予角色集合（授予 ≥2 角色才显示切换入口，设计文档结论） */
    private Set<RoleCode> grantedRoles = new HashSet<>();

    /** 全局默认角色（跨端同步，SW-12） */
    private RoleCode defaultRole;

    /** 判断某角色是否已授予。 */
    public boolean hasRole(RoleCode role) {
        return grantedRoles != null && grantedRoles.contains(role);
    }

    /** 授予角色数量（≥2 才开放切换入口）。 */
    public int grantedRoleCount() {
        return grantedRoles == null ? 0 : grantedRoles.size();
    }
}
