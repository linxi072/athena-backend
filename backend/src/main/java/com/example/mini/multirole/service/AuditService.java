package com.example.mini.multirole.service;

import com.example.mini.multirole.domain.RoleCode;
import com.example.mini.multirole.domain.TerminalType;

/**
 * 审计留痕服务（SW-11：切换审计 + 审批）。生产替换为 SRS §19.4 的结构化审计落库。
 */
public interface AuditService {

    void recordSwitch(Long userId, TerminalType terminal, RoleCode from, RoleCode to,
                      boolean success, String message);

    void recordApproval(String approvalId, Long operatorId, String action, boolean success);
}
