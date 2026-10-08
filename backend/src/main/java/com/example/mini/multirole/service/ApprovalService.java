package com.example.mini.multirole.service;

import com.example.mini.multirole.domain.Approval;
import com.example.mini.multirole.domain.RoleCode;
import com.example.mini.multirole.domain.TerminalType;

import java.util.List;

/**
 * 角色切换审批服务（SW-13）。
 */
public interface ApprovalService {

    /** 创建升权切换审批单。 */
    Approval create(Long userId, TerminalType terminal, RoleCode from, RoleCode to);

    /** 管理员通过审批（通过后立即触发实际切换，由调用方执行）。 */
    Approval approve(String approvalId, Long operatorId);

    /** 管理员拒绝审批。 */
    Approval reject(String approvalId, Long operatorId, String reason);

    /** 申请人查询审批单。 */
    Approval query(String approvalId);

    /** 管理员视角：列出待审审批单。 */
    List<Approval> listPending();
}
