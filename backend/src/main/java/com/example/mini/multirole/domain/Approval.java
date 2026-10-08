package com.example.mini.multirole.domain;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 角色切换审批单（升权触发，SW-13）。
 *
 * <p>升权切换不直接换发 Token，而是先落审批单（PENDING），
 * 由管理员审核通过后才实际执行切换（{@code RoleSwitchService.doSwitch}）。</p>
 */
@Data
public class Approval implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 审批单号 */
    private String id;
    /** 申请人 */
    private Long userId;
    /** 生效端 */
    private TerminalType terminal;
    /** 切换前角色 */
    private RoleCode fromRole;
    /** 目标角色（升权） */
    private RoleCode toRole;
    /** 审批状态 */
    private ApprovalStatus status;
    /** 申请时间 */
    private LocalDateTime createdAt;
    /** 决定时间（通过/拒绝/过期） */
    private LocalDateTime decidedAt;
    /** 拒绝原因 / 备注 */
    private String reason;
    /** 审批人 */
    private Long operatorId;

    /** 是否仍处于待审。 */
    public boolean isPending() {
        return status == ApprovalStatus.PENDING;
    }
}
