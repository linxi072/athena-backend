package com.example.mini.multirole.service;

import com.example.mini.multirole.common.BizException;
import com.example.mini.multirole.common.ErrorCode;
import com.example.mini.multirole.domain.Approval;
import com.example.mini.multirole.domain.ApprovalStatus;
import com.example.mini.multirole.domain.RoleCode;
import com.example.mini.multirole.domain.TerminalType;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * {@link ApprovalService} 内存实现（演示 / 单测）。
 * 生产替换为 MyBatis-Plus 持久化 + 消息通知。
 */
@Service
public class InMemoryApprovalService implements ApprovalService {

    private final Map<String, Approval> store = new ConcurrentHashMap<>();
    private final AtomicLong seq = new AtomicLong(1);

    @Override
    public Approval create(Long userId, TerminalType terminal, RoleCode from, RoleCode to) {
        Approval a = new Approval();
        a.setId("AP" + System.nanoTime() + "-" + seq.getAndIncrement());
        a.setUserId(userId);
        a.setTerminal(terminal);
        a.setFromRole(from);
        a.setToRole(to);
        a.setStatus(ApprovalStatus.PENDING);
        a.setCreatedAt(LocalDateTime.now());
        store.put(a.getId(), a);
        return a;
    }

    @Override
    public Approval approve(String approvalId, Long operatorId) {
        Approval a = required(approvalId);
        ensurePending(a);
        a.setStatus(ApprovalStatus.APPROVED);
        a.setDecidedAt(LocalDateTime.now());
        a.setOperatorId(operatorId);
        return a;
    }

    @Override
    public Approval reject(String approvalId, Long operatorId, String reason) {
        Approval a = required(approvalId);
        ensurePending(a);
        a.setStatus(ApprovalStatus.REJECTED);
        a.setDecidedAt(LocalDateTime.now());
        a.setOperatorId(operatorId);
        a.setReason(reason);
        return a;
    }

    @Override
    public Approval query(String approvalId) {
        return required(approvalId);
    }

    @Override
    public List<Approval> listPending() {
        List<Approval> list = new ArrayList<>();
        for (Approval a : store.values()) {
            if (a.isPending()) {
                list.add(a);
            }
        }
        return list;
    }

    private Approval required(String approvalId) {
        Approval a = store.get(approvalId);
        if (a == null) {
            throw new BizException(ErrorCode.ROLE_SWITCH_REJECTED, "审批单不存在");
        }
        return a;
    }

    private void ensurePending(Approval a) {
        if (!a.isPending()) {
            throw new BizException(ErrorCode.APPROVAL_EXPIRED, "审批单已处理，状态=" + a.getStatus().getCode());
        }
    }
}
