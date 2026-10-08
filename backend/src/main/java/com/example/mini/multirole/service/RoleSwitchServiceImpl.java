package com.example.mini.multirole.service;

import com.example.mini.multirole.common.BizException;
import com.example.mini.multirole.common.ErrorCode;
import com.example.mini.multirole.domain.AccountStatus;
import com.example.mini.multirole.domain.Approval;
import com.example.mini.multirole.domain.RoleCode;
import com.example.mini.multirole.domain.SysUser;
import com.example.mini.multirole.domain.TerminalType;
import com.example.mini.multirole.dto.SwitchRoleRequest;
import com.example.mini.multirole.dto.SwitchRoleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link RoleSwitchService} 实现。
 *
 * <p>规则闭环（设计文档 SW-1~SW-13）：
 * <ul>
 *   <li>SW-1 切换换发该端 Token；SW-4 成功后清空权限/菜单/业务列表缓存防越权残留</li>
 *   <li>SW-7 失败保持原角色、不清除缓存（回滚）</li>
 *   <li>SW-8 当前角色被回收时强制回退默认角色</li>
 *   <li>SW-10 切换频率限制 10 次/分钟</li>
 *   <li>SW-11 审计留痕；SW-12 设为默认角色则跨端同步（defaultRole 全局）</li>
 *   <li>SW-13 升权（目标等级高于当前）触发审批，审批通过后才实际切换</li>
 * </ul>
 * </p>
 */
@Service
public class RoleSwitchServiceImpl implements RoleSwitchService {

    private static final Logger log = LoggerFactory.getLogger(RoleSwitchServiceImpl.class);

    /** 升权频率阈值（SW-10） */
    private static final int RATE_LIMIT = 10;
    private static final long RATE_WINDOW_SECONDS = 60;

    private final UserService userService;
    private final SessionService sessionService;
    private final CacheService cacheService;
    private final ApprovalService approvalService;
    private final AuditService auditService;

    /** 切换频率计数：key = userId@terminal -> 最近切换时间戳（SW-10） */
    private final Map<String, Deque<Instant>> rateBuckets = new ConcurrentHashMap<>();

    public RoleSwitchServiceImpl(UserService userService, SessionService sessionService,
                                CacheService cacheService, ApprovalService approvalService,
                                AuditService auditService) {
        this.userService = userService;
        this.sessionService = sessionService;
        this.cacheService = cacheService;
        this.approvalService = approvalService;
        this.auditService = auditService;
    }

    @Override
    public SwitchRoleResult switchRole(SwitchRoleRequest req) {
        SysUser user = userService.loadUser(req.getUserId());
        TerminalType terminal = TerminalType.of(req.getTerminal());
        RoleCode toRole = RoleCode.of(req.getTargetRole());

        // 前置校验：绑定 / 账号状态 / 限流 / 授予 / 登录态
        if (!user.isPhoneBound()) {
            throw new BizException(ErrorCode.PHONE_NOT_BOUND);
        }
        if (!user.getAccountStatus().accessible()) {
            throw new BizException(user.getAccountStatus() == AccountStatus.FROZEN
                    ? ErrorCode.ACCOUNT_FROZEN : ErrorCode.ACCOUNT_CLOSED);
        }
        enforceRateLimit(user.getId(), terminal); // SW-10
        if (user.getGrantedRoleCount() < 2) {
            throw new BizException(ErrorCode.ROLE_NOT_GRANTED, "未授予可切换角色（需 ≥2 个）");
        }
        if (!user.hasRole(toRole)) {
            throw new BizException(ErrorCode.ROLE_NOT_GRANTED); // SW-? 目标角色未授予（1001）
        }

        RoleCode currentRole = sessionService.getCurrentRole(user.getId(), terminal);
        if (currentRole == null) {
            throw new BizException(ErrorCode.SESSION_INVALID, "当前端未登录，请先登录");
        }
        // SW-8：当前角色被回收，强制回退默认角色
        if (!user.hasRole(currentRole)) {
            log.warn("当前角色 {} 已被回收，强制回退默认角色 {}", currentRole, user.getDefaultRole());
            sessionService.switchRoleInSession(user.getId(), terminal, user.getDefaultRole());
            currentRole = user.getDefaultRole();
        }

        // SW-13：升权触发审批，不直接切换
        if (toRole.isHigherThan(currentRole)) {
            Approval approval = approvalService.create(user.getId(), terminal, currentRole, toRole);
            auditService.recordSwitch(user.getId(), terminal, currentRole, toRole,
                    true, "升权触发审批:" + approval.getId());
            return SwitchRoleResult.pending(approval.getId());
        }

        // 非升权：即时切换
        return doSwitch(user, terminal, currentRole, toRole, req.isApplyAsDefault());
    }

    @Override
    public SwitchRoleResult applyApprovedSwitch(String approvalId) {
        Approval approval = approvalService.query(approvalId);
        if (!approval.isPending()) {
            if (approval.getStatus() == com.example.mini.multirole.domain.ApprovalStatus.REJECTED) {
                throw new BizException(ErrorCode.ROLE_SWITCH_REJECTED);
            }
            if (approval.getStatus() == com.example.mini.multirole.domain.ApprovalStatus.EXPIRED) {
                throw new BizException(ErrorCode.APPROVAL_EXPIRED);
            }
        }
        SysUser user = userService.loadUser(approval.getUserId());
        return doSwitch(user, approval.getTerminal(), approval.getFromRole(), approval.getToRole(), false);
    }

    /**
     * 实际执行切换（被即时切换与审批通过后复用）。
     */
    private SwitchRoleResult doSwitch(SysUser user, TerminalType terminal,
                                     RoleCode fromRole, RoleCode toRole, boolean applyAsDefault) {
        // SW-6 草稿：记录切换前的角色用于失败回滚
        RoleCode previousRole = fromRole;
        try {
            // SW-1 换发该端 Token
            String newToken = sessionService.switchRoleInSession(user.getId(), terminal, toRole);
            // SW-4 清空角色相关缓存，防止越权数据残留
            cacheService.clearRoleCache(user.getId(), terminal);
            // SW-12 跨端默认角色同步（全局 defaultRole）
            boolean synced = false;
            if (applyAsDefault) {
                user.setDefaultRole(toRole);
                synced = true;
            }
            // SW-11 审计
            auditService.recordSwitch(user.getId(), terminal, previousRole, toRole, true, "SWITCHED");
            return SwitchRoleResult.switched(newToken, toRole, synced);
        } catch (Exception e) {
            // SW-7 失败回滚：恢复原角色，且不清除缓存
            try {
                sessionService.switchRoleInSession(user.getId(), terminal, previousRole);
            } catch (Exception ignore) {
                log.error("回滚失败 userId={} terminal={}", user.getId(), terminal, ignore);
            }
            auditService.recordSwitch(user.getId(), terminal, previousRole, toRole, false,
                    "ROLLBACK:" + e.getMessage());
            log.error("角色切换失败并回滚 userId={} terminal={} {}->{}",
                    user.getId(), terminal, previousRole, toRole, e);
            throw new BizException(ErrorCode.SYSTEM_ERROR, "角色切换失败已回滚：" + e.getMessage());
        }
    }

    /** SW-10 频率限制：滑动窗口（60s 内 ≤10 次）。 */
    private void enforceRateLimit(Long userId, TerminalType terminal) {
        String key = userId + "@" + terminal.getCode();
        Deque<Instant> bucket = rateBuckets.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (bucket) {
            Instant now = Instant.now();
            Instant cutoff = now.minusSeconds(RATE_WINDOW_SECONDS);
            while (!bucket.isEmpty() && bucket.peekFirst().isBefore(cutoff)) {
                bucket.pollFirst();
            }
            if (bucket.size() >= RATE_LIMIT) {
                throw new BizException(ErrorCode.ROLE_SWITCH_RATE_LIMIT);
            }
            bucket.addLast(now);
        }
    }
}
