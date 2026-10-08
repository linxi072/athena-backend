package com.example.mini.multirole.dto;

import com.example.mini.multirole.domain.RoleCode;

/**
 * 角色切换结果。
 *
 * <ul>
 *   <li>SWITCHED：已即时换发 Token 完成切换</li>
 *   <li>PENDING：升权触发审批，待管理员通过（token 为 null）</li>
 * </ul>
 */
public class SwitchRoleResult {

    public enum Outcome { SWITCHED, PENDING }

    private Outcome outcome;
    /** 换发后的新 Token（PENDING 时为 null） */
    private String token;
    /** 切换后当前角色 */
    private RoleCode currentRole;
    /** 是否已将目标角色设为跨端默认角色（SW-12） */
    private boolean defaultRoleSynced;
    /** 审批单号（PENDING 时有值） */
    private String approvalId;

    public static SwitchRoleResult switched(String token, RoleCode currentRole, boolean defaultRoleSynced) {
        SwitchRoleResult r = new SwitchRoleResult();
        r.outcome = Outcome.SWITCHED;
        r.token = token;
        r.currentRole = currentRole;
        r.defaultRoleSynced = defaultRoleSynced;
        return r;
    }

    public static SwitchRoleResult pending(String approvalId) {
        SwitchRoleResult r = new SwitchRoleResult();
        r.outcome = Outcome.PENDING;
        r.approvalId = approvalId;
        return r;
    }

    public Outcome getOutcome() { return outcome; }
    public String getToken() { return token; }
    public RoleCode getCurrentRole() { return currentRole; }
    public boolean isDefaultRoleSynced() { return defaultRoleSynced; }
    public String getApprovalId() { return approvalId; }
}
