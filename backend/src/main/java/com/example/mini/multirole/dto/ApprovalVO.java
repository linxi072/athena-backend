package com.example.mini.multirole.dto;

import com.example.mini.multirole.domain.Approval;
import com.example.mini.multirole.domain.ApprovalStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审批单视图（审批中心 / 申请人查询）。
 */
@Data
public class ApprovalVO {

    private String id;
    private String fromRole;
    private String toRole;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime decidedAt;
    private String reason;

    public static ApprovalVO from(Approval a) {
        ApprovalVO v = new ApprovalVO();
        v.id = a.getId();
        v.fromRole = a.getFromRole() == null ? null : a.getFromRole().getCode();
        v.toRole = a.getToRole() == null ? null : a.getToRole().getCode();
        v.status = a.getStatus() == null ? null : a.getStatus().getCode();
        v.createdAt = a.getCreatedAt();
        v.decidedAt = a.getDecidedAt();
        v.reason = a.getReason();
        return v;
    }
}
