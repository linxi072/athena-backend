package com.example.mini.multirole.service;

import com.example.mini.multirole.domain.RoleCode;
import com.example.mini.multirole.domain.TerminalType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * {@link AuditService} 内存实现（演示 / 单测）。
 * 生产替换为 SRS §19.4 {@code sys_audit_log} 结构化落库 + 脱敏展示（ST-05）。
 */
@Service
public class InMemoryAuditService implements AuditService {

    private static final Logger log = LoggerFactory.getLogger(InMemoryAuditService.class);

    /** 审计事件（线程安全列表，便于单测断言） */
    public final List<AuditEvent> events = new CopyOnWriteArrayList<>();
    /** 最近一次结果，便于单测断言 */
    public final Map<String, Object> lastOutcome = new ConcurrentHashMap<>();

    @Override
    public void recordSwitch(Long userId, TerminalType terminal, RoleCode from, RoleCode to,
                            boolean success, String message) {
        AuditEvent e = new AuditEvent("ROLE_SWITCH", userId, terminal,
                from == null ? null : from.getCode(), to == null ? null : to.getCode(),
                success, message, LocalDateTime.now());
        events.add(e);
        log.info("AUDIT {} userId={} terminal={} {}->{} success={} msg={}",
                e.action, userId, terminal, e.fromRole, e.toRole, success, message);
    }

    @Override
    public void recordApproval(String approvalId, Long operatorId, String action, boolean success) {
        AuditEvent e = new AuditEvent("ROLE_APPROVAL:" + action, operatorId, null,
                null, null, success, "approvalId=" + approvalId, LocalDateTime.now());
        events.add(e);
        log.info("AUDIT {} operatorId={} success={} approvalId={}", e.action, operatorId, success, approvalId);
    }

    /** 审计事件载体。 */
    public static class AuditEvent {
        public final String action;
        public final Long userId;
        public final TerminalType terminal;
        public final String fromRole;
        public final String toRole;
        public final boolean success;
        public final String message;
        public final LocalDateTime at;

        public AuditEvent(String action, Long userId, TerminalType terminal,
                         String fromRole, String toRole, boolean success, String message, LocalDateTime at) {
            this.action = action;
            this.userId = userId;
            this.terminal = terminal;
            this.fromRole = fromRole;
            this.toRole = toRole;
            this.success = success;
            this.message = message;
            this.at = at;
        }
    }
}
