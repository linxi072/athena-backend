package com.example.mini.multirole.service;

import com.example.mini.multirole.dto.SwitchRoleRequest;
import com.example.mini.multirole.dto.SwitchRoleResult;

/**
 * 角色切换核心服务（对应《小程序多角色切换功能设计》v1.3 全部规则）。
 */
public interface RoleSwitchService {

    /**
     * 执行一次切换请求。
     *
     * @return 即时切换结果（SWITCHED）或升权审批待审（PENDING）
     */
    SwitchRoleResult switchRole(SwitchRoleRequest req);

    /**
     * 管理员审批通过后，按审批单实际执行切换（SW-13 升权落地）。
     */
    SwitchRoleResult applyApprovedSwitch(String approvalId);
}
